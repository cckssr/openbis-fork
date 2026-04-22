package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import org.apache.sshd.common.file.util.BaseFileSystem;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpenBISFileSystem extends BaseFileSystem<OpenBISSftpPath> {
    private final OpenBISUser openBISUser;
    private final OpenBISFileSystemProvider openBISFileSystemProvider;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public OpenBISFileSystem(
            OpenBISUser openBISUser,
            OpenBISFileSystemProvider openBISFileSystemProvider
    ) {
        super(openBISFileSystemProvider);
        this.openBISUser = openBISUser;
        openBISFileSystemProvider.acceptCreatedFileSystem(this);
        this.openBISFileSystemProvider = openBISFileSystemProvider;
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

    protected OpenBISSftpPath create(String root, List<String> names) {
        return new OpenBISSftpPath(this, root, names);
    }
}
