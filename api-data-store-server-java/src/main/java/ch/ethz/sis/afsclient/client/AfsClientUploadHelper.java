package ch.ethz.sis.afsclient.client;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.api.ClientAPI.FileCollisionListener;
import ch.ethz.sis.afsapi.api.ClientAPI.TransferMonitorListener;
import ch.ethz.sis.afsapi.api.OperationsAPI;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsclient.client.AfsClientDownloadHelper.ChunkIterable;
import ch.ethz.sis.transaction.api.TransactionOperationException;
import lombok.NonNull;
import lombok.SneakyThrows;

public class AfsClientUploadHelper
{
    private static final int MAX_UPLOAD_THREADS = 4;

    private static final String AFS_SERVER_PATH_SEPARATOR = "/";

    public static boolean upload(
            AfsClient afsClient,
            @NonNull Path sourcePath, @NonNull String destinationOwner, @NonNull Path destinationPath,
            FileCollisionListener fileCollisionListener,
            @NonNull TransferMonitorListener transferMonitorListener, boolean transactional) throws Exception
    {
        sourcePath = sourcePath.normalize().toAbsolutePath();
        UploadCurrentsAndTotals currentsAndTotals = new UploadCurrentsAndTotals();

        //Preliminary argument validation
        if (!destinationPath.startsWith(java.io.File.separator))
        {
            throw new IllegalArgumentException("destinationPath must be absolute");
        }
        if (!Files.exists(sourcePath))
        {
            throw new IllegalArgumentException("sourcePath must exist");
        }

        Optional<File> destinationInfo = getServerFilePresence(afsClient, destinationOwner, toServerPathString(destinationPath));

        if (destinationInfo.isEmpty() && !transactional)
        {
            throw new IllegalArgumentException("destinationPath not found");
        }

        // Preliminary local-tree scan to compute total size
        doPreliminaryScanToInitializeUploadMonitor(afsClient, transferMonitorListener, sourcePath);

        Cache cache = null;

        if (transactional)
        {
            // Within a transaction we cannot list once we start writing. Therefore, we need to list all the existing files upfront.
            cache = doPreliminaryScanToInitializeCache(afsClient, sourcePath, destinationOwner, destinationPath, destinationInfo.orElse(null));
        }

        // Initializer
        PathIterator pathIterator = new PathIterator(sourcePath);
        Iterator<Path> iterator = pathIterator.iterator();

        // Producer
        List<Chunk> chunkBatchToWriteInOneCall = new ArrayList<>();
        int currentSize = 0;

        Semaphore maxThreadLimitSemaphore = new Semaphore(MAX_UPLOAD_THREADS);
        List<ReadAndUploadRunnable> asyncTransferRunnables = Collections.synchronizedList(new LinkedList<>());
        try
        {
            while (iterator.hasNext())
            {
                Path nextFile = iterator.next();
                String absServerPathAsStr = computeAbsoluteServerPath(sourcePath, destinationPath, nextFile, destinationInfo.orElse(null));
                Path absServerPath = Path.of(absServerPathAsStr);

                if (!Files.isDirectory(nextFile))
                {
                    long nextFileSize = Files.size(nextFile);
                    Optional<Path> precheckedNextFile =
                            checkAndPrepareRegularFilePaths(afsClient, nextFile, nextFileSize, destinationOwner, absServerPathAsStr,
                                    fileCollisionListener, transactional, cache);

                    if (precheckedNextFile.isPresent())
                    {
                        if (nextFileSize > 0)
                        {
                            currentsAndTotals.startTracking(nextFile.toAbsolutePath(), nextFileSize, Files.getLastModifiedTime(nextFile).toMillis());
                            transferMonitorListener.start(nextFile.toAbsolutePath(), absServerPath, nextFileSize);
                        } else
                        {
                            transferMonitorListener.start(nextFile.toAbsolutePath(), absServerPath, 0);
                            transferMonitorListener.add(nextFile.toAbsolutePath(), absServerPath, 0, true);
                        }

                        ChunkIterable chunkIterable = new ChunkIterable(destinationOwner, absServerPathAsStr, Files.size(precheckedNextFile.get()),
                                afsClient.getMaxReadSizeInBytes() - currentSize);
                        Iterator<Chunk> chunkIterator = chunkIterable.iterator();
                        while (chunkIterator.hasNext())
                        {
                            Chunk nextChunk = chunkIterator.next();
                            if (!currentsAndTotals.skipForConcurrentModification(precheckedNextFile.get()))
                            {
                                chunkBatchToWriteInOneCall.add(nextChunk);
                                currentSize += nextChunk.getLimit();
                                chunkIterable.setChunkAvailableSize(afsClient.getMaxReadSizeInBytes() - currentSize);

                                if (currentSize >= afsClient.getMaxReadSizeInBytes() || !chunkIterator.hasNext())
                                {
                                    // Read and upload to be scheduled on a different thread
                                    Chunk[] requestChunks = chunkBatchToWriteInOneCall.toArray(Chunk[]::new);

                                    maxThreadLimitSemaphore.acquire();
                                    if (transferMonitorListener.getException() != null)
                                    {
                                        throw transferMonitorListener.getException();
                                    }
                                    if (transferMonitorListener.isStop())
                                    {
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
                                            asyncTransferRunnables, cache, transactional);
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
                    }

                } else if (Files.isDirectory(nextFile))
                {
                    Optional<File> serverDirectory = getServerFilePresence(afsClient, destinationOwner, absServerPathAsStr, cache);

                    if (fileCollisionListener.precheck(nextFile.toAbsolutePath(), absServerPath, serverDirectory.isPresent())
                            != ClientAPI.CollisionAction.Skip)
                    {
                        if (serverDirectory.isEmpty())
                        {
                            if (!afsClient.create(destinationOwner, absServerPathAsStr, true))
                            {
                                throw new RuntimeException(String.format("Could not create directory %s on server", absServerPathAsStr));
                            }
                        } else if (!serverDirectory.get().getDirectory())
                        {
                            throw new IllegalArgumentException(
                                    String.format("Cannot overwrite server regular file %s with local directory %s", absServerPathAsStr, nextFile));
                        }
                    }
                }
            }

            //Final synchronization and check of thread results
            maxThreadLimitSemaphore.acquire(MAX_UPLOAD_THREADS);
            if (transferMonitorListener.getException() != null)
            {
                throw transferMonitorListener.getException();
            }
            if (transferMonitorListener.isStop())
            {
                return false;
            }
        } catch (Exception e)
        {
            transferMonitorListener.failed(e);
            throw e;
        }

        if (currentsAndTotals.isAllCompleted())
        {
            transferMonitorListener.success();
            return true;
        } else
        {
            transferMonitorListener.failed(new IllegalStateException("Incomplete upload"));
            return false;
        }
    }

    private static String computeAbsoluteServerPath(Path sourcePath, Path destinationPath, Path nextFile, File destinationInfo)
    {
        Path absoluteServerPath;
        Path relativeSourcePath = sourcePath.toAbsolutePath().relativize(nextFile.toAbsolutePath());
        //Deal with case: sourcePath regular file, destinationPath directory
        if (Files.isRegularFile(nextFile) && relativeSourcePath.toString().isEmpty() && (destinationInfo == null || destinationInfo.getDirectory()))
        {
            absoluteServerPath = destinationPath.resolve(Optional.ofNullable(nextFile.getFileName()).orElse(Path.of("")));
        } else
        {
            absoluteServerPath = destinationPath.resolve(relativeSourcePath);
        }
        return toServerPathString(absoluteServerPath);
    }

    private static Optional<Path> checkAndPrepareRegularFilePaths(@NonNull AfsClient afsClient,
            @NonNull Path localFile,
            long localFileSize,
            @NonNull String destinationOwner,
            @NonNull String absoluteServerPath,
            @NonNull FileCollisionListener fileCollisionListener,
            boolean transactional, Cache cache) throws Exception
    {
        Optional<File> serverFile = getServerFilePresence(afsClient, destinationOwner, absoluteServerPath, cache);

        ClientAPI.CollisionAction collisionAction = fileCollisionListener.precheck(localFile, Path.of(absoluteServerPath), serverFile.isPresent());

        if (collisionAction.equals(ClientAPI.CollisionAction.Override))
        {
            if (serverFile.isPresent())
            {
                File presentServerFile = serverFile.get();
                if (presentServerFile.getDirectory())
                {
                    throw new RuntimeException(
                            String.format("Cannot overwrite server directory %s with local regular file %s", absoluteServerPath, localFile));
                }
            }

            if (localFileSize == 0) {
                if (serverFile.isEmpty()) {
                    // explicitly create an empty file as no data will be uploaded to create it (local file size == 0)
                    afsClient.create(destinationOwner, absoluteServerPath, false);
                }
            } else {
                if (transactional) {
                    if (serverFile.isPresent()) {
                        // inside a transaction we can write directly to the file without a risk of destroying it
                        afsClient.snapshot(destinationOwner, absoluteServerPath);
                        afsClient.truncate(destinationOwner, absoluteServerPath, 0L);
                    }
                } else
                {
                    // outside a transaction we write to a temp file and then rename it, first we need to delete an old temp file if exists
                    String twinAbsoluteServerPath = toServerPathString(TemporaryPathUtil.getTwinTemporaryPath(Path.of(absoluteServerPath)));
                    deleteServerRegularFile(afsClient, destinationOwner, twinAbsoluteServerPath, false);
                }
            }

            return Optional.of(localFile);

        } else if (collisionAction.equals(ClientAPI.CollisionAction.Resume))
        {

            throw new UnsupportedOperationException("CollisionAction.Resume not yet supported");

        } else if (collisionAction.equals(ClientAPI.CollisionAction.Skip))
        {

            return Optional.empty();

        } else
        {
            throw new IllegalStateException(
                    String.format("Unhandled CollisionAction for collision between local-file %s and server-file %s", localFile, absoluteServerPath));
        }
    }

    private static void deleteServerRegularFile(AfsClient afsClient, String destinationOwner, String absoluteServerPath, boolean trash) throws Exception
    {
        try
        {
            afsClient.delete(destinationOwner, absoluteServerPath, trash);
        } catch (Exception e)
        {
            if (!isPathNotInStoreError(e))
            {
                throw e;
            }
        }
    }

    public static Optional<File> getServerFilePresence(@NonNull OperationsAPI afsClient,
            @NonNull String destinationOwner,
            @NonNull String absoluteServerPath) throws Exception
    {
        return getServerFilePresence(afsClient, destinationOwner, absoluteServerPath, null);
    }

    private static Optional<File> getServerFilePresence(@NonNull OperationsAPI afsClient,
            @NonNull String destinationOwner,
            @NonNull String absoluteServerPath, Cache cache) throws Exception
    {
        if (cache != null && cache.hasFile(destinationOwner, absoluteServerPath))
        {
            return Optional.ofNullable(cache.getFile(destinationOwner, absoluteServerPath));
        }

        try
        {
            File[] files = afsClient.list(destinationOwner, absoluteServerPath, false);

            Optional<File> file;

            if (files.length == 1 && files[0].getPath().equals(absoluteServerPath))
            {
                file = Optional.of(files[0]);
            } else
            {
                file = Optional.of(new File(destinationOwner, absoluteServerPath,
                        Optional.ofNullable(Path.of(absoluteServerPath).getFileName()).map(Objects::toString).orElse(""), true, null, null));
            }

            if (cache != null)
            {
                cache.putFile(destinationOwner, absoluteServerPath, file.get());
            }

            return file;
        } catch (Exception e)
        {
            if (isPathNotInStoreError(e))
            {
                if (cache != null)
                {
                    cache.putFile(destinationOwner, absoluteServerPath, null);
                }

                return Optional.empty();
            } else
            {
                throw e;
            }
        }
    }

    //TODO devise a better way to to this!!!
    public static boolean isPathNotInStoreError(Exception e)
    {
        String message;

        if (e instanceof TransactionOperationException)
        {
            message = e.getCause().getMessage();
        } else
        {
            message = e.getMessage();
        }

        if (message != null && message.contains("NoSuchFileException"))
        {
            return true;
        }
        return false;
    }

    private static class ReadAndUploadRunnable implements Runnable
    {
        private final AfsClient afsClient;

        private final UploadCurrentsAndTotals currentsAndTotals;

        private final Chunk[] chunksToBeRequested;

        private final Path sourcePath;

        private final String ownerId;

        private final Path destinationPath;

        private final TransferMonitorListener transferMonitorListener;

        private final Semaphore maxThreadLimitSemaphore;

        private final List<ReadAndUploadRunnable> asyncTransferRunnables;

        private final Cache cache;

        private final boolean transactional;

        private ReadAndUploadRunnable(AfsClient afsClient,
                UploadCurrentsAndTotals currentsAndTotals,
                Chunk[] requestChunks,
                Path sourcePath,
                String ownerId,
                Path destinationPath,
                TransferMonitorListener transferMonitorListener,
                Semaphore maxThreadLimitSemaphore,
                List<ReadAndUploadRunnable> asyncTransferRunnables, Cache cache, boolean transactional)
        {
            this.afsClient = afsClient;
            this.currentsAndTotals = currentsAndTotals;
            this.chunksToBeRequested = requestChunks;
            this.sourcePath = sourcePath;
            this.ownerId = ownerId;
            this.destinationPath = destinationPath;
            this.transferMonitorListener = transferMonitorListener;
            this.maxThreadLimitSemaphore = maxThreadLimitSemaphore;
            this.asyncTransferRunnables = asyncTransferRunnables;
            this.cache = cache;
            this.transactional = transactional;
        }

        @Override
        public void run()
        {
            try
            {
                int i = 0;
                Chunk[] requestChunks = new Chunk[chunksToBeRequested.length];
                for (Chunk chunk : chunksToBeRequested)
                {
                    Path chunkServerPath = Path.of(chunk.getSource());
                    Path relativeServerPath = destinationPath.toAbsolutePath().relativize(chunkServerPath);
                    Path localPath;
                    if (Files.isDirectory(sourcePath))
                    {
                        localPath = sourcePath.resolve(relativeServerPath);
                    } else
                    {
                        localPath = sourcePath;
                    }

                    try (RandomAccessFile randAccessFile = new RandomAccessFile(localPath.toString(), "rw"))
                    {
                        FileChannel channel = randAccessFile.getChannel();

                        byte[] bytes = new byte[chunk.getLimit()];
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        int readByteCount = channel.read(buffer, chunk.getOffset());
                        if (readByteCount != chunk.getLimit())
                        {
                            throw new IllegalStateException(
                                    String.format("Not all bytes for chunk of %s could be read from %s", chunk.getSource(), localPath));
                        }

                        if (transactional)
                        {
                            requestChunks[i] =
                                    new Chunk(chunk.getOwner(), toServerPathString(chunkServerPath), chunk.getOffset(), chunk.getLimit(), bytes);
                        } else
                        {
                        requestChunks[i] =
                                new Chunk(chunk.getOwner(), toServerPathString(TemporaryPathUtil.getTwinTemporaryPath(chunkServerPath)),
                                        chunk.getOffset(),
                                        chunk.getLimit(), bytes);
                        }
                    }
                    i++;
                }

                if (!uploadChunks(afsClient, requestChunks))
                {
                    throw new RuntimeException("Failure uploading chunks");
                }

                for (Chunk chunk : chunksToBeRequested)
                {
                    Path chunkServerPath = Path.of(chunk.getSource());
                    Path relativeServerPath = destinationPath.toAbsolutePath().relativize(chunkServerPath);
                    Path localPath;
                    if (Files.isDirectory(sourcePath))
                    {
                        localPath = sourcePath.resolve(relativeServerPath);
                    } else
                    {
                        localPath = sourcePath;
                    }
                    boolean completed =
                            currentsAndTotals.updateCurrentAmountsAndCheckCompletion(afsClient, localPath, ownerId, chunkServerPath.toAbsolutePath(),
                                    chunk.getLimit(), transactional);
                    if (completed && !transactional)
                    {
                        // create a snapshot of the file before updating it
                        Optional<File> serverFile = getServerFilePresence(afsClient, chunk.getOwner(), chunk.getSource(), cache);
                        if(serverFile.isPresent())
                        {
                            afsClient.snapshot(chunk.getOwner(), chunk.getSource());
                        }

                        // copy data from the twin file to the updated file
                        String twinAbsoluteServerPath = toServerPathString(TemporaryPathUtil.getTwinTemporaryPath(chunkServerPath));
                        afsClient.copy(chunk.getOwner(), twinAbsoluteServerPath, chunk.getOwner(), chunk.getSource());

                        // delete the twin file
                        deleteServerRegularFile(afsClient, chunk.getOwner(), twinAbsoluteServerPath, false);
                    }
                    transferMonitorListener.add(localPath, chunkServerPath, chunk.getLimit(), completed);
                }

            } catch (Exception e)
            {
                transferMonitorListener.failed(e);
            } finally
            {
                maxThreadLimitSemaphore.release();
                asyncTransferRunnables.remove(this);
            }
        }
    }

    private static long doPreliminaryScanToInitializeUploadMonitor(AfsClient afsClient, TransferMonitorListener transferMonitor, Path sourcePath)
            throws IOException
    {
        PathIterator pathIterator = new PathIterator(sourcePath);
        long totalSize = 0;
        for (Path file : pathIterator)
        {
            if (Files.isRegularFile(file))
            {
                totalSize += Files.size(file);
            }
        }
        transferMonitor.init(System.currentTimeMillis(), totalSize);
        return totalSize;
    }

    private static Cache doPreliminaryScanToInitializeCache(AfsClient afsClient, final @NonNull Path sourcePath,
            final @NonNull String destinationOwner, Path destinationPath, final File destinationInfo) throws Exception
    {
        PathIterator pathIterator = new PathIterator(sourcePath);
        Cache cache = new Cache();

        for (Path file : pathIterator)
        {
            String absoluteServerPath = computeAbsoluteServerPath(sourcePath, destinationPath, file, destinationInfo);
            getServerFilePresence(afsClient, destinationOwner, absoluteServerPath, cache);
        }

        return cache;
    }

    @SneakyThrows
    synchronized public static Boolean uploadChunks(@NonNull AfsClient afsClient, @NonNull Chunk[] requestChunks)
    {
        return afsClient.write(requestChunks);
    }

    private static class PathIterator implements Iterable<Path>
    {

        private final Path root;

        public PathIterator(Path root)
        {
            this.root = root;
        }

        @Override
        public Iterator<Path> iterator()
        {
            return new Iterator<Path>()
            {
                private Queue<Path> todo = new LinkedList<>(List.of(root));

                @Override
                public boolean hasNext()
                {
                    return !todo.isEmpty();
                }

                @SneakyThrows
                @Override
                public Path next()
                {
                    Path head = todo.remove();
                    if (Files.isDirectory(head))
                    {
                        try (Stream<Path> pathStream = Files.list(head)) {
                            List<Path> dirPaths = pathStream
                                    .filter(entry -> !TemporaryPathUtil.isTwinTemporaryPath(entry))
                                    .filter(entry -> !isAfsHiddenFile(entry))
                                    .collect(Collectors.toList());
                            todo.addAll(dirPaths);
                        }
                    }
                    return head;
                }
            };
        }

    }

    static class UploadCurrentsAndTotals
    {
        private final @NonNull Map<Path, Long> lastModificationTimestamps = new HashMap<>();

        private final @NonNull Map<Path, String> checksums = new HashMap<>();

        private final @NonNull Map<Path, Long> totals = new HashMap<>();

        private final @NonNull Map<Path, Long> currents = new HashMap<>();

        private final @NonNull Set<Path> concurrentModification = new HashSet<>();

        synchronized boolean updateCurrentAmountsAndCheckCompletion(@NonNull AfsClient afsClient, @NonNull Path fromPath, @NonNull String ownerId,
                @NonNull Path toPath, int writtenByteCount, boolean transactional) throws Exception
        {
            Long expectedSrcLastModification = lastModificationTimestamps.get(fromPath);
            if (expectedSrcLastModification == null || Files.getLastModifiedTime(fromPath).toMillis() != expectedSrcLastModification)
            {
                concurrentModification.add(fromPath);
                return false;
            }

            Long current = currents.get(fromPath);
            if (current == null)
            {
                current = 0L;
            }
            current += writtenByteCount;
            currents.put(fromPath, current);

            Long total = totals.get(fromPath);
            if (total != null && current >= total)
            {
                totals.remove(fromPath);
                currents.remove(fromPath);
                lastModificationTimestamps.remove(fromPath);
                String expectedChecksum = checksums.remove(fromPath);

                if (transactional)
                {
                    // TODO check only hash
                    return true;
                } else
                {
                    Optional<File> remoteTmpToFile =
                            getServerFilePresence(afsClient, ownerId, toServerPathString(TemporaryPathUtil.getTwinTemporaryPath(toPath)));
                    if (remoteTmpToFile.isPresent() &&
                            remoteTmpToFile.get().getSize() != null &&
                            (long) remoteTmpToFile.get().getSize() == total &&
                            Files.exists(fromPath) && Files.size(fromPath) == total &&
                            Files.getLastModifiedTime(fromPath).toMillis() == expectedSrcLastModification &&
                            afsClient.hash(ownerId, remoteTmpToFile.get().getPath()).equals(expectedChecksum)
                    )
                    {
                        return true;
                    } else
                    {
                        concurrentModification.add(fromPath);
                        return false;
                    }
                }
            } else
            {
                return false;
            }
        }

        synchronized void startTracking(@NonNull Path from, long size, long lastModificationTs) throws Exception
        {
            lastModificationTimestamps.put(from, lastModificationTs);
            totals.put(from, size);
            checksums.put(from, AfsClientDownloadHelper.computeLocalMD5(from));
        }

        synchronized boolean isAllCompleted()
        {
            return totals.isEmpty() && concurrentModification.isEmpty();
        }

        synchronized boolean skipForConcurrentModification(@NonNull Path fromPath)
        {
            return concurrentModification.contains(fromPath);
        }
    }

    static class Cache
    {

        private final Map<String, File> fileMap = new HashMap<>();

        public void putFile(String owner, String path, File file)
        {
            fileMap.put(owner + path, file);
        }

        public boolean hasFile(String owner, String path)
        {
            return fileMap.containsKey(owner + path);
        }

        public File getFile(String owner, String path)
        {
            return fileMap.get(owner + path);
        }
    }

    public static String toServerPathString(@NonNull Path path)
    {
        StringBuilder stringBuilder = new StringBuilder();
        path.forEach(segment -> stringBuilder.append(AFS_SERVER_PATH_SEPARATOR).append(segment.toString()));
        if (stringBuilder.isEmpty())
        {
            stringBuilder.append(AFS_SERVER_PATH_SEPARATOR);
        }
        return stringBuilder.toString();
    }

    public static final String HIDDEN_AFS_DIRECTORY = ".afs";

    public static boolean isAfsHiddenFile(@NonNull Path path)
    {
        return StreamSupport.stream(path.toAbsolutePath().normalize().spliterator(), false).anyMatch(
                pathSegment -> pathSegment.toString().equals(HIDDEN_AFS_DIRECTORY)
        );
    }
}