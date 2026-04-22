package ch.ethz.sis.afssftp.filesystemview;

import lombok.NonNull;

import java.util.List;

public interface FtpPathTranslator {
    @NonNull List<@NonNull String> toPathSegments(@NonNull OpenBISSftpNodeChain nodeChain) throws MalformedPathException;
    @NonNull OpenBISSftpNodeChain fromPathSegments(@NonNull List<@NonNull String> pathSegments) throws MalformedPathException;

    class MalformedPathException extends Exception {
        public MalformedPathException() {}

        public MalformedPathException(String message) {
            super(message);
        }
    }
}
