package ch.ethz.sis.afssftp.authentication;

import ch.ethz.sis.openbis.generic.OpenBIS;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class User {
    @NonNull final String username;
    @NonNull final String sessionToken;
}
