package ch.ethz.sis.sftp.authentication;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class User {
    @NonNull final String username;
    @NonNull final String sessionToken;
}
