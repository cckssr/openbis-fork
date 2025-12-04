/*
 * Copyright ETH 2010 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.systemtests.suite.allservers.environment;

import java.nio.file.Path;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.Role;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestEnvironment;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestFacade;

/**
 * @author pkupczyk
 */

public final class AllServersIntegrationTestEnvironment
{

    public static final String TEST_INTERACTIVE_SESSION_KEY = "integration-test-interactive-session-key";

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
            environment.createDataStoreServer();
            environment.createAfsServer(IntegrationTestEnvironment.loadProperties(Path.of("etc/suite/allservers/afs/service.properties")));
            environment.createRoCrateServer();
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
