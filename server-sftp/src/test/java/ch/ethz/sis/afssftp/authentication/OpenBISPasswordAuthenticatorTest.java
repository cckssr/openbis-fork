package ch.ethz.sis.afssftp.authentication;

import ch.ethz.sis.openbis.generic.OpenBIS;
import junit.framework.TestCase;
import org.apache.sshd.server.session.ServerSession;
import org.mockito.Mockito;

import static ch.ethz.sis.afssftp.authentication.OpenBISPasswordAuthenticator.USER_ATTRIBUTE;

public class OpenBISPasswordAuthenticatorTest extends TestCase {
    public void testAuthenticateTrue() {
        ServerSession serverSessionMock = Mockito.mock(ServerSession.class);
        String user = "useR1";
        String pwd = "pwD2";
        String sessionToken = "good-session-!";
        OpenBISPasswordAuthenticator passwordAuthenticator = Mockito.spy(new OpenBISPasswordAuthenticator());
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openBISMock).when(passwordAuthenticator).getOpenBISClient();
        Mockito.doReturn(sessionToken).when(openBISMock).login(user, pwd);
        assertTrue(passwordAuthenticator.authenticate(user, pwd, serverSessionMock));
        Mockito.verify(openBISMock, Mockito.times(1)).login(user, pwd);
        Mockito.verify(serverSessionMock, Mockito.times(1)).setAttribute(
                USER_ATTRIBUTE,
                OpenBISUser.builder()
                        .username(user)
                        .password(pwd)
                        .sessionToken(sessionToken)
                        .build()
        );
    }

    public void testAuthenticateFalse() {
        ServerSession serverSessionMock = Mockito.mock(ServerSession.class);
        String user = "useR1";
        String pwd = "pwD2";
        OpenBISPasswordAuthenticator passwordAuthenticator = Mockito.spy(new OpenBISPasswordAuthenticator());
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openBISMock).when(passwordAuthenticator).getOpenBISClient();
        Mockito.doReturn(null).when(openBISMock).login(user, pwd);
        assertFalse(passwordAuthenticator.authenticate(user, pwd, serverSessionMock));
        Mockito.verify(openBISMock, Mockito.times(1)).login(user, pwd);
        Mockito.verify(serverSessionMock, Mockito.times(0)).setAttribute(
                Mockito.any(),
                Mockito.any()
        );
    }
}