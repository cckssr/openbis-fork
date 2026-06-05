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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.datasource;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;

/**
 * Tests for the parsing of the new entity-selection request parameters of {@link DataSourceRequestHandler}.
 *
 * @author Tufail Tak
 */
public class DataSourceRequestHandlerTest
{

    @Test
    public void testParseExportablePermIds()
    {
        // Given
        List<String> tokens = Arrays.asList("SAMPLE:20240101120000000-1", "PROJECT:20240101120000000-2");

        // When
        List<ExportablePermId> result = DataSourceRequestHandler.parseExportablePermIds(tokens);

        // Then
        assertEquals(result.size(), 2);
        assertEquals(result.get(0), new ExportablePermId(ExportableKind.SAMPLE, "20240101120000000-1"));
        assertEquals(result.get(1), new ExportablePermId(ExportableKind.PROJECT, "20240101120000000-2"));
    }

    @Test
    public void testParseExportablePermIdsTrimsWhitespace()
    {
        // Given
        List<String> tokens = Arrays.asList("  DATASET : CODE-1  ");

        // When
        List<ExportablePermId> result = DataSourceRequestHandler.parseExportablePermIds(tokens);

        // Then
        assertEquals(result.get(0), new ExportablePermId(ExportableKind.DATASET, "CODE-1"));
    }

    @Test
    public void testParseExportablePermIdsWithNullReturnsEmptyList()
    {
        assertEquals(DataSourceRequestHandler.parseExportablePermIds(null), Collections.emptyList());
    }

    @Test
    public void testParseExportablePermIdsWithoutSeparatorFails()
    {
        try
        {
            DataSourceRequestHandler.parseExportablePermIds(Arrays.asList("SAMPLE-without-kind"));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex)
        {
            assertTrue(ex.getMessage().contains("SAMPLE-without-kind"), ex.getMessage());
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseExportablePermIdsWithUnknownKindFails()
    {
        DataSourceRequestHandler.parseExportablePermIds(Arrays.asList("UNKNOWN/perm"));
    }

    @Test
    public void testGetFlag()
    {
        assertEquals(DataSourceRequestHandler.getFlag(Map.of("f", List.of("true")), "f"), true);
        assertEquals(DataSourceRequestHandler.getFlag(Map.of("f", List.of("false")), "f"), false);
        assertEquals(DataSourceRequestHandler.getFlag(Map.of("f", List.of("TRUE")), "f"), true);
        assertEquals(DataSourceRequestHandler.getFlag(Map.of("other", List.of("true")), "f"), false);
        assertEquals(DataSourceRequestHandler.getFlag(Map.of("f", Collections.emptyList()), "f"), false);
    }

}
