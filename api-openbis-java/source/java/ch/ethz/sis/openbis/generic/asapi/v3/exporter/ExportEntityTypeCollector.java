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
package ch.ethz.sis.openbis.generic.asapi.v3.exporter;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.ICodeHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ExportEntityTypeCollector
{
    private static final Map<Class<?>, ExportableKind> EXPORTABLE_KIND_BY_ENTITY_TYPE;

    static
    {
        final Map<Class<?>, ExportableKind> map = new HashMap<>();
        map.put(SampleType.class, ExportableKind.SAMPLE_TYPE);
        map.put(ExperimentType.class, ExportableKind.EXPERIMENT_TYPE);
        map.put(DataSetType.class, ExportableKind.DATASET_TYPE);
        EXPORTABLE_KIND_BY_ENTITY_TYPE = Collections.unmodifiableMap(map);
    }

    private ExportEntityTypeCollector()
    {
        throw new UnsupportedOperationException("Instantiation of a utility class.");
    }

    public static List<ExportablePermId> expandReference(final IApplicationServerApi api,
            final String sessionToken, final List<ExportablePermId> exportablePermIds)
    {
        return exportablePermIds.stream().flatMap(exportablePermId ->
        {
            final Stream<ExportablePermId> expandedExportablePermIds = getExpandedExportablePermIds(api, sessionToken,
                    exportablePermId, new HashSet<>(Collections.singletonList(exportablePermId)));
            return Stream.concat(expandedExportablePermIds, Stream.of(exportablePermId));
        }).distinct().collect(Collectors.toList());
    }

    private static Stream<ExportablePermId> getExpandedExportablePermIds(final IApplicationServerApi api,
            final String sessionToken, final ExportablePermId exportablePermId, final Set<ExportablePermId> processedIds)
    {
        final ExportableKind exportableKind = exportablePermId.getExportableKind();
        final IEntityType entityType = fetchEntityType(api, sessionToken, exportableKind, exportablePermId.getPermId());

        if (entityType == null)
        {
            return Stream.of();
        }

        final ExportableKind entityTypeKind = EXPORTABLE_KIND_BY_ENTITY_TYPE.get(entityType.getClass());
        Stream<ExportablePermId> resultStream = entityTypeKind != null
                ? Stream.of(new ExportablePermId(entityTypeKind, entityType.getCode()))
                : Stream.of();

        if (entityTypeKind == ExportableKind.SAMPLE_TYPE)
        {
            final SampleType sampleType = (SampleType) entityType;
            if (!sampleType.getTypeGroupAssignments().isEmpty())
            {
                resultStream = Stream.concat(sampleType.getTypeGroupAssignments().stream().flatMap(assignment ->
                {
                    final TypeGroup typeGroup = assignment.getTypeGroup();
                    return Stream.of(new ExportablePermId(ExportableKind.TYPE_GROUP, typeGroup.getCode()));
                }), resultStream);
            }
        }

        return Stream.concat(entityType.getPropertyAssignments().stream().flatMap(propertyAssignment ->
                {
                    final PropertyType propertyType = propertyAssignment.getPropertyType();
                    switch (propertyType.getDataType())
                    {
                        case CONTROLLEDVOCABULARY:
                        {
                            return Stream.of(new ExportablePermId(ExportableKind.VOCABULARY_TYPE,
                                    propertyType.getVocabulary().getPermId().getPermId()));
                        }
                        case SAMPLE:
                        {
                            return getExportablePermIdStreamForEntityType(api, sessionToken, processedIds,
                                    propertyType.getSampleType(), ExportableKind.SAMPLE_TYPE);
                        }
                        default:
                        {
                            return Stream.empty();
                        }
                    }
                }), resultStream);
    }

    private static Stream<ExportablePermId> getExportablePermIdStreamForEntityType(final IApplicationServerApi api,
            final String sessionToken, final Set<ExportablePermId> processedIds, final ICodeHolder codeHolder,
            final ExportableKind exportableKind)
    {
        if (codeHolder != null)
        {
            final ExportablePermId entityPropertyExportablePermId = new ExportablePermId(exportableKind, codeHolder.getCode());

            if (processedIds.contains(entityPropertyExportablePermId))
            {
                return Stream.empty();
            } else
            {
                processedIds.add(entityPropertyExportablePermId);

                final Stream<ExportablePermId> entityPropertyExpandedExportablePermIds =
                        getExpandedExportablePermIds(api, sessionToken, entityPropertyExportablePermId, processedIds);

                return Stream.concat(entityPropertyExpandedExportablePermIds,
                        Stream.of(entityPropertyExportablePermId));
            }
        } else
        {
            return Stream.empty();
        }
    }

    private static IEntityType fetchEntityType(final IApplicationServerApi api, final String sessionToken,
            final ExportableKind exportableKind, final String permId)
    {
        switch (exportableKind)
        {
            case SAMPLE:
            {
                return fetchTypeOfSample(api, sessionToken, permId);
            }
            case EXPERIMENT:
            {
                return fetchTypeOfExperiment(api, sessionToken, permId);
            }
            case DATASET:
            {
                return fetchTypeOfDataSet(api, sessionToken, permId);
            }
            case SAMPLE_TYPE:
            {
                return fetchSampleType(api, sessionToken, permId);
            }
            case EXPERIMENT_TYPE:
            {
                return fetchExperimentType(api, sessionToken, permId);
            }
            case DATASET_TYPE:
            {
                return fetchDataSetType(api, sessionToken, permId);
            }
            default:
            {
                return null;
            }
        }
    }

    public static SampleType fetchTypeOfSample(final IApplicationServerApi api, final String sessionToken, final String permId)
    {
        final SampleFetchOptions fetchOptions = new SampleFetchOptions();
        configureSampleTypeFetchOptions(fetchOptions.withType());

        final Map<ISampleId, Sample> samples = api.getSamples(sessionToken,
                Collections.singletonList(new SamplePermId(permId)), fetchOptions);

        assert samples.size() <= 1;

        final Iterator<Sample> iterator = samples.values().iterator();
        return iterator.hasNext() ? iterator.next().getType() : null;
    }

    public static ExperimentType fetchTypeOfExperiment(final IApplicationServerApi api, final String sessionToken, final String permId)
    {
        final ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
        configureExperimentTypeFetchOptions(fetchOptions.withType());

        final Map<IExperimentId, Experiment> experiments = api.getExperiments(sessionToken,
                Collections.singletonList(new ExperimentPermId(permId)), fetchOptions);

        assert experiments.size() <= 1;

        final Iterator<Experiment> iterator = experiments.values().iterator();
        return iterator.hasNext() ? iterator.next().getType() : null;
    }

    public static DataSetType fetchTypeOfDataSet(final IApplicationServerApi api, final String sessionToken, final String permId)
    {
        final DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        configureDataSetTypeFetchOptions(fetchOptions.withType());

        final Map<IDataSetId, DataSet> dataSets = api.getDataSets(sessionToken,
                Collections.singletonList(new DataSetPermId(permId)), fetchOptions);

        assert dataSets.size() <= 1;

        final Iterator<DataSet> iterator = dataSets.values().iterator();
        return iterator.hasNext() ? iterator.next().getType() : null;
    }

    public static SampleType fetchSampleType(final IApplicationServerApi api, final String sessionToken, final String permId)
    {
        final SampleTypeFetchOptions fetchOptions = new SampleTypeFetchOptions();
        configureSampleTypeFetchOptions(fetchOptions);

        final Map<IEntityTypeId, SampleType> sampleTypes = api.getSampleTypes(sessionToken,
                Collections.singletonList(new EntityTypePermId(permId, EntityKind.SAMPLE)), fetchOptions);

        assert sampleTypes.size() <= 1;

        final Iterator<SampleType> iterator = sampleTypes.values().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public static ExperimentType fetchExperimentType(final IApplicationServerApi api, final String sessionToken, final String permId)
    {
        final ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
        configureExperimentTypeFetchOptions(fetchOptions);

        final Map<IEntityTypeId, ExperimentType> experimentTypes = api.getExperimentTypes(sessionToken,
                Collections.singletonList(new EntityTypePermId(permId, EntityKind.EXPERIMENT)), fetchOptions);

        assert experimentTypes.size() <= 1;

        final Iterator<ExperimentType> iterator = experimentTypes.values().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public static DataSetType fetchDataSetType(final IApplicationServerApi api, final String sessionToken, final String permId)
    {
        final DataSetTypeFetchOptions fetchOptions = new DataSetTypeFetchOptions();
        configureDataSetTypeFetchOptions(fetchOptions);

        final Map<IEntityTypeId, DataSetType> dataSetTypes = api.getDataSetTypes(sessionToken,
                Collections.singletonList(new EntityTypePermId(permId, EntityKind.DATA_SET)), fetchOptions);

        assert dataSetTypes.size() <= 1;

        final Iterator<DataSetType> iterator = dataSetTypes.values().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public static void configureSampleTypeFetchOptions(final SampleTypeFetchOptions fetchOptions)
    {
        fetchOptions.withValidationPlugin().withScript();
        fetchOptions.withTypeGroupAssignments().withTypeGroup();
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = fetchOptions.withPropertyAssignments();
        final PropertyTypeFetchOptions propertyTypeFetchOptions = propertyAssignmentFetchOptions.withPropertyType();
        propertyTypeFetchOptions.withVocabulary();
        propertyTypeFetchOptions.withSampleType();
        propertyAssignmentFetchOptions.withPlugin().withScript();
    }

    public static void configureExperimentTypeFetchOptions(final ExperimentTypeFetchOptions fetchOptions)
    {
        fetchOptions.withValidationPlugin().withScript();
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = fetchOptions.withPropertyAssignments();
        propertyAssignmentFetchOptions.withPropertyType().withVocabulary();
        propertyAssignmentFetchOptions.withPropertyType().withSampleType();
        propertyAssignmentFetchOptions.withPlugin().withScript();
    }

    public static void configureDataSetTypeFetchOptions(final DataSetTypeFetchOptions fetchOptions)
    {
        fetchOptions.withValidationPlugin().withScript();
        final PropertyAssignmentFetchOptions propertyAssignmentFetchOptions = fetchOptions.withPropertyAssignments();
        propertyAssignmentFetchOptions.withPropertyType().withVocabulary();
        propertyAssignmentFetchOptions.withPropertyType().withSampleType();
        propertyAssignmentFetchOptions.withPlugin().withScript();
    }

}
