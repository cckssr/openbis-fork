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
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, false)
                ),
                "/dir/img.png",
                null
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
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
                ),
                "/dir/img.png",
                null
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
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("identifier"),
                    Optional.empty(),
                    null,
                    null,
                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, false)
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
        FtpPathLister.EntityDescriptor entityDescriptor1 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
                ),
                "/dir/subdir",
                null
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
        FtpPathLister.EntityDescriptor entityDescriptor2 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
                ),
                "/",
                null
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
        FtpPathLister.EntityDescriptor entityDescriptor3 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, false)
                ),
                "/dir/subdir",
                null
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

        //SPACE
        FtpPathLister.EntityDescriptor entityDescriptor4 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.SPACE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("space_id_1"),
                Optional.empty(),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor4)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).createSpace(Mockito.anyString());
        virtualFileSystemProvider.createDirectory(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .createSpace("space_id_1");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //PROJECT
        FtpPathLister.EntityDescriptor entityDescriptor5 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.PROJECT,
                Optional.of("space_1"),
                Optional.of("project_1"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("/SPACE_1/PROJECT_1"),
                Optional.empty(),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor5)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).createProject(Mockito.anyString(), Mockito.anyString());
        virtualFileSystemProvider.createDirectory(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .createProject("space_1", "project_1");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //EXPERIMENT
        FtpPathLister.EntityDescriptor entityDescriptor6 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.EXPERIMENT,
                Optional.of("space_1"),
                Optional.of("project_1"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("experiment name!!!"),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor6)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).createExperiment(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        virtualFileSystemProvider.createDirectory(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .createExperiment("space_1", "project_1", "experiment name!!!");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //SAMPLE, FOLDER
        for (boolean folder : List.of(false, true)) {
            for (boolean withProject : List.of(false, true)) {
                for (boolean withExperiment : List.of(false, true)) {
                    for (boolean withParentSample : List.of(false, true)) {
                        FtpPathLister.EntityDescriptor entityDescriptor7 = new FtpPathLister.EntityDescriptor(
                                folder ? SftpNode.Type.FOLDER : SftpNode.Type.SAMPLE,
                                Optional.of("space_1"),
                                Optional.ofNullable(withProject ? "project_1" : null),
                                Optional.ofNullable(withExperiment ? "experiment_1" : null),
                                Optional.ofNullable(withParentSample ? "sample_1" : null),
                                Optional.empty(),
                                Optional.of("sample name!!!"),
                                null,
                                null,
                                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
                        );
                        Mockito.doReturn(Optional.of(entityDescriptor7)).when(ftpPathLister)
                                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
                        Mockito.doNothing().when(sftpListUtil).createSample(
                                Mockito.any(), Mockito.any(), Mockito.any(),
                                Mockito.any(), Mockito.any(), Mockito.anyBoolean()
                        );
                        virtualFileSystemProvider.createDirectory(examplePath);
                        Mockito.verify(sftpListUtil, Mockito.times(1))
                                .createSample(
                                        "space_1",
                                        withProject ? "project_1" : null,
                                        withExperiment ? "experiment_1" : null,
                                        withParentSample ? "sample_1" : null,
                                        "sample name!!!",
                                        folder
                                );
                        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);
                    }
                }
            }
        }

        //other
        for (SftpNode.Type type : List.of(SftpNode.Type.SUBLEVEL, SftpNode.Type.ROOT, SftpNode.Type.DATA_SET)) {
            FtpPathLister.EntityDescriptor entityDescriptor10 = new FtpPathLister.EntityDescriptor(
                    type,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("id"),
                    Optional.empty(),
                    null,
                    null,
                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
            );
            Mockito.doReturn(Optional.of(entityDescriptor10)).when(ftpPathLister)
                    .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
            Exception exc = null;
            try {
                virtualFileSystemProvider.createDirectory(examplePath);
            } catch (Exception e) {
                exc = e;
            }
            assertEquals(UnsupportedOperationException.class, exc.getClass());
            Mockito.clearInvocations(sftpListUtil, sftpFileUtil);
        }
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
        FtpPathLister.EntityDescriptor entityDescriptor1 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
                ),
                "/dir/subdir",
                null
        );
        Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpFileUtil, Mockito.times(1))
                .deleteAfsFile("EXPERIMENT_3", "/dir/subdir", user);
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //AFS path root "/" with data-mutable entity
        FtpPathLister.EntityDescriptor entityDescriptor2 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
                ),
                "/",
                null
        );
        Mockito.doReturn(Optional.of(entityDescriptor2)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));

        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpFileUtil, Mockito.times(1))
                .deleteAfsFile("EXPERIMENT_3", "/", user);
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //AFS path with non-data-mutable entity
        FtpPathLister.EntityDescriptor entityDescriptor3 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, false)
                ),
                "/dir/subdir",
                null
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

        //SPACE
        FtpPathLister.EntityDescriptor entityDescriptor4 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.SPACE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("space_id_1"),
                Optional.empty(),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor4)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).deleteSpace(Mockito.anyString());
        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .deleteSpace("space_id_1");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //PROJECT
        FtpPathLister.EntityDescriptor entityDescriptor5 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.PROJECT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("/SPACE_1/PROJECT_1"),
                Optional.empty(),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor5)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).deleteProject(Mockito.anyString());
        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .deleteProject("/SPACE_1/PROJECT_1");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //EXPERIMENT
        FtpPathLister.EntityDescriptor entityDescriptor6 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.EXPERIMENT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("experiment_1"),
                Optional.empty(),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor6)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).deleteExperiment(Mockito.anyString());
        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .deleteExperiment("experiment_1");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //SAMPLE
        FtpPathLister.EntityDescriptor entityDescriptor7 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.SAMPLE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("sample_1"),
                Optional.empty(),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor7)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).deleteSample(Mockito.anyString());
        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .deleteSample("sample_1");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //FOLDER
        FtpPathLister.EntityDescriptor entityDescriptor8 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.FOLDER,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("folder_1"),
                Optional.empty(),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor8)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).deleteSample(Mockito.anyString());
        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .deleteSample("folder_1");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //DATASET
        FtpPathLister.EntityDescriptor entityDescriptor9 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.DATA_SET,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("dataset_1"),
                Optional.empty(),
                null,
                null,
                new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
        );
        Mockito.doReturn(Optional.of(entityDescriptor9)).when(ftpPathLister)
                .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
        Mockito.doNothing().when(sftpListUtil).deleteDataSet(Mockito.anyString());
        virtualFileSystemProvider.delete(examplePath);
        Mockito.verify(sftpListUtil, Mockito.times(1))
                .deleteDataSet("dataset_1");
        Mockito.clearInvocations(sftpListUtil, sftpFileUtil);

        //other
        for (SftpNode.Type type : List.of(SftpNode.Type.SUBLEVEL, SftpNode.Type.ROOT)) {
            FtpPathLister.EntityDescriptor entityDescriptor10 = new FtpPathLister.EntityDescriptor(
                    type,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("id"),
                    Optional.empty(),
                    null,
                    null,
                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
            );
            Mockito.doReturn(Optional.of(entityDescriptor10)).when(ftpPathLister)
                    .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments));
            Exception exc = null;
            try {
                virtualFileSystemProvider.delete(examplePath);
            } catch (Exception e) {
                exc = e;
            }
            assertEquals(UnsupportedOperationException.class, exc.getClass());
            Mockito.clearInvocations(sftpListUtil, sftpFileUtil);
        }
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
                "(EXPERIMENT_3)",
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
                "(EXPERIMENT_4)",
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
                                    new FtpPathLister.EntityDescriptor(
                                            SftpNode.Type.AFS_FILE,
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            new FtpPathLister.EntityDescriptor(
                                                    SftpNode.Type.EXPERIMENT,
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.of("EXPERIMENT_3"),
                                                    Optional.empty(),
                                                    null,
                                                    null,
                                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, isMutableEntityPath1)
                                            ),
                                            "/dir/file1",
                                            null
                                    ) : new FtpPathLister.EntityDescriptor(
                                            SftpNode.Type.EXPERIMENT,
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.of("(EXPERIMENT_3)"),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            null,
                                            null,
                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, isMutableEntityPath1)
                                    );
                            Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                                    .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));

                            FtpPathLister.EntityDescriptor entityDescriptor2 = isAfsDataPath2 ?
                                    new FtpPathLister.EntityDescriptor(
                                            SftpNode.Type.AFS_FILE,
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            new FtpPathLister.EntityDescriptor(
                                                    SftpNode.Type.EXPERIMENT,
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.of("EXPERIMENT_4"),
                                                    Optional.empty(),
                                                    null,
                                                    null,
                                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, isMutableEntityPath2)
                                            ),
                                            "/dir/file2",
                                            null
                                    ) : new FtpPathLister.EntityDescriptor(
                                            SftpNode.Type.EXPERIMENT,
                                            Optional.empty(),
                                            Optional.empty(),
                                    Optional.empty(),
                                            Optional.empty(),
                                            Optional.of("EXPERIMENT_4"),
                                            Optional.empty(),
                                            null,
                                            null,
                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, isMutableEntityPath2)
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
                "(EXPERIMENT_3)",
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
                "(EXPERIMENT_4)",
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
                                    new FtpPathLister.EntityDescriptor(
                                            SftpNode.Type.AFS_FILE,
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            new FtpPathLister.EntityDescriptor(
                                                    SftpNode.Type.EXPERIMENT,
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.of("EXPERIMENT_3"),
                                                    Optional.empty(),
                                                    null,
                                                    null,
                                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, isMutableEntityPath1)
                                            ),
                                            "/dir/file1",
                                            null
                                    ) : new FtpPathLister.EntityDescriptor(
                                            SftpNode.Type.EXPERIMENT,
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.of("EXPERIMENT_3"),
                                            Optional.empty(),
                                            null,
                                            null,
                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, isMutableEntityPath1)
                                    );
                            Mockito.doReturn(Optional.of(entityDescriptor1)).when(ftpPathLister)
                                    .toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));

                            FtpPathLister.EntityDescriptor entityDescriptor2 = isAfsDataPath2 ?
                                    new FtpPathLister.EntityDescriptor(
                                            SftpNode.Type.AFS_FILE,
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            new FtpPathLister.EntityDescriptor(
                                                    SftpNode.Type.EXPERIMENT,
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.of("EXPERIMENT_4"),
                                                    Optional.empty(),
                                                    null,
                                                    null,
                                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, isMutableEntityPath2)
                                            ),
                                            "/dir/file2",
                                            null
                                    ) : new FtpPathLister.EntityDescriptor(
                                            SftpNode.Type.EXPERIMENT,
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.empty(),
                                            Optional.of("EXPERIMENT_4"),
                                            Optional.empty(),
                                            null,
                                            null,
                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, isMutableEntityPath2)
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

        //Test renaming entities
        for (CopyOption copyOption : List.of(StandardCopyOption.values())) {
            for (SftpNode.Type type1 : SftpNode.Type.values()) {
                for (SftpNode.Type type2 : SftpNode.Type.values()) {
                    for (Optional<String> identifier1 : List.of(Optional.empty().map(Object::toString), Optional.of("id1"), Optional.of("id2"))) {
                        for (Optional<String> identifier2 : List.of(Optional.empty().map(Object::toString), Optional.of("id1"), Optional.of("id2"))) {
                            Mockito.clearInvocations(sftpListUtil, sftpFileUtil);
                            Mockito.doReturn(Optional.of(new FtpPathLister.EntityDescriptor(
                                    type1,
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    identifier1,
                                    Optional.empty(),
                                    null,
                                    null,
                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, false)
                            ))).when(ftpPathLister).toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments1));
                            Mockito.doReturn(Optional.of(new FtpPathLister.EntityDescriptor(
                                    type2,
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    identifier2,
                                    Optional.of("New name@@@ #"),
                                    null,
                                    null,
                                    new SftpListUtil.EntityBasicInfo(true, 123L, 345L, false)
                            ))).when(ftpPathLister).toEntityDescriptor(new StandardPathTranslator().fromPathSegments(pathSegments2));

                            if (type1 != SftpNode.Type.AFS_FILE && type2 != SftpNode.Type.AFS_FILE) {
                                if (type1 != type2 || identifier1.isEmpty() || !identifier1.equals(identifier2)) {
                                    Exception exception = null;
                                    try {
                                        virtualFileSystemProvider.move(examplePath1, examplePath2, copyOption);
                                    } catch (Exception e) {
                                        exception = e;
                                    }
                                    assertEquals(UnsupportedOperationException.class, exception.getClass());
                                } else {
                                    switch (type1) {
                                        case SAMPLE, FOLDER -> {
                                            Mockito.doNothing().when(sftpListUtil).renameSample(Mockito.anyString(), Mockito.anyString());
                                            virtualFileSystemProvider.move(examplePath1, examplePath2, copyOption);
                                            Mockito.verify(sftpListUtil, Mockito.times(1))
                                                    .renameSample(identifier1.get(), "New name@@@ #");
                                        }
                                        case EXPERIMENT -> {
                                            Mockito.doNothing().when(sftpListUtil).renameExperiment(Mockito.anyString(), Mockito.anyString());
                                            virtualFileSystemProvider.move(examplePath1, examplePath2, copyOption);
                                            Mockito.verify(sftpListUtil, Mockito.times(1))
                                                    .renameExperiment(identifier1.get(), "New name@@@ #");
                                        }
                                        default -> {
                                            Exception exception = null;
                                            try {
                                                virtualFileSystemProvider.move(examplePath1, examplePath2, copyOption);
                                            } catch (Exception e) {
                                                exception = e;
                                            }
                                            assertEquals(UnsupportedOperationException.class, exception.getClass());
                                        }
                                    }
                                }
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
                "(EXPERIMENT_3)",
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
                "(EXPERIMENT_3)",
                "files",
                "dir",
                "img2.png"
        );
        Path examplePath2 = new SftpPath(virtualFileSystem, "/",
                pathSegments2
        );

        FtpPathLister.EntityDescriptor entityDescriptor1 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
                ),
                "/dir/img.png",
                null
        );

        FtpPathLister.EntityDescriptor entityDescriptor2 = new FtpPathLister.EntityDescriptor(
                SftpNode.Type.AFS_FILE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new FtpPathLister.EntityDescriptor(
                        SftpNode.Type.EXPERIMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("EXPERIMENT_3"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        null,
                        null,
                        new SftpListUtil.EntityBasicInfo(true, 123L, 345L, true)
                ),
                "/dir/img2.png",
                null
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
                "(EXPERIMENT_3)",
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
                "(EXPERIMENT_3)",
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
                "(EXPERIMENT_3)",
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
                "(EXPERIMENT_3)",
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
                "(EXPERIMENT_3)",
                "files",
                "dir",
                "img.png"
        );
        SftpPath examplePath = new SftpPath(virtualFileSystem, "/",
                pathSegments
        );

        SftpNodeChain sftpNodeChain = ftpPathTranslator.fromPathSegments(examplePath.getPathSegments());
        SftpFileAttributes sampleAttributes = SftpListUtil.getDefaultAbstractDirectoryAttributes(false, null, null);
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
                "(EXPERIMENT_3)",
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
                "(EXPERIMENT_3)",
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