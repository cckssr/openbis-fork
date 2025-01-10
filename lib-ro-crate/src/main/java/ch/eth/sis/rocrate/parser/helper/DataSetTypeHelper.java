package ch.eth.sis.rocrate.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.DatasetTypeImportHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataSetTypeHelper extends BasicImportHelper
{
    String currentCode = null;

    private Map<String, DataSetType> accumulator = new HashMap<>();

    public DataSetTypeHelper(ImportModes mode,
            ImportOptions options)
    {
        super(mode, options);
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
        String code = getValueByColumnName(header, values, DatasetTypeImportHelper.Attribute.Code);
        this.currentCode = code;
        String description =
                getValueByColumnName(header, values, DatasetTypeImportHelper.Attribute.Description);
        String validationScript = getValueByColumnName(header, values,
                DatasetTypeImportHelper.Attribute.ValidationScript);
        String internal =
                getValueByColumnName(header, values, DatasetTypeImportHelper.Attribute.Internal);

        DataSetType type = new DataSetType();
        DataSetTypeFetchOptions fetchOptions = new DataSetTypeFetchOptions();
        fetchOptions.withPropertyAssignments();
        type.setFetchOptions(fetchOptions);
        type.setPropertyAssignments(new ArrayList<>());
        type.setCode(code);
        type.setDescription(description);

        type.setValidationPlugin(null);
        type.setManagedInternally(false);
        accumulator.put(code, type);

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

    public Map<EntityTypePermId, IEntityType> getResult()
    {
        return accumulator.values().stream()
                .collect(Collectors.toMap(
                        x -> new EntityTypePermId(x.getCode(), EntityKind.DATA_SET), x -> x));
    }

    public String getCurrentCode()
    {
        return currentCode;
    }
}
