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

package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.helper;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.ros.startup.RoCrateServerParameter;
import ch.ethz.sis.openbis.ros.startup.StartupMain;
import jakarta.ws.rs.Produces;

public class OpeBISProvider
{
    private OpeBISProvider()
    {
    }

    @Produces
    public static OpenBIS createClient(String personalAccessToken) {
        String openBISUrl =
                StartupMain.getConfiguration().getStringProperty(RoCrateServerParameter.openBISUrl);
        int openBISTimeout = StartupMain.getConfiguration()
                .getIntegerProperty(RoCrateServerParameter.openBISTimeout);
        OpenBIS openBIS = new OpenBIS(openBISUrl, openBISTimeout);
        openBIS.setSessionToken(personalAccessToken);
        return openBIS;
    }

    public static String getUrl()
    {
        return StartupMain.getConfiguration().getStringProperty(RoCrateServerParameter.openBISUrl);
    }

    public static int getTimeOut()
    {
        return StartupMain.getConfiguration()
                .getIntegerProperty(RoCrateServerParameter.openBISTimeout);
    }
}
