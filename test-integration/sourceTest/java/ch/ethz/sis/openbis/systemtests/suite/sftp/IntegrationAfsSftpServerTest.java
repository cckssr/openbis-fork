package ch.ethz.sis.openbis.systemtests.suite.sftp;

import ch.ethz.sis.openbis.systemtests.suite.sftp.environment.AfsSftpServerIntegrationTestEnvironment;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.List;

public class IntegrationAfsSftpServerTest
{

    private static final String SFTP_HOST = "localhost";
    private static final int SFTP_PORT = 2223;
    private static final long SFTP_TIMEOUT = 5000;
    private static final String USER = "admin";

    @BeforeSuite
    public void beforeSuite()
    {
        AfsSftpServerIntegrationTestEnvironment.start();
    }

    @AfterSuite
    public void afterSuite()
    {
        AfsSftpServerIntegrationTestEnvironment.stop();
    }

    @Test
    public void test() throws Exception
    {
        //TODO write real test cases
        try (SshClient client = SshClient.setUpDefaultClient())
        {
            client.start();

            try (ClientSession session = client.connect(USER, SFTP_HOST, SFTP_PORT)
                    .verify(SFTP_TIMEOUT)
                    .getSession())
            {
                session.addPasswordIdentity(AfsSftpServerIntegrationTestEnvironment.PASSWORD);
                session.auth().verify(SFTP_TIMEOUT);

                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {

                    List<SftpClient.DirEntry> dirEntries = sftp.readDir(sftp.openDir("/spaces"));
                    dirEntries.stream().forEach(
                            dirEntry -> {
                                System.out.println(dirEntry);
                            }
                    );
                }
            } finally
            {
                client.stop();
            }
        }
        System.out.println("Test example");
    }
}
