package ch.ethz.sis.afssftp.util;

import ch.ethz.sis.afssftp.authentication.OpenBISUser;
import ch.ethz.sis.openbis.generic.OpenBIS;
import junit.framework.TestCase;
import org.mockito.Mockito;

public class OpenBISClientUtilTest extends TestCase {
    public void testGetOpenBISClient() {
        OpenBISUser user = Mockito.spy(
                OpenBISUser.builder()
                        .username("user1")
                        .password("pWD1")
                        .sessionToken("sess1on")
                        .build());
        Mockito.doNothing().when(user).checkAndRenewSessionIfNecessary(Mockito.any(OpenBIS.class));
        OpenBISClientUtil openBISClientUtil = new OpenBISClientUtil();
        openBISClientUtil.getOpenBISClient(user);
        Mockito.verify(user, Mockito.times(1)).checkAndRenewSessionIfNecessary(
                Mockito.any(OpenBIS.class)
        );
    }

    public void testGetAfsClient() {
        OpenBISUser user = Mockito.spy(
                OpenBISUser.builder()
                        .username("user1")
                        .password("pWD1")
                        .sessionToken("sess1on")
                        .build());
        Mockito.doNothing().when(user).checkAndRenewSessionIfNecessary(Mockito.any(AfsClientProxy.class));
        OpenBISClientUtil openBISClientUtil = Mockito.spy(new OpenBISClientUtil());
        Mockito.doReturn("http://test.com:8080/afs-server")
                .when(openBISClientUtil).getAfsUrl();
        openBISClientUtil.getAfsClient(user);
        Mockito.verify(user, Mockito.times(1)).checkAndRenewSessionIfNecessary(
                Mockito.any(AfsClientProxy.class)
        );
    }
}