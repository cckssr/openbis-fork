package ch.ethz.sis.openbis.systemtests.suite.sftp.environment;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestEnvironment;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestFacade;

import java.nio.file.Path;

public final class AfsSftpServerIntegrationTestEnvironment
{
    public static final String DEFAULT_SPACE = "DEFAULT";

    public static final String TEST_SPACE = "TEST";

    public static final String INSTANCE_ADMIN = "admin";

    public static final String DEFAULT_SPACE_ADMIN = "default_space_admin";

    public static final String TEST_SPACE_ADMIN = "test_space_admin";

    public static final String TEST_SPACE_OBSERVER = "test_space_observer";

    public static final String PASSWORD = "password";

    public static IntegrationTestEnvironment environment;

    public static void start()
    {
        if (environment == null)
        {
            environment = new IntegrationTestEnvironment();
            environment.createApplicationServer();
            environment.createAfsServer(IntegrationTestEnvironment.loadProperties(Path.of("etc/suite/sftp/afs/service.properties")));
            environment.createAfsSftpServer();
            environment.start();
            createTestData();
        }
    }

    public static void stop()
    {
        if (environment != null)
        {
            environment.stop();
            environment = null;
        }
    }

    private static void createTestData()
    {
        OpenBIS openBIS = environment.createOpenBIS();
        openBIS.login(INSTANCE_ADMIN, PASSWORD);

        IntegrationTestFacade facade = new IntegrationTestFacade(environment);
        facade.createSpace(openBIS, TEST_SPACE);
        facade.createUser(openBIS, TEST_SPACE_ADMIN, TEST_SPACE, Role.ADMIN);
        facade.createUser(openBIS, TEST_SPACE_OBSERVER, TEST_SPACE, Role.OBSERVER);
        facade.createUser(openBIS, DEFAULT_SPACE_ADMIN, DEFAULT_SPACE, Role.ADMIN);
    }

}
