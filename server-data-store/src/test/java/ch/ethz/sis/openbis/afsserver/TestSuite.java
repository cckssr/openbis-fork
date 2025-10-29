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
package ch.ethz.sis.openbis.afsserver;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ch.ethz.sis.afsserver.client.DummyAuthApiClientTest;
import ch.ethz.sis.afsserver.impl.ApiServerAdapterTest;
import ch.ethz.sis.afsserver.impl.ApiServerTest;
import ch.ethz.sis.openbis.afsserver.client.OpenBisAuthApiClientTest;
import ch.ethz.sis.openbis.afsserver.server.archiving.messages.FinalizeDataSetArchivingMessageTest;
import ch.ethz.sis.openbis.afsserver.server.archiving.messages.UpdateDataSetArchivingStatusMessageTest;
import ch.ethz.sis.openbis.afsserver.server.common.OpenBISConfigurationTest;
import ch.ethz.sis.openbis.afsserver.server.common.ShareIdManagerTest;
import ch.ethz.sis.openbis.afsserver.server.messages.DeleteDataSetFromStoreMessageTest;
import ch.ethz.sis.openbis.afsserver.server.messages.DeleteFileMessageTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ApiServerTest.class,
        ApiServerAdapterTest.class,
        DummyAuthApiClientTest.class,
        OpenBisAuthApiClientTest.class,
        OpenBISConfigurationTest.class,
        ShareIdManagerTest.class,
        FinalizeDataSetArchivingMessageTest.class,
        UpdateDataSetArchivingStatusMessageTest.class,
        DeleteDataSetFromStoreMessageTest.class,
        DeleteFileMessageTest.class
})

public class TestSuite
{

}
