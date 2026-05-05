package ch.ethz.sis.afssftp.filesystemview;

import lombok.NonNull;

import java.io.IOException;
import java.util.List;

public interface FtpPathLister {
    @NonNull List<@NonNull SftpNodeChain> list(@NonNull SftpNodeChain directory) throws IOException;
    SftpFileAttributes readAttributes(@NonNull SftpNodeChain nodeChain) throws IOException;
}
