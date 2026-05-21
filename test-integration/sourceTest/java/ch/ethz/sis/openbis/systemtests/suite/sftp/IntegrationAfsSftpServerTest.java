package ch.ethz.sis.openbis.systemtests.suite.sftp;

import ch.ethz.sis.openbis.systemtests.suite.sftp.environment.AfsSftpServerIntegrationTestEnvironment;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class IntegrationAfsSftpServerTest
{
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
    public void test()
    {
        System.out.println("Test example");
    }
}
