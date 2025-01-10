package ch.eth.sis.rocrate.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.handler.JSONHandler;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.PropertyTypeImportHelper;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.ethz.sis.openbis.generic.server.xls.importer.utils.PropertyTypeSearcher.SAMPLE_DATA_TYPE_MANDATORY_TYPE;

public class PropertyHelper extends BasicImportHelper
{
    Map<String, PropertyType> accumulator = new HashMap<>();

    VocabularyHelper vocabularyHelper;

    public PropertyHelper(ImportModes mode,
            ImportOptions options, VocabularyHelper vocabularyHelper)
    {
        super(mode, options);
        this.vocabularyHelper = vocabularyHelper;
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
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(header, values, PropertyTypeImportHelper.Attribute.Code);
        String propertyLabel = getValueByColumnName(header, values,
                PropertyTypeImportHelper.Attribute.PropertyLabel);
        String description = getValueByColumnName(header, values,
                PropertyTypeImportHelper.Attribute.Description);
        String dataTypeXLS =
                getValueByColumnName(header, values, PropertyTypeImportHelper.Attribute.DataType);

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
                PropertyTypeImportHelper.Attribute.VocabularyCode);
        String metadata =
                getValueByColumnName(header, values, PropertyTypeImportHelper.Attribute.Metadata);
        String multiValued = getValueByColumnName(header, values,
                PropertyTypeImportHelper.Attribute.MultiValued);
        String internal =
                getValueByColumnName(header, values, PropertyTypeImportHelper.Attribute.Internal);

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
            creation.setMetaData(JSONHandler.parseMetaData(metadata));
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
