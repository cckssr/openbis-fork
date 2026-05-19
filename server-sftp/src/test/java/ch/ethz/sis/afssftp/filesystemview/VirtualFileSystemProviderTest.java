package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.StaticInitializer;
import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.afssftp.filesystemview.impl.standard.StandardPathTranslator;
import ch.ethz.sis.afssftp.helpers.TestHelper;
import ch.ethz.sis.afssftp.util.SftpFileUtil;
import ch.ethz.sis.afssftp.util.SftpListUtil;
import junit.framework.TestCase;
import lombok.NonNull;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class VirtualFileSystemProviderTest extends TestCase {
    {
        StaticInitializer.initialize();
    }

    public interface SeekableByteChannelProducer {
        SeekableByteChannel newByteChannel(VirtualFileSystemProvider virtualFileSystemProvider, Path path, Set<? extends OpenOption> set, FileAttribute<?>... fileAttributes) throws IOException;
    }

    public void stubForTestNewFileChannel(@NonNull SeekableByteChannelProducer seekableByteChannelProducer) throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        // AFS cases
        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "Good experiment (EXPERIMENT_3)",
                "files",
                "dir",
                "img.png"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );
        Mockito.doReturn(Optional.of(new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                null,
                false,
                new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", false, null, null),
                "/dir/img.png"
        ))).when(ftpPathLister).toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        AfsFileChannel afsFileChannel1 = new AfsFileChannel("EXPERIMENT_3", "/dir/img.png", user, 0L, true, false);
        Mockito.doReturn(afsFileChannel1).when(sftpFileUtil).createAfsFileChannel(
                "EXPERIMENT_3", "/dir/img.png", user, Set.of(StandardOpenOption.READ), false
        );
        assertEquals(afsFileChannel1, seekableByteChannelProducer.newByteChannel(virtualFileSystemProvider, examplePath, Set.of(StandardOpenOption.READ)));
        Mockito.verify(sftpFileUtil, Mockito.times(1)).createAfsFileChannel(
                "EXPERIMENT_3",
                "/dir/img.png",
                user,
                Set.of(StandardOpenOption.READ),
                false
        );


        Mockito.doReturn(Optional.of(new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                null,
                false,
                new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", true, null, null),
                "/dir/img.png"
        ))).when(ftpPathLister).toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        AfsFileChannel afsFileChannel2 = new AfsFileChannel("EXPERIMENT_3", "/dir/img.png", user, 0L, false, true);
        Mockito.doReturn(afsFileChannel2).when(sftpFileUtil).createAfsFileChannel(
                "EXPERIMENT_3", "/dir/img.png", user, Set.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND), true
        );
        assertEquals(afsFileChannel2, seekableByteChannelProducer.newByteChannel(virtualFileSystemProvider, examplePath, Set.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND)));
        Mockito.verify(sftpFileUtil, Mockito.times(1)).createAfsFileChannel(
                "EXPERIMENT_3",
                "/dir/img.png",
                user,
                Set.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND),
                true
        );

        // Non-AFS cases
        for (SftpNode.Type type : List.of(
                SftpNode.Type.SPACE,
                SftpNode.Type.PROJECT,
                SftpNode.Type.EXPERIMENT,
                SftpNode.Type.FOLDER,
                SftpNode.Type.SAMPLE,
                SftpNode.Type.DATA_SET)
        ) {
            Mockito.doReturn(Optional.of(new FtpPathLister.EntityDescriptor(
                    type,
                    "identifier",
                    false,
                    null,
                    null
            ))).when(ftpPathLister).toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

            Exception exception = null;
            try {
                seekableByteChannelProducer.newByteChannel(virtualFileSystemProvider, examplePath, Set.of(StandardOpenOption.READ));
            } catch (Exception e) {
                exception = e;
            }
            assertEquals(UnsupportedOperationException.class, exception.getClass());
        }
    }

    public void testNewFileChannel() throws Exception {
        stubForTestNewFileChannel(VirtualFileSystemProvider::newFileChannel);
    }

    public void testNewByteChannel() throws Exception {
        stubForTestNewFileChannel(VirtualFileSystemProvider::newByteChannel);
    }

    public void testNewDirectoryStream() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir"
        );

        List<String> pathSegments1 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "file1"
        );
        List<String> pathSegments2 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "file2"
        );
        List<String> pathSegments3 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "file3"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );

        List<Path> pathList = List.of(
                new SftpPath(new VirtualFileSystem(user, virtualFileSystemProvider), "/", pathSegments1),
                new SftpPath(new VirtualFileSystem(user, virtualFileSystemProvider), "/", pathSegments2),
                new SftpPath(new VirtualFileSystem(user, virtualFileSystemProvider), "/", pathSegments3)
        );

        Mockito.doReturn(pathList.stream().map(
            path -> {
                try {
                    return new StandardPathTranslator().fromPathSegments(((SftpPath) path).getPathSegments());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
        ).toList()).when(ftpPathLister).list(
                new StandardPathTranslator().fromPathSegments(pathSegments)
        );

        DirectoryStream<Path> directoryStream = virtualFileSystemProvider.newDirectoryStream(examplePath, null);
        List<Path> streamedPaths = new ArrayList<>();
        directoryStream.iterator().forEachRemaining(streamedPaths::add);
        assertEquals(pathList, streamedPaths);

        DirectoryStream<Path> filteredDirectoryStream = virtualFileSystemProvider.newDirectoryStream(
                examplePath,
                (path) -> !"file2".equals(path.getFileName().toString())
        );
        List<Path> streamedFilteredPaths = new ArrayList<>();
        filteredDirectoryStream.iterator().forEachRemaining(streamedFilteredPaths::add);
        assertEquals(List.of(
                new SftpPath(new VirtualFileSystem(user, virtualFileSystemProvider), "/", pathSegments1),
                new SftpPath(new VirtualFileSystem(user, virtualFileSystemProvider), "/", pathSegments3)
        ), streamedFilteredPaths);
    }

    public void testCreateDirectory() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "subdir"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );

        Mockito.doNothing().when(sftpListUtil).tryToCreateAfsFileRootIfNecessary("EXPERIMENT_3");
        Mockito.doNothing().when(sftpFileUtil).createAfsDirectory(
                Mockito.eq("EXPERIMENT_3"), Mockito.anyString(), Mockito.eq(user)
        );

        //AFS path with data-mutable entity
        FtpPathLister.EntityDescriptor entityDescriptor1 = new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                null,
                false,
                new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", true, null, null),
                "/dir/subdir"
        );
        Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        virtualFileSystemProvider.createDirectory(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .tryToCreateAfsFileRootIfNecessary("EXPERIMENT_3");
        Mockito.verify(sftpFileUtil, Mockito.times(1))
                .createAfsDirectory("EXPERIMENT_3", "/dir/subdir", user);
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //AFS path root "/" with data-mutable entity
        FtpPathLister.EntityDescriptor entityDescriptor2 = new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                null,
                false,
                new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", true, null, null),
                "/"
        );
        Mockito.doReturn(Optional.of(entityDescriptor2)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        virtualFileSystemProvider.createDirectory(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .tryToCreateAfsFileRootIfNecessary("EXPERIMENT_3");
        Mockito.verify(sftpFileUtil, Mockito.times(0))
                .createAfsDirectory("EXPERIMENT_3", "/dir/subdir", user);
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //AFS path with non-data-mutable entity
        FtpPathLister.EntityDescriptor entityDescriptor3 = new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                null,
                false,
                new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", false, null, null),
                "/dir/subdir"
        );
        Mockito.doReturn(Optional.of(entityDescriptor3)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        Exception exception = null;
        try {
            virtualFileSystemProvider.createDirectory(examplePath);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(UnsupportedOperationException.class, exception.getClass());
        Mockito.verify(sftpListUtil, Mockito.times(0))
                .tryToCreateAfsFileRootIfNecessary(Mockito.anyString());
        Mockito.verify(sftpFileUtil, Mockito.times(0))
                .createAfsDirectory(Mockito.anyString(), Mockito.anyString(), Mockito.eq(user));
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //Non-AFS data
        FtpPathLister.EntityDescriptor entityDescriptor4 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.EXPERIMENT,
                "EXPERIMENT_3",
                true,
                null, null
        );
        Mockito.doReturn(Optional.of(entityDescriptor4)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        exception = null;
        try {
            virtualFileSystemProvider.createDirectory(examplePath);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(UnsupportedOperationException.class, exception.getClass());
        Mockito.verify(sftpListUtil, Mockito.times(0))
                .tryToCreateAfsFileRootIfNecessary(Mockito.anyString());
        Mockito.verify(sftpFileUtil, Mockito.times(0))
                .createAfsDirectory(Mockito.anyString(), Mockito.anyString(), Mockito.eq(user));
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);
    }

    public void testDelete() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "subdir"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );

        Mockito.doNothing().when(sftpFileUtil).deleteAfsFile(
                Mockito.eq("EXPERIMENT_3"), Mockito.anyString(), Mockito.eq(user)
        );

        //AFS path with data-mutable entity
        FtpPathLister.EntityDescriptor entityDescriptor1 = new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                null,
                false,
                new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", true, null, null),
                "/dir/subdir"
        );
        Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpFileUtil, Mockito.times(1))
                .deleteAfsFile("EXPERIMENT_3", "/dir/subdir", user);
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //AFS path root "/" with data-mutable entity
        FtpPathLister.EntityDescriptor entityDescriptor2 = new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                null,
                false,
                new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", true, null, null),
                "/"
        );
        Mockito.doReturn(Optional.of(entityDescriptor2)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpFileUtil, Mockito.times(0))
                .deleteAfsFile("EXPERIMENT_3", "/dir/subdir", user);
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //AFS path with non-data-mutable entity
        FtpPathLister.EntityDescriptor entityDescriptor3 = new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                null,
                false,
                new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", false, null, null),
                "/dir/subdir"
        );
        Mockito.doReturn(Optional.of(entityDescriptor3)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        Exception exception = null;
        try {
            virtualFileSystemProvider.delete(examplePath);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(UnsupportedOperationException.class, exception.getClass());
        Mockito.verify(sftpFileUtil, Mockito.times(0))
                .deleteAfsFile(Mockito.anyString(), Mockito.anyString(), Mockito.eq(user));
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //Non-AFS data
        FtpPathLister.EntityDescriptor entityDescriptor4 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.EXPERIMENT,
                "EXPERIMENT_3",
                true,
                null, null
        );
        Mockito.doReturn(Optional.of(entityDescriptor4)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        exception = null;
        try {
            virtualFileSystemProvider.delete(examplePath);
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(UnsupportedOperationException.class, exception.getClass());
        Mockito.verify(sftpFileUtil, Mockito.times(0))
                .deleteAfsFile(Mockito.anyString(), Mockito.anyString(), Mockito.eq(user));
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);
    }

    public void testCopy() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments1 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "file1"
        );
        Path examplePath1 = new SftpPath(virtualFileSystem, "/",
                pathSegments1
        );

        List<String> pathSegments2 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_4",
                "files",
                "dir",
                "file2"
        );
        Path examplePath2 = new SftpPath(virtualFileSystem, "/",
                pathSegments2
        );

        Mockito.doNothing().when(sftpFileUtil).copyAfsFile(
                Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(),
                Mockito.eq(user), Mockito.anyBoolean()
        );

        for (boolean isAfsDataPath1: List.of(true, false)) {
            for (boolean isAfsDataPath2: List.of(true, false)) {
                for (boolean isMutableEntityPath1: List.of(true, false)) {
                    for (boolean isMutableEntityPath2: List.of(true, false)) {
                        for(Set<StandardCopyOption> copyOptions : List.of(Set.of(StandardCopyOption.REPLACE_EXISTING), new HashSet<StandardCopyOption>())) {
                            Mockito.clearInvocations(sftpFileUtil);

                            FtpPathLister.EntityDescriptor entityDescriptor1 = isAfsDataPath1 ?
                                    new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                                            null,
                                            false,
                                            new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", isMutableEntityPath1, null, null),
                                            "/dir/file1"
                                    ) : new FtpPathLister.EntityDescriptor(
                                    SftpNode.Type.EXPERIMENT,
                                    "EXPERIMENT_3",
                                    isMutableEntityPath1,
                                    null, null
                            );
                            Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                                    .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));

                            FtpPathLister.EntityDescriptor entityDescriptor2 = isAfsDataPath2 ?
                                    new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                                            null,
                                            false,
                                            new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_4", isMutableEntityPath2, null, null),
                                            "/dir/file2"
                                    ) : new FtpPathLister.EntityDescriptor(
                                    SftpNode.Type.EXPERIMENT,
                                    "EXPERIMENT_4",
                                    isMutableEntityPath2,
                                    null, null
                            );
                            Mockito.doReturn(Optional.of(entityDescriptor2)).when(ftpPathLister)
                                    .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments2));

                            Exception exception = null;
                            try {
                                virtualFileSystemProvider.copy(examplePath1, examplePath2, copyOptions.toArray(CopyOption[]::new));
                            } catch (Exception e) {
                                exception = e;
                            }

                            if (isAfsDataPath1 && isAfsDataPath2 && isMutableEntityPath2) {
                                assertNull(exception);
                                Mockito.verify(sftpFileUtil, Mockito.times(1)).copyAfsFile(
                                        "EXPERIMENT_3", "/dir/file1",
                                        "EXPERIMENT_4", "/dir/file2",
                                        user,
                                        copyOptions.contains(StandardCopyOption.REPLACE_EXISTING)
                                );
                            } else {
                                assertEquals(UnsupportedOperationException.class, exception.getClass());
                                Mockito.verify(sftpFileUtil, Mockito.times(0)).copyAfsFile(
                                        Mockito.anyString(), Mockito.anyString(),
                                        Mockito.anyString(), Mockito.anyString(),
                                        Mockito.eq(user),
                                        Mockito.anyBoolean()
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public void testMove() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments1 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "file1"
        );
        Path examplePath1 = new SftpPath(virtualFileSystem, "/",
                pathSegments1
        );

        List<String> pathSegments2 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_4",
                "files",
                "dir",
                "file2"
        );
        Path examplePath2 = new SftpPath(virtualFileSystem, "/",
                pathSegments2
        );

        Mockito.doNothing().when(sftpFileUtil).moveAfsFile(
                Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(),
                Mockito.eq(user), Mockito.anyBoolean()
        );

        for (boolean isAfsDataPath1: List.of(true, false)) {
            for (boolean isAfsDataPath2: List.of(true, false)) {
                for (boolean isMutableEntityPath1: List.of(true, false)) {
                    for (boolean isMutableEntityPath2: List.of(true, false)) {
                        for(Set<StandardCopyOption> copyOptions : List.of(Set.of(StandardCopyOption.REPLACE_EXISTING), new HashSet<StandardCopyOption>())) {
                            Mockito.clearInvocations(sftpFileUtil);

                            FtpPathLister.EntityDescriptor entityDescriptor1 = isAfsDataPath1 ?
                                    new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                                            null,
                                            false,
                                            new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", isMutableEntityPath1, null, null),
                                            "/dir/file1"
                                    ) : new FtpPathLister.EntityDescriptor(
                                    SftpNode.Type.EXPERIMENT,
                                    "EXPERIMENT_3",
                                    isMutableEntityPath1,
                                    null, null
                            );
                            Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                                    .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));

                            FtpPathLister.EntityDescriptor entityDescriptor2 = isAfsDataPath2 ?
                                    new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
                                            null,
                                            false,
                                            new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_4", isMutableEntityPath2, null, null),
                                            "/dir/file2"
                                    ) : new FtpPathLister.EntityDescriptor(
                                    SftpNode.Type.EXPERIMENT,
                                    "EXPERIMENT_4",
                                    isMutableEntityPath2,
                                    null, null
                            );
                            Mockito.doReturn(Optional.of(entityDescriptor2)).when(ftpPathLister)
                                    .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments2));

                            Exception exception = null;
                            try {
                                virtualFileSystemProvider.move(examplePath1, examplePath2, copyOptions.toArray(CopyOption[]::new));
                            } catch (Exception e) {
                                exception = e;
                            }

                            if (isAfsDataPath1 && isAfsDataPath2 && isMutableEntityPath1 && isMutableEntityPath2) {
                                assertNull(exception);
                                Mockito.verify(sftpFileUtil, Mockito.times(1)).moveAfsFile(
                                        "EXPERIMENT_3", "/dir/file1",
                                        "EXPERIMENT_4", "/dir/file2",
                                        user,
                                        copyOptions.contains(StandardCopyOption.REPLACE_EXISTING)
                                );
                            } else {
                                assertEquals(UnsupportedOperationException.class, exception.getClass());
                                Mockito.verify(sftpFileUtil, Mockito.times(0)).moveAfsFile(
                                        Mockito.anyString(), Mockito.anyString(),
                                        Mockito.anyString(), Mockito.anyString(),
                                        Mockito.eq(user),
                                        Mockito.anyBoolean()
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public void testIsSameFile() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments1 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img.png"
        );
        Path examplePath1 = new SftpPath(virtualFileSystem, "/",
                pathSegments1
        );

        List<String> pathSegments2 = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img2.png"
        );
        Path examplePath2 = new SftpPath(virtualFileSystem, "/",
                pathSegments2
        );

        FtpPathLister.EntityDescriptor entityDescriptor1 = new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
            null,
            false,
            new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", true, null, null),
            "/dir/img.png"
        );

        FtpPathLister.EntityDescriptor entityDescriptor2 = new FtpPathLister.EntityDescriptor(SftpNode.Type.AFS_FILE,
            null,
            false,
            new FtpPathLister.EntityDescriptor(SftpNode.Type.EXPERIMENT, "EXPERIMENT_3", true, null, null),
            "/dir/img2.png"
        );

        assertTrue(virtualFileSystemProvider.isSameFile(examplePath1, examplePath1));
        assertTrue(virtualFileSystemProvider.isSameFile(examplePath2, examplePath2));

        Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));
        Mockito.doReturn(Optional.of(entityDescriptor2)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments2));
        assertFalse(virtualFileSystemProvider.isSameFile(examplePath1, examplePath2));

        Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));
        Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments2));
        assertTrue(virtualFileSystemProvider.isSameFile(examplePath1, examplePath2));

        Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));
        Mockito.doReturn(Optional.empty()).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments2));
        assertFalse(virtualFileSystemProvider.isSameFile(examplePath1, examplePath2));
        assertTrue(virtualFileSystemProvider.isSameFile(examplePath1, examplePath1));

        Mockito.doReturn(Optional.empty()).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));
        Mockito.doReturn(Optional.of(entityDescriptor2)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments2));
        assertFalse(virtualFileSystemProvider.isSameFile(examplePath1, examplePath2));
        assertTrue(virtualFileSystemProvider.isSameFile(examplePath1, examplePath1));

        Mockito.doReturn(Optional.empty()).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));
        Mockito.doReturn(Optional.empty()).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments2));
        assertFalse(virtualFileSystemProvider.isSameFile(examplePath1, examplePath2));
        assertTrue(virtualFileSystemProvider.isSameFile(examplePath1, examplePath1));
    }

    public void testIsHidden() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img.png"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );
        assertFalse(virtualFileSystemProvider.isHidden(examplePath));
    }

    public void testGetFileStore() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img.png"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );
        assertNull(virtualFileSystemProvider.getFileStore(examplePath));
    }

    public void testCheckAccess() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img.png"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );
        virtualFileSystemProvider.checkAccess(examplePath, AccessMode.READ);
        virtualFileSystemProvider.checkAccess(examplePath, AccessMode.WRITE);
        virtualFileSystemProvider.checkAccess(examplePath, AccessMode.EXECUTE);
    }

    public void testGetFileAttributeView() {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img.png"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );
        assertNull(virtualFileSystemProvider.getFileAttributeView(examplePath,
                BasicFileAttributeView.class));
    }

    public void testReadAttributes() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img.png"
        );
        SftpPath examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );

        SftpNodeChain sftpNodeChain = ftpPathTranslator.fromPathSegments(examplePath.getPathSegments());
        SftpFileAttributes sampleAttributes = SftpListUtil.getDefaultAbstractDirectoryAttributes();
        Mockito.doReturn(sampleAttributes)
                        .when(ftpPathLister).readAttributes(sftpNodeChain);
        assertEquals(sampleAttributes, virtualFileSystemProvider.readAttributes(examplePath, BasicFileAttributes.class));
        assertEquals(sampleAttributes, virtualFileSystemProvider.readAttributes(examplePath, PosixFileAttributes.class));
        assertEquals(Map.of(
                "isRegularFile", sampleAttributes.isRegularFile(),
                "isDirectory",  sampleAttributes.isDirectory(),
                "isSymbolicLink", sampleAttributes.isSymbolicLink(),
                "permissions", sampleAttributes.permissions(),
                "size", sampleAttributes.getSize(),
                "lastModifiedTime", sampleAttributes.getModifiedTime(),
                "lastAccessTime", sampleAttributes.getAccessTime(),
                "owner", sampleAttributes.owner(),
                "group", sampleAttributes.group()
        ), virtualFileSystemProvider.readAttributes(examplePath, "any-string"));
    }

    public void testSetAttribute() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img.png"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );

        Exception exception = null;
        try {
            virtualFileSystemProvider.setAttribute(examplePath,
                    "lastModifiedTime",
                    FileTime.fromMillis(System.currentTimeMillis()));
        } catch (Exception e) {
            exception = e;
        }
        assertEquals(UnsupportedOperationException.class, exception.getClass());
    }

    public void testGetNodeChainFromPath() throws Exception {
        User user = User.builder().username("user").sessionToken("session").build();
        FtpPathTranslator ftpPathTranslator = Mockito.spy(new StandardPathTranslator());
        FtpPathLister ftpPathLister = Mockito.mock(FtpPathLister.class);
        SftpListUtil sftpListUtil = Mockito.spy(new SftpListUtil(user));
        SftpFileUtil sftpFileUtil = Mockito.spy(new SftpFileUtil(user));
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user,
                ftpPathTranslator,
                ftpPathLister,
                sftpListUtil,
                sftpFileUtil
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        virtualFileSystemProvider.acceptCreatedFileSystem(virtualFileSystem);

        List<String> pathSegments = List.of("spaces",
                "SPACE_1",
                "projects",
                "PROJECT_2",
                "experiments",
                "EXPERIMENT_3",
                "files",
                "dir",
                "img.png"
        );
        Path examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );
        SftpNodeChain nodeChain = new StandardPathTranslator().fromPathSegments(pathSegments);
        assertEquals(nodeChain, virtualFileSystemProvider.getNodeChainFromPath(examplePath));
        Mockito.verify(ftpPathTranslator, Mockito.times(1)).fromPathSegments(
                pathSegments
        );
    }

    public void testGetPathFromNodeChain() {
        User user = User.builder().username("user").sessionToken("session").build();
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(
                user
        );
        VirtualFileSystem virtualFileSystem = new VirtualFileSystem(user, virtualFileSystemProvider);
        SftpNodeChain sftpNodeChain = new SftpNodeChain(
                List.of(
                TestHelper.createRandomNodeOfType(SftpNode.Type.ROOT),
                TestHelper.createRandomNodeOfType(SftpNode.Type.SUBLEVEL).toBuilder()
                        .identifier(Optional.of(StandardPathTranslator.SPACE_TYPE_LABEL)).build(),
                TestHelper.createRandomNodeOfType(SftpNode.Type.SPACE)
                )
        );
        Path sftpPath = virtualFileSystemProvider.getPathFromNodeChain(sftpNodeChain);
        assertTrue(sftpPath instanceof SftpPath);
        assertEquals(virtualFileSystem, sftpPath.getFileSystem());
        assertEquals(new SftpPath(virtualFileSystem, "/", Collections.emptyList()), sftpPath.getRoot());
        assertEquals(List.of("spaces", sftpNodeChain.getLast().get().getIdentifier().get()), ((SftpPath) sftpPath).getPathSegments());
    }

    public void testToDirectoryStream() {
        User user = User.builder().username("user").sessionToken("session").build();
        VirtualFileSystemProvider virtualFileSystemProvider = new VirtualFileSystemProvider(user);
        List<Path> pathList = List.of(
                new SftpPath(new VirtualFileSystem(user, virtualFileSystemProvider), "/", List.of("dir1", "file1")),
                new SftpPath(new VirtualFileSystem(user, virtualFileSystemProvider), "/", List.of("dir1", "file2")),
                new SftpPath(new VirtualFileSystem(user, virtualFileSystemProvider), "/", List.of("dir1", "file3"))
        );
        DirectoryStream<Path> directoryStream = VirtualFileSystemProvider.toDirectoryStream(pathList);
        List<Path> streamedPaths = new ArrayList<>();
        directoryStream.iterator().forEachRemaining(streamedPaths::add);
        assertEquals(pathList, streamedPaths);
    }
}