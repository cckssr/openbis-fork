package ch.ethz.sis.afssftp.filesystemview;

import lombok.NonNull;

import java.util.List;

public interface FtpPathLister {
    @NonNull List<@NonNull SftpNodeChain> list(@NonNull SftpNodeChain directory);
    SftpFileAttributes readAttributes(@NonNull SftpNodeChain nodeChain);
}
