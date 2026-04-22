package ch.ethz.sis.afssftp.authentication;

import ch.ethz.sis.openbis.generic.OpenBIS;
import junit.framework.TestCase;
import org.mockito.Mockito;

public class OpenBISUserTest extends TestCase {

    public void testCheckAndRenewOpenBISSessionIfNecessaryIsNotNecessary() {
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("usR2")
                .password("Pwd3")
                .sessionToken("sessiontkn")
                .build();

        Mockito.doReturn(true).when(openBISMock).isSessionActive();
        openBISUser.checkAndRenewSessionIfNecessary(openBISMock);
        Mockito.verify(
                openBISMock, Mockito.times(1)
        ).isSessionActive();
        Mockito.verify(openBISMock, Mockito.times(0)).login(Mockito.anyString(), Mockito.anyString());
    }

    public void testCheckAndRenewOpenBISSessionIfNecessaryIsNecessary() {
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("usR2")
                .password("Pwd3")
                .sessionToken("sessiontkn")
                .build();

        Mockito.doReturn(false).when(openBISMock).isSessionActive();
        openBISUser.checkAndRenewSessionIfNecessary(openBISMock);
        Mockito.verify(
                openBISMock, Mockito.times(1)
        ).isSessionActive();
        Mockito.verify(openBISMock, Mockito.times(1))
                .login(openBISUser.getUsername(), openBISUser.getPassword());
    }
}