package ch.ethz.sis.openbis.generic.excel.v3.from.helper;

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
import ch.ethz.sis.openbis.generic.excel.v3.from.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.excel.v3.from.utils.IAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropertyAssignmentHelper extends BasicImportHelper
{

    public enum Attribute implements IAttribute
    {
        Version("Version", false, false),
        Code("Code", true, true),
        Mandatory("Mandatory", true, false),
        DefaultValue("Default Value", false, false),
        ShowInEditViews("Show in edit views", true, false),
        Section("Section", true, false),
        PropertyLabel("Property label", true, false),
        DataType("Data type", true, true),
        VocabularyCode("Vocabulary code", true, true),
        Description("Description", true, false),
        Metadata("Metadata", false, false),
        DynamicScript("Dynamic script", false, false),
        OntologyId("Ontology Id", false, false),
        OntologyVersion("Ontology Version", false, false),
        OntologyAnnotationId("Ontology Annotation Id", false, false),
        MultiValued("Multivalued", false, false),
        Unique("Unique", false, false),
        Pattern("Pattern", false, false),
        PatternType("Pattern Type", false, false),
        InternalAssignment("Internal Assignment", false, false),
        Internal("Internal", false, false);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

        public String getHeaderName()
        {
            return headerName;
        }

        @Override
        public boolean isMandatory()
        {
            return mandatory;
        }

        @Override
        public boolean isUpperCase()
        {
            return upperCase;
        }
    }

    EntityTypePermId permId;

    DataSetTypeHelper dataSetTypeHelper;

    CollectionTypeHelper collectionTypeHelper;

    ObjectTypeHelper objectTypeHelper;

    PropertyHelper propertyHelper;

    ImportTypes importTypes;

    public PropertyAssignmentHelper(
            DataSetTypeHelper dataSetTypeHelper,
            CollectionTypeHelper collectionTypeHelper,
            ObjectTypeHelper objectTypeHelper,
            PropertyHelper propertyHelper)
    {
        this.dataSetTypeHelper = dataSetTypeHelper;
        this.collectionTypeHelper = collectionTypeHelper;
        this.objectTypeHelper = objectTypeHelper;
        this.propertyHelper = propertyHelper;
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
                Attribute.Code);
        String mandatory = getValueByColumnName(headers, values,
                Attribute.Mandatory);
        String defaultValue = getValueByColumnName(headers, values,
                Attribute.DefaultValue);
        String showInEditViews = getValueByColumnName(headers, values,
                Attribute.ShowInEditViews);
        String section = getValueByColumnName(headers, values,
                Attribute.Section);
        String script = getValueByColumnName(headers, values,
                Attribute.DynamicScript);
        String unique = getValueByColumnName(headers, values,
                Attribute.Unique);
        String pattern = getValueByColumnName(headers, values,
                Attribute.Pattern);
        String patternType = getValueByColumnName(headers, values,
                Attribute.PatternType);
        String internalAssignment = getValueByColumnName(headers, values,
                Attribute.InternalAssignment);

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
                Attribute.Code);

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
