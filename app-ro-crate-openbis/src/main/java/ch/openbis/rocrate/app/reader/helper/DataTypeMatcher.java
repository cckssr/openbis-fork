package ch.openbis.rocrate.app.reader.helper;

import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class DataTypeMatcher
{

    private static List<DataType> orderedTypes =
            List.of(DataType.INTEGER, DataType.REAL, DataType.BOOLEAN, DataType.DATE,
                    DataType.HYPERLINK, DataType.SAMPLE, DataType.SAMPLE, DataType.VARCHAR);

    public static DataType findDataType(Serializable value, Set<DataType> possibleTypes,
            Map<String, IMetadataEntry> entities)
    {
        return orderedTypes.stream().filter(possibleTypes::contains)
                .filter(x -> matches(value, x, entities))
                .findFirst().orElseThrow();

    }

    public static boolean matches(Serializable value, DataType dataType,
            Map<String, IMetadataEntry> entities)
    {
        if (dataType == DataType.SAMPLE)
        {
            return entities.containsKey(value.toString());
        }
        return matches(value, dataType);

    }

    public static boolean matches(Serializable value, DataType dataType)
    {

        switch (dataType)
        {
            case HYPERLINK:
                try
                {
                    URL url = new URL(value.toString()).toURI().toURL();
                    return true;

                } catch (MalformedURLException | URISyntaxException e)
                {
                    return false;
                }

            case INTEGER:
                try
                {
                    Long.parseLong(value.toString());
                    return true;

                } catch (NumberFormatException e)
                {
                    return false;
                }
            case TIMESTAMP:
                try
                {
                    SimpleDateFormat ISO8601DATEFORMAT =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);
                    ISO8601DATEFORMAT.parse(value.toString());
                    return true;
                } catch (ParseException e)
                {
                    return false;
                }

            case DATE:
                try
                {

                    DateTimeFormatter.ISO_INSTANT.parse(value.toString());
                    return true;
                } catch (RuntimeException e)
                {
                    return false;
                }
            case BOOLEAN:
                try
                {

                    return Stream.of("true", "false", "0", "1")
                            .anyMatch(x -> x.equals(value.toString()));
                } catch (RuntimeException e)
                {
                    return false;
                }

            case SAMPLE:
                try
                {
                    String[] identifiers;
                    if (value instanceof String)
                    {
                        identifiers = new String[] { (String) value };
                    } else
                    {

                        identifiers = (String[]) value;
                    }
                    for (String identifier : identifiers)
                    {

                        String[] parts = identifier.toString().split("/");
                        if (parts.length != 4)
                        {
                            return false;
                        }
                        String space = parts[1];
                        String project = parts[2];
                        String code = parts[3];
                    }
                    return true;
                } catch (RuntimeException e)
                {
                    return false;
                }


            case VARCHAR:
                return true;
            case REAL:
                try
                {
                    Double.parseDouble(value.toString());
                    return true;

                } catch (NumberFormatException e)
                {
                    return false;
                }



        }
        return false;

    }

    public static String suffixTypeCode(String code, DataType dataType)
    {
        return code + dataType.name().toUpperCase();
    }

    public static List<DataType> getDataTypesInPriorityOrder()
    {
        return List.of(DataType.HYPERLINK, DataType.REAL, DataType.INTEGER, DataType.SAMPLE,
                DataType.DATE, DataType.TIMESTAMP, DataType.VARCHAR);

    }

    public static PropertyAssignment matchPropertyAssignment(
            Map<String, PropertyAssignment> codeToPropertyAssignment, String key,
            Serializable value)
    {
        PropertyAssignment assignment = codeToPropertyAssignment.get(key);
        if (assignment != null)
        {
            return assignment;
        }
        for (DataType dataType : DataTypeMatcher.getDataTypesInPriorityOrder())
        {
            String newCode = DataTypeMatcher.suffixTypeCode(key, dataType);
            if (DataTypeMatcher.matches(value, dataType) && codeToPropertyAssignment.containsKey(
                    newCode))
            {
                return codeToPropertyAssignment.get(newCode);
            }
        }
        return null;
    }

}
