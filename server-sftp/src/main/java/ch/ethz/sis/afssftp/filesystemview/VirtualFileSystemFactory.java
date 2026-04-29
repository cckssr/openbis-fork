package ch.ethz.sis.afssftp.filesystemview;

import ch.ethz.sis.afssftp.authentication.PasswordAuthenticator;
import ch.ethz.sis.afssftp.authentication.User;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.SessionContext;

import java.io.IOException;
import java.util.Collections;

public class VirtualFileSystemFactory implements FileSystemFactory {
    @Override
    public SftpPath getUserHomeDir(SessionContext session) throws IOException {
        return new SftpPath(createFileSystem(session), "/", Collections.emptyList());
    }

    @Override
    public VirtualFileSystem createFileSystem(SessionContext session) throws IOException {
        User sessionUser = session.getAttribute(PasswordAuthenticator.USER_ATTRIBUTE);
        if (sessionUser != null) {
            VirtualFileSystemProvider virtualFileSystemProvider =
                    new VirtualFileSystemProvider(sessionUser);
            return new VirtualFileSystem(sessionUser, virtualFileSystemProvider);
        } else {
            throw new IOException("Unauthenticated session");
        }
    }
}
