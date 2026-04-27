package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import ch.ethz.sis.afssftp.util.OpenBISClientUtil;
import ch.ethz.sis.afssftp.util.OpenBISListUtil;
import lombok.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class AfsFileChannel extends FileChannel {
    private final @NonNull String entityId;
    private final @NonNull String afsPath;

    private final @NonNull OpenBISUser openBISUser;
    private final @NonNull OpenBISClientUtil clientUtil;
    private final @NonNull OpenBISListUtil listUtil;

    private final @NonNull AtomicLong position;
    private final boolean readOpenOption;
    private final boolean writeOpenOption;

    public AfsFileChannel(
            @NonNull String entityId,
            @NonNull String afsPath,
            @NonNull OpenBISUser openBISUser,
            long initialPosition,
            boolean readOpenOption,
            boolean writeOpenOption) {
        this.entityId = entityId;
        this.afsPath = afsPath;

        this.openBISUser = openBISUser;
        this.clientUtil = new OpenBISClientUtil();
        this.listUtil = new OpenBISListUtil(openBISUser);

        this.position = new AtomicLong(initialPosition);
        this.readOpenOption = readOpenOption;
        this.writeOpenOption = writeOpenOption;
    }

    @Override
    protected void implCloseChannel() throws IOException {
        //No necessary action
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (readOpenOption) {
            if (dst.remaining() > 0) {
                long size = size();
                long pos = position.get();

                if (pos >= size) {
                    return -1;
                } else {
                    long readableBytes = size - pos;
                    byte[] bytes;
                    try {
                        bytes = clientUtil.getAfsClient(openBISUser).read(
                                entityId,
                                afsPath,
                                position.get(),
                                IntStream.of(
                                        dst.remaining(),
                                        (int) Long.min(readableBytes, Integer.MAX_VALUE),
                                        AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES).min().getAsInt()
                        );
                    } catch (Exception e) {
                        throw new IOException("Error reading from AFS");
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
    public long read(ByteBuffer[] dsts, int dstsOffset, int dstsCount) throws IOException {
        if (readOpenOption) {
            long remaining = IntStream.range(dstsOffset, dstsOffset + dstsCount)
                    .mapToLong( index -> dsts[index].remaining() ).sum();
            if (remaining > 0) {
                long size = size();
                long pos = position.get();

                if (pos >= size) {
                    return -1;
                } else {
                    long readableBytes = size - pos;
                    byte[] bytes;
                    try {
                        bytes = clientUtil.getAfsClient(openBISUser).read(
                                entityId,
                                afsPath,
                                position.get(),
                                IntStream.of(
                                        (int) Long.min(remaining, Integer.MAX_VALUE),
                                        (int) Long.min(readableBytes, Integer.MAX_VALUE),
                                        AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES).min().getAsInt()
                        );
                    } catch (Exception e) {
                        throw new IOException("Error reading from AFS");
                    }
                    for (int index = dstsOffset, bytesOffset = 0;
                         index<dstsCount && bytesOffset < bytes.length;
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
    public int write(ByteBuffer src) throws IOException {
        if (writeOpenOption) {
            if (src.remaining() > 0) {
                long size = size();
                long pos = position.get();

                if (pos > size) {
                    fillWithZero(size, pos);
                }
                int bytesToWrite = (int) Long.min(
                        AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES,
                        src.remaining()
                );

                byte[] bytes = new byte[bytesToWrite];
                src.get(bytes);

                try {
                    if ( !clientUtil.getAfsClient(openBISUser).write(
                            entityId,
                            afsPath,
                            pos,
                            bytes) ) {
                        throw new IOException("Error writing to AFS");
                    }
                } catch (Exception e) {
                    throw new IOException("Error writing to AFS");
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
    public long write(ByteBuffer[] srcs, int srcsOffset, int srcsCount) throws IOException {
        if (writeOpenOption) {
            long remaining = IntStream.range(srcsOffset, srcsOffset + srcsCount)
                    .mapToLong( index -> srcs[index].remaining() ).sum();
            if (remaining > 0) {
                long size = size();
                long pos = position.get();

                if (pos > size) {
                    fillWithZero(size, pos);
                }
                int bytesToWrite = (int) Long.min(
                        AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES,
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
                    if ( !clientUtil.getAfsClient(openBISUser).write(
                            entityId,
                            afsPath,
                            pos,
                            bytes) ) {
                        throw new IOException("Error writing to AFS");
                    }
                } catch (Exception e) {
                    throw new IOException("Error writing to AFS");
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
    public FileChannel position(long newPosition) throws IOException {
        position.set(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return listUtil.getDefaultAfsFileAttributes(
                entityId, afsPath
        ).get().getSize();
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        if (writeOpenOption) {
            try {
                if ( !clientUtil.getAfsClient(openBISUser).truncate(entityId, afsPath, size) ) {
                    throw new IOException("Error truncating AFS-file");
                }
            } catch (Exception e) {
                throw new IOException("Error truncating AFS-file");
            }
            if (position.get() > size) {
                position.set(size);
            }
            return this;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void force(boolean force) throws IOException {
        //No necessary action
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel dst) throws IOException {
        if (readOpenOption) {
            int maxBytesToBeRead = IntStream.of(
                    (int) Long.min(count, Integer.MAX_VALUE),
                    AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES).min().getAsInt();

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
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        if (writeOpenOption) {
            int maxBytesToBeWrite = IntStream.of(
                    (int) Long.min(count, Integer.MAX_VALUE),
                    AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES).min().getAsInt();

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
            if (dst.remaining() > 0) {
                long size = size();

                if (position >= size) {
                    return -1;
                } else {
                    long readableBytes = size - position;
                    byte[] bytes;
                    try {
                        bytes = clientUtil.getAfsClient(openBISUser).read(
                                entityId,
                                afsPath,
                                position,
                                IntStream.of(
                                        dst.remaining(),
                                        (int) Long.min(readableBytes, Integer.MAX_VALUE),
                                        AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES).min().getAsInt()
                        );
                    } catch (Exception e) {
                        throw new IOException("Error reading from AFS");
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
    public int write(ByteBuffer src, long position) throws IOException {
        if (writeOpenOption) {
            if (src.remaining() > 0) {
                long size = size();

                if (position > size) {
                    fillWithZero(size, position);
                }
                int bytesToWrite = (int) Long.min(
                        AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES,
                        src.remaining()
                );

                byte[] bytes = new byte[bytesToWrite];
                src.get(bytes);

                try {
                    if ( !clientUtil.getAfsClient(openBISUser).write(
                            entityId,
                            afsPath,
                            position,
                            bytes) ) {
                        throw new IOException("Error writing to AFS");
                    }
                } catch (Exception e) {
                    throw new IOException("Error writing to AFS");
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
                    AfsClient.DEFAULT_PACKAGE_SIZE_IN_BYTES,
                    endExclusive - index
            );
            try {
                if ( !clientUtil.getAfsClient(openBISUser).write(
                        entityId,
                        afsPath,
                        index,
                        new byte[bytesToWrite]) ) {
                    throw new IOException("Error writing to AFS");
                }
            } catch (Exception e) {
                throw new IOException("Error writing to AFS");
            }
            index = index + bytesToWrite;
        }
    }
}
