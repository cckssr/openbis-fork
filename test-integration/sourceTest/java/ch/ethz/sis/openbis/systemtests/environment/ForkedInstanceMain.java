/*
 * Copyright ETH 2024 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtests.environment;

import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Entry point of a forked openBIS instance (a second AS + DSS running in its own JVM).
 */
public final class ForkedInstanceMain
{

    public static final String AS_PROPERTIES_PROPERTY = "forked.instance.as.properties";

    public static final String DSS_PROPERTIES_PROPERTY = "forked.instance.dss.properties";

    public static final String STARTED_MARKER = "FORKED_INSTANCE_STARTED";

    public static void main(String[] args) throws Exception
    {
        Properties asProperties = loadProperties(AS_PROPERTIES_PROPERTY);
        Properties dssProperties = loadProperties(DSS_PROPERTIES_PROPERTY);

        IntegrationTestEnvironment environment = new IntegrationTestEnvironment();
        environment.createApplicationServer(asProperties);
        environment.createDataStoreServer(dssProperties);

        // Stop the servers (and drop the instance's databases) when the parent kills this process.
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            try
            {
                environment.stop();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }, "forked-instance-shutdown"));

        environment.start();

        // Signal readiness to the parent (which also polls the AS over HTTP) and then park until killed.
        System.out.println(STARTED_MARKER);
        System.out.flush();

        new CountDownLatch(1).await();
    }

    private static Properties loadProperties(String pathProperty)
    {
        return IntegrationTestEnvironment.loadProperties(Path.of(requiredProperty(pathProperty)));
    }

    private static String requiredProperty(String name)
    {
        String value = System.getProperty(name);
        if (value == null || value.isBlank())
        {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }

    private ForkedInstanceMain()
    {
    }

}
