package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afssftp.StaticInitializer;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.AfsFileChannel;
import ch.ethz.sis.openbis.generic.OpenBIS;
import junit.framework.TestCase;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SftpFileUtilTest extends TestCase {
    {
        StaticInitializer.initialize();
    }

    public void testCreateAfsFileChannel() throws Exception {
        String entityId = "entity-id-1";
        String afsPath = "/dir1/dir2/file2.txt";
        User user = User.builder().username("user1").sessionToken("session-tkn-1").build();
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        SftpListUtil sftpListUtil = Mockito.mock(SftpListUtil.class);

        SftpFileUtil sftpFileUtil = Mockito.spy(
                new SftpFileUtil(user, openBISClientUtil, sftpListUtil)
        );

        OpenBIS.AfsServerFacade afsClientMock = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClientMock).when(openBISClientUtil).getAfsClient(user);

        // Cannot write AFS data on data-immutable entity
        for (StandardOpenOption writeOption : List.of(
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            Exception exception = null;
            try {
                sftpFileUtil.createAfsFileChannel(
                        entityId,
                        afsPath,
                        user,
                        Set.of(writeOption),
                        false
                );
            } catch (Exception e) {
                exception = e;
            }
            assertEquals(UnsupportedOperationException.class, exception.getClass());
        }

        // Create new AFS file if necessary
        Mockito.doReturn(Optional.empty())
            .when(sftpListUtil).getAfsFilePresence(
                        entityId,
                        afsPath
            );
        for (boolean isAfsEntityMutable: List.of(false, true)) {
            for (StandardOpenOption createOption : List.of(
                    StandardOpenOption.CREATE,
                    StandardOpenOption.CREATE_NEW
            )) {
                Mockito.clearInvocations(sftpListUtil, afsClientMock);

                try {
                    sftpFileUtil.createAfsFileChannel(
                            entityId,
                            afsPath,
                            user,
                            Set.of(createOption),
                            isAfsEntityMutable
                    );
                } catch (Exception e) {}

                Mockito.verify(sftpListUtil, Mockito.times(isAfsEntityMutable ? 1: 0))
                        .tryToCreateAfsFileRootIfNecessary(entityId);
                Mockito.verify(afsClientMock, Mockito.times(isAfsEntityMutable ? 1: 0))
                        .create(entityId, afsPath, false);
            }

            for (StandardOpenOption nonCreateOption : List.of(
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.APPEND
            )) {
                Mockito.clearInvocations(sftpListUtil, afsClientMock);

                try {
                    sftpFileUtil.createAfsFileChannel(
                            entityId,
                            afsPath,
                            user,
                            Set.of(nonCreateOption),
                            isAfsEntityMutable
                    );
                } catch (Exception e) {}

                Mockito.verify(sftpListUtil, Mockito.times(0))
                        .tryToCreateAfsFileRootIfNecessary(entityId);
                Mockito.verify(afsClientMock, Mockito.times(0))
                        .create(entityId, afsPath, false);
            }
        }

        // Cannot work with AFS directory
        File afsDirectory = new File(entityId, afsPath, "file2.txt", true, 0L, Instant.now().atOffset(ZoneOffset.UTC));
        Mockito.doReturn(Optional.of(afsDirectory))
                .when(sftpListUtil).getAfsFilePresence(
                        entityId,
                        afsPath
                );
        for (boolean isAfsEntityMutable: List.of(false, true)) {
            for (StandardOpenOption openOption : List.of(
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.APPEND
            )) {
                Mockito.clearInvocations(sftpListUtil, afsClientMock);

                Exception exception = null;
                try {
                    sftpFileUtil.createAfsFileChannel(
                            entityId,
                            afsPath,
                            user,
                            Set.of(openOption),
                            isAfsEntityMutable
                    );
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }

        // Cannot work with AFS non-existent file
        Mockito.doReturn(Optional.empty())
                .when(sftpListUtil).getAfsFilePresence(
                        entityId,
                        afsPath
                );
        for (boolean isAfsEntityMutable: List.of(false, true)) {
            for (StandardOpenOption openOption : List.of(
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.APPEND
            )) {
                Mockito.clearInvocations(sftpListUtil, afsClientMock);

                Exception exception = null;
                try {
                    sftpFileUtil.createAfsFileChannel(
                            entityId,
                            afsPath,
                            user,
                            Set.of(openOption),
                            isAfsEntityMutable
                    );
                } catch (Exception e) {
                    exception = e;
                }
                assertEquals(UnsupportedOperationException.class, exception.getClass());
            }
        }

        // Different position according to open-options with AFS regular file
        File afsRegularFile = new File(entityId, afsPath, "file2.txt", false, 1543L, Instant.now().atOffset(ZoneOffset.UTC));
        Mockito.doReturn(Optional.of(afsRegularFile))
                .when(sftpListUtil).getAfsFilePresence(
                        entityId,
                        afsPath
                );
        for (StandardOpenOption openOption : List.of(
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.APPEND
        )) {
            Mockito.clearInvocations(sftpListUtil, afsClientMock);

            AfsFileChannel afsFileChannel = sftpFileUtil.createAfsFileChannel(
                    entityId,
                    afsPath,
                    user,
                    Set.of(openOption),
                    true
            );

            if (openOption == StandardOpenOption.TRUNCATE_EXISTING) {
                Mockito.verify(afsClientMock, Mockito.times(1)).truncate(
                        entityId, afsPath, 0L
                );
            }

            if (openOption == StandardOpenOption.APPEND) {
                assertEquals(1543L, afsFileChannel.position());
            } else {
                assertEquals(0L, afsFileChannel.position());
            }
        }
    }

    public void testDeleteAfsFile() throws Exception {
        User user = User.builder().username("user1").sessionToken("session-tkn-1").build();
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        OpenBIS.AfsServerFacade afsClientMock = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClientMock).when(openBISClientUtil).getAfsClient(user);

        SftpFileUtil sftpFileUtil = new SftpFileUtil(user, openBISClientUtil, sftpListUtil);

        String entityId = "entity-id-1";
        String afsPath = "/dir/file1";
        for (boolean afsFilePresent : List.of(false, true)) {
            for (boolean deleteSuccess : List.of(false, true)) {
                Mockito.clearInvocations(afsClientMock);
                File afsFile = afsFilePresent ? new File(
                        entityId, afsPath, "file1",
                        false, 10L,
                        Instant.ofEpochMilli(System.currentTimeMillis())
                            .atOffset(ZoneOffset.UTC)
                ) : null;

                Mockito.doReturn(Optional.ofNullable(afsFile)).when(sftpListUtil)
                        .getAfsFilePresence(entityId, afsPath);
                Mockito.doReturn(deleteSuccess).when(afsClientMock)
                        .delete(entityId, afsPath, true);

                Exception exception = null;
                try {
                    sftpFileUtil.deleteAfsFile(entityId, afsPath, user);
                } catch (Exception e) {
                    exception = e;
                }

                if (afsFilePresent) {
                    Mockito.verify(afsClientMock, Mockito.times(1))
                            .delete(entityId, afsPath, true);
                    if (deleteSuccess) {
                        assertNull(exception);
                    } else {
                        assertEquals(IOException.class, exception.getClass());
                    }
                } else {
                    Mockito.verify(afsClientMock, Mockito.times(0))
                            .delete(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
                }
            }
        }
    }

    public void testCreateAfsDirectory() {
        User user = User.builder().username("user1").sessionToken("session-tkn-1").build();
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        OpenBIS.AfsServerFacade afsClientMock = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClientMock).when(openBISClientUtil).getAfsClient(user);

        SftpFileUtil sftpFileUtil = new SftpFileUtil(user, openBISClientUtil, sftpListUtil);

        String entityId = "entity-id-1";
        String afsPath = "/dir";
        for (boolean afsFilePresent : List.of(false, true)) {
            for (boolean createDirSuccess : List.of(false, true)) {
                Mockito.clearInvocations(afsClientMock);
                File afsFile = afsFilePresent ? new File(
                        entityId, afsPath, "dir",
                        true, 10L,
                        Instant.ofEpochMilli(System.currentTimeMillis())
                        .atOffset(ZoneOffset.UTC)
                ) : null;

                Mockito.doReturn(Optional.ofNullable(afsFile)).when(sftpListUtil)
                        .getAfsFilePresence(entityId, afsPath);
                Mockito.doReturn(createDirSuccess).when(afsClientMock)
                        .create(entityId, afsPath, true);

                Exception exception = null;
                try {
                    sftpFileUtil.createAfsDirectory(entityId, afsPath, user);
                } catch (Exception e) {
                    exception = e;
                }

                if (!afsFilePresent) {
                    Mockito.verify(afsClientMock, Mockito.times(1))
                            .create(entityId, afsPath, true);
                    if (createDirSuccess) {
                        assertNull(exception);
                    } else {
                        assertEquals(IOException.class, exception.getClass());
                    }
                } else {
                    assertEquals(IOException.class, exception.getClass());
                    Mockito.verify(afsClientMock, Mockito.times(0))
                            .create(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
                }
            }
        }
    }

    public void testCopyAfsFile() {
        User user = User.builder().username("user1").sessionToken("session-tkn-1").build();
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        OpenBIS.AfsServerFacade afsClientMock = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClientMock).when(openBISClientUtil).getAfsClient(user);

        SftpFileUtil sftpFileUtil = new SftpFileUtil(user, openBISClientUtil, sftpListUtil);

        String entityId1 = "entity-id-1";
        String afsPath1 = "/dir/file1";
        String entityId2 = "entity-id-2";
        String afsPath2 = "/dir/file2";
        for (boolean afsPresentFile1 : List.of(false, true)) {
            for (boolean afsPresentFile2 : List.of(false, true)) {
                for (boolean replaceExisting : List.of(false, true)) {
                    for (boolean copySuccess : List.of(false, true)) {
                        Mockito.clearInvocations(afsClientMock);
                        File afsFile1 = afsPresentFile1 ? new File(
                                entityId1, afsPath1, "file1",
                                false, 10L,
                                Instant.ofEpochMilli(System.currentTimeMillis())
                                .atOffset(ZoneOffset.UTC)
                        ) : null;

                        File afsFile2 = afsPresentFile2 ? new File(
                                entityId2, afsPath2, "file2",
                                false, 10L,
                                Instant.ofEpochMilli(System.currentTimeMillis())
                                .atOffset(ZoneOffset.UTC)
                        ) : null;

                        Mockito.doReturn(Optional.ofNullable(afsFile1)).when(sftpListUtil)
                                .getAfsFilePresence(entityId1, afsPath1);
                        Mockito.doReturn(Optional.ofNullable(afsFile2)).when(sftpListUtil)
                                .getAfsFilePresence(entityId2, afsPath2);
                        Mockito.doReturn(copySuccess).when(afsClientMock)
                                .copy(entityId1, afsPath1, entityId2, afsPath2);

                        Exception exception = null;
                        try {
                            sftpFileUtil.copyAfsFile(
                                    entityId1, afsPath1,
                                    entityId2, afsPath2,
                                    user,
                                    replaceExisting
                            );
                        } catch (Exception e) {
                            exception = e;
                        }

                        if (afsPresentFile1 && (!afsPresentFile2 || replaceExisting)) {
                            Mockito.verify(afsClientMock, Mockito.times(1))
                                    .copy(
                                            entityId1, afsPath1,
                                            entityId2, afsPath2
                                    );
                            if (copySuccess) {
                                assertNull(exception);
                            } else {
                                assertEquals(IOException.class, exception.getClass());
                            }
                        } else {
                            assertEquals(IOException.class, exception.getClass());
                            Mockito.verify(afsClientMock, Mockito.times(0))
                                    .copy(
                                            Mockito.anyString(), Mockito.anyString(),
                                            Mockito.anyString(), Mockito.anyString()
                                    );
                        }
                    }
                }
            }
        }
    }

    public void testMoveAfsFile() {
        User user = User.builder().username("user1").sessionToken("session-tkn-1").build();
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        OpenBIS.AfsServerFacade afsClientMock = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClientMock).when(openBISClientUtil).getAfsClient(user);

        SftpFileUtil sftpFileUtil = new SftpFileUtil(user, openBISClientUtil, sftpListUtil);

        String entityId1 = "entity-id-1";
        String afsPath1 = "/dir/file1";
        String entityId2 = "entity-id-2";
        String afsPath2 = "/dir/file2";
        for (boolean afsPresentFile1 : List.of(false, true)) {
            for (boolean afsPresentFile2 : List.of(false, true)) {
                for (boolean replaceExisting : List.of(false, true)) {
                    for (boolean moveSuccess : List.of(false, true)) {
                        Mockito.clearInvocations(afsClientMock);
                        File afsFile1 = afsPresentFile1 ? new File(
                                entityId1, afsPath1, "file1",
                                false, 10L,
                                Instant.ofEpochMilli(System.currentTimeMillis())
                                .atOffset(ZoneOffset.UTC)
                        ) : null;

                        File afsFile2 = afsPresentFile2 ? new File(
                                entityId2, afsPath2, "file2",
                                false, 10L,
                                Instant.ofEpochMilli(System.currentTimeMillis())
                                .atOffset(ZoneOffset.UTC)
                        ) : null;

                        Mockito.doReturn(Optional.ofNullable(afsFile1)).when(sftpListUtil)
                                .getAfsFilePresence(entityId1, afsPath1);
                        Mockito.doReturn(Optional.ofNullable(afsFile2)).when(sftpListUtil)
                                .getAfsFilePresence(entityId2, afsPath2);
                        Mockito.doReturn(moveSuccess).when(afsClientMock)
                                .move(entityId1, afsPath1, entityId2, afsPath2);

                        Exception exception = null;
                        try {
                            sftpFileUtil.moveAfsFile(
                                    entityId1, afsPath1,
                                    entityId2, afsPath2,
                                    user,
                                    replaceExisting
                            );
                        } catch (Exception e) {
                            exception = e;
                        }

                        if (afsPresentFile1 && !afsPresentFile2) {
                            Mockito.verify(afsClientMock, Mockito.times(1))
                                    .move(
                                            entityId1, afsPath1,
                                            entityId2, afsPath2
                                    );
                            if (moveSuccess) {
                                assertNull(exception);
                            } else {
                                assertEquals(IOException.class, exception.getClass());
                            }
                        } else {
                            assertEquals(IOException.class, exception.getClass());
                            Mockito.verify(afsClientMock, Mockito.times(0))
                                    .move(
                                            Mockito.anyString(), Mockito.anyString(),
                                            Mockito.anyString(), Mockito.anyString()
                                    );
                        }
                    }
                }
            }
        }
    }
}