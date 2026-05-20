package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.StaticInitializer;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.helpers.TestHelper;
import ch.ethz.sis.afssftp.util.OpenBISClientUtil;
import ch.ethz.sis.afssftp.util.SftpListUtil;
import ch.ethz.sis.openbis.generic.OpenBIS;
import junit.framework.TestCase;
import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertArrayEquals;

public class AfsFileChannelTest extends TestCase {

    static {
        StaticInitializer.initialize();
    }

    public void testImplCloseChannel() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        long position = 0L;
        boolean readOption = true;
        boolean writeOption = true;

        AfsFileChannel afsFileChannel = new AfsFileChannel(
                entityId,
                afsPath,
                user,
                clientUtil,
                listUtil,
                new AtomicLong(position),
                readOption,
                writeOption
        );

        afsFileChannel.implCloseChannel();
    }

    public void testRead() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean readOption : List.of(false, true)) {
            boolean writeOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (readOption) {
                ByteBuffer fakeContent = TestHelper.getRandomByteBuffer(30000);
                Mockito.doAnswer((invocation) -> {
                    int offset = (int) (long) invocation.getArgument(2);
                    int limit = invocation.getArgument(3);
                    return Arrays.copyOfRange(fakeContent.array(), offset, offset + limit);
                }).when(afsClient).read(
                        Mockito.eq(entityId),
                        Mockito.eq(afsPath),
                        Mockito.anyLong(),
                        Mockito.anyInt()
                );
                Mockito.doReturn(30000L).when(afsFileChannel).size();

                assertEquals(10000L, afsFileChannel.position());
                ByteBuffer byteBuffer = ByteBuffer.allocate(10000);
                byteBuffer.position(10000);
                assertEquals(0, afsFileChannel.read(byteBuffer));
                assertEquals(10000L, afsFileChannel.position());
                byteBuffer.position(10);
                byteBuffer.limit(9990);
                afsFileChannel.position(30000);
                assertEquals(-1, afsFileChannel.read(byteBuffer));
                afsFileChannel.position(30005);
                assertEquals(-1, afsFileChannel.read(byteBuffer));

                Mockito.clearInvocations(afsFileChannel);
                afsFileChannel.position(5);
                byteBuffer.position(100);
                byteBuffer.limit(9900);
                assertEquals(9800, afsFileChannel.read(byteBuffer));
                assertArrayEquals(
                        Arrays.copyOfRange(fakeContent.array(), 5, 9805),
                        Arrays.copyOfRange(byteBuffer.array(), 100, 9900));
                Mockito.verify(afsClient, Mockito.times(1)).read(
                        entityId,
                        afsPath,
                        5L,
                        9800
                );
                assertEquals(9805, afsFileChannel.position());
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.read(ByteBuffer.allocate(1000));
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testReadWithMultipleBuffers() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean readOption : List.of(false, true)) {
            boolean writeOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (readOption) {
                ByteBuffer fakeContent = TestHelper.getRandomByteBuffer(30000);
                Mockito.doAnswer((invocation) -> {
                    int offset = (int) (long) invocation.getArgument(2);
                    int limit = invocation.getArgument(3);
                    return Arrays.copyOfRange(fakeContent.array(), offset, offset + limit);
                }).when(afsClient).read(
                        Mockito.eq(entityId),
                        Mockito.eq(afsPath),
                        Mockito.anyLong(),
                        Mockito.anyInt()
                );
                Mockito.doReturn(30000L).when(afsFileChannel).size();

                assertEquals(10000L, afsFileChannel.position());
                ByteBuffer[] byteBuffers = new ByteBuffer[] {
                        ByteBuffer.allocate(5000),
                        ByteBuffer.allocate(5000),
                        ByteBuffer.allocate(5000),
                        ByteBuffer.allocate(5000)
                };
                byteBuffers[1].position(5000);
                byteBuffers[2].position(5000);
                assertEquals(0, afsFileChannel.read(byteBuffers, 1, 2));
                assertEquals(10000L, afsFileChannel.position());
                byteBuffers[1].position(10);
                byteBuffers[2].position(0);
                byteBuffers[2].limit(4990);
                afsFileChannel.position(30000);
                assertEquals(-1, afsFileChannel.read(byteBuffers, 1, 2));
                afsFileChannel.position(30005);
                assertEquals(-1, afsFileChannel.read(byteBuffers, 1, 2));

                Mockito.clearInvocations(afsFileChannel);
                afsFileChannel.position(5);
                byteBuffers[1].position(100);
                byteBuffers[2].position(0);
                byteBuffers[2].limit(4900);
                assertEquals(9800, afsFileChannel.read(byteBuffers, 1, 2));
                assertArrayEquals(
                        Arrays.copyOfRange(fakeContent.array(), 5, 9805),
                        ByteBuffer.allocate(9800)
                            .put(Arrays.copyOfRange(byteBuffers[1].array(), 100, 5000))
                            .put(Arrays.copyOfRange(byteBuffers[2].array(), 0, 4900))
                            .array()
                        );
                Mockito.verify(afsClient, Mockito.times(1)).read(
                        entityId,
                        afsPath,
                        5L,
                        9800
                );
                assertEquals(9805, afsFileChannel.position());
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.read(new ByteBuffer[]{
                            ByteBuffer.allocate(1000),
                            ByteBuffer.allocate(1000)
                    });
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testReadWithPosition() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean readOption : List.of(false, true)) {
            boolean writeOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (readOption) {
                ByteBuffer fakeContent = TestHelper.getRandomByteBuffer(30000);
                Mockito.doAnswer((invocation) -> {
                    int offset = (int) (long) invocation.getArgument(2);
                    int limit = invocation.getArgument(3);
                    return Arrays.copyOfRange(fakeContent.array(), offset, offset + limit);
                }).when(afsClient).read(
                        Mockito.eq(entityId),
                        Mockito.eq(afsPath),
                        Mockito.anyLong(),
                        Mockito.anyInt()
                );
                Mockito.doReturn(30000L).when(afsFileChannel).size();

                assertEquals(10000L, afsFileChannel.position());
                ByteBuffer byteBuffer = ByteBuffer.allocate(10000);
                byteBuffer.position(10000);
                assertEquals(0, afsFileChannel.read(byteBuffer, 10000));
                assertEquals(10000L, afsFileChannel.position());
                byteBuffer.position(10);
                byteBuffer.limit(9990);
                assertEquals(-1, afsFileChannel.read(byteBuffer, 30000));
                assertEquals(-1, afsFileChannel.read(byteBuffer, 30005));

                Mockito.clearInvocations(afsFileChannel);
                byteBuffer.position(100);
                byteBuffer.limit(9900);
                assertEquals(9800, afsFileChannel.read(byteBuffer, 5));
                assertArrayEquals(
                        Arrays.copyOfRange(fakeContent.array(), 5, 9805),
                        Arrays.copyOfRange(byteBuffer.array(), 100, 9900));
                Mockito.verify(afsClient, Mockito.times(1)).read(
                        entityId,
                        afsPath,
                        5L,
                        9800
                );
                assertEquals(10000, afsFileChannel.position());
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.read(ByteBuffer.allocate(1000), 10);
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testWrite() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean writeOption : List.of(false, true)) {
            boolean readOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (writeOption) {
                Mockito.doReturn(true).when(afsClient)
                        .write(
                                Mockito.eq(entityId),
                                Mockito.eq(afsPath),
                                Mockito.anyLong(),
                                Mockito.any(byte[].class)
                        );

                assertEquals(10000L, afsFileChannel.position());
                ByteBuffer byteBuffer = TestHelper.getRandomByteBuffer(10000);
                byteBuffer.position(10000);
                assertEquals(0, afsFileChannel.write(byteBuffer));
                assertEquals(10000L, afsFileChannel.position());
                Mockito.doReturn(5000L).when(afsFileChannel).size();
                byteBuffer.position(10);
                byteBuffer.limit(9990);
                assertEquals(9980, afsFileChannel.write(byteBuffer));
                Mockito.verify(afsFileChannel, Mockito.times(1)).fillWithZero(5000, 10000);
                Mockito.verify(afsClient, Mockito.times(1)).write(
                        entityId,
                        afsPath,
                        10000L,
                        Arrays.copyOfRange(byteBuffer.array(), 10, 9990)
                );
                assertEquals(19980, afsFileChannel.position());

                Mockito.clearInvocations(afsFileChannel);
                afsFileChannel.position(5);
                byteBuffer.position(100);
                byteBuffer.limit(9900);
                assertEquals(9800, afsFileChannel.write(byteBuffer));
                Mockito.verify(afsFileChannel, Mockito.times(0))
                        .fillWithZero(Mockito.anyInt(), Mockito.anyInt());
                Mockito.verify(afsClient, Mockito.times(1)).write(
                        entityId,
                        afsPath,
                        5L,
                        Arrays.copyOfRange(byteBuffer.array(), 100, 9900)
                );
                assertEquals(9805, afsFileChannel.position());
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.write(ByteBuffer.allocate(1000));
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testWriteWithMultipleBuffers() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean writeOption : List.of(false, true)) {
            boolean readOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (writeOption) {
                Mockito.doReturn(true).when(afsClient)
                        .write(
                                Mockito.eq(entityId),
                                Mockito.eq(afsPath),
                                Mockito.anyLong(),
                                Mockito.any(byte[].class)
                        );

                assertEquals(10000L, afsFileChannel.position());
                ByteBuffer[] byteBuffers = new ByteBuffer[] {
                        TestHelper.getRandomByteBuffer(5000),
                        TestHelper.getRandomByteBuffer(5000),
                        TestHelper.getRandomByteBuffer(5000),
                        TestHelper.getRandomByteBuffer(5000)
                };
                byteBuffers[1].position(5000);
                byteBuffers[2].position(5000);
                assertEquals(0, afsFileChannel.write(byteBuffers, 1, 2));
                assertEquals(10000L, afsFileChannel.position());
                Mockito.doReturn(5000L).when(afsFileChannel).size();
                byteBuffers[1].position(10);
                byteBuffers[2].position(0);
                byteBuffers[2].limit(4990);
                assertEquals(9980, afsFileChannel.write(byteBuffers, 1, 2));
                Mockito.verify(afsFileChannel, Mockito.times(1)).fillWithZero(5000, 10000);
                Mockito.verify(afsClient, Mockito.times(1)).write(
                        entityId,
                        afsPath,
                        10000L,
                        ByteBuffer.allocate(9980)
                                .put(Arrays.copyOfRange(byteBuffers[1].array(), 10, 5000))
                                .put(Arrays.copyOfRange(byteBuffers[2].array(), 0, 4990))
                                .array()
                );
                assertEquals(19980, afsFileChannel.position());

                Mockito.clearInvocations(afsFileChannel);
                afsFileChannel.position(5);
                byteBuffers[1].position(100);
                byteBuffers[2].position(0);
                byteBuffers[2].limit(4900);
                assertEquals(9800, afsFileChannel.write(byteBuffers, 1, 2));
                Mockito.verify(afsFileChannel, Mockito.times(0))
                        .fillWithZero(Mockito.anyInt(), Mockito.anyInt());
                Mockito.verify(afsClient, Mockito.times(1)).write(
                        entityId,
                        afsPath,
                        5L,
                        ByteBuffer.allocate(9800)
                                .put(Arrays.copyOfRange(byteBuffers[1].array(), 100, 5000))
                                .put(Arrays.copyOfRange(byteBuffers[2].array(), 0, 4900))
                                .array()
                );
                assertEquals(9805, afsFileChannel.position());
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.write(new ByteBuffer[]{
                            ByteBuffer.allocate(1000),
                            ByteBuffer.allocate(1000)
                    });
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testWriteWithPosition() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean writeOption : List.of(false, true)) {
            boolean readOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (writeOption) {
                Mockito.doReturn(true).when(afsClient)
                        .write(
                                Mockito.eq(entityId),
                                Mockito.eq(afsPath),
                                Mockito.anyLong(),
                                Mockito.any(byte[].class)
                        );

                assertEquals(10000L, afsFileChannel.position());
                ByteBuffer byteBuffer = TestHelper.getRandomByteBuffer(10000);
                byteBuffer.position(10000);
                assertEquals(0, afsFileChannel.write(byteBuffer, 10000));
                assertEquals(10000L, afsFileChannel.position());
                Mockito.doReturn(5000L).when(afsFileChannel).size();
                byteBuffer.position(10);
                byteBuffer.limit(9990);
                assertEquals(9980, afsFileChannel.write(byteBuffer, 10000));
                Mockito.verify(afsFileChannel, Mockito.times(1)).fillWithZero(5000, 10000);
                Mockito.verify(afsClient, Mockito.times(1)).write(
                        entityId,
                        afsPath,
                        10000L,
                        Arrays.copyOfRange(byteBuffer.array(), 10, 9990)
                );
                assertEquals(10000, afsFileChannel.position());

                Mockito.clearInvocations(afsFileChannel);
                byteBuffer.position(100);
                byteBuffer.limit(9900);
                assertEquals(9800, afsFileChannel.write(byteBuffer, 5));
                Mockito.verify(afsFileChannel, Mockito.times(0))
                        .fillWithZero(Mockito.anyInt(), Mockito.anyInt());
                Mockito.verify(afsClient, Mockito.times(1)).write(
                        entityId,
                        afsPath,
                        5L,
                        Arrays.copyOfRange(byteBuffer.array(), 100, 9900)
                );
                assertEquals(10000, afsFileChannel.position());
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.write(ByteBuffer.allocate(1000), 10);
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testPosition() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        long position = 0L;
        boolean readOption = true;
        boolean writeOption = true;

        AfsFileChannel afsFileChannel = new AfsFileChannel(
                entityId,
                afsPath,
                user,
                clientUtil,
                listUtil,
                new AtomicLong(position),
                readOption,
                writeOption
        );

        assertEquals(0L, afsFileChannel.position());
        afsFileChannel.position(4563);
        assertEquals(4563L, afsFileChannel.position());
        afsFileChannel.position(937543);
        assertEquals(937543L, afsFileChannel.position());
    }

    public void testSize() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        long position = 0L;
        boolean readOption = true;
        boolean writeOption = true;

        AfsFileChannel afsFileChannel = new AfsFileChannel(
                entityId,
                afsPath,
                user,
                clientUtil,
                listUtil,
                new AtomicLong(position),
                readOption,
                writeOption
        );

        File existingFile = new File(entityId,
                afsPath,
                "file2.png",
                false,
                345729L,
                Instant.now().atOffset(ZoneOffset.UTC)
        );
        Mockito.doReturn(Optional.of(existingFile)).when(listUtil)
                        .getAfsFilePresence(entityId, afsPath);
        assertEquals(345729L, afsFileChannel.size());
    }

    public void testTruncate() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean writeOption : List.of(false, true)) {
            boolean readOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (writeOption) {
                Mockito.doReturn(1000).when(afsFileChannel).getAfsClientMaxPackageSize();
                Mockito.doReturn(true).when(afsClient)
                        .truncate(
                                Mockito.eq(entityId),
                                Mockito.eq(afsPath),
                                Mockito.anyLong()
                        );

                assertEquals(10000L, afsFileChannel.position());
                afsFileChannel.truncate(5020);
                Mockito.verify(afsClient, Mockito.times(1)).truncate(
                        entityId, afsPath, 5020L
                );
                assertEquals(5020, afsFileChannel.position());

                afsFileChannel.truncate(235043);
                Mockito.verify(afsClient, Mockito.times(1)).truncate(
                        entityId, afsPath, 235043L
                );
                assertEquals(5020, afsFileChannel.position());
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.truncate(5020);
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testForce() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        long position = 0L;
        boolean readOption = true;
        boolean writeOption = true;

        AfsFileChannel afsFileChannel = new AfsFileChannel(
                entityId,
                afsPath,
                user,
                clientUtil,
                listUtil,
                new AtomicLong(position),
                readOption,
                writeOption
        );

        afsFileChannel.force(false);
        afsFileChannel.force(true);
    }

    public void testTransferTo() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean readOption : List.of(false, true)) {
            boolean writeOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (readOption) {
                ByteBuffer fakeContent = TestHelper.getRandomByteBuffer(30000);
                Mockito.doAnswer((invocation) -> {
                    int offset = (int) (long) invocation.getArgument(2);
                    int limit = invocation.getArgument(3);
                    return Arrays.copyOfRange(fakeContent.array(), offset, offset + limit);
                }).when(afsClient).read(
                        Mockito.eq(entityId),
                        Mockito.eq(afsPath),
                        Mockito.anyLong(),
                        Mockito.anyInt()
                );
                Mockito.doReturn(30000L).when(afsFileChannel).size();

                WritableByteChannel writableByteChannel = Mockito.spy(new WritableByteChannel() {
                    @Override
                    public int write(ByteBuffer byteBuffer) throws IOException {
                        int remaining = byteBuffer.remaining();
                        byteBuffer.get(new byte[remaining]);
                        return remaining;
                    }

                    @Override
                    public boolean isOpen() {
                        return true;
                    }

                    @Override
                    public void close() throws IOException {

                    }
                });

                assertEquals(1000, afsFileChannel.transferTo(10, 1000, writableByteChannel));
                ArgumentCaptor<ByteBuffer> byteBufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
                Mockito.verify(afsFileChannel, Mockito.times(1))
                        .read(byteBufferArgumentCaptor.capture(), Mockito.eq(10L));
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.transferTo(10, 100, Mockito.mock(WritableByteChannel.class));
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testTransferFrom() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 10000L;

        for (boolean writeOption : List.of(false, true)) {
            boolean readOption = true;

            AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    clientUtil,
                    listUtil,
                    new AtomicLong(position),
                    readOption,
                    writeOption
            ));

            if (writeOption) {
                ByteBuffer fakeContent = TestHelper.getRandomByteBuffer(30000);
                Mockito.doReturn(true).when(afsClient).write(
                        Mockito.eq(entityId),
                        Mockito.eq(afsPath),
                        Mockito.anyLong(),
                        Mockito.any(byte[].class)
                );
                Mockito.doReturn(30000L).when(afsFileChannel).size();

                ReadableByteChannel readableByteChannel = Mockito.spy(new ReadableByteChannel() {
                    @Override
                    public int read(ByteBuffer byteBuffer) throws IOException {
                        int remaining = byteBuffer.remaining();
                        byte[] readBytes = new byte[remaining];
                        fakeContent.get(readBytes);
                        byteBuffer.put(readBytes);
                        return remaining;
                    }

                    @Override
                    public boolean isOpen() {
                        return true;
                    }

                    @Override
                    public void close() throws IOException {

                    }
                });

                assertEquals(1000, afsFileChannel.transferFrom(readableByteChannel, 10, 1000));
                ArgumentCaptor<ByteBuffer> byteBufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
                Mockito.verify(afsFileChannel, Mockito.times(1))
                        .write(byteBufferArgumentCaptor.capture(), Mockito.eq(10L));
                Assert.assertArrayEquals(byteBufferArgumentCaptor.getValue().array(), Arrays.copyOfRange(fakeContent.array(), 0, 1000));
            } else {
                Exception exception = null;
                try {
                    afsFileChannel.transferFrom(Mockito.mock(ReadableByteChannel.class), 10, 100);
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }
    }

    public void testMap() {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        long position = 0L;
        boolean readOption = true;
        boolean writeOption = true;

        AfsFileChannel afsFileChannel = new AfsFileChannel(
                entityId,
                afsPath,
                user,
                clientUtil,
                listUtil,
                new AtomicLong(position),
                readOption,
                writeOption
        );

        Exception exception = null;
        try {
            afsFileChannel.map(FileChannel.MapMode.READ_ONLY, 0L , 100L);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(UnsupportedOperationException.class, exception.getClass());
    }

    public void testLock() {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        long position = 0L;
        boolean readOption = true;
        boolean writeOption = true;

        AfsFileChannel afsFileChannel = new AfsFileChannel(
                entityId,
                afsPath,
                user,
                clientUtil,
                listUtil,
                new AtomicLong(position),
                readOption,
                writeOption
        );

        Exception exception = null;
        try {
            afsFileChannel.lock(0L , 100L, true);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(UnsupportedOperationException.class, exception.getClass());
    }

    public void testTryLock() {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        long position = 0L;
        boolean readOption = true;
        boolean writeOption = true;

        AfsFileChannel afsFileChannel = new AfsFileChannel(
                entityId,
                afsPath,
                user,
                clientUtil,
                listUtil,
                new AtomicLong(position),
                readOption,
                writeOption
        );

        Exception exception = null;
        try {
            afsFileChannel.tryLock(0L , 100L, true);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(UnsupportedOperationException.class, exception.getClass());
    }

    public void testFillWithZero() throws Exception {
        String entityId = "entity-id";
        String afsPath = "/dir0/dir1/file2.png";
        User user = User.builder()
                .username("user_name").sessionToken("session-tkn-0").build();
        SftpListUtil listUtil = Mockito.spy(new SftpListUtil(user));
        OpenBISClientUtil clientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClient = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClient).when(clientUtil).getAfsClient(user);
        long position = 0L;
        boolean readOption = true;
        boolean writeOption = true;

        AfsFileChannel afsFileChannel = Mockito.spy(new AfsFileChannel(
                entityId,
                afsPath,
                user,
                clientUtil,
                listUtil,
                new AtomicLong(position),
                readOption,
                writeOption
        ));
        Mockito.doReturn(1000).when(afsFileChannel).getAfsClientMaxPackageSize();
        Mockito.doReturn(true).when(afsClient)
                .write(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyLong(),
                        Mockito.any(byte[].class)
                );

        afsFileChannel.fillWithZero(0, 5020);
        Mockito.verify(afsClient).write(entityId, afsPath, 0L, new byte[1000]);
        Mockito.verify(afsClient).write(entityId, afsPath, 1000L, new byte[1000]);
        Mockito.verify(afsClient).write(entityId, afsPath, 2000L, new byte[1000]);
        Mockito.verify(afsClient).write(entityId, afsPath, 3000L, new byte[1000]);
        Mockito.verify(afsClient).write(entityId, afsPath, 4000L, new byte[1000]);
        Mockito.verify(afsClient).write(entityId, afsPath, 5000L, new byte[20]);
    }
}