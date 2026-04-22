package ch.ethz.sis.afssftp.filesystemview;

import lombok.NonNull;

import java.util.List;

public interface FtpPathLister {
    @NonNull List<@NonNull OpenBISSftpNodeChain> list(@NonNull OpenBISSftpNodeChain directory);
    OpenBISSftpFileAttributes readAttributes(@NonNull OpenBISSftpNodeChain nodeChain);
}
