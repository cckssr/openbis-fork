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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import ch.ethz.sis.afsapi.api.ClientAPI.FileCollisionListener;
import ch.ethz.sis.afsapi.api.ClientAPI.TransferMonitorListener;

public class AfsClientDownloadHelper
{
    private static final int MAX_DOWNLOAD_THREADS = 4;

    public static boolean download(
            AfsClient afsClient,
            @NonNull String sourceOwner, @NonNull Path sourcePath, @NonNull Path destinationPath,
            FileCollisionListener fileCollisionListener,
            @NonNull TransferMonitorListener transferMonitorListener) throws Exception {

        //Preliminary argument validation
        if (!sourcePath.isAbsolute()) {
            throw new IllegalArgumentException("sourcePath must be absolute");
        }
        if (!Files.exists(destinationPath)) {
            throw new IllegalArgumentException("destinationPath must exist");
        }
        File[] initialList = afsClient.list(sourceOwner, sourcePath.toString(), false);
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

                if(Boolean.FALSE.equals(nextFile.getDirectory())) {
                    Path localPathForFile= computeLocalPathForFile(sourcePath, destinationPath, nextFile);
                    Optional<File> precheckedNextFile = createRegularFile(localPathForFile, nextFile, fileCollisionListener);

                    if(precheckedNextFile.isPresent()) {
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
                                DownloadAndSaveRunnable downloadAndSaveRunnable = new DownloadAndSaveRunnable(afsClient,
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
                    createDirectory(localPathForDirectory, nextFile);
                }
            }

            //Final synchronization and check of thread results
            maxThreadLimitSemaphore.acquire(MAX_DOWNLOAD_THREADS);
            if (transferMonitorListener.getException() != null) {
                throw transferMonitorListener.getException();
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
        private final Chunk[] requestChunks;
        private final Path localPath;

        private final TransferMonitorListener transferMonitorListener;

        private final Semaphore maxThreadLimitSemaphore;
        private final List<DownloadAndSaveRunnable> downloadAndSaveRunnables;

        private DownloadAndSaveRunnable(AfsClient afsClient,
                                        Chunk[] requestChunks,
                                        Path localPath,
                                        TransferMonitorListener transferMonitorListener,
                                        Semaphore maxThreadLimitSemaphore,
                                        List<DownloadAndSaveRunnable> downloadAndSaveRunnables) {
            this.afsClient = afsClient;
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
                save(localPath, transferMonitorListener, chunks);
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
        for(File file : fileIterator) {
            if( Boolean.FALSE.equals(file.getDirectory()) ) {
                totalSize += file.getSize();
            }
        }
        transferMonitor.init(System.currentTimeMillis(), totalSize);
        return totalSize;
    }

    private static Path computeLocalPathForFile(Path sourcePath, Path destinationPath, File serverFile) {
        Path relativeServerPath = sourcePath.toAbsolutePath().relativize(Path.of(serverFile.getPath()));
        Path localPath;
        if(relativeServerPath.toString().isEmpty()) {
            //Deal with different possible types (regular file or directory) of initial source and destination paths
            if (serverFile.getDirectory()) {
                if(Files.isDirectory(destinationPath)) {
                    localPath = destinationPath;
                } else {
                    throw new IllegalArgumentException(String.format("Cannot overwrite non-directory file %s with server-directory file %s", destinationPath, serverFile.getPath()));
                }
            } else {
                if(Files.isDirectory(destinationPath)) {
                    localPath = destinationPath.resolve(Path.of(serverFile.getName()));
                } else {
                    localPath = destinationPath;
                }
            }
        } else {
            if(Files.isDirectory(destinationPath)) {
                localPath = destinationPath.resolve(relativeServerPath);
            } else {
                throw new IllegalArgumentException(String.format("Cannot create sub-file %s in non-directory %s", relativeServerPath, destinationPath));
            }
        }
        return localPath;
    }

    private static Optional<File> createRegularFile(Path localPathForFile, File serverFile, FileCollisionListener fileCollisionListener) throws IOException {
        if (Files.exists(localPathForFile)) {

            Path serverFileAbsolutePath = Path.of(serverFile.getPath());
            ClientAPI.CollisionAction collisionAction = fileCollisionListener.collision(serverFileAbsolutePath, localPathForFile);

            if (collisionAction.equals(ClientAPI.CollisionAction.Override)) {
                if (!Files.isRegularFile(localPathForFile)) {
                    throw new IllegalArgumentException(String.format("Cannot overwrite non-regular file %s with regular file %s", localPathForFile, serverFile.getPath()));
                }
            } else if(collisionAction.equals(ClientAPI.CollisionAction.Resume)) {
                throw new UnsupportedOperationException("CollisionAction.Resume not yet supported");

            } else if(collisionAction.equals(ClientAPI.CollisionAction.Skip)) {
                return Optional.empty();

            } else {
                throw new IllegalStateException(String.format("Unhandled CollisionAction for collision between local-file %s and server-file %s", localPathForFile, serverFileAbsolutePath));

            }
        } else {
            Files.createFile(localPathForFile);
        }

        try (RandomAccessFile randAccessFile = new RandomAccessFile(localPathForFile.toString(), "rw")) {
            randAccessFile.setLength(serverFile.getSize());
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
        if(initialList.length == 1 && initialList[0].getPath().equals(sourcePath.toAbsolutePath().toString())) {
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
            @NonNull Path localPath,
            @NonNull TransferMonitorListener transferMonitor,
            @NonNull Chunk[] chunks) {

        for(Chunk chunk : chunks) {
            Path sourceChunkPath = Path.of(chunk.getSource());

            try (RandomAccessFile randAccessFile = new RandomAccessFile(localPath.toString(), "rw")) {
                FileChannel channel = randAccessFile.getChannel();

                ByteBuffer buffer = ByteBuffer.wrap(chunk.getData());
                int writtenByteCount = channel.write(buffer, chunk.getOffset());
                if (writtenByteCount != chunk.getData().length) {
                    throw new IllegalStateException(String.format("Not all bytes from chunk of %s could be written to %s", chunk.getSource(), localPath));
                }
                transferMonitor.add(sourceChunkPath, writtenByteCount);
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
                private Queue<File> todo = new LinkedList<>(List.of(initial));

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
                        todo.addAll(Arrays.asList(dirPaths));
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
}