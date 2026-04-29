package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.authentication.User;
import org.apache.sshd.common.file.util.BaseFileSystem;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class VirtualFileSystem extends BaseFileSystem<SftpPath> {
    private final User user;
    private final VirtualFileSystemProvider virtualFileSystemProvider;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public VirtualFileSystem(
            User user,
            VirtualFileSystemProvider virtualFileSystemProvider
    ) {
        super(virtualFileSystemProvider);
        this.user = user;
        virtualFileSystemProvider.acceptCreatedFileSystem(this);
        this.virtualFileSystemProvider = virtualFileSystemProvider;
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
    }

    @Override
    public boolean isOpen() {
        return closed.get();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(Path.of("/"));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Unsupported file-stores");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Set.of("posix");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Unsupported user-principal lookup-service");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Unsupported watch-services");
    }

    protected SftpPath create(String root, List<String> names) {
        return new SftpPath(this, root, names);
    }
}
