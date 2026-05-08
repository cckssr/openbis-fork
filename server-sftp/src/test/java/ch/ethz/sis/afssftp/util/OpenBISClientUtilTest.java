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
}