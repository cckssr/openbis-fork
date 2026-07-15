package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.conf.Parameters;
import ch.ethz.sis.afssftp.util.OpenBISClientUtil;
import ch.ethz.sis.afssftp.util.SftpListUtil;
import lombok.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class AfsFileChannel extends FileChannel {
    private final @NonNull String entityId;
    private final @NonNull String afsPath;

    private final @NonNull User user;
    private final @NonNull OpenBISClientUtil clientUtil;
    private final @NonNull SftpListUtil listUtil;

    private final @NonNull AtomicLong position;
    private final boolean readOpenOption;
    private final boolean writeOpenOption;

    WriteBuffer writeBuffer = new WriteBuffer();
    ReadCache readCache = new ReadCache();
    SizeCache sizeCache = new SizeCache();

    static final ConcurrentHashMap<User, Integer> globalFileChannelPerSessionCounters = new ConcurrentHashMap<>();
    final ConcurrentHashMap<User, Integer> fileChannelPerSessionCounters;

    private final static ScheduledThreadPoolExecutor cacheTimer = new ScheduledThreadPoolExecutor(4);

    public AfsFileChannel(
            @NonNull String entityId,
            @NonNull String afsPath,
            @NonNull User user,
            long initialPosition,
            boolean readOpenOption,
            boolean writeOpenOption) {
        this(
            entityId,
            afsPath,
            user,
            initialPosition,
            readOpenOption,
            writeOpenOption,
            globalFileChannelPerSessionCounters
        );
    }

    AfsFileChannel(
            @NonNull String entityId,
            @NonNull String afsPath,
            @NonNull User user,
            long initialPosition,
            boolean readOpenOption,
            boolean writeOpenOption,
            ConcurrentHashMap<User, Integer> fileChannelPerSessionCounters) {

        this.fileChannelPerSessionCounters = fileChannelPerSessionCounters;
        incrementFileChannelCounter(user);

        this.entityId = entityId;
        this.afsPath = afsPath;

        this.user = user;
        this.clientUtil = new OpenBISClientUtil();
        this.listUtil = new SftpListUtil(user);

        this.position = new AtomicLong(initialPosition);
        this.readOpenOption = readOpenOption;
        this.writeOpenOption = writeOpenOption;
    }

    @Override
    protected void implCloseChannel() throws IOException {
        writeBuffer.close();
        readCache.close();
        decrementFileChannelCounter(user);
    }

    @Override
    synchronized public int read(ByteBuffer dst) throws IOException {
        if (readOpenOption) {
            int destinationBufferSpace = dst.remaining();
            if (destinationBufferSpace > 0) {
                long size = size();
                long pos = position.get();

                if (pos >= size) {
                    return -1;
                } else {
                    long readableBytes = size - pos;
                    int readSize = IntStream.of(
                        (int) Long.min(destinationBufferSpace, Integer.MAX_VALUE),
                        (int) Long.min(readableBytes, Integer.MAX_VALUE),
                        getAfsClientMaxPackageSize()).min().getAsInt();

                    byte[] bytes;
                    try {
                        bytes = readData(pos, readSize);
                    } catch (Exception e) {
                        throw new IOException("Error reading from AFS", e);
                    }
                    dst.put(bytes);
                    position.set(pos + bytes.length);
                    return bytes.length;
                }
            } else {
                return 0;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    synchronized public long read(ByteBuffer[] dsts, int dstsOffset, int dstsCount) throws IOException {
        if (readOpenOption) {
            long destinationBufferSpace = IntStream.range(dstsOffset, dstsOffset + dstsCount)
                    .mapToLong( index -> dsts[index].remaining() ).sum();
            if (destinationBufferSpace > 0) {
                long size = size();
                long pos = position.get();

                if (pos >= size) {
                    return -1;
                } else {
                    long readableBytes = size - pos;
                    int readSize = IntStream.of(
                            (int) Long.min(destinationBufferSpace, Integer.MAX_VALUE),
                            (int) Long.min(readableBytes, Integer.MAX_VALUE),
                            getAfsClientMaxPackageSize()).min().getAsInt();

                    byte[] bytes;

                    try {
                        bytes = readData(pos, readSize);
                    } catch (Exception e) {
                        throw new IOException("Error reading from AFS", e);
                    }
                    for (int index = dstsOffset, bytesOffset = 0;
                         index<dstsOffset+dstsCount && bytesOffset < bytes.length;
                         index++
                    ) {
                        ByteBuffer src = dsts[index];
                        int remainingBytes = bytes.length - bytesOffset;
                        int transferredBytes = Integer.min(remainingBytes, src.remaining());
                        src.put(bytes, bytesOffset, transferredBytes);
                        bytesOffset += transferredBytes;
                    }
                    position.set(pos + bytes.length);
                    return bytes.length;
                }
            } else {
                return 0;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    synchronized public int write(ByteBuffer src) throws IOException {
        if (writeOpenOption) {
            if (src.remaining() > 0) {
                long size = size();
                long pos = position.get();

                if (pos > size) {
                    writeBuffer.flush(null);
                    long realSize = internalRealSize();
                    if (pos > realSize) {
                        fillWithZero(realSize, pos);
                    }
                }
                int bytesToWrite = (int) Long.min(
                        getAfsClientMaxPackageSize(),
                        src.remaining()
                );

                byte[] bytes = new byte[bytesToWrite];
                src.get(bytes);

                try {
                    writeData(bytes, pos);
                } catch (Exception e) {
                    throw new IOException("Error writing to AFS", e);
                }
                position.set(pos + bytesToWrite);
                return bytes.length;
            } else {
                return 0;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    synchronized public long write(ByteBuffer[] srcs, int srcsOffset, int srcsCount) throws IOException {
        if (writeOpenOption) {
            long remaining = IntStream.range(srcsOffset, srcsOffset + srcsCount)
                    .mapToLong( index -> srcs[index].remaining() ).sum();
            if (remaining > 0) {
                long size = size();
                long pos = position.get();

                if (pos > size) {
                    writeBuffer.flush(null);
                    long realSize = internalRealSize();
                    if (pos > realSize) {
                        fillWithZero(realSize, pos);
                    }
                }
                int bytesToWrite = (int) Long.min(
                        getAfsClientMaxPackageSize(),
                        remaining
                );

                byte[] bytes = new byte[bytesToWrite];

                for(int index = srcsOffset, bytesOffset = 0;
                    index < srcsOffset + srcsCount && bytesOffset < bytes.length;
                    index++) {
                    int movedBytes = Integer.min(srcs[index].remaining(), bytes.length - bytesOffset);
                    srcs[index].get(bytes, bytesOffset, movedBytes);
                    bytesOffset = bytesOffset + movedBytes;
                }

                try {
                    writeData(bytes, pos);
                } catch (Exception e) {
                    throw new IOException("Error writing to AFS", e);
                }
                position.set(pos + bytesToWrite);
                return bytes.length;
            } else {
                return 0;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long position() throws IOException {
        return position.get();
    }

    @Override
    synchronized public FileChannel position(long newPosition) throws IOException {
        position.set(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return sizeCache.getCachedSize();
    }

    long internalRealSize() throws IOException {
        return listUtil.getAfsFilePresence(
                entityId, afsPath
        ).map(File::getSize).orElseThrow();
    }

    @Override
    synchronized public FileChannel truncate(long size) throws IOException {
        if (writeOpenOption) {
            writeBuffer.flush(null);
            try {
                if ( !clientUtil.getAfsClient(user).truncate(entityId, afsPath, size) ) {
                    throw new IOException("Error truncating AFS-file");
                }
            } catch (Exception e) {
                throw new IOException("Error truncating AFS-file", e);
            }
            if (position.get() > size) {
                position.set(size);
            }

            if (readOpenOption) {
                readCache.clear();
            }
            sizeCache.clear();
            return this;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    synchronized public void force(boolean force) throws IOException {
        writeBuffer.flush(null);
        readCache.clear();
        sizeCache.clear();
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel dst) throws IOException {
        if (readOpenOption) {
            int maxBytesToBeRead = IntStream.of(
                    (int) Long.min(count, Integer.MAX_VALUE),
                    getAfsClientMaxPackageSize()).min().getAsInt();

            ByteBuffer byteBuffer = ByteBuffer.allocate(maxBytesToBeRead);

            int ret = read(byteBuffer, position);
            byteBuffer.flip();
            while (byteBuffer.remaining() > 0) {
                dst.write(byteBuffer);
            }
            return ret;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    synchronized public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        if (writeOpenOption) {
            int maxBytesToBeWrite = IntStream.of(
                    (int) Long.min(count, Integer.MAX_VALUE),
                    getAfsClientMaxPackageSize()).min().getAsInt();

            ByteBuffer byteBuffer = ByteBuffer.allocate(maxBytesToBeWrite);

            int ret = write(byteBuffer, position);
            byteBuffer.flip();
            while (byteBuffer.remaining() > 0) {
                src.read(byteBuffer);
            }
            return ret;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        if (readOpenOption) {
            int destinationBufferSpace = dst.remaining();
            if (destinationBufferSpace > 0) {
                long size = size();

                if (position >= size) {
                    return -1;
                } else {
                    long readableBytes = size - position;
                    int readSize = IntStream.of(
                            (int) Long.min(destinationBufferSpace, Integer.MAX_VALUE),
                            (int) Long.min(readableBytes, Integer.MAX_VALUE),
                            getAfsClientMaxPackageSize()).min().getAsInt();

                    byte[] bytes;
                    try {
                        bytes = readData(position, readSize);
                    } catch (Exception e) {
                        throw new IOException("Error reading from AFS", e);
                    }
                    dst.put(bytes);
                    return bytes.length;
                }
            } else {
                return 0;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    synchronized public int write(ByteBuffer src, long position) throws IOException {
        if (writeOpenOption) {
            if (src.remaining() > 0) {
                long size = size();

                if (position > size) {
                    writeBuffer.flush(null);
                    long realSize = internalRealSize();
                    if (position > realSize) {
                        fillWithZero(realSize, position);
                    }
                }
                int bytesToWrite = (int) Long.min(
                        getAfsClientMaxPackageSize(),
                        src.remaining()
                );

                byte[] bytes = new byte[bytesToWrite];
                src.get(bytes);

                try {
                    writeData(bytes, position);
                } catch (Exception e) {
                    throw new IOException("Error writing to AFS", e);
                }
                return bytes.length;
            } else {
                return 0;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }

    void fillWithZero(long beginInclusive, long endExclusive) throws IOException {
        long index = beginInclusive;
        while ( index < endExclusive ) {
            int bytesToWrite = (int) Long.min(
                    getAfsClientMaxPackageSize(),
                    endExclusive - index
            );
            try {
                if ( !clientUtil.getAfsClient(user).write(
                        entityId,
                        afsPath,
                        index,
                        new byte[bytesToWrite]) ) {
                    throw new IOException("Error writing to AFS");
                }
            } catch (Exception e) {
                throw new IOException("Error writing to AFS", e);
            }
            index = index + bytesToWrite;
        }
        if (readOpenOption) {
            readCache.clear();
        }
    }

    int getAfsClientMaxPackageSize() {
        return Parameters.getMaxAfsClientChunkSize();
    }

    class WriteBuffer {
        private long offset = 0L;
        private List<byte[]> bufferedBytes = null;
        private int accSize = 0;

        private long timestamp;

        private ScheduledFuture<Void> flushingCallback = null;

        synchronized void addChunk(long position, byte[] bytes) throws IOException {
            if (bufferedBytes != null && System.currentTimeMillis() < timestamp + Parameters.getAfsCacheTimeoutMillis() &&
                    position == offset + accSize &&
                    accSize + bytes.length < getAfsClientMaxPackageSize()
            ) {
                bufferedBytes.add(bytes);
                accSize = accSize + bytes.length;
            } else {
                if (bufferedBytes != null) {
                    flush(null);
                }
                offset = position;
                bufferedBytes = new ArrayList<>(Collections.singletonList(bytes));
                accSize = bytes.length;
                timestamp = System.currentTimeMillis();
                if (flushingCallback == null) {
                    long callbackTsCheck = timestamp;
                    flushingCallback = cacheTimer.schedule( () -> {
                        try {
                            flush(callbackTsCheck); return null;
                        } catch (Exception e) { throw new RuntimeException(e); }
                    }, Parameters.getAfsCacheTimeoutMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }

        synchronized void flush(Long callBackTsCheck) throws IOException {
            if (callBackTsCheck != null) {
                flushingCallback = null;
                if (callBackTsCheck != timestamp) {
                    //Early return: write-buffer was already flushed before callback
                    return;
                }
            }

            if (bufferedBytes != null) {
                byte[] accBytes = new byte[accSize];
                int i = 0;
                for(byte[] bytes : bufferedBytes) {
                    System.arraycopy(bytes, 0, accBytes, i, bytes.length);
                    i = i + bytes.length;
                }
                try {
                    if ( !clientUtil.getAfsClient(user).write(
                            entityId,
                            afsPath,
                            offset,
                            accBytes) ) {
                        throw new IOException("Error writing to AFS");
                    }
                } catch (Exception e) {
                    throw new IOException("Error writing to AFS", e);
                }
                bufferedBytes = null;
                accSize = 0;
                sizeCache.clear();
            }
        }

        synchronized Optional<Long> getVirtualUpperBoundary() {
            if (bufferedBytes != null) {
                return Optional.of(offset + accSize);
            } else {
                return Optional.empty();
            }
        }

        synchronized void close() throws IOException {
            if (flushingCallback != null) {
                flushingCallback.cancel(false);
            }
            flush(null);
        }
    }

    class ReadCache {
        private long offset = 0L;
        private byte[] cache = null;
        private long timestamp = 0L;

        private final ScheduledFuture<?> cacheCleaningTask =
            cacheTimer.scheduleWithFixedDelay(
                () -> {
                    if (timestamp < System.currentTimeMillis() - Parameters.getAfsCacheTimeoutMillis()) {
                        clear();
                    }
                }, 1, 1, TimeUnit.SECONDS
            );

        synchronized byte[] getCachedData(long position, int size) throws IOException {
            if (cache != null &&
                    System.currentTimeMillis() < timestamp + Parameters.getAfsCacheTimeoutMillis() &&
                    offset <= position &&
                    position + size <= offset + cache.length
            ) {
                return Arrays.copyOfRange(cache, (int) (position - offset), (int) (position - offset) + size);
            } else {
                refreshCache(position);
                return Arrays.copyOfRange(cache, (int) (position - offset), (int) (position - offset) + size);
            }
        }

        synchronized void refreshCache(long position) throws IOException {
            if (writeOpenOption) {
                writeBuffer.flush(null);
            }
            long readableBytes = size() - position;
            offset = position;
            cache = clientUtil.getAfsClient(user).read(
                    entityId,
                    afsPath,
                    position,
                    IntStream.of(
                            (int) Long.min(readableBytes, Integer.MAX_VALUE),
                            getAfsClientMaxPackageSize()).min().getAsInt()
            );
            timestamp = System.currentTimeMillis();
        }

        synchronized void clear() {
            offset = 0L;
            cache = null;
        }

        synchronized void close() {
            cacheCleaningTask.cancel(false);
            clear();
        }
    }

    class SizeCache {
        private Long fileSize;
        private long timestamp = 0L;

        synchronized long getCachedSize() throws IOException {
            long fileSizeWithoutBufferedWrites;
            if (fileSize != null &&
                    System.currentTimeMillis() < timestamp + Parameters.getAfsCacheTimeoutMillis()
            ) {
                fileSizeWithoutBufferedWrites = fileSize;
            } else {
                refreshCache();
                fileSizeWithoutBufferedWrites = Objects.requireNonNull(fileSize);
            }
            return Long.max(
                    fileSizeWithoutBufferedWrites,
                    writeBuffer.getVirtualUpperBoundary().orElse(0L)
            );
        }

        synchronized void refreshCache() throws IOException {
            fileSize = internalRealSize();
            timestamp = System.currentTimeMillis();
        }

        synchronized void trackNewWrite(long beginInclusive, long endExclusive) {
            if (fileSize != null) {
                fileSize = Long.max(fileSize, endExclusive);
            }
        }

        synchronized void clear() {
            fileSize = null;
        }
    }

    byte[] readData(long pos, int readSize) throws IOException {
        if (writeOpenOption) {
            writeBuffer.flush(null);
        }

        byte[] bytes;
        if (Parameters.isSkipAfsChannelCaching()) {
            bytes = clientUtil.getAfsClient(user).read(
                    entityId,
                    afsPath,
                    pos,
                    readSize
            );
        } else {
            bytes = readCache.getCachedData(pos, readSize);
        }

        return bytes;
    }

    void writeData(byte[] bytes, long pos) throws IOException {
        if (readOpenOption) {
            readCache.clear();
        }

        if (Parameters.isSkipAfsChannelCaching()) {
            if ( !clientUtil.getAfsClient(user).write(
                    entityId,
                    afsPath,
                    pos,
                    bytes)
            ) {
                throw new IOException("Error writing to AFS");
            }
        } else{
            writeBuffer.addChunk(pos, bytes);
        }

        sizeCache.trackNewWrite(pos, pos + bytes.length);
    }

    void incrementFileChannelCounter(@NonNull User user) {
        fileChannelPerSessionCounters.compute(user, (key, currentValue) -> {
            if (currentValue != null) {
                int newValue = currentValue + 1;
                if (newValue > Parameters.getMaxFileChannelsPerSession()) {
                    throw new RuntimeException(new IOException("Too many AFS-file-channels"));
                }
                return newValue;
            } else {
                return 1;
            }
        });
    }

    void decrementFileChannelCounter(@NonNull User user) {
        fileChannelPerSessionCounters.compute(user, (key, currentValue) -> {
            if (currentValue != null) {
                int newValue = currentValue - 1;
                return newValue > 0 ? newValue : null;
            } else {
                return null;
            }
        });
    }

    ///////////////// For unit tests /////////////////
    // For unit-tests only
    AfsFileChannel(
            @NonNull String entityId,
            @NonNull String afsPath,
            @NonNull User user,
            @NonNull OpenBISClientUtil clientUtil,
            @NonNull SftpListUtil listUtil,
            @NonNull AtomicLong position,
            boolean readOpenOption,
            boolean writeOpenOption
    ) {
        this.entityId = entityId;
        this.afsPath = afsPath;
        this.user = user;
        this.clientUtil = clientUtil;
        this.listUtil = listUtil;
        this.position = position;
        this.readOpenOption = readOpenOption;
        this.writeOpenOption = writeOpenOption;
        this.fileChannelPerSessionCounters = globalFileChannelPerSessionCounters;
    }

    // For unit-tests only
    void setMockWriteBuffer(WriteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    // For unit-tests only
    void setMockReadCache(ReadCache readCache) {
        this.readCache = readCache;
    }

    // For unit-tests only
    void setMockSizeCache(SizeCache sizeCache) {
        this.sizeCache = sizeCache;
    }
}
