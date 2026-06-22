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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer.datasourceconnector;

import static org.testng.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.config.BasicAuthCredentials;
import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;

/**
 * Tests that {@link DataSourceConnector} encodes the entity selection into the resource list request URL.
 *
 * @author Tufail Tak
 */
public class DataSourceConnectorTest
{
    private static final Logger LOG = LogFactory.getLogger(LogCategory.OPERATION, DataSourceConnectorTest.class);

    private static final String URL = "https://host:8444/datastore_server/re-sync";

    private DataSourceConnector newConnector()
    {
        return new DataSourceConnector(URL, new BasicAuthCredentials("realm", "user", "pass"), LOG);
    }

    @Test
    public void testCreateRequestUrlWithPermIdsAndFlags()
    {
        // Given
        List<ExportablePermId> permIds = List.of(
                new ExportablePermId(ExportableKind.SAMPLE, "20240101120000000-1"),
                new ExportablePermId(ExportableKind.PROJECT, "20240101120000000-2"));

        // When
        String url = newConnector().createRequestUrl(permIds, true, true, false, false, false);

        // Then
        assertEquals(url, URL + "?verb=resourcelist.xml"
                + "&exportable_perm_id=SAMPLE:20240101120000000-1"
                + "&exportable_perm_id=PROJECT:20240101120000000-2"
                + "&with_levels_above=true"
                + "&with_levels_below=true"
                + "&with_objects_and_data_sets_parents=false"
                + "&with_objects_and_data_sets_children=false"
                + "&with_objects_and_data_sets_other_spaces=false");
    }

    @Test
    public void testCreateRequestUrlWithoutPermIdsStillEmitsFlags()
    {
        // When
        String url = newConnector().createRequestUrl(Collections.emptyList(), false, false, false, false, false);

        // Then
        assertEquals(url, URL + "?verb=resourcelist.xml"
                + "&with_levels_above=false"
                + "&with_levels_below=false"
                + "&with_objects_and_data_sets_parents=false"
                + "&with_objects_and_data_sets_children=false"
                + "&with_objects_and_data_sets_other_spaces=false");
    }

}
