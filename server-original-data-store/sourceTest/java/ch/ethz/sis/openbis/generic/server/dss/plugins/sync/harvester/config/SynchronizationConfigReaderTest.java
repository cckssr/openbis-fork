/*
 * Copyright ETH 2026 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.config;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;

/**
 * Tests for {@link SynchronizationConfigReader}, in particular the mandatory {@code exportable-perm-ids} setting:
 * the harvester must refuse to start if it is missing or empty, since an empty selection would make the data source
 * return an empty resourcelist which, combined with {@code deletion-allowed}, would delete everything previously synced.
 *
 * @author Tufail Tak
 */
public class SynchronizationConfigReaderTest
{

    private static final String MANDATORY_PROPERTIES =
            "email-addresses = admin@example.com\n"
                    + "resource-list-url = https://example.com/datastore_server/re-sync\n"
                    + "data-source-openbis-url = https://example.com/openbis\n"
                    + "data-source-dss-url = https://example.com/datastore_server\n"
                    + "data-source-auth-realm = OAI-PMH\n"
                    + "data-source-auth-user = source-user\n"
                    + "data-source-auth-pass = source-pass\n"
                    + "harvester-user = harvester-user\n"
                    + "harvester-pass = harvester-pass\n";

    @Test
    public void testReadConfigurationFailsWhenExportablePermIdsIsMissing() throws IOException
    {
        File configFile = writeConfig("[TEST]\n" + MANDATORY_PROPERTIES);

        assertExportablePermIdsRequired(configFile);
    }

    @Test
    public void testReadConfigurationFailsWhenExportablePermIdsIsEmpty() throws IOException
    {
        File configFile = writeConfig("[TEST]\n" + MANDATORY_PROPERTIES + "exportable-perm-ids =\n");

        assertExportablePermIdsRequired(configFile);
    }

    @Test
    public void testReadConfigurationSucceedsWhenExportablePermIdsIsProvided() throws IOException
    {
        File configFile = writeConfig("[TEST]\n" + MANDATORY_PROPERTIES
                + "file-service-repository-path = /tmp/afs\n"
                + "exportable-perm-ids = SAMPLE:20170610109309206-1234\n");

        List<SyncConfig> configs = SynchronizationConfigReader.readConfiguration(configFile);

        assertEquals(configs.size(), 1);
        assertEquals(configs.get(0).getExportablePermIds(),
                List.of(new ExportablePermId(ExportableKind.SAMPLE, "20170610109309206-1234")));
    }

    private void assertExportablePermIdsRequired(File configFile) throws IOException
    {
        try
        {
            SynchronizationConfigReader.readConfiguration(configFile);
            fail("ConfigurationFailureException expected");
        } catch (ConfigurationFailureException ex)
        {
            assertTrue(ex.getMessage().contains("exportable-perm-ids"), ex.getMessage());
        }
    }

    private File writeConfig(String content) throws IOException
    {
        File file = File.createTempFile("harvester-config", ".txt");
        file.deleteOnExit();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

}
