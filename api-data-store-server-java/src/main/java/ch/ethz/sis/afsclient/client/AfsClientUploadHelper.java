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
            @NonNull TransferMonitorListener transferMonitor) throws Exception {

        //Preliminary argument validation
        if (!destinationPath.isAbsolute()) {
            throw new IllegalArgumentException("destinationPath must be absolute");
        }
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("sourcePath must exist");
        }
        File destinationInfo = getServerFilePresence(afsClient, destinationOwner, destinationPath.toString()).orElseThrow(()->new IllegalArgumentException("destinationPath not found"));

        // Preliminary local-tree scan to compute total size
        doPreliminaryScanToInitializeUploadMonitor(afsClient, transferMonitor, sourcePath);

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
                    Optional<Path> precheckedNextFile = checkAndPrepareRegularFilePaths(afsClient, nextFile, destinationOwner, absoluteServerPath, fileCollisionListener);

                    if(precheckedNextFile.isPresent()) {
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
                                if (transferMonitor.getException() != null) {
                                    throw transferMonitor.getException();
                                }
                                ReadAndUploadRunnable readAndUploadRunnable = new ReadAndUploadRunnable(afsClient,
                                        requestChunks,
                                        sourcePath,
                                        destinationPath,
                                        transferMonitor,
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
                    if (serverDirectory.isEmpty()) {
                        if (!afsClient.create(destinationOwner, absoluteServerPath, true)) {
                            throw new RuntimeException(String.format("Could not create directory %s on server", absoluteServerPath));
                        }
                    } else if (!serverDirectory.get().getDirectory()) {
                        throw new IllegalArgumentException(String.format("Cannot overwrite server regular file %s with local directory %s", absoluteServerPath, nextFile));
                    }
                }
            }

            //Final synchronization and check of thread results
            maxThreadLimitSemaphore.acquire(MAX_UPLOAD_THREADS);
        } catch (Exception e) {
            transferMonitor.failed(e);
            throw e;
        }

        transferMonitor.success();

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
                                                                  @NonNull String destinationOwner,
                                                                  @NonNull String absoluteServerPath,
                                                                  @NonNull FileCollisionListener fileCollisionListener) throws Exception {
        Optional<File> serverFile = getServerFilePresence(afsClient, destinationOwner, absoluteServerPath);

        if (serverFile.isEmpty()) {
            afsClient.create(destinationOwner, absoluteServerPath, false);
            return Optional.of(localFile);
        } else {
            File presentServerFile = serverFile.get();

            if (presentServerFile.getDirectory()) {
                throw new RuntimeException(String.format("Cannot overwrite server directory %s with local regular file %s", absoluteServerPath, localFile));
            }

            ClientAPI.CollisionAction collisionAction = fileCollisionListener.collision(localFile, Path.of(absoluteServerPath));
            if (collisionAction.equals(ClientAPI.CollisionAction.Override)) {
                return Optional.of(localFile);

            } else if(collisionAction.equals(ClientAPI.CollisionAction.Resume)) {
                throw new UnsupportedOperationException("CollisionAction.Resume not yet supported");

            } else if(collisionAction.equals(ClientAPI.CollisionAction.Skip)) {
                return Optional.empty();

            } else {
                throw new IllegalStateException(String.format("Unhandled CollisionAction for collision between local-file %s and server-file %s", localFile, absoluteServerPath));

            }
        }
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
    private static boolean isPathNotInStoreError(Exception e) {
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
        private final Chunk[] requestChunks;
        private final Path sourcePath;
        private final Path destinationPath;

        private final TransferMonitorListener transferMonitorListener;

        private final Semaphore maxThreadLimitSemaphore;
        private final List<ReadAndUploadRunnable> asyncTransferRunnables;

        private ReadAndUploadRunnable(AfsClient afsClient,
                                      Chunk[] requestChunks,
                                      Path sourcePath,
                                      Path destinationPath,
                                      TransferMonitorListener transferMonitorListener,
                                      Semaphore maxThreadLimitSemaphore,
                                      List<ReadAndUploadRunnable> asyncTransferRunnables) {
            this.afsClient = afsClient;
            this.requestChunks = requestChunks;
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
            this.transferMonitorListener = transferMonitorListener;
            this.maxThreadLimitSemaphore = maxThreadLimitSemaphore;
            this.asyncTransferRunnables = asyncTransferRunnables;
        }

        @Override
        public void run() {
            try {
                int i = 0;
                for (Chunk chunk: requestChunks) {
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
                        transferMonitorListener.add(localPath, chunk.getLimit());
                        requestChunks[i] = new Chunk(chunk.getOwner(), chunk.getSource(), chunk.getOffset(), chunk.getLimit(), bytes);
                    }
                    i++;
                }

                if (!uploadChunks(afsClient, requestChunks)) {
                    throw new RuntimeException("Failure uploading chunks");
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
                        List<Path> dirPaths = Files.list(head).collect(Collectors.toList());
                        todo.addAll(dirPaths);
                    }
                    return head;
                }
            };
        }

    }

}