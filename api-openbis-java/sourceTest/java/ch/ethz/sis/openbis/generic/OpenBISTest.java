/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic;


import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeoutException;

public class OpenBISTest
{

    private DummyHttpServer httpServer;
    private final int PORT = 8818;
    private final String URL = "http://localhost:" + PORT;

    static int timeout = 10 * 60 * 1000;

    @BeforeMethod
    public void setUp() throws Exception
    {
        httpServer = new DummyHttpServer(8818, "/openbis/openbis/rmi-application-server-v3");
        httpServer.start();
    }

    @AfterMethod
    public void tearDown()
    {
        httpServer.stop();
    }


    @Test
    public void testLogin() throws Exception
    {

        final String responseToken = "admin-12345";

        httpServer.setNextResponse(responseToken);

        OpenBIS openBIS = new OpenBIS(URL, timeout);
        String token = openBIS.login("admin", "aaa");

        Assert.assertEquals(token, responseToken);

    }

    @Test
    public void testIdleTimeoutLessThanDefault() throws Exception
    {

        final String responseToken = "admin-12345";

        httpServer.setNextResponse(responseToken);
        httpServer.setIdleTime(1000);


        OpenBIS openBIS = new OpenBIS(URL, 100);
        try
        {
            openBIS.login("admin", "aaa");
            Assert.fail();
        } catch(Exception e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertNotNull(e.getCause().getCause());
            Throwable cause = e.getCause().getCause();

            Assert.assertEquals(cause.getClass(), TimeoutException.class);
            TimeoutException exception = (TimeoutException) cause;
            Assert.assertEquals(exception.getMessage(), "Idle timeout 100 ms");
        }
    }

    @Test
    public void testTotalTimeoutMoreThanDefault() throws Exception
    {

        final String responseToken = "admin-12345";

        httpServer.setNextResponse(responseToken);
        // Default is 30 seconds
        httpServer.setIdleTime(31 * 1000L);


        OpenBIS openBIS = new OpenBIS(URL);
        try
        {
            openBIS.login("admin", "aaa");
            Assert.fail();
        } catch(Exception e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertNotNull(e.getCause().getCause());
            Throwable cause = e.getCause().getCause();

            Assert.assertEquals(cause.getClass(), TimeoutException.class);
            TimeoutException exception = (TimeoutException) cause;
            Assert.assertEquals(exception.getMessage(), "Total timeout 30000 ms elapsed");
        }
    }

    @Test
    public void testIdleTimeoutMoreThanDefault() throws Exception
    {

        final String responseToken = "admin-12345";

        httpServer.setNextResponse(responseToken);
        httpServer.setIdleTime(32 * 1000L);


        OpenBIS openBIS = new OpenBIS(URL, 31000);
        try
        {
            openBIS.login("admin", "aaa");
            Assert.fail();
        } catch(Exception e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertNotNull(e.getCause().getCause());
            Throwable cause = e.getCause().getCause();

            Assert.assertEquals(cause.getClass(), TimeoutException.class);
            TimeoutException exception = (TimeoutException) cause;
            Assert.assertEquals(exception.getMessage(), "Total timeout 31000 ms elapsed");
        }
    }

}
