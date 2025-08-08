package ch.ethz.sis.afsclient.client;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import ch.ethz.sis.afsapi.api.ClientAPI.FileCollisionListener;
import ch.ethz.sis.afsapi.api.ClientAPI.TransferMonitorListener;
import ch.ethz.sis.afsclient.client.AfsClientDownloadHelper.ChunkIterable;

public class AfsClientUploadHelper
{
    private static final int MAX_UPLOAD_THREADS = 4;

    public static boolean upload(
            AfsClient afsClient,
            @NonNull Path sourcePath, @NonNull String destinationOwner, @NonNull Path destinationPath,
            FileCollisionListener fileCollisionListener,
            @NonNull TransferMonitorListener transferMonitorListener) throws Exception {
        UploadCurrentsAndTotals currentsAndTotals = new UploadCurrentsAndTotals();

        //Preliminary argument validation
        if (!destinationPath.isAbsolute()) {
            throw new IllegalArgumentException("destinationPath must be absolute");
        }
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("sourcePath must exist");
        }
        File destinationInfo = getServerFilePresence(afsClient, destinationOwner, destinationPath.toString()).orElseThrow(()->new IllegalArgumentException("destinationPath not found"));

        // Preliminary local-tree scan to compute total size
        doPreliminaryScanToInitializeUploadMonitor(afsClient, transferMonitorListener, sourcePath);

        // Initializer
        PathIterator pathIterator = new PathIterator(sourcePath);
        Iterator<Path> iterator = pathIterator.iterator();

        // Producer
        List<Chunk> chunkBatchToWriteInOneCall = new ArrayList<>();
        int currentSize = 0;

        Semaphore maxThreadLimitSemaphore = new Semaphore(MAX_UPLOAD_THREADS);
        List<ReadAndUploadRunnable> asyncTransferRunnables = Collections.synchronizedList(new LinkedList<>());
        try {
            while (iterator.hasNext()) {
                Path nextFile = iterator.next();
                String absoluteServerPath = computeAbsoluteServerPath(sourcePath, destinationPath, nextFile, destinationInfo);

                if( !Files.isDirectory(nextFile) ) {
                    long nextFileSize = Files.size(nextFile);
                    Optional<Path> precheckedNextFile = checkAndPrepareRegularFilePaths(afsClient, nextFile, nextFileSize, destinationOwner, absoluteServerPath, fileCollisionListener);

                    if(precheckedNextFile.isPresent()) {
                        if(nextFileSize > 0) {
                            currentsAndTotals.startTracking(nextFile.toAbsolutePath(), nextFileSize, Files.getLastModifiedTime(nextFile).toMillis());
                            transferMonitorListener.start(nextFile.toAbsolutePath(), Path.of(absoluteServerPath), nextFileSize);
                        } else {
                            transferMonitorListener.start(nextFile.toAbsolutePath(), Path.of(absoluteServerPath), 0);
                            transferMonitorListener.add(nextFile.toAbsolutePath(), Path.of(absoluteServerPath), 0, true);
                        }

                        ChunkIterable chunkIterable = new ChunkIterable(destinationOwner, absoluteServerPath, Files.size(precheckedNextFile.get()), afsClient.getMaxReadSizeInBytes() - currentSize);
                        Iterator<Chunk> chunkIterator = chunkIterable.iterator();
                        while (chunkIterator.hasNext()) {
                            Chunk nextChunk = chunkIterator.next();
                            chunkBatchToWriteInOneCall.add(nextChunk);
                            currentSize += nextChunk.getLimit();
                            chunkIterable.setChunkAvailableSize(afsClient.getMaxReadSizeInBytes() - currentSize);

                            if (currentSize >= afsClient.getMaxReadSizeInBytes() || !chunkIterator.hasNext()) {
                                // Read and upload to be scheduled on a different thread
                                Chunk[] requestChunks = chunkBatchToWriteInOneCall.toArray(Chunk[]::new);

                                maxThreadLimitSemaphore.acquire();
                                if (transferMonitorListener.getException() != null) {
                                    throw transferMonitorListener.getException();
                                }
                                if (transferMonitorListener.isStop()) {
                                    return false;
                                }
                                ReadAndUploadRunnable readAndUploadRunnable = new ReadAndUploadRunnable(afsClient,
                                        currentsAndTotals,
                                        requestChunks,
                                        sourcePath,
                                        destinationOwner,
                                        destinationPath,
                                        transferMonitorListener,
                                        maxThreadLimitSemaphore,
                                        asyncTransferRunnables);
                                Thread threadHandle = new Thread(readAndUploadRunnable);
                                threadHandle.start();
                                asyncTransferRunnables.add(readAndUploadRunnable);

                                // Clean and retrieve next package
                                chunkBatchToWriteInOneCall.clear();
                                currentSize = 0;
                                chunkIterable.setChunkAvailableSize(afsClient.getMaxReadSizeInBytes());
                            }
                        }
                    }

                } else if (Files.isDirectory(nextFile)) {
                    Optional<File> serverDirectory = getServerFilePresence(afsClient, destinationOwner, absoluteServerPath);

                    if(fileCollisionListener.precheck(nextFile.toAbsolutePath(), Path.of(absoluteServerPath), serverDirectory.isPresent()) != ClientAPI.CollisionAction.Skip) {
                        if (serverDirectory.isEmpty()) {
                            if (!afsClient.create(destinationOwner, absoluteServerPath, true)) {
                                throw new RuntimeException(String.format("Could not create directory %s on server", absoluteServerPath));
                            }
                        } else if (!serverDirectory.get().getDirectory()) {
                            throw new IllegalArgumentException(String.format("Cannot overwrite server regular file %s with local directory %s", absoluteServerPath, nextFile));
                        }
                    }
                }
            }

            //Final synchronization and check of thread results
            maxThreadLimitSemaphore.acquire(MAX_UPLOAD_THREADS);
            if (transferMonitorListener.getException() != null) {
                throw transferMonitorListener.getException();
            }
            if (transferMonitorListener.isStop()) {
                return false;
            }
        } catch (Exception e) {
            transferMonitorListener.failed(e);
            throw e;
        }

        transferMonitorListener.success();

        return true;
    }

    private static String computeAbsoluteServerPath(Path sourcePath, Path destinationPath, Path nextFile, File destinationInfo) {
        String absoluteServerPath;
        Path relativeSourcePath = sourcePath.toAbsolutePath().relativize(nextFile.toAbsolutePath());
        //Deal with case: sourcePath regular file, destinationPath directory
        if( Files.isRegularFile(nextFile) && relativeSourcePath.toString().isEmpty() && destinationInfo.getDirectory() ) {
            absoluteServerPath = destinationPath.resolve(nextFile.getFileName()).toString();
        } else {
            absoluteServerPath = destinationPath.resolve(relativeSourcePath).toString();
        }
        return absoluteServerPath;
    }

    private static Optional<Path> checkAndPrepareRegularFilePaths(@NonNull AfsClient afsClient,
                                                                  @NonNull Path localFile,
                                                                  long localFileSize,
                                                                  @NonNull String destinationOwner,
                                                                  @NonNull String absoluteServerPath,
                                                                  @NonNull FileCollisionListener fileCollisionListener) throws Exception {
        Optional<File> serverFile = getServerFilePresence(afsClient, destinationOwner, absoluteServerPath);

        ClientAPI.CollisionAction collisionAction = fileCollisionListener.precheck(localFile, Path.of(absoluteServerPath), serverFile.isPresent());

        if (collisionAction.equals(ClientAPI.CollisionAction.Override)) {

            if (serverFile.isPresent()) {
                File presentServerFile = serverFile.get();

                if (presentServerFile.getDirectory()) {
                    throw new RuntimeException(String.format("Cannot overwrite server directory %s with local regular file %s", absoluteServerPath, localFile));
                }
            }

            if(localFileSize > 0) {
                String tmpTwinServerPath = TemporaryPathUtil.getTwinTemporaryPath(Path.of(absoluteServerPath)).toString();
                deleteAndRecreateServerRegularFile(afsClient, destinationOwner, tmpTwinServerPath);
            } else {
                deleteServerRegularFile(afsClient, destinationOwner, TemporaryPathUtil.getTwinTemporaryPath(Path.of(absoluteServerPath)).toString());
                if(serverFile.isPresent()) {
                    if(serverFile.get().getSize() > 0) {
                        deleteAndRecreateServerRegularFile(afsClient, destinationOwner, absoluteServerPath);
                    }
                } else {
                    afsClient.create(destinationOwner, absoluteServerPath, false);
                }
            }

            return Optional.of(localFile);

        } else if(collisionAction.equals(ClientAPI.CollisionAction.Resume)) {

            throw new UnsupportedOperationException("CollisionAction.Resume not yet supported");

        } else if(collisionAction.equals(ClientAPI.CollisionAction.Skip)) {

            return Optional.empty();

        } else {
            throw new IllegalStateException(String.format("Unhandled CollisionAction for collision between local-file %s and server-file %s", localFile, absoluteServerPath));
        }
    }

    private static void deleteServerRegularFile(AfsClient afsClient, String destinationOwner, String absoluteServerPath) throws Exception {
        try {
            afsClient.delete(destinationOwner, absoluteServerPath);
        } catch (Exception e) {
            if (!isPathNotInStoreError(e)) {
                throw e;
            }
        }
    }

    private static void deleteAndRecreateServerRegularFile(@NonNull AfsClient afsClient, @NonNull String destinationOwner, @NonNull String serverAbsolutePath) throws Exception {
        deleteServerRegularFile(afsClient, destinationOwner, serverAbsolutePath);
        afsClient.create(destinationOwner, serverAbsolutePath, false);
    }

    private static void moveServerRegularFile(@NonNull AfsClient afsClient, @NonNull String destinationOwner, @NonNull String serverAbsolutePathSrc, @NonNull String serverAbsolutePathDest) throws Exception {
        deleteServerRegularFile(afsClient, destinationOwner, serverAbsolutePathDest);
        afsClient.move(destinationOwner, serverAbsolutePathSrc, destinationOwner, serverAbsolutePathDest);
    }

    public static Optional<File> getServerFilePresence(@NonNull AfsClient afsClient,
                                                  @NonNull String destinationOwner,
                                                  @NonNull String absoluteServerPath) throws Exception {
        try {
            File[] files = afsClient.list(destinationOwner, absoluteServerPath, false);

            if(files.length == 1 && files[0].getPath().equals(absoluteServerPath)) {
                return Optional.of(files[0]);
            } else {
                return Optional.of(new File(destinationOwner, absoluteServerPath, Optional.ofNullable(Path.of(absoluteServerPath).getFileName()).map(Objects::toString).orElse(""), true, null, null));
            }

        } catch (Exception e) {
            if (isPathNotInStoreError(e)) {
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    //TODO devise a better way to to this!!!
    public static boolean isPathNotInStoreError(Exception e) {
        if (e instanceof IllegalArgumentException) {
            String message = e.getMessage();
            if (message != null && message.contains("NoSuchFileException")) {
                return true;
            }
        }
        return false;
    }

    private static class ReadAndUploadRunnable implements Runnable {
        private final AfsClient afsClient;
        private final UploadCurrentsAndTotals currentsAndTotals;
        private final Chunk[] chunksToBeRequested;
        private final Path sourcePath;
        private final String ownerId;
        private final Path destinationPath;

        private final TransferMonitorListener transferMonitorListener;

        private final Semaphore maxThreadLimitSemaphore;
        private final List<ReadAndUploadRunnable> asyncTransferRunnables;

        private ReadAndUploadRunnable(AfsClient afsClient,
                                      UploadCurrentsAndTotals currentsAndTotals,
                                      Chunk[] requestChunks,
                                      Path sourcePath,
                                      String ownerId,
                                      Path destinationPath,
                                      TransferMonitorListener transferMonitorListener,
                                      Semaphore maxThreadLimitSemaphore,
                                      List<ReadAndUploadRunnable> asyncTransferRunnables) {
            this.afsClient = afsClient;
            this.currentsAndTotals = currentsAndTotals;
            this.chunksToBeRequested = requestChunks;
            this.sourcePath = sourcePath;
            this.ownerId = ownerId;
            this.destinationPath = destinationPath;
            this.transferMonitorListener = transferMonitorListener;
            this.maxThreadLimitSemaphore = maxThreadLimitSemaphore;
            this.asyncTransferRunnables = asyncTransferRunnables;
        }

        @Override
        public void run() {
            try {
                int i = 0;
                Chunk[] requestChunks = new Chunk[chunksToBeRequested.length];
                for (Chunk chunk: chunksToBeRequested) {
                    Path chunkServerPath = Path.of(chunk.getSource());
                    Path relativeServerPath = destinationPath.toAbsolutePath().relativize(chunkServerPath);
                    Path localPath;
                    if (Files.isDirectory(sourcePath)) {
                        localPath = sourcePath.resolve(relativeServerPath);
                    } else {
                        localPath = sourcePath;
                    }

                    try (RandomAccessFile randAccessFile = new RandomAccessFile(localPath.toString(), "rw")) {
                        FileChannel channel = randAccessFile.getChannel();

                        byte[] bytes = new byte[chunk.getLimit()];
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        int readByteCount = channel.read(buffer, chunk.getOffset());
                        if (readByteCount != chunk.getLimit()) {
                            throw new IllegalStateException(String.format("Not all bytes for chunk of %s could be read from %s", chunk.getSource(), localPath));
                        }
                        requestChunks[i] = new Chunk(chunk.getOwner(), TemporaryPathUtil.getTwinTemporaryPath(chunkServerPath).toString(), chunk.getOffset(), chunk.getLimit(), bytes);
                    }
                    i++;
                }

                if (!uploadChunks(afsClient, requestChunks)) {
                    throw new RuntimeException("Failure uploading chunks");
                }

                for (Chunk chunk: chunksToBeRequested) {
                    Path chunkServerPath = Path.of(chunk.getSource());
                    Path relativeServerPath = destinationPath.toAbsolutePath().relativize(chunkServerPath);
                    Path localPath;
                    if (Files.isDirectory(sourcePath)) {
                        localPath = sourcePath.resolve(relativeServerPath);
                    } else {
                        localPath = sourcePath;
                    }
                    boolean completed = currentsAndTotals.updateCurrentAmountsAndCheckCompletion(afsClient, localPath, ownerId, chunkServerPath.toAbsolutePath(), chunk.getLimit());
                    if (completed) {
                        moveServerRegularFile(afsClient, chunk.getOwner(), TemporaryPathUtil.getTwinTemporaryPath(chunkServerPath).toString(), chunk.getSource());
                    }
                    transferMonitorListener.add(localPath, chunkServerPath, chunk.getLimit(), completed);
                }

            } catch (Exception e) {
                transferMonitorListener.failed(e);
            } finally {
                maxThreadLimitSemaphore.release();
                asyncTransferRunnables.remove(this);
            }
        }
    }

    private static long doPreliminaryScanToInitializeUploadMonitor(AfsClient afsClient, TransferMonitorListener transferMonitor, Path sourcePath) throws IOException {
        PathIterator pathIterator = new PathIterator(sourcePath);
        long totalSize = 0;
        for(Path file : pathIterator) {
            if(Files.isRegularFile(file)) {
                totalSize += Files.size(file);
            }
        }
        transferMonitor.init(System.currentTimeMillis(), totalSize);
        return totalSize;
    }

    @SneakyThrows
    synchronized public static Boolean uploadChunks(@NonNull AfsClient afsClient, @NonNull Chunk[] requestChunks) {
        return afsClient.write(requestChunks);
    }

    private static class PathIterator implements Iterable<Path> {

        private final Path root;

        public PathIterator(Path root) {
            this.root = root;
        }

        @Override
        public Iterator<Path> iterator() {
            return new Iterator<Path>() {
                private Queue<Path> todo = new LinkedList<>(List.of(root));

                @Override
                public boolean hasNext() {
                    return !todo.isEmpty();
                }

                @SneakyThrows
                @Override
                public Path next() {
                    Path head = todo.remove();
                    if (Files.isDirectory(head)) {
                        List<Path> dirPaths = Files.list(head)
                                .filter(entry -> !entry.getFileName().toString().startsWith("."))
                                .filter(entry -> !TemporaryPathUtil.isTwinTemporaryPath(entry))
                                .collect(Collectors.toList());
                        todo.addAll(dirPaths);
                    }
                    return head;
                }
            };
        }

    }

    static class UploadCurrentsAndTotals {
        private final @NonNull Map<Path, Long> lastModificationTimestamps = new HashMap<>();
        private final @NonNull Map<Path, Long> totals = new HashMap<>();
        private final @NonNull Map<Path, Long> currents = new HashMap<>();

        synchronized boolean updateCurrentAmountsAndCheckCompletion(@NonNull AfsClient afsClient, @NonNull Path fromPath, @NonNull String ownerId, @NonNull Path toPath, int writtenByteCount) throws Exception {
            Long current = currents.get(fromPath);
            if (current == null) {
                current = 0L;
            }
            current+=writtenByteCount;
            currents.put(fromPath, current);

            Long total = totals.get(fromPath);
            if ( total != null && current >= total ) {
                totals.remove(fromPath);
                currents.remove(fromPath);
                Long expectedSrcLastModification = lastModificationTimestamps.remove(fromPath);

                Optional<File> remoteTmpToFile = getServerFilePresence(afsClient, ownerId, TemporaryPathUtil.getTwinTemporaryPath(toPath.toAbsolutePath()).toString());
                if(remoteTmpToFile.isPresent() && (long) remoteTmpToFile.get().getSize() == total &&
                        Files.exists(fromPath) && Files.size(fromPath) == total &&
                        Files.getLastModifiedTime(fromPath).toMillis() == expectedSrcLastModification
                ) {
                    return true;
                } else {
                    throw new IllegalStateException(String.format("Inconsistent upload result from %s to %s", fromPath, toPath));
                }
            } else {
                return false;
            }
        }

        synchronized void startTracking(@NonNull Path from, long size, long lastModificationTs) {
            lastModificationTimestamps.put(from, lastModificationTs);
            totals.put(from, size);
        }
    }

}