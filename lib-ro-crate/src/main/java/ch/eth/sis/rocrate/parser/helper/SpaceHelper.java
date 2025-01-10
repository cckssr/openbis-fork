package ch.eth.sis.rocrate.parser.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.BasicImportHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.SpaceImportHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpaceHelper extends BasicImportHelper
{
    Map<String, Space> accumulator = new HashMap<>();

    public SpaceHelper(ImportModes mode,
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
        String code = getValueByColumnName(header, values, SpaceImportHelper.Attribute.Code);
        String description =
                getValueByColumnName(header, values, SpaceImportHelper.Attribute.Description);

        Space space = new Space();
        space.setCode(code);
        space.setDescription(description);
        accumulator.put(code, space);

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

    Space getSpace(String key)
    {
        return accumulator.get(key);
    }

    public Map<String, Space> getResult()
    {
        return new HashMap<>(accumulator);
    }

}
