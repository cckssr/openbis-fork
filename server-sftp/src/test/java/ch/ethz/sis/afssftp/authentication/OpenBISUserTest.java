package ch.ethz.sis.afssftp.authentication;

import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.afssftp.util.AfsClientProxy;
import ch.ethz.sis.openbis.generic.OpenBIS;
import junit.framework.TestCase;
import org.mockito.Mockito;

import java.net.URI;

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

    public void testCheckAndRenewAfsClientSessionIfNecessaryIsNotNecessary() throws Exception {
        AfsClientProxy afsClientProxyMock = Mockito.spy(new AfsClientProxy(
                new AfsClient(URI.create("http://localhost:8080/afs-server"))
        ));
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("usR2")
                .password("Pwd3")
                .sessionToken("sessiontkn")
                .build();

        Mockito.doReturn(true).when(afsClientProxyMock).isSessionValid();
        openBISUser.checkAndRenewSessionIfNecessary(afsClientProxyMock);
        Mockito.verify(
                afsClientProxyMock, Mockito.times(1)
        ).isSessionValid();
        Mockito.verify(afsClientProxyMock, Mockito.times(0)).login(Mockito.anyString(), Mockito.anyString());
    }

    public void testCheckAndRenewAfsClientSessionIfNecessaryIsNecessary() throws Exception {
        AfsClientProxy afsClientProxyMock = Mockito.spy(new AfsClientProxy(
                new AfsClient(URI.create("http://localhost:8080/afs-server"))
        ));
        OpenBISUser openBISUser = OpenBISUser.builder()
                .username("usR2")
                .password("Pwd3")
                .sessionToken("sessiontkn")
                .build();

        Mockito.doReturn(false).when(afsClientProxyMock).isSessionValid();
        Mockito.doReturn("token").when(afsClientProxyMock).login(Mockito.anyString(), Mockito.anyString());
        openBISUser.checkAndRenewSessionIfNecessary(afsClientProxyMock);
        Mockito.verify(
                afsClientProxyMock, Mockito.times(1)
        ).isSessionValid();
        Mockito.verify(afsClientProxyMock, Mockito.times(1))
                .login(openBISUser.getUsername(), openBISUser.getPassword());
    }
}