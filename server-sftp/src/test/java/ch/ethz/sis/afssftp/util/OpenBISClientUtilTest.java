package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afssftp.authentication.User;
import ch.ethz.sis.openbis.generic.OpenBIS;
import junit.framework.TestCase;
import org.mockito.Mockito;

public class OpenBISClientUtilTest extends TestCase {
    public void testGetOpenBISClient() {
        User user = Mockito.spy(
                User.builder()
                        .username("user1")
                        .sessionToken("sess1on")
                        .build());
        OpenBISClientUtil openBISClientUtil = Mockito.spy(new OpenBISClientUtil());
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openBISMock).when(openBISClientUtil).getOpenBISClient();
        openBISClientUtil.getOpenBISClient(user);
        Mockito.verify(openBISMock, Mockito.times(1)).setSessionToken(user.getSessionToken());
    }

    public void testGetAfsClient() {
        User user = Mockito.spy(
                User.builder()
                        .username("user1")
                        .sessionToken("sess1on")
                        .build());
        OpenBISClientUtil openBISClientUtil = Mockito.spy(new OpenBISClientUtil());
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openBISMock).when(openBISClientUtil).getOpenBISClient();
        Mockito.doReturn("http://test.com:8080/afs-server")
                .when(openBISClientUtil).getAfsUrl();
        openBISClientUtil.getAfsClient(user);
        Mockito.verify(openBISMock, Mockito.times(1)).setSessionToken(user.getSessionToken());
    }

    public void testLoginWithUserAndPassword() {
        String user = "uSr";
        String password = "pwd";
        String wrongPwd = "wrong-pwd";
        OpenBISClientUtil openBISClientUtil = Mockito.spy(new OpenBISClientUtil());
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openBISMock).when(openBISClientUtil).getOpenBISClient();
        Mockito.doReturn("session-tkn").when(
                openBISMock
        ).login(user, password);
        assertEquals("session-tkn", openBISClientUtil.login(user, password));
        Mockito.verify(openBISMock, Mockito.times(1)).login(user, password);
        assertNull(openBISClientUtil.login(user, wrongPwd));
        Mockito.verify(openBISMock, Mockito.times(1)).login(user, wrongPwd);
    }

    public void testLoginWithPAT() {
        String pat = "$PAT-12345";
        OpenBISClientUtil openBISClientUtil = Mockito.spy(new OpenBISClientUtil());
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openBISMock).when(openBISClientUtil).getOpenBISClient();
        for (boolean correctPAT: new boolean[]{ false, true }) {
            Mockito.doReturn(correctPAT).when(
                    openBISMock
            ).isSessionActive();
            assertEquals(correctPAT ? pat : null, openBISClientUtil.login("?", pat));
        }
    }

    public void testLoginWithBlankValues() {
        OpenBISClientUtil openBISClientUtil = Mockito.spy(new OpenBISClientUtil());
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        Mockito.doReturn(openBISMock).when(openBISClientUtil).getOpenBISClient();
        String NON_EMPTY = "non-empty";
        for (String user : new String[]{ null, "", " \t ", NON_EMPTY}) {
            for (String pwd : new String[]{ null, "", " \t ", NON_EMPTY}) {
                if (!NON_EMPTY.equals(user) || !NON_EMPTY.equals(pwd)) {
                    assertNull(openBISClientUtil.login(user, pwd));
                }
            }
        }
        Mockito.verify(openBISMock, Mockito.times(0))
                .login(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(openBISMock, Mockito.times(0))
                .isSessionActive();
    }
}