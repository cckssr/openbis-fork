package ch.ethz.sis.afssftp.filesystemview;

import lombok.NonNull;
import org.apache.sshd.common.file.util.BasePath;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

public class OpenBISSftpPath extends BasePath<OpenBISSftpPath, OpenBISFileSystem> {
    List<String> pathSegments;

    protected OpenBISSftpPath(OpenBISFileSystem fileSystem, String root, List<String> names) {
        super(fileSystem, root, names);
        this.pathSegments = names;
    }

    @Override
    public Path toRealPath(@NonNull LinkOption... linkOptions) throws IOException {
        throw new UnsupportedOperationException("Unsupported conversion to real file-system path");
    }

    public List<String> getPathSegments() {
        return pathSegments;
    }
}
