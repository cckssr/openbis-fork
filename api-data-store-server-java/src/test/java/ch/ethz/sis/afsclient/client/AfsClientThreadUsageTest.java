/*
 * Copyright ETH 2026 Zurich, Scientific IT Services
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
package ch.ethz.sis.afsclient.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.ServerSocket;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Each java.net.http.HttpClient starts its own threads, which only end once the client is garbage collected. When {@link AfsClient}
 * built a new HttpClient per request, the openBIS sync data source failed with "OutOfMemoryError: unable to create native thread" while
 * listing AFS data of thousands of samples. A long sequence of requests on one {@link AfsClient} must not start threads per request.
 */
public class AfsClientThreadUsageTest
{
    private static final String HTTP_SERVER_PATH = "/fileserver";

    // before the fix, 20 requests already started 77 threads
    private static final int NUMBER_OF_REQUESTS = 50;

    private static final int MAX_STARTED_THREADS = 20;

    private DummyHttpServer httpServer;

    private AfsClient afsClient;

    @Before
    public void setUp() throws Exception
    {
        int port = findFreePort();
        httpServer = new DummyHttpServer(port, HTTP_SERVER_PATH);
        httpServer.start();
        afsClient = new AfsClient(new URI("http", null, "localhost", port, HTTP_SERVER_PATH, null, null));
        afsClient.login("test", "test");
    }

    @After
    public void tearDown()
    {
        if (afsClient != null)
        {
            afsClient.close();
        }
        if (httpServer != null)
        {
            httpServer.stop();
        }
    }

    @Test
    public void manyRequests_doNotStartThreadsPerRequest() throws Exception
    {
        httpServer.setFixedResponse("{\"result\": true}");

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long startedBefore = threadMXBean.getTotalStartedThreadCount();
        for (int i = 0; i < NUMBER_OF_REQUESTS; i++)
        {
            afsClient.isSessionValid();
        }

        long startedThreads = threadMXBean.getTotalStartedThreadCount() - startedBefore;
        assertTrue(NUMBER_OF_REQUESTS + " requests started " + startedThreads + " threads (expected at most " + MAX_STARTED_THREADS
                + "): AfsClient creates a new HttpClient per request", startedThreads <= MAX_STARTED_THREADS);
    }

    @Test
    public void close_endsThreadsOfHttpClient() throws Exception
    {
        httpServer.setFixedResponse("{\"result\": \"session-token\"}");
        afsClient.close();

        Set<Thread> threadsBefore = Thread.getAllStackTraces().keySet();
        AfsClient client = new AfsClient(afsClient.getServerUri());
        client.login("test", "test");
        // only threads of the HttpClient (named "HttpClient-<id>-SelectorManager" / "HttpClient-<id>-Worker-<n>"), other threads of the JVM
        // started meanwhile (e.g. of the common fork join pool) may live longer
        Set<Thread> startedThreads = new HashSet<>(Thread.getAllStackTraces().keySet());
        startedThreads.removeAll(threadsBefore);
        startedThreads.removeIf(thread -> thread.getName().startsWith("HttpClient-") == false);
        assertFalse("request started no HttpClient threads", startedThreads.isEmpty());

        client.close();

        for (Thread thread : startedThreads)
        {
            thread.join(10000);
        }
        Set<String> aliveThreads = startedThreads.stream().filter(Thread::isAlive).map(Thread::getName).collect(Collectors.toSet());
        assertTrue("threads still alive after close: " + aliveThreads, aliveThreads.isEmpty());
    }

    @Test
    public void requestAfterClose_fails() throws Exception
    {
        afsClient.close();
        try
        {
            afsClient.isSessionValid();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e)
        {
            assertTrue(e.getMessage(), e.getMessage().contains("closed"));
        }
    }

    /**
     * Closing must not fail for a client that never sent a request (its HttpClient was never built), nor when repeated for a client whose
     * HttpClient exists (afsClient logged in during setUp).
     */
    @Test
    public void close_isIdempotentAndWorksWithoutRequests()
    {
        AfsClient client = new AfsClient(afsClient.getServerUri());
        client.close();
        client.close();
        afsClient.close();
        afsClient.close();
    }

    private static int findFreePort() throws Exception
    {
        try (ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
    }
}
