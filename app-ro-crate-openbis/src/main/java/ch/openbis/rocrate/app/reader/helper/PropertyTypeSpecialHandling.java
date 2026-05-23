package ch.openbis.rocrate.app.reader.helper;

import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.openbis.rocrate.app.Constants;

public class PropertyTypeSpecialHandling
{

    public static boolean requiresFileHandling(IPropertyType propertyType)
    {
        if (propertyType.getId().equals("schema:hasPart") && propertyType.getDomain().stream()
                .anyMatch(x -> x.getId().equals(
                        Constants.GRAPH_ID_OBJECT)))
        {
            return true;
        }

        propertyType.getDomain().stream().anyMatch(x -> x.getId().equals("File"));

        return false;
    }

}
