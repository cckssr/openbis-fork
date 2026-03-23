package ch.ethz.sis.openbis.systemtest.asapi.v3;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.CreationId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.PhysicalDataCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.*;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DataSetCommonEntityTest extends AbstractCommonEntityTest<DataSetTypeCreation, DataSetType, DataSetTypeUpdate, DataSetCreation, DataSet, DataSetUpdate, IDataSetId>
{
    @Override
    protected DataSetTypeCreation newTypeCreation()
    {
        return new DataSetTypeCreation();
    }

    @Override
    protected DataSetCreation newEntityCreation(DataSetType type, String code)
    {
        PhysicalDataCreation physicalCreation = new PhysicalDataCreation();
        physicalCreation.setLocation("test/location/" + code);
        physicalCreation.setLocatorTypeId(new RelativeLocationLocatorTypePermId());
        physicalCreation.setStorageFormatId(new ProprietaryStorageFormatPermId());

        DataSetCreation creation = new DataSetCreation();
        creation.setCode(code);
        creation.setDataSetKind(DataSetKind.PHYSICAL);
        creation.setTypeId(type.getPermId());
        creation.setExperimentId(new ExperimentIdentifier("/CISD/NEMO/EXP1"));
        creation.setDataStoreId(new DataStorePermId("STANDARD"));
        creation.setPhysicalData(physicalCreation);
        creation.setCreationId(new CreationId(code));

        return creation;
    }

    @Override
    protected DataSetTypeUpdate newTypeUpdate(String typeCode)
    {
        DataSetTypeUpdate update = new DataSetTypeUpdate();
        update.setTypeId(new EntityTypePermId(typeCode));
        return update;
    }

    @Override
    protected DataSetUpdate newEntityUpdate(String permId)
    {
        DataSetUpdate update = new DataSetUpdate();
        update.setDataSetId(new DataSetPermId(permId));
        return update;
    }

    @Override
    protected void createType(String sessionToken, DataSetTypeCreation creations)
    {
        v3api.createDataSetTypes(sessionToken, Arrays.asList(creations));
    }

    @Override
    protected DataSetType getType(String sessionToken, String typeCode)
    {
        final DataSetTypeFetchOptions fo = new DataSetTypeFetchOptions();
        fo.withPropertyAssignments().withPropertyType();
        fo.withPropertyAssignments().withRegistrator();

        final EntityTypePermId permId = new EntityTypePermId(typeCode);
        return v3api.getDataSetTypes(sessionToken, Collections.singletonList(permId), fo)
                .get(permId);
    }

    @Override
    protected IDataSetId createEntity(String sessionToken, DataSetCreation creation)
    {
        List<DataSetPermId> permIds = v3api.createDataSets(sessionToken, Arrays.asList(creation));
        return permIds.get(0);

    }

    @Override
    protected DataSet getEntity(String sessionToken, IDataSetId id)
    {
        DataSetFetchOptions fo = new DataSetFetchOptions();
        fo.withProperties();
        Map<IDataSetId, DataSet> dataSets = v3api.getDataSets(sessionToken, Arrays.asList(id), fo);
        return dataSets.values().iterator().next();
    }

    @Override
    protected void updateType(String sessionToken, DataSetTypeUpdate update)
    {
        v3api.updateDataSetTypes(sessionToken, Arrays.asList(update));
    }

    @Override
    protected void updateEntity(String sessionToken, DataSetUpdate update)
    {
        v3api.updateDataSets(sessionToken, Arrays.asList(update));
    }

    @Test
    public void testDummy() {
        //dummy test for a simple test run
    }
}
