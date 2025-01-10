package ch.eth.sis.rocrate.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IPropertyAssignmentsHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.IPropertyTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.PropertyAssignmentImportHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropertyAssignmentHelper extends BasicImportHelper
{

    EntityTypePermId permId;

    ImportTypes importTypes;

    DataSetTypeHelper dataSetTypeHelper;

    CollectionTypeHelper collectionTypeHelper;

    ObjectTypeHelper objectTypeHelper;

    PropertyHelper propertyHelper;

    public PropertyAssignmentHelper(
            ImportModes mode,
            ImportOptions options,
            DataSetTypeHelper dataSetTypeHelper,
            CollectionTypeHelper collectionTypeHelper,
            ObjectTypeHelper objectTypeHelper,
            PropertyHelper propertyHelper)
    {
        super(mode, options);
        this.dataSetTypeHelper = dataSetTypeHelper;
        this.collectionTypeHelper = collectionTypeHelper;
        this.objectTypeHelper = objectTypeHelper;
        this.propertyHelper = propertyHelper;
    }

    @Override
    protected ImportTypes getTypeName()
    {
        return null;
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return false;
    }

    @Override
    protected void createObject(Map<String, Integer> headers, List<String> values, int page,
            int line)
    {

        String code = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.Code);
        String mandatory = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.Mandatory);
        String defaultValue = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.DefaultValue);
        String showInEditViews = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.ShowInEditViews);
        String section = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.Section);
        String script = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.DynamicScript);
        String unique = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.Unique);
        String pattern = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.Pattern);
        String patternType = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.PatternType);
        String internalAssignment = getValueByColumnName(headers, values,
                PropertyAssignmentImportHelper.Attribute.InternalAssignment);

        PropertyAssignment creation = new PropertyAssignment();
        PropertyType propertyType = propertyHelper.accumulator.get(code);
        PropertyAssignmentFetchOptions propertyAssignmentFetchOptions =
                new PropertyAssignmentFetchOptions();
        propertyAssignmentFetchOptions.withPropertyType();
        creation.setFetchOptions(propertyAssignmentFetchOptions);
        propertyType.setCode(code);
        creation.setPropertyType(propertyType);
        creation.setMandatory(Boolean.parseBoolean(mandatory));
        //creation.setInitialValueForExistingEntities(defaultValue);
        creation.setShowInEditView(Boolean.parseBoolean(showInEditViews));
        creation.setSection(section);
        creation.setUnique(Boolean.parseBoolean(unique));
        creation.setPattern(pattern);
        creation.setPatternType(patternType);
        creation.setManagedInternally(false);

        List<PropertyAssignment> assignments = new ArrayList<>();

        Set<String> existingCodes = Set.of();

        // Add property assignment
        assignments.add(creation);

        switch (importTypes)
        {
            case EXPERIMENT_TYPE:
                ExperimentType experimentType =
                        (ExperimentType) collectionTypeHelper.getResult().get(permId);
                List<PropertyAssignment> assignmentsExperiment =
                        new ArrayList<>(experimentType.getPropertyAssignments());
                assignmentsExperiment.add(creation);
                experimentType.setPropertyAssignments(assignmentsExperiment);

                break;
            case SAMPLE_TYPE:
                SampleType sampleType = (SampleType) objectTypeHelper.getResult().get(permId);
                List<PropertyAssignment> assignmentsSampleType =
                        new ArrayList<>(sampleType.getPropertyAssignments());
                assignmentsSampleType.add(creation);
                sampleType.setPropertyAssignments(assignmentsSampleType);

                break;
            case DATASET_TYPE:
                DataSetType dataSetType = (DataSetType) dataSetTypeHelper.getResult().get(permId);
                List<PropertyAssignment> assignments1 =
                        new ArrayList<>(dataSetType.getPropertyAssignments());
                assignments1.add(creation);
                dataSetType.setPropertyAssignments(assignments1);

                break;
        }
    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {

    }

    @Override
    protected void validateHeader(Map<String, Integer> header)
    {

    }

    private ArrayList<PropertyAssignmentCreation> getPropertyAssignmentsForUpdate()
    {
        IEntityType entityType = null;
        switch (importTypes)
        {
            case EXPERIMENT_TYPE:
                ExperimentTypeFetchOptions eOptions = new ExperimentTypeFetchOptions();
                eOptions.withPropertyAssignments();
                eOptions.withPropertyAssignments().withPropertyType();
                eOptions.withPropertyAssignments().withPlugin();
                dataSetTypeHelper.getResult().get(permId);
                break;
            case SAMPLE_TYPE:
                SampleTypeFetchOptions sOptions = new SampleTypeFetchOptions();
                sOptions.withPropertyAssignments();
                sOptions.withPropertyAssignments().withPropertyType();
                sOptions.withPropertyAssignments().withPlugin();
                break;
            case DATASET_TYPE:
                DataSetTypeFetchOptions dOptions = new DataSetTypeFetchOptions();
                dOptions.withPropertyAssignments();
                dOptions.withPropertyAssignments().withPropertyType();
                dOptions.withPropertyAssignments().withPlugin();
                break;
        }

        ArrayList<PropertyAssignmentCreation> newPropertyAssignmentCreations = new ArrayList<>();
        for (PropertyAssignment propertyAssignment : entityType.getPropertyAssignments())
        {
            PropertyAssignmentCreation creation = new PropertyAssignmentCreation();
            creation.setPropertyTypeId(propertyAssignment.getPropertyType().getPermId());
            if (propertyAssignment.getPlugin() != null)
            {
                creation.setPluginId(propertyAssignment.getPlugin().getPermId());
            }
            creation.setMandatory(propertyAssignment.isMandatory());
            creation.setOrdinal(propertyAssignment.getOrdinal());
            creation.setSection(propertyAssignment.getSection());
            creation.setShowInEditView(propertyAssignment.isShowInEditView());
            newPropertyAssignmentCreations.add(creation);
        }
        return newPropertyAssignmentCreations;
    }

    public void importBlock(List<List<String>> page, int pageIndex, int start, int end,
            ImportTypes importTypes)
    {
        this.importTypes = importTypes;

        Map<String, Integer> header = parseHeader(page.get(start), false);
        String code = getValueByColumnName(header, page.get(start + 1),
                PropertyAssignmentImportHelper.Attribute.Code);

        switch (importTypes)
        {
            case EXPERIMENT_TYPE:
                this.permId = new EntityTypePermId(code, EntityKind.EXPERIMENT);
                break;
            case SAMPLE_TYPE:
                this.permId = new EntityTypePermId(code, EntityKind.SAMPLE);
                break;
            case DATASET_TYPE:
                this.permId = new EntityTypePermId(code, EntityKind.DATA_SET);
                break;
        }

        IPropertyAssignmentsHolder propertyAssignmentsHolder =
                getPropertyAssignmentHolder(this.permId);
        if (propertyAssignmentsHolder != null)
        {

            super.importBlock(page, pageIndex, start + 2, end);
        }


        /*
        IPropertyAssignmentsHolder propertyAssignmentsHolder = getPropertyAssignmentHolder(this.permId);
        if(propertyAssignmentsHolder != null) {
            generateExistingCodes(propertyAssignmentsHolder);
            super.importBlock(page, pageIndex, start + 2, end);
        }*/
    }

    private IPropertyAssignmentsHolder getPropertyAssignmentHolder(IEntityTypeId permId)
    {
        switch (importTypes)
        {
            case EXPERIMENT_TYPE:
                ExperimentTypeFetchOptions experimentFetchOptions =
                        new ExperimentTypeFetchOptions();
                experimentFetchOptions.withPropertyAssignments().withPropertyType();
                experimentFetchOptions.withPropertyAssignments().withPlugin();
                return collectionTypeHelper.getResult().get(permId);
            case SAMPLE_TYPE:
                SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
                sampleTypeFetchOptions.withPropertyAssignments().withPropertyType();
                sampleTypeFetchOptions.withPropertyAssignments().withPlugin();
                return objectTypeHelper.getResult().get(permId);
            case DATASET_TYPE:
                DataSetTypeFetchOptions dataSetTypeFetchOptions = new DataSetTypeFetchOptions();
                dataSetTypeFetchOptions.withPropertyAssignments().withPropertyType();
                dataSetTypeFetchOptions.withPropertyAssignments().withPlugin();
                return dataSetTypeHelper.getResult().get(permId);
            default:
                return null;
        }
    }

    private int indexOf(IPropertyTypeId permId, ArrayList<PropertyAssignmentCreation> creations)
    {
        for (int index = 0; index < creations.size(); index++)
        {
            PropertyAssignmentCreation creation = creations.get(index);
            if (creation.getPropertyTypeId().equals(permId))
            {
                return index;
            }
        }
        return -1;
    }
}
