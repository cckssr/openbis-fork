package ch.ethz.sis.afssftp.authentication;

import junit.framework.TestCase;
import org.apache.sshd.server.session.ServerSession;
import org.mockito.Mockito;

import static ch.ethz.sis.afssftp.authentication.PasswordAuthenticator.USER_ATTRIBUTE;

public class PasswordAuthenticatorTest extends TestCase {
    public void testAuthenticateTrue() {
        ServerSession serverSessionMock = Mockito.mock(ServerSession.class);
        String user = "useR1";
        String pwd = "pwD2";
        String sessionToken = "good-session-!";
        AuthenticationProvider auMock = Mockito.mock(AuthenticationProvider.class);
        PasswordAuthenticator passwordAuthenticator = Mockito.spy(new PasswordAuthenticator(auMock));
        Mockito.doReturn(auMock).when(passwordAuthenticator).getAuthenticationProvider();
        Mockito.doReturn(sessionToken).when(auMock).login(user, pwd);
        assertTrue(passwordAuthenticator.authenticate(user, pwd, serverSessionMock));
        Mockito.verify(auMock, Mockito.times(1)).login(user, pwd);
        Mockito.verify(serverSessionMock, Mockito.times(1)).setAttribute(
                USER_ATTRIBUTE,
                User.builder()
                        .username(user)
                        .sessionToken(sessionToken)
                        .build()
        );
    }

    public void testAuthenticateFalse() {
        ServerSession serverSessionMock = Mockito.mock(ServerSession.class);
        String user = "useR1";
        String pwd = "pwD2";
        AuthenticationProvider auMock = Mockito.mock(AuthenticationProvider.class);
        PasswordAuthenticator passwordAuthenticator = Mockito.spy(new PasswordAuthenticator(auMock));
        Mockito.doReturn(auMock).when(passwordAuthenticator).getAuthenticationProvider();
        Mockito.doReturn(null).when(auMock).login(user, pwd);
        assertFalse(passwordAuthenticator.authenticate(user, pwd, serverSessionMock));
        Mockito.verify(auMock, Mockito.times(1)).login(user, pwd);
        Mockito.verify(serverSessionMock, Mockito.times(0)).setAttribute(
                Mockito.any(),
                Mockito.any()
        );
    }
}