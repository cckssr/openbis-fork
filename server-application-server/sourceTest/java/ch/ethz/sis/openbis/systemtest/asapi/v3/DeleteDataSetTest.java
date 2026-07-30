/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.systemtest.asapi.v3;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.delete.DataSetDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.id.IDeletionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.history.PropertyHistoryEntry;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.systemsx.cisd.common.action.IDelegatedAction;
import ch.systemsx.cisd.openbis.generic.shared.dto.SamplePE;
import ch.systemsx.cisd.openbis.systemtest.authorization.ProjectAuthorizationUser;
import junit.framework.Assert;

/**
 * @author pkupczyk
 */
public class DeleteDataSetTest extends AbstractDeletionTest
{

    private static DataSetDeletionOptions options;

    public static DataSetDeletionOptions getOptions()
    {
        if (options == null)
        {
            options = new DataSetDeletionOptions();
            options.setReason("Just for testing");
        }
        return options;
    }

    @Test
    public void testDeleteEmptyList()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        IDeletionId deletionId = v3api.deleteDataSets(sessionToken, new ArrayList<DataSetPermId>(), getOptions());
        Assert.assertNull(deletionId);
    }

    @Test
    public void testDeleteDataSet()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        DataSetPermId dataSetId = new DataSetPermId("COMPONENT_1A");

        IDeletionId deletionId = v3api.deleteDataSets(sessionToken, Collections.singletonList(dataSetId), getOptions());
        Assert.assertNotNull(deletionId);

        assertDataSetDoesNotExist(dataSetId);
    }

    @Test
    public void testDeleteContainerDataSet()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        DataSetPermId container = new DataSetPermId("CONTAINER_1");
        DataSetPermId component1 = new DataSetPermId("COMPONENT_1A");
        DataSetPermId component2 = new DataSetPermId("COMPONENT_1B");

        IDeletionId deletionId = v3api.deleteDataSets(sessionToken, Collections.singletonList(container), getOptions());
        Assert.assertNotNull(deletionId);

        assertDataSetDoesNotExist(container);
        assertDataSetDoesNotExist(component1);
        assertDataSetDoesNotExist(component2);
    }

    @Test
    public void testDeleteComplexContainerDataSet()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        DataSetPermId container = new DataSetPermId("CONTAINER_3B");
        DataSetPermId component1 = new DataSetPermId("COMPONENT_3A");
        DataSetPermId component2 = new DataSetPermId("COMPONENT_3AB");

        IDeletionId deletionId = v3api.deleteDataSets(sessionToken, Collections.singletonList(container), getOptions());
        Assert.assertNotNull(deletionId);

        assertDataSetDoesNotExist(container);
        assertDataSetExists(component1);
        assertDataSetExists(component2);
    }

    @Test
    public void testDeleteComplexContainerDataSet1()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        DataSetPermId container = new DataSetPermId("CONTAINER_3A");
        DataSetPermId component1 = new DataSetPermId("COMPONENT_3A");
        DataSetPermId component2 = new DataSetPermId("COMPONENT_3AB");

        IDeletionId deletionId = v3api.deleteDataSets(sessionToken, Collections.singletonList(container), getOptions());
        Assert.assertNotNull(deletionId);

        assertDataSetDoesNotExist(container);
        assertDataSetDoesNotExist(component1);
        assertDataSetExists(component2);
    }

    @Test
    public void testDeleteDSWithAdminUserInAnotherSpace()
    {
        final DataSetPermId permId = new DataSetPermId("20120619092259000-22");

        assertUnauthorizedObjectAccessException(new IDelegatedAction()
        {
            @Override
            public void execute()
            {
                String sessionToken = v3api.login(TEST_ROLE_V3, PASSWORD);

                v3api.deleteDataSets(sessionToken, Collections.singletonList(permId), getOptions());
            }
        }, permId);
    }

    @Test(dataProviderClass = ProjectAuthorizationUser.class, dataProvider = ProjectAuthorizationUser.PROVIDER_WITH_ETL)
    public void testDeleteWithProjectAuthorization(ProjectAuthorizationUser user)
    {
        String sessionToken = v3api.login(user.getUserId(), PASSWORD);

        IDataSetId dataSetId = new DataSetPermId("20120628092259000-41");

        if (user.isDisabledProjectUser())
        {
            assertAuthorizationFailureException(new IDelegatedAction()
            {
                @Override
                public void execute()
                {
                    v3api.deleteDataSets(sessionToken, Arrays.asList(dataSetId), getOptions());
                }
            });
        } else if (user.isInstanceUserOrTestSpaceUserOrEnabledTestProjectUser())
        {
            v3api.deleteDataSets(sessionToken, Arrays.asList(dataSetId), getOptions());
        } else
        {
            assertUnauthorizedObjectAccessException(new IDelegatedAction()
            {
                @Override
                public void execute()
                {
                    v3api.deleteDataSets(sessionToken, Arrays.asList(dataSetId), getOptions());
                }
            }, dataSetId);
        }
    }

    @Test
    public void testLogging()
    {
        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        DataSetDeletionOptions o = new DataSetDeletionOptions();
        o.setReason("test-reason");

        v3api.deleteDataSets(sessionToken, Arrays.asList(new DataSetPermId("TEST-LOGGING-1"), new DataSetPermId("TEST-LOGGING-2")), o);

        assertAccessLog(
                "delete-data-sets  DATA_SET_IDS('[TEST-LOGGING-1, TEST-LOGGING-2]') DELETION_OPTIONS('DataSetDeletionOptions[reason=test-reason]')");
    }

    @Test
    public void testDeleteSampleOfASampleProperty()
    {
        Comparator<PropertyHistoryEntry> PROPERTY_HISTORY_COMPARATOR = Comparator.comparing(
                        PropertyHistoryEntry::getPropertyName).thenComparing(e -> e.getPropertyValue().length())
                .thenComparing(PropertyHistoryEntry::getPropertyValue);

        String sessionToken = v3api.login(TEST_USER, PASSWORD);

        // create SAMPLE-PROPERTY-SINGLE-VALUED and SAMPLE-PROPERTY-MULTI-VALUED property types
        PropertyTypePermId propertyTypeSingleValued =
                createASamplePropertyType(sessionToken, null, "SAMPLE-PROPERTY-SINGLE-VALUED-" + System.currentTimeMillis(), false);
        PropertyTypePermId propertyTypeMultiValued =
                createASamplePropertyType(sessionToken, null, "SAMPLE-PROPERTY-MULTI-VALUED-" + System.currentTimeMillis(), true);
        EntityTypePermId dataSetType = createADataSetType(sessionToken, false, propertyTypeSingleValued, propertyTypeMultiValued);

        ExperimentPermId propertySampleExperimentPermId = createCisdExperiment();
        SamplePermId propertySampleAPermId = createCisdSample(propertySampleExperimentPermId, "SAMPLE-A-" + System.currentTimeMillis());
        SamplePermId propertySampleBPermId = createCisdSample(propertySampleExperimentPermId, "SAMPLE-B-" + System.currentTimeMillis());
        SamplePE propertySampleA = daoFactory.getSampleDAO().tryToFindByPermID(propertySampleAPermId.getPermId());
        SamplePE propertySampleB = daoFactory.getSampleDAO().tryToFindByPermID(propertySampleBPermId.getPermId());

        // create sample with SAMPLE-PROPERTY-SINGLE-VALUED = SAMPLE-A and SAMPLE-PROPERTY-MULTI-VALUED = SAMPLE-A, SAMPLE-B
        DataSetCreation dataSetCreation = physicalDataSetCreation();
        dataSetCreation.setTypeId(dataSetType);
        dataSetCreation.setExperimentId(new ExperimentIdentifier("/TEST-SPACE/TEST-PROJECT/EXP-SPACE-TEST"));
        dataSetCreation.setProperty(propertyTypeSingleValued.getPermId(), propertySampleAPermId.getPermId());
        dataSetCreation.setMultiValueSampleProperty(propertyTypeMultiValued.getPermId(), List.of(propertySampleAPermId, propertySampleBPermId));
        DataSetPermId dataSetPermId = v3api.createDataSets(sessionToken, Arrays.asList(dataSetCreation)).get(0);

        SampleDeletionOptions deletionOptions = new SampleDeletionOptions();
        deletionOptions.setReason("a test");

        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withSampleProperties();
        fetchOptions.withPropertiesHistory();

        DataSet dataSet = v3api.getDataSets(sessionToken, Arrays.asList(dataSetPermId), fetchOptions).get(dataSetPermId);
        List<PropertyHistoryEntry> propertiesHistory =
                dataSet.getPropertiesHistory().stream().map(e -> (PropertyHistoryEntry) e).collect(Collectors.toList());
        assertEquals(propertiesHistory.size(), 3);
        Collections.sort(propertiesHistory, PROPERTY_HISTORY_COMPARATOR);

        // property history entries have SAMPLE-A and SAMPLE-B values as perm ids
        assertEquals(propertiesHistory.get(0).getPropertyName(), propertyTypeMultiValued.getPermId());
        assertEquals(propertiesHistory.get(0).getPropertyValue(), String.valueOf(propertySampleA.getPermId()));
        assertNotNull(propertiesHistory.get(0).getValidFrom());
        assertNull(propertiesHistory.get(0).getValidTo());

        assertEquals(propertiesHistory.get(1).getPropertyName(), propertyTypeMultiValued.getPermId());
        assertEquals(propertiesHistory.get(1).getPropertyValue(), String.valueOf(propertySampleB.getPermId()));
        assertNotNull(propertiesHistory.get(1).getValidFrom());
        assertNull(propertiesHistory.get(1).getValidTo());

        assertEquals(propertiesHistory.get(2).getPropertyName(), propertyTypeSingleValued.getPermId());
        assertEquals(propertiesHistory.get(2).getPropertyValue(), String.valueOf(propertySampleA.getPermId()));
        assertNotNull(propertiesHistory.get(2).getValidFrom());
        assertNull(propertiesHistory.get(2).getValidTo());

        // move SAMPLE-A to trash
        IDeletionId deletionId = v3api.deleteSamples(sessionToken, Arrays.asList(propertySampleAPermId), deletionOptions);

        dataSet = v3api.getDataSets(sessionToken, Arrays.asList(dataSetPermId), fetchOptions).get(dataSetPermId);
        assertEquals(dataSet.getSampleProperties().size(), 1);
        assertSamplePermIdsInOrder(dataSet.getSampleProperties().get(propertyTypeMultiValued.getPermId()), propertySampleBPermId.getPermId());
        assertEquals(dataSet.getProperties().size(), 1);
        assertEquals(((Serializable[]) dataSet.getProperties().get(propertyTypeMultiValued.getPermId()))[0], propertySampleBPermId.getPermId());
        propertiesHistory =
                dataSet.getPropertiesHistory().stream().map(e -> (PropertyHistoryEntry) e).collect(Collectors.toList());
        assertEquals(propertiesHistory.size(), 3);
        Collections.sort(propertiesHistory, PROPERTY_HISTORY_COMPARATOR);

        // property history entries have SAMPLE-A value as tech id and SAMPLE-B value as perm id
        assertEquals(propertiesHistory.get(0).getPropertyName(), propertyTypeMultiValued.getPermId());
        assertEquals(propertiesHistory.get(0).getPropertyValue(), String.valueOf(propertySampleA.getId()));
        assertNotNull(propertiesHistory.get(0).getValidFrom());
        assertNull(propertiesHistory.get(0).getValidTo());

        assertEquals(propertiesHistory.get(1).getPropertyName(), propertyTypeMultiValued.getPermId());
        assertEquals(propertiesHistory.get(1).getPropertyValue(), String.valueOf(propertySampleB.getPermId()));
        assertNotNull(propertiesHistory.get(1).getValidFrom());
        assertNull(propertiesHistory.get(1).getValidTo());

        assertEquals(propertiesHistory.get(2).getPropertyName(), propertyTypeSingleValued.getPermId());
        assertEquals(propertiesHistory.get(2).getPropertyValue(), String.valueOf(propertySampleA.getId()));
        assertNotNull(propertiesHistory.get(2).getValidFrom());
        assertNull(propertiesHistory.get(2).getValidTo());

        // delete SAMPLE-A permanently
        v3api.confirmDeletions(sessionToken, Arrays.asList(deletionId));

        dataSet = v3api.getDataSets(sessionToken, Arrays.asList(dataSetPermId), fetchOptions).get(dataSetPermId);
        assertEquals(dataSet.getSampleProperties().size(), 1);
        assertSamplePermIdsInOrder(dataSet.getSampleProperties().get(propertyTypeMultiValued.getPermId()), propertySampleBPermId.getPermId());
        assertEquals(dataSet.getProperties().size(), 1);
        assertEquals(((Serializable[]) dataSet.getProperties().get(propertyTypeMultiValued.getPermId()))[0], propertySampleBPermId.getPermId());
        propertiesHistory =
                dataSet.getPropertiesHistory().stream().map(e -> (PropertyHistoryEntry) e).collect(Collectors.toList());
        assertEquals(propertiesHistory.size(), 3);
        Collections.sort(propertiesHistory, PROPERTY_HISTORY_COMPARATOR);

        // property history entries have SAMPLE-A value as tech id and SAMPLE-B value as perm id
        assertEquals(propertiesHistory.get(0).getPropertyName(), propertyTypeMultiValued.getPermId());
        assertEquals(propertiesHistory.get(0).getPropertyValue(), String.valueOf(propertySampleA.getId()));
        assertNotNull(propertiesHistory.get(0).getValidFrom());
        assertNotNull(propertiesHistory.get(0).getValidTo());

        assertEquals(propertiesHistory.get(1).getPropertyName(), propertyTypeMultiValued.getPermId());
        assertEquals(propertiesHistory.get(1).getPropertyValue(), String.valueOf(propertySampleB.getPermId()));
        assertNotNull(propertiesHistory.get(1).getValidFrom());
        assertNull(propertiesHistory.get(1).getValidTo());

        assertEquals(propertiesHistory.get(2).getPropertyName(), propertyTypeSingleValued.getPermId());
        assertEquals(propertiesHistory.get(2).getPropertyValue(), String.valueOf(propertySampleA.getId()));
        assertNotNull(propertiesHistory.get(2).getValidFrom());
        assertNotNull(propertiesHistory.get(2).getValidTo());

        // update SAMPLE-PROPERTY-SINGLE-VALUED and SAMPLE-PROPERTY-MULTI-VALUED property values
        DataSetUpdate update = new DataSetUpdate();
        update.setDataSetId(dataSetPermId);
        update.setProperty(propertyTypeSingleValued.getPermId(), null);
        update.setProperty(propertyTypeMultiValued.getPermId(), null);
        v3api.updateDataSets(sessionToken, List.of(update));

        dataSet = v3api.getDataSets(sessionToken, Arrays.asList(dataSetPermId), fetchOptions).get(dataSetPermId);
        assertEquals(dataSet.getProperties().size(), 0);
        assertEquals(dataSet.getSampleProperties().size(), 0);
    }

    @Test
    public void testDeleteSampleWithSampleProperty()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        PropertyTypePermId propertyType = createASamplePropertyType(sessionToken, null);
        EntityTypePermId dataSetType = createADataSetType(sessionToken, true, propertyType);
        DataSetCreation creation = physicalDataSetCreation();
        creation.setTypeId(dataSetType);
        creation.setExperimentId(new ExperimentIdentifier("/TEST-SPACE/TEST-PROJECT/EXP-SPACE-TEST"));
        SamplePermId propertySamplePermId = createCisdSample(createCisdExperiment());
        creation.setProperty(propertyType.getPermId(), propertySamplePermId.getPermId());
        DataSetPermId dataSetPermId = v3api.createDataSets(sessionToken, Arrays.asList(creation)).get(0);
        DataSetDeletionOptions deletionOptions = new DataSetDeletionOptions();
        deletionOptions.setReason("a test");

        // When
        IDeletionId deletionId = v3api.deleteDataSets(sessionToken, Arrays.asList(dataSetPermId), deletionOptions);

        // Then
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withSampleProperties();
        assertEquals(v3api.getDataSets(sessionToken, Arrays.asList(dataSetPermId), fetchOptions).toString(), "{}");
        v3api.confirmDeletions(sessionToken, Arrays.asList(deletionId));
        assertEquals(v3api.getDataSets(sessionToken, Arrays.asList(dataSetPermId), fetchOptions).toString(), "{}");
    }

    @Test
    public void testDeleteDataSetOfTypeWhichForbidsDeletion()
    {
        // Given
        String sessionToken = v3api.login(TEST_USER, PASSWORD);
        DataSetTypeCreation typeCreation = new DataSetTypeCreation();
        typeCreation.setCode("UNDELETABLE");
        typeCreation.setDisallowDeletion(true);
        EntityTypePermId undeletableType = v3api.createDataSetTypes(sessionToken, Arrays.asList(typeCreation)).get(0);

        DataSetCreation creation = new DataSetCreation();
        creation.setAutoGeneratedCode(true);
        creation.setDataStoreId(new DataStorePermId("STANDARD"));
        creation.setSampleId(new SampleIdentifier("/CISD/NOE/CP-TEST-2"));
        creation.setDataSetKind(DataSetKind.CONTAINER);
        creation.setTypeId(undeletableType);
        DataSetPermId dataSetId = v3api.createDataSets(sessionToken, Arrays.asList(creation)).get(0);
        DataSetDeletionOptions deletionOptions = new DataSetDeletionOptions();
        deletionOptions.setReason("test");

        // When
        assertUserFailureException(Void -> v3api.deleteDataSets(sessionToken, Arrays.asList(dataSetId), deletionOptions),
                // Then
                "Data set " + dataSetId.getPermId() + " can not be deleted");
    }

    // waiting for better times
    // @Test
    // public void testDeleteContainerInDifferentExperiment()
    // {
    // String sessionToken = v3api.login(TEST_USER, PASSWORD);
    //
    // DataSetPermId containerA = new DataSetPermId("CONTAINER_3A");
    // DataSetPermId containerB = new DataSetPermId("CONTAINER_3B");
    // DataSetPermId component1 = new DataSetPermId("COMPONENT_3A");
    // DataSetPermId component2 = new DataSetPermId("COMPONENT_3AB");
    //
    // DataSetPermId componentDifferentExperiment = new DataSetPermId("COMPONENT_3Ax");
    //
    // assertDataSetExists(containerA);
    // assertDataSetExists(containerB);
    // assertDataSetExists(component1);
    // assertDataSetExists(component2);
    // assertDataSetExists(componentDifferentExperiment);
    //
    // IDeletionId deletionId = v3api.deleteDataSets(sessionToken, Collections.singletonList(containerA), getOptions());
    // Assert.assertNotNull(deletionId);
    // deletionId = v3api.deleteDataSets(sessionToken, Collections.singletonList(containerB), getOptions());
    // Assert.assertNotNull(deletionId);
    //
    // assertDataSetDoesNotExist(containerA);
    // assertDataSetDoesNotExist(containerB);
    // assertDataSetDoesNotExist(component1);
    // assertDataSetDoesNotExist(component2);
    // assertDataSetExists(componentDifferentExperiment);
    // }

}
