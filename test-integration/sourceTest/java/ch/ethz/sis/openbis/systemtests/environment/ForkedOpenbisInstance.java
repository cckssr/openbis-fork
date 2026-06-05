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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;

/**
 * A second, full openBIS instance (AS + DSS) running in its own JVM
 */
public final class ForkedOpenbisInstance
{

    private static final Logger log = LogFactory.getLogger(ForkedOpenbisInstance.class);

    private static final String INSTANCE_ADMIN = "admin";

    private static final String PASSWORD = "password";

    private static final long READINESS_TIMEOUT_MILLIS = 300_000L;

    private static final long READINESS_POLL_INTERVAL_MILLIS = 2_000L;

    private static final long STOP_TIMEOUT_SECONDS = 60L;

    private final String projectName;

    private String label;

    private String asProperties;

    private String dssProperties;

    private Process process;

    private final CountDownLatch startedLatch = new CountDownLatch(1);

    private final List<String> outputLines = Collections.synchronizedList(new ArrayList<>());

    public ForkedOpenbisInstance(String projectName)
    {
        this.projectName = projectName;
        this.label = projectName;
    }


    public ForkedOpenbisInstance withLabel(String label)
    {
        this.label = label;
        return this;
    }

    public ForkedOpenbisInstance withApplicationServer(String properties)
    {
        this.asProperties = properties;
        return this;
    }

    public ForkedOpenbisInstance withDataStoreServer(String properties)
    {
        this.dssProperties = properties;
        return this;
    }

    public void start()
    {
        if (process != null)
        {
            throw new IllegalStateException("Forked instance '" + projectName + "' is already running.");
        }
        if (asProperties == null || dssProperties == null)
        {
            throw new IllegalStateException("Both an application server and a data store server must be configured.");
        }

        try
        {
            log.info("Starting forked openBIS instance '" + projectName + "' (AS " + getOpenBISUrl()
                    + ", DSS " + getDSSUrl() + ").");
            process = new ProcessBuilder(buildCommand())
                    .directory(new File(System.getProperty("user.dir")))
                    .redirectErrorStream(true)
                    .start();

            startOutputReader();
            waitUntilReady();
            log.info("Forked openBIS instance '" + projectName + "' is ready.");
        } catch (Exception e)
        {
            stop();
            throw new RuntimeException("Starting forked openBIS instance '" + projectName + "' failed.", e);
        }
    }

    public void stop()
    {
        if (process == null)
        {
            return;
        }
        try
        {
            log.info("Stopping forked openBIS instance '" + projectName + "'.");
            // SIGTERM triggers the child's shutdown hook, which stops the servers and drops its databases.
            process.destroy();
            if (!process.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            {
                log.error("Forked openBIS instance '" + projectName + "' did not stop in time; killing it.");
                process.destroyForcibly().waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            log.info("Stopped forked openBIS instance '" + projectName + "'.");
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while stopping forked openBIS instance '" + projectName + "'.", e);
        } finally
        {
            process = null;
        }
    }

    public String getProjectName()
    {
        return projectName;
    }

    public String getOpenBISUrl()
    {
        return TestInstanceHostUtils.getOpenBISUrl(projectName);
    }

    public String getDSSUrl()
    {
        return TestInstanceHostUtils.getDSSUrl(projectName);
    }


    public OpenBIS createOpenBIS()
    {
        return new OpenBIS(getOpenBISUrl() + TestInstanceHostUtils.getOpenBISPath(),
                getDSSUrl() + TestInstanceHostUtils.getDSSPath(),
                getOpenBISUrl() + TestInstanceHostUtils.getAFSPath());
    }

    private List<String> buildCommand()
    {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        List<String> command = new ArrayList<>();
        command.add(java);
        command.add("-cp");
        // The child runs the same code as this test worker, so it inherits this JVM's classpath verbatim.
        command.add(System.getProperty("java.class.path"));

        command.add("-Dant.project.name=" + projectName);
        command.add("-Duser.timezone=Europe/Zurich");
        command.add("-Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StrErrLog");
        command.add("--add-opens");
        command.add("java.base/java.lang=ALL-UNNAMED");
        command.add("--add-opens");
        command.add("java.base/java.lang.reflect=ALL-UNNAMED");
        command.add("--add-opens");
        command.add("java.base/java.io=ALL-UNNAMED");

        command.add("-D" + ForkedInstanceMain.AS_PROPERTIES_PROPERTY + "=" + asProperties);
        command.add("-D" + ForkedInstanceMain.DSS_PROPERTIES_PROPERTY + "=" + dssProperties);

        command.add(ForkedInstanceMain.class.getName());
        return command;
    }

    private void startOutputReader()
    {
        Thread reader = new Thread(() ->
        {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                String line;
                while ((line = in.readLine()) != null)
                {
                    // Forward the child's output so its logs remain visible in the test output.
                    outputLines.add(line);
                    System.out.println("[" + label + "] " + line);
                    if (line.contains(ForkedInstanceMain.STARTED_MARKER))
                    {
                        startedLatch.countDown();
                    }
                }
            } catch (Exception e)
            {
                // The stream closes when the child exits; nothing to do.
            }
        }, projectName + "-output-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void waitUntilReady() throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + READINESS_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline)
        {
            if (startedLatch.await(READINESS_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS))
            {
                // Both of the child's servers have booted; confirm the AS answers a login before proceeding.
                if (canLogin())
                {
                    return;
                }
            }
            if (!process.isAlive())
            {
                throw new RuntimeException("Forked openBIS instance '" + projectName + "' exited during startup with code "
                        + process.exitValue() + "." + getCapturedOutput());
            }
        }
        throw new RuntimeException("Forked openBIS instance '" + projectName + "' was not ready within "
                + READINESS_TIMEOUT_MILLIS + " ms." + getCapturedOutput());
    }

    private String getCapturedOutput()
    {
        synchronized (outputLines)
        {
            if (outputLines.isEmpty())
            {
                return "";
            }
            return "\nCaptured forked output:\n" + String.join("\n", outputLines);
        }
    }

    private boolean canLogin()
    {
        try
        {
            OpenBIS openBIS = createOpenBIS();
            String sessionToken = openBIS.login(INSTANCE_ADMIN, PASSWORD);
            if (sessionToken == null)
            {
                return false;
            }
            openBIS.logout();
            return true;
        } catch (Exception e)
        {
            return false;
        }
    }

}
