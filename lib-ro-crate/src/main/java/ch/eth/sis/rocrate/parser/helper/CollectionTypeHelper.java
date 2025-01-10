package ch.eth.sis.rocrate.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.ExperimentTypeImportHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CollectionTypeHelper extends BasicImportHelper
{

    Map<String, ExperimentType> accumulator = new HashMap();

    public CollectionTypeHelper(ImportModes mode,
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
        String code =
                getValueByColumnName(header, values, ExperimentTypeImportHelper.Attribute.Code);
        String description = getValueByColumnName(header, values,
                ExperimentTypeImportHelper.Attribute.Description);
        String validationScript = getValueByColumnName(header, values,
                ExperimentTypeImportHelper.Attribute.ValidationScript);
        String internal =
                getValueByColumnName(header, values, ExperimentTypeImportHelper.Attribute.Internal);

        ExperimentType creation = new ExperimentType();
        ExperimentTypeFetchOptions fetchOptions = new ExperimentTypeFetchOptions();
        fetchOptions.withPropertyAssignments();
        creation.setFetchOptions(fetchOptions);

        creation.setCode(code);
        creation.setDescription(description);
        creation.setManagedInternally(false);
        creation.setPropertyAssignments(new ArrayList<>());

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

    public Map<EntityTypePermId, IEntityType> getResult()
    {
        return accumulator.values().stream()
                .collect(Collectors.toMap(
                        x -> new EntityTypePermId(x.getCode(), EntityKind.EXPERIMENT), x -> x));
    }

}
