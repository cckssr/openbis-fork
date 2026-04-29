package ch.ethz.sis.afssftp.authentication;

import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;

public class PasswordAuthenticator implements org.apache.sshd.server.auth.password.PasswordAuthenticator
{
    public static final AttributeRepository.AttributeKey<User> USER_ATTRIBUTE =
            new AttributeRepository.AttributeKey<User>();

    private final AuthenticationProvider authenticationProvider;

    public PasswordAuthenticator(AuthenticationProvider authenticationProvider)
    {
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException, AsyncAuthException {
        String sessionToken = authenticationProvider.login(username, password);
        if ( sessionToken != null ) {
            session.setAttribute(
                    USER_ATTRIBUTE,
                    User.builder()
                            .username(username)
                            .password(password)  //TODO: decide if session already lasts long enough, so that password is not needed for session-renewal
                            .sessionToken(sessionToken)
                            .build()
            );
            return true;
        } else {
            return false;
        }
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }
}
