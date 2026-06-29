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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;

/**
 * Tests for the parsing of the new entity-selection request parameters of {@link DataSourceRequestHandler}.
 *
 * @author Tufail Tak
 */
public class DataSourceRequestHandlerTest
{
    private static final String SESSION_TOKEN = "test-token";

    private Mockery mockery;

    private IApplicationServerApi api;

    @BeforeMethod
    public void beforeMethod()
    {
        mockery = new Mockery();
        api = mockery.mock(IApplicationServerApi.class);
    }

    @AfterMethod
    public void afterMethod()
    {
        mockery.assertIsSatisfied();
    }

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

    @Test
    public void testCollectEntityTypeSeedsWithNoEntitiesReturnsNothing()
    {
        List<ExportablePermId> seeds = DataSourceRequestHandler.collectEntityTypeSeeds(api, SESSION_TOKEN, new EnumMap<>(ExportableKind.class));

        assertEquals(seeds, Collections.emptyList());
    }

    @Test
    public void testCollectEntityTypeSeedsDerivesTypesOfSamplesExperimentsAndDataSets()
    {
        Map<ExportableKind, List<String>> permIdsByKind = new EnumMap<>(ExportableKind.class);
        permIdsByKind.put(ExportableKind.SAMPLE, List.of("SAMPLE-1"));
        permIdsByKind.put(ExportableKind.EXPERIMENT, List.of("EXPERIMENT-1"));
        permIdsByKind.put(ExportableKind.DATASET, List.of("DATASET-1"));

        Sample sample = sample("SAMPLE-1", "SAMPLE_TYPE_A");
        Experiment experiment = experiment("EXPERIMENT-1", "EXPERIMENT_TYPE_A");
        DataSet dataSet = dataSet("DATASET-1", "DATASET_TYPE_A");

        mockery.checking(new Expectations()
        {
            {
                allowing(api).getSamples(with(SESSION_TOKEN), with(List.of(new SamplePermId("SAMPLE-1"))), with(any(SampleFetchOptions.class)));
                will(returnValue(Map.of(sample.getPermId(), sample)));

                allowing(api).getExperiments(with(SESSION_TOKEN), with(List.of(new ExperimentPermId("EXPERIMENT-1"))),
                        with(any(ExperimentFetchOptions.class)));
                will(returnValue(Map.of(experiment.getPermId(), experiment)));

                allowing(api).getDataSets(with(SESSION_TOKEN), with(List.of(new DataSetPermId("DATASET-1"))), with(any(DataSetFetchOptions.class)));
                will(returnValue(Map.of(dataSet.getPermId(), dataSet)));
            }
        });

        List<ExportablePermId> seeds = DataSourceRequestHandler.collectEntityTypeSeeds(api, SESSION_TOKEN, permIdsByKind);

        assertEquals(new HashSet<>(seeds), Set.of(
                new ExportablePermId(ExportableKind.SAMPLE_TYPE, "SAMPLE_TYPE_A"),
                new ExportablePermId(ExportableKind.EXPERIMENT_TYPE, "EXPERIMENT_TYPE_A"),
                new ExportablePermId(ExportableKind.DATASET_TYPE, "DATASET_TYPE_A")));
    }

    private static Sample sample(String permId, String typeCode)
    {
        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();

        SampleType type = new SampleType();
        type.setCode(typeCode);

        Sample sample = new Sample();
        sample.setFetchOptions(fetchOptions);
        sample.setPermId(new SamplePermId(permId));
        sample.setType(type);
        return sample;
    }

    private static Experiment experiment(String permId, String typeCode)
    {
        ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        fetchOptions.withType();

        ExperimentType type = new ExperimentType();
        type.setCode(typeCode);

        Experiment experiment = new Experiment();
        experiment.setFetchOptions(fetchOptions);
        experiment.setPermId(new ExperimentPermId(permId));
        experiment.setType(type);
        return experiment;
    }

    private static DataSet dataSet(String permId, String typeCode)
    {
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withType();

        DataSetType type = new DataSetType();
        type.setCode(typeCode);

        DataSet dataSet = new DataSet();
        dataSet.setFetchOptions(fetchOptions);
        dataSet.setPermId(new DataSetPermId(permId));
        dataSet.setType(type);
        return dataSet;
    }

}
