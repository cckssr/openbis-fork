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
import java.util.Map;
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

    public void testGetAfsFilePresence() {
        User user = User.builder().username("user1").sessionToken("session-tkn-1").build();
        OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
        OpenBIS.AfsServerFacade afsClientMock = Mockito.mock(OpenBIS.AfsServerFacade.class);
        Mockito.doReturn(afsClientMock).when(openBISClientUtil).getAfsClient(user);

        SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);

        // Root: found
        File fileInRoot = new File("entity-1", "/a.txt", "a.txt", false, 10L, Instant.now().atOffset(ZoneOffset.UTC));
        Mockito.doReturn(new File[]{fileInRoot}).when(afsClientMock).list("entity-1", "/", false);
        Optional<File> root = sftpListUtil.getAfsFilePresence("entity-1", "/");
        assertTrue(root.isPresent());
        assertEquals("/", root.get().getPath());
        assertTrue(root.get().getDirectory());

        // Root: not found
        Mockito.doThrow(new RuntimeException("NoSuchFileException")).when(afsClientMock).list("entity-1", "/", false);
        assertTrue(sftpListUtil.getAfsFilePresence("entity-1", "/").isEmpty());

        // Root: exception
        Mockito.doThrow(new RuntimeException("OtherException")).when(afsClientMock).list("entity-1", "/", false);
        Exception exception = null;
        try {
            sftpListUtil.getAfsFilePresence("entity-1", "/");
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(RuntimeException.class, exception.getClass());
        assertEquals("OtherException", exception.getMessage());

        // Non-root: found
        File fileInDir = new File("entity-1", "/dir/a.txt", "a.txt", false, 10L, Instant.now().atOffset(ZoneOffset.UTC));
        File file2InDir = new File("entity-1", "/dir/b.txt", "b.txt", false, 10L, Instant.now().atOffset(ZoneOffset.UTC));
        Mockito.doReturn(new File[]{fileInDir, file2InDir}).when(afsClientMock).list("entity-1", "/dir", false);
        Optional<File> retrievedFile = sftpListUtil.getAfsFilePresence("entity-1", "/dir/a.txt");
        assertTrue(retrievedFile.isPresent());
        assertEquals(fileInDir, retrievedFile.get());

        // Non-root: not found
        Mockito.doThrow(new RuntimeException("NoSuchFileException")).when(afsClientMock).list("entity-1", "/dir", false);
        assertTrue(sftpListUtil.getAfsFilePresence("entity-1", "/dir/a.txt").isEmpty());

        // Non-root: exception
        Mockito.doThrow(new RuntimeException("OtherException")).when(afsClientMock).list("entity-1", "/dir", false);
        Exception exception2 = null;
        try {
            sftpListUtil.getAfsFilePresence("entity-1", "/dir/a.txt");
        } catch (Exception e) {
            exception2 = e;
        }
        assertNotNull(exception2);
        assertEquals(RuntimeException.class, exception2.getClass());
        assertEquals("OtherException", exception2.getMessage());
    }


    public void testGetAfsFilePresenceBatch() {
        for (boolean tryFetchSiblings : List.of(false, true)) {
            for (boolean tryFetchChildren : List.of(false, true)) {
                User user = User.builder().username("user1").sessionToken("session-tkn-1").build();
                OpenBISClientUtil openBISClientUtil = Mockito.mock(OpenBISClientUtil.class);
                OpenBIS.AfsServerFacade afsClientMock = Mockito.mock(OpenBIS.AfsServerFacade.class);
                Mockito.doReturn(afsClientMock).when(openBISClientUtil).getAfsClient(user);

                SftpListUtil sftpListUtil = new SftpListUtil(user, openBISClientUtil);

                // Root: found
                File fileInRoot = new File("entity-1", "/a.txt", "a.txt", false, 10L, Instant.now().atOffset(ZoneOffset.UTC));
                File file2InRoot = new File("entity-1", "/b.txt", "b.txt", false, 10L, Instant.now().atOffset(ZoneOffset.UTC));
                Mockito.doReturn(new File[]{fileInRoot, file2InRoot}).when(afsClientMock).list("entity-1", "/", false);
                Map<String, File> rootFiles = sftpListUtil.getAfsFilePresenceBatch("entity-1", "/", tryFetchSiblings, tryFetchChildren);
                assertEquals("/", rootFiles.get("/").getPath());
                assertTrue(rootFiles.get("/").getDirectory());
                assertEquals(tryFetchChildren, rootFiles.values().containsAll(List.of(fileInRoot, file2InRoot)));

                // Root: not found
                Mockito.doThrow(new RuntimeException("NoSuchFileException")).when(afsClientMock).list("entity-1", "/", false);
                assertTrue(sftpListUtil.getAfsFilePresence("entity-1", "/").isEmpty());

                // Root: exception
                Mockito.doThrow(new RuntimeException("OtherException")).when(afsClientMock).list("entity-1", "/", false);
                Exception exception = null;
                try {
                    sftpListUtil.getAfsFilePresenceBatch("entity-1", "/", tryFetchSiblings, tryFetchChildren);
                } catch (Exception e) {
                    exception = e;
                }
                assertNotNull(exception);
                assertEquals(RuntimeException.class, exception.getClass());
                assertEquals("OtherException", exception.getMessage());

                // Non-root: regular file found
                File fileInDir = new File("entity-1", "/dir/a.txt", "a.txt", false, 10L, Instant.now().atOffset(ZoneOffset.UTC));
                File file2InDir = new File("entity-1", "/dir/b.txt", "b.txt", false, 10L, Instant.now().atOffset(ZoneOffset.UTC));
                Mockito.doReturn(new File[]{fileInDir, file2InDir}).when(afsClientMock).list("entity-1", "/dir", false);
                Mockito.doReturn(new File[]{fileInDir}).when(afsClientMock).list("entity-1", "/dir/a.txt", false);
                Map<String, File> retrievedFiles = sftpListUtil.getAfsFilePresenceBatch("entity-1", "/dir/a.txt", tryFetchSiblings, tryFetchChildren);
                assertEquals(fileInDir, retrievedFiles.get("/dir/a.txt"));
                if (tryFetchSiblings) {
                    assertTrue(retrievedFiles.values().containsAll(List.of(fileInDir, file2InDir)));
                } else {
                    assertEquals(1, retrievedFiles.size());
                }

                // Non-root: regular file not found
                Mockito.doThrow(new RuntimeException("NoSuchFileException")).when(afsClientMock).list("entity-1", "/dir/a.txt", false);
                assertTrue(sftpListUtil.getAfsFilePresenceBatch("entity-1", "/dir/a.txt", tryFetchSiblings, tryFetchChildren).isEmpty());

                // Non-root: directory found
                File dirInDir = new File("entity-1", "/dir/subdir", "subdir", true, null, Instant.now().atOffset(ZoneOffset.UTC));
                Mockito.doReturn(new File[]{fileInDir, dirInDir}).when(afsClientMock).list("entity-1", "/dir", false);
                Mockito.doReturn(new File[]{file2InDir}).when(afsClientMock).list("entity-1", "/dir/subdir", false);
                Map<String, File> retrievedFiles2 = sftpListUtil.getAfsFilePresenceBatch("entity-1", "/dir/subdir", tryFetchSiblings, tryFetchChildren);
                assertEquals(dirInDir, retrievedFiles2.get("/dir/subdir"));
                if (tryFetchSiblings || tryFetchChildren) {
                    assertEquals(tryFetchSiblings, retrievedFiles2.values().containsAll(List.of(fileInDir, dirInDir)));
                    assertEquals(tryFetchChildren, retrievedFiles2.values().contains(file2InDir));
                } else {
                    assertEquals(1, retrievedFiles2.size());
                }

                // Non-root: directory not found
                Mockito.doThrow(new RuntimeException("NoSuchFileException")).when(afsClientMock).list("entity-1", "/dir/subdir", false);
                assertTrue(sftpListUtil.getAfsFilePresenceBatch("entity-1", "/dir/subdir", tryFetchSiblings, tryFetchChildren).isEmpty());

                // Non-root: exception
                Mockito.doThrow(new RuntimeException("OtherException")).when(afsClientMock).list("entity-1", "/dir/a.txt", false);
                Exception exception2 = null;
                try {
                    sftpListUtil.getAfsFilePresenceBatch("entity-1", "/dir/a.txt", tryFetchSiblings, tryFetchChildren);
                } catch (Exception e) {
                    exception2 = e;
                }
                assertNotNull(exception2);
                assertEquals(RuntimeException.class, exception2.getClass());
                assertEquals("OtherException", exception2.getMessage());
            }
        }
    }
}