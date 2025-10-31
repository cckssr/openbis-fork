package ch.ethz.sis.afsclient.client;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.api.OperationsAPI;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import ch.ethz.sis.afsapi.api.ClientAPI.FileCollisionListener;
import ch.ethz.sis.afsapi.api.ClientAPI.TransferMonitorListener;

public class AfsClientDownloadHelper {
    private static final int MAX_DOWNLOAD_THREADS = 4;

    public static boolean download(
            AfsClient afsClient,
            @NonNull String sourceOwner, @NonNull Path sourcePath, @NonNull Path destinationPath,
            FileCollisionListener fileCollisionListener,
            @NonNull TransferMonitorListener transferMonitorListener) throws Exception {
        DownloadCurrentsAndTotals currentsAndTotals = new DownloadCurrentsAndTotals();

        //Preliminary argument validation
        if (!sourcePath.startsWith(java.io.File.separator)) {
            throw new IllegalArgumentException("sourcePath must be absolute");
        }
        if (!Files.exists(destinationPath)) {
            throw new IllegalArgumentException("destinationPath must exist");
        }
        File[] initialList = afsClient.list(sourceOwner, AfsClientUploadHelper.toServerPathString(sourcePath), false);
        checkInitialList(initialList, sourcePath, destinationPath);

        // Preliminary server-tree scan to compute total size
        doPreliminaryScanToInitializeDownloadMonitor(afsClient, transferMonitorListener, initialList);

        // Initializer
        FileIterator fileIterator = new FileIterator(initialList, afsClient);
        Iterator<File> iterator = fileIterator.iterator();

        // Producer
        List<Chunk> chunkBatchToReadInOneCall = new ArrayList<>();
        int currentSize = 0;

        Semaphore maxThreadLimitSemaphore = new Semaphore(MAX_DOWNLOAD_THREADS);
        List<DownloadAndSaveRunnable> asyncTransferRunnables = Collections.synchronizedList(new LinkedList<>());
        try {
            while (iterator.hasNext()) {
                File nextFile = iterator.next();
                Path nextFromPath = Path.of(nextFile.getPath());

                if (Boolean.FALSE.equals(nextFile.getDirectory())) {
                    Path localPathForFile = computeLocalPathForFile(sourcePath, destinationPath, nextFile);
                    Optional<File> precheckedNextFile = createRegularFile(localPathForFile, nextFile, fileCollisionListener);

                    if (precheckedNextFile.isPresent()) {
                        if (nextFile.getSize() > 0) {
                            currentsAndTotals.startTracking(nextFromPath, nextFile.getSize(), nextFile.getLastModifiedTime().toInstant().toEpochMilli());
                            transferMonitorListener.start(nextFromPath, localPathForFile.toAbsolutePath(), nextFile.getSize());
                        } else {
                            transferMonitorListener.start(nextFromPath, localPathForFile.toAbsolutePath(), 0);
                            transferMonitorListener.add(nextFromPath, localPathForFile.toAbsolutePath(), 0, true);
                        }

                        ChunkIterable chunkIterable = new ChunkIterable(precheckedNextFile.get(), afsClient.getMaxReadSizeInBytes());
                        Iterator<Chunk> chunkIterator = chunkIterable.iterator();
                        while (chunkIterator.hasNext()) {
                            Chunk nextChunk = chunkIterator.next();
                            chunkBatchToReadInOneCall.add(nextChunk);
                            currentSize += nextChunk.getLimit();
                            chunkIterable.setChunkAvailableSize(afsClient.getMaxReadSizeInBytes() - currentSize);

                            if (currentSize >= afsClient.getMaxReadSizeInBytes() || !chunkIterator.hasNext()) {

                                // Download and save to be scheduled on a different thread
                                Chunk[] requestChunks = chunkBatchToReadInOneCall.toArray(Chunk[]::new);

                                maxThreadLimitSemaphore.acquire();
                                if (transferMonitorListener.getException() != null) {
                                    throw transferMonitorListener.getException();
                                }
                                if (transferMonitorListener.isStop()) {
                                    return false;
                                }
                                DownloadAndSaveRunnable downloadAndSaveRunnable = new DownloadAndSaveRunnable(afsClient,
                                        currentsAndTotals,
                                        requestChunks,
                                        localPathForFile,
                                        transferMonitorListener,
                                        maxThreadLimitSemaphore,
                                        asyncTransferRunnables);
                                Thread threadHandle = new Thread(downloadAndSaveRunnable);
                                threadHandle.start();
                                asyncTransferRunnables.add(downloadAndSaveRunnable);

                                // Clean and retrieve next package
                                chunkBatchToReadInOneCall.clear();
                                currentSize = 0;
                                chunkIterable.setChunkAvailableSize(afsClient.getMaxReadSizeInBytes());
                            }
                        }
                    }

                } else if (Boolean.TRUE.equals(nextFile.getDirectory())) {
                    Path localPathForDirectory = computeLocalPathForFile(sourcePath, destinationPath, nextFile);
                    boolean localPathAlreadyExists = Files.exists(localPathForDirectory);
                    if (fileCollisionListener.precheck(nextFromPath, localPathForDirectory.toAbsolutePath(), localPathAlreadyExists) != ClientAPI.CollisionAction.Skip) {
                        createDirectory(localPathForDirectory, nextFile);
                    }
                }
            }

            //Final synchronization and check of thread results
            maxThreadLimitSemaphore.acquire(MAX_DOWNLOAD_THREADS);
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

    private static class DownloadAndSaveRunnable implements Runnable {
        private final AfsClient afsClient;
        private final DownloadCurrentsAndTotals currentsAndTotals;
        private final Chunk[] requestChunks;
        private final Path localPath;

        private final TransferMonitorListener transferMonitorListener;

        private final Semaphore maxThreadLimitSemaphore;
        private final List<DownloadAndSaveRunnable> downloadAndSaveRunnables;

        private DownloadAndSaveRunnable(AfsClient afsClient,
                                        DownloadCurrentsAndTotals currentsAndTotals,
                                        Chunk[] requestChunks,
                                        Path localPath,
                                        TransferMonitorListener transferMonitorListener,
                                        Semaphore maxThreadLimitSemaphore,
                                        List<DownloadAndSaveRunnable> downloadAndSaveRunnables) {
            this.afsClient = afsClient;
            this.currentsAndTotals = currentsAndTotals;
            this.requestChunks = requestChunks;
            this.localPath = localPath;
            this.transferMonitorListener = transferMonitorListener;
            this.maxThreadLimitSemaphore = maxThreadLimitSemaphore;
            this.downloadAndSaveRunnables = downloadAndSaveRunnables;
        }

        @Override
        public void run() {
            try {
                Chunk[] chunks = downloadChunks(afsClient, requestChunks);
                save(afsClient, localPath, transferMonitorListener, chunks, currentsAndTotals);
            } catch (Exception e) {
                transferMonitorListener.failed(e);
            } finally {
                maxThreadLimitSemaphore.release();
                downloadAndSaveRunnables.remove(this);
            }
        }

    }

    private static long doPreliminaryScanToInitializeDownloadMonitor(AfsClient afsClient, TransferMonitorListener transferMonitor, File[] initialList) {
        FileIterator fileIterator = new FileIterator(initialList, afsClient);
        long totalSize = 0;
        for (File file : fileIterator) {
            if (Boolean.FALSE.equals(file.getDirectory())) {
                totalSize += file.getSize();
            }
        }
        transferMonitor.init(System.currentTimeMillis(), totalSize);
        return totalSize;
    }

    private static Path computeLocalPathForFile(Path sourcePath, Path destinationPath, File serverFile) {
        Path relativeServerPath = sourcePath.relativize(Path.of(serverFile.getPath()));
        Path localPath;
        if (relativeServerPath.toString().isEmpty()) {
            //Deal with different possible types (regular file or directory) of initial source and destination paths
            if (serverFile.getDirectory()) {
                if (Files.isDirectory(destinationPath)) {
                    localPath = destinationPath;
                } else {
                    throw new IllegalArgumentException(String.format("Cannot overwrite non-directory file %s with server-directory file %s", destinationPath, serverFile.getPath()));
                }
            } else {
                if (Files.isDirectory(destinationPath)) {
                    localPath = destinationPath.resolve(Path.of(serverFile.getName()));
                } else {
                    localPath = destinationPath;
                }
            }
        } else {
            if (Files.isDirectory(destinationPath)) {
                localPath = destinationPath.resolve(relativeServerPath);
            } else {
                throw new IllegalArgumentException(String.format("Cannot create sub-file %s in non-directory %s", relativeServerPath, destinationPath));
            }
        }
        return localPath;
    }

    private static Optional<File> createRegularFile(Path localPathForFile, File serverFile, FileCollisionListener fileCollisionListener) throws IOException {
        Path serverFileAbsolutePath = Path.of(serverFile.getPath());
        Path twinTmpLocalPath = TemporaryPathUtil.getTwinTemporaryPath(localPathForFile);

        boolean localFileAlreadyExists = Files.exists(localPathForFile);
        ClientAPI.CollisionAction collisionAction = fileCollisionListener.precheck(serverFileAbsolutePath, localPathForFile, localFileAlreadyExists);

        if (collisionAction.equals(ClientAPI.CollisionAction.Override)) {
            if (localFileAlreadyExists) {
                if (!Files.isRegularFile(localPathForFile)) {
                    throw new IllegalArgumentException(String.format("Cannot overwrite non-regular file %s with regular file %s", localPathForFile, serverFile.getPath()));
                }
            }

            if (serverFile.getSize() > 0) {
                try {
                    Files.createFile(twinTmpLocalPath);
                } catch (FileAlreadyExistsException exception) {
                }

                try (RandomAccessFile randAccessFile = new RandomAccessFile(twinTmpLocalPath.toString(), "rw")) {
                    randAccessFile.setLength(serverFile.getSize());
                }
            } else {
                Files.deleteIfExists(twinTmpLocalPath);
                if (localFileAlreadyExists) {
                    if (Files.size(localPathForFile) > 0) {
                        try (RandomAccessFile randAccessFile = new RandomAccessFile(localPathForFile.toString(), "rw")) {
                            randAccessFile.setLength(0);
                        }
                    }
                } else {
                    Files.createFile(localPathForFile);
                }
            }

        } else if (collisionAction.equals(ClientAPI.CollisionAction.Resume)) {
            throw new UnsupportedOperationException("CollisionAction.Resume not yet supported");

        } else if (collisionAction.equals(ClientAPI.CollisionAction.Skip)) {
            return Optional.empty();

        } else {
            throw new IllegalStateException(String.format("Unhandled CollisionAction for collision between local-file %s and server-file %s", localPathForFile, serverFileAbsolutePath));
        }

        return Optional.of(serverFile);
    }

    private static void createDirectory(Path localPathForDirectory, File nextFile) throws IOException {
        if (Files.exists(localPathForDirectory)) {
            if (!Files.isDirectory(localPathForDirectory)) {
                throw new IllegalArgumentException(String.format("Cannot overwrite non-directory file %s with directory %s", localPathForDirectory, nextFile.getPath()));
            }
        } else {
            Files.createDirectory(localPathForDirectory);
        }
    }

    private static boolean checkInitialList(@NonNull File[] initialList, @NonNull Path sourcePath, @NonNull Path destinationPath) throws Exception {
        if (initialList.length == 1 && Path.of(initialList[0].getPath()).equals(sourcePath)) {
            //sourcePath is a regular file
            return true;
        } else {
            //sourcePath is a regular file, so destinationPath can only be a directory
            if (!Files.isDirectory(destinationPath)) {
                throw new IllegalArgumentException(String.format("Cannot overwrite non-directory file %s with directory %s", destinationPath, sourcePath));
            } else {
                return true;
            }
        }
    }

    @SneakyThrows
    public static Chunk[] downloadChunks(@NonNull AfsClient afsClient, @NonNull Chunk[] requestChunks) {
        Chunk[] chunks = afsClient.read(requestChunks);
        return chunks;
    }

    @SneakyThrows
    static synchronized Void save(
            @NonNull AfsClient afsClient,
            @NonNull Path localPath,
            @NonNull TransferMonitorListener transferMonitor,
            @NonNull Chunk[] chunks,
            @NonNull DownloadCurrentsAndTotals currentsAndTotals) {

        for (Chunk chunk : chunks) {
            Path fromPath = Path.of(chunk.getSource());
            Path twinTmpLocalPath = TemporaryPathUtil.getTwinTemporaryPath(localPath);

            try (RandomAccessFile randAccessFile = new RandomAccessFile(twinTmpLocalPath.toString(), "rw")) {
                FileChannel channel = randAccessFile.getChannel();

                ByteBuffer buffer = ByteBuffer.wrap(chunk.getData());
                int writtenByteCount = channel.write(buffer, chunk.getOffset());
                if (writtenByteCount != chunk.getData().length) {
                    throw new IllegalStateException(String.format("Not all bytes from chunk of %s could be written to %s", chunk.getSource(), localPath));
                }

                boolean completed = currentsAndTotals.updateCurrentAmountsAndCheckCompletion(afsClient, chunk.getOwner(), fromPath, localPath, writtenByteCount);
                if (completed) {
                    Files.move(twinTmpLocalPath, localPath, StandardCopyOption.REPLACE_EXISTING);
                }
                transferMonitor.add(fromPath, localPath.toAbsolutePath(), writtenByteCount, completed);
            }
        }

        return null;
    }

    private static class FileIterator implements Iterable<File> {

        private final File[] initial;

        private final OperationsAPI client;

        public FileIterator(File[] initial, OperationsAPI client) {
            this.initial = initial;
            this.client = client;
        }

        @Override
        public Iterator<File> iterator() {
            return new Iterator<File>() {
                private Queue<File> todo = new LinkedList<>(Arrays.stream(initial)
                        .filter(entry -> !TemporaryPathUtil.isTwinTemporaryPath(Path.of(entry.getPath())))
                        .collect(Collectors.toCollection(LinkedList::new)));

                @Override
                public synchronized boolean hasNext() {
                    return !todo.isEmpty();
                }

                @SneakyThrows
                @Override
                public synchronized File next() {
                    File head = todo.remove();
                    if (head.getDirectory()) {
                        File[] dirPaths = client.list(head.getOwner(), head.getPath(), false);
                        todo.addAll(Arrays.stream(dirPaths)
                                .filter(entry -> !entry.getName().startsWith("."))
                                .filter(entry -> !TemporaryPathUtil.isTwinTemporaryPath(Path.of(entry.getPath())))
                                .collect(Collectors.toList()));
                    }
                    return head;
                }
            };
        }
    }

    public static class ChunkIterable implements Iterable<Chunk> {
        private final String owner;
        private final String absoluteServerPath;
        private final long size;

        private int chunkAvailableSize;

        public ChunkIterable(@NonNull String owner, @NonNull String absoluteServerPath, long size, int chunkAvailableSize) {
            this.owner = owner;
            this.absoluteServerPath = absoluteServerPath;
            this.size = size;
            this.chunkAvailableSize = chunkAvailableSize;
        }

        public ChunkIterable(@NonNull File file, int chunkAvailableSize) {
            this.owner = file.getOwner();
            this.absoluteServerPath = file.getPath();
            this.size = file.getSize();
            this.chunkAvailableSize = chunkAvailableSize;
        }

        int getChunkAvailableSize() {
            return chunkAvailableSize;
        }

        public void setChunkAvailableSize(int chunkAvailableSize) {
            this.chunkAvailableSize = chunkAvailableSize;
        }

        @Override
        public Iterator<Chunk> iterator() {
            return new Iterator<Chunk>() {
                private long offset = 0;

                @Override
                public synchronized boolean hasNext() {
                    return offset < size;
                }

                @SneakyThrows
                @Override
                public synchronized Chunk next() {
                    Integer limit = getChunkAvailableSize();
                    if (offset + limit > size) {
                        long reminder = size - offset;
                        limit = safeLongToInt(reminder);
                    }

                    Chunk chunk = new Chunk(
                            owner,
                            absoluteServerPath,
                            offset,
                            limit,
                            new byte[0]
                    );
                    offset += chunk.getLimit();
                    return chunk;
                }

                private Integer safeLongToInt(long value) {
                    if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                        throw new ArithmeticException("Value out of range for Integer: " + value);
                    }
                    return (int) value;
                }
            };
        }
    }

    static class DownloadCurrentsAndTotals {
        private final @NonNull Map<Path, Long> lastModificationTimestamps = new HashMap<>();
        private final @NonNull Map<Path, Long> totals = new HashMap<>();
        private final @NonNull Map<Path, Long> currents = new HashMap<>();

        synchronized boolean updateCurrentAmountsAndCheckCompletion(@NonNull AfsClient afsClient, @NonNull String ownerId, @NonNull Path fromPath, @NonNull Path toPath, int writtenByteCount) throws Exception {
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

                Optional<File> remoteFromPath = AfsClientUploadHelper.getServerFilePresence(afsClient, ownerId, AfsClientUploadHelper.toServerPathString(fromPath));
                Path twinLocalToPath = TemporaryPathUtil.getTwinTemporaryPath(toPath);
                if(Files.exists(twinLocalToPath) && Files.size(twinLocalToPath) == total &&
                        remoteFromPath.isPresent() && (long) remoteFromPath.get().getSize() == total &&
                        remoteFromPath.get().getLastModifiedTime().toInstant().toEpochMilli() == expectedSrcLastModification
                ) {
                    totals.remove(fromPath);
                    currents.remove(fromPath);
                    return true;
                } else {
                    throw new IllegalStateException(String.format("Inconsistent download result from %s to %s", fromPath, toPath));
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

