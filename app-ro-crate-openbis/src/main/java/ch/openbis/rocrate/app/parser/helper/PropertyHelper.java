package ch.openbis.rocrate.app.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.openbis.rocrate.app.parser.IAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.openbis.rocrate.app.parser.searcher.PropertyTypeSearcher.SAMPLE_DATA_TYPE_MANDATORY_TYPE;

public class PropertyHelper extends BasicImportHelper
{

    public enum Attribute implements IAttribute
    {
        Version("Version", false, false),
        Code("Code", true, true),
        Mandatory("Mandatory", false, false),
        DefaultValue("Default Value",
                false, false),  // Ignored, only used by PropertyAssignmentImportHelper
        ShowInEditViews("Show in edit views", false, false),
        Section("Section", false, false),
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
        Internal("Internal", false, false),
        InternalAssignment("Internal Assignment", false, false);

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

    Map<String, PropertyType> accumulator = new HashMap<>();

    VocabularyHelper vocabularyHelper;

    public PropertyHelper(VocabularyHelper vocabularyHelper)
    {
        this.vocabularyHelper = vocabularyHelper;
    }

    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        return false;
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String propertyLabel = getValueByColumnName(header, values,
                Attribute.PropertyLabel);
        String description = getValueByColumnName(header, values,
                Attribute.Description);
        String dataTypeXLS =
                getValueByColumnName(header, values, Attribute.DataType);

        DataType dataType = null;
        String dataTypeObjectType = null;
        if (dataTypeXLS.contains(SAMPLE_DATA_TYPE_MANDATORY_TYPE))
        {
            dataType = DataType.valueOf(dataTypeXLS.split(SAMPLE_DATA_TYPE_MANDATORY_TYPE)[0]);
            dataTypeObjectType = dataTypeXLS.split(SAMPLE_DATA_TYPE_MANDATORY_TYPE)[1];
        } else
        {
            dataType = DataType.valueOf(dataTypeXLS);
        }

        String vocabularyCode = getValueByColumnName(header, values,
                Attribute.VocabularyCode);
        String metadata =
                getValueByColumnName(header, values, Attribute.Metadata);
        String multiValued = getValueByColumnName(header, values,
                Attribute.MultiValued);
        String internal =
                getValueByColumnName(header, values, Attribute.Internal);

        PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
        fetchOptions.withVocabulary();
        PropertyType creation = new PropertyType();
        creation.setCode(code);
        creation.setLabel(propertyLabel);
        creation.setDescription(description);
        creation.setDataType(dataType);
        creation.setFetchOptions(fetchOptions);

        /*
        if (dataType == DataType.SAMPLE && dataTypeObjectType != null)
        {
            creation.setSampleTypeId(new EntityTypePermId(dataTypeObjectType, EntityKind.SAMPLE));
        }

         */

        creation.setManagedInternally(false);
        creation.setMultiValue(false);

        if (dataType == DataType.CONTROLLEDVOCABULARY && vocabularyCode != null && !vocabularyCode.isEmpty())
        {
            Vocabulary vocab = vocabularyHelper.getResult().get(vocabularyCode);

            creation.setVocabulary(vocab);
        } else if (dataType != DataType.CONTROLLEDVOCABULARY && vocabularyCode != null && !vocabularyCode.isEmpty())
        {
            throw new UserFailureException(
                    "Ambiguous Property type declaration, the dataType is not CONTROLLEDVOCABULARY but it has a controlled vocabulary set.");
        }
        if (metadata != null && !metadata.trim().isEmpty())
        {
            creation.setMetaData(Map.of("val", metadata));
        }
        accumulator.put(code, creation);

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
}
