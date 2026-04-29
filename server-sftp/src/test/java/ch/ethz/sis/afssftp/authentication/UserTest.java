package ch.ethz.sis.afssftp.authentication;

import ch.ethz.sis.openbis.generic.OpenBIS;
import junit.framework.TestCase;
import org.mockito.Mockito;

public class UserTest extends TestCase {

    public void testCheckAndRenewOpenBISSessionIfNecessaryIsNotNecessary() {
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("usR2")
                .password("Pwd3")
                .sessionToken("sessiontkn")
                .build();

        Mockito.doReturn(true).when(openBISMock).isSessionActive();
        user.checkAndRenewSessionIfNecessary(openBISMock);
        Mockito.verify(
                openBISMock, Mockito.times(1)
        ).isSessionActive();
        Mockito.verify(openBISMock, Mockito.times(0)).login(Mockito.anyString(), Mockito.anyString());
    }

    public void testCheckAndRenewOpenBISSessionIfNecessaryIsNecessary() {
        OpenBIS openBISMock = Mockito.mock(OpenBIS.class);
        User user = User.builder()
                .username("usR2")
                .password("Pwd3")
                .sessionToken("sessiontkn")
                .build();

        Mockito.doReturn(false).when(openBISMock).isSessionActive();
        user.checkAndRenewSessionIfNecessary(openBISMock);
        Mockito.verify(
                openBISMock, Mockito.times(1)
        ).isSessionActive();
        Mockito.verify(openBISMock, Mockito.times(1))
                .login(user.getUsername(), user.getPassword());
    }
}