/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.afsserver.worker.providers.impl;

import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfiguration;
import ch.ethz.sis.afsserver.worker.providers.AuthenticationInfoProvider;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.shared.startup.Configuration;

public class OpenBISAuthenticationInfoProvider implements AuthenticationInfoProvider
{

    private OpenBISConfiguration openBISConfiguration;

    @Override
    public void init(Configuration configuration) throws Exception
    {
        openBISConfiguration = OpenBISConfiguration.getInstance(configuration);
    }

    @Override
    public String login(String userId, String password)
    {
        OpenBIS openBIS = openBISConfiguration.getOpenBIS();
        return openBIS.login(userId, password);
    }

    @Override
    public Boolean isSessionValid(String sessionToken)
    {
        OpenBIS openBIS = openBISConfiguration.getOpenBIS();
        openBIS.setSessionToken(sessionToken);
        return openBIS.isSessionActive();
    }

    @Override
    public Boolean logout(String sessionToken)
    {
        OpenBIS openBIS = openBISConfiguration.getOpenBIS();
        openBIS.setSessionToken(sessionToken);
        openBIS.logout();
        return Boolean.TRUE;
    }
}
