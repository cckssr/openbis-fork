package ch.ethz.sis.openbis.generic.excel.v3.from.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.excel.v3.from.utils.IAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpaceHelper extends BasicImportHelper
{
    Map<SpacePermId, Space> accumulator = new HashMap<>();

    public enum Attribute implements IAttribute
    {
        Code("Code", true, true),
        Description("Description", true, false);

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

    public SpaceHelper()
    {
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
        String description =
                getValueByColumnName(header, values, Attribute.Description);

        Space space = new Space();
        space.setCode(code);
        space.setPermId(new SpacePermId(code));
        space.setDescription(description);
        accumulator.put(space.getPermId(), space);

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

    Space getSpace(SpacePermId key)
    {
        return accumulator.get(key);
    }

    public Map<SpacePermId, Space> getResult()
    {
        return new HashMap<>(accumulator);
    }

}
