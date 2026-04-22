package ch.ethz.sis.afssftp.authentication;

import ch.ethz.sis.afssftp.util.OpenBISClientUtil;
import ch.ethz.sis.openbis.generic.OpenBIS;
import lombok.NonNull;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;

public class OpenBISPasswordAuthenticator implements PasswordAuthenticator {
    public static final AttributeRepository.AttributeKey<OpenBISUser> USER_ATTRIBUTE =
            new AttributeRepository.AttributeKey<OpenBISUser>();
    public static final OpenBISClientUtil openBISClientUtil = new OpenBISClientUtil();

    @Override
    public boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException, AsyncAuthException {
        OpenBIS openBISClient = getOpenBISClient();
        String sessionToken = openBISClient.login(username, password);
        if ( sessionToken != null ) {
            session.setAttribute(
                    USER_ATTRIBUTE,
                    OpenBISUser.builder()
                            .username(username)
                            .password(password)
                            .sessionToken(sessionToken)
                            .build()
            );
            return true;
        } else {
            return false;
        }
    }

    @NonNull OpenBIS getOpenBISClient() {
        return openBISClientUtil.getOpenBISClient();
    }
}
