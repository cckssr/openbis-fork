package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.authentication.OpenBISPasswordAuthenticator;
import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.SessionContext;

import java.io.IOException;
import java.util.Collections;

public class OpenBISFileSystemFactory implements FileSystemFactory {
    @Override
    public OpenBISSftpPath getUserHomeDir(SessionContext session) throws IOException {
        return new OpenBISSftpPath(createFileSystem(session), "/", Collections.emptyList());
    }

    @Override
    public OpenBISFileSystem createFileSystem(SessionContext session) throws IOException {
        OpenBISUser sessionUser = session.getAttribute(OpenBISPasswordAuthenticator.USER_ATTRIBUTE);
        if (sessionUser != null) {
            OpenBISFileSystemProvider openBISFileSystemProvider =
                    new OpenBISFileSystemProvider(sessionUser);
            return new OpenBISFileSystem(sessionUser, openBISFileSystemProvider);
        } else {
            throw new IOException("Unauthenticated session");
        }
    }
}
