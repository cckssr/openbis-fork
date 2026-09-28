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

import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.ServerSocket;
import java.net.URI;

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

    private static int findFreePort() throws Exception
    {
        try (ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
    }
}
