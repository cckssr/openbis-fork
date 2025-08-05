package ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This checks a schema for _internal_ consistency. This is meant as a quick feedback mechanism for
 * problems that do not need further context, e.g. knowledge of the database.
 */
public class RoCrateSchemaValidation
{

    private RoCrateSchemaValidation()
    {
    }

    public static ValidationResult validate(OpenBisModel openBisModel)
    {
        Map<String, List<PropertyProblem>> entitiesToMissingProperties = new LinkedHashMap<>();
        Map<String, List<PropertyProblem>> entititesToUndefinedProperties = new LinkedHashMap<>();
        Map<String, List<PropertyProblem>> wrongDataTypes = new LinkedHashMap<>();

        for (AbstractEntityPropertyHolder entity : openBisModel.getEntities().values())
        {
            if (entity instanceof Sample)
            {
                Sample sample = (Sample) entity;
                IEntityType entityType =
                        openBisModel.getEntityTypes().get(sample.getType().getPermId());
                SampleType sampleType = (SampleType) entityType;
                List<PropertyProblem> unknownProperties = new ArrayList<>();
                List<PropertyProblem> missingMandatoryProperties = new ArrayList<>();
                List<PropertyProblem> currentWrongDataTypes = new ArrayList<>();
                Map<String, PropertyAssignment> codeToPropertyAssignment =
                        entityType.getPropertyAssignments().stream()
                                .distinct()
                                .collect(Collectors.toMap(x -> x.getPropertyType().getCode(),
                                        x -> x));

                for (Map.Entry<String, Serializable> property : sample.getProperties().entrySet())
                {
                    PropertyAssignment propertyAssignment =
                            codeToPropertyAssignment.get(property.getKey());

                    if (propertyAssignment == null && !property.getKey().equalsIgnoreCase("name"))
                    {
                        unknownProperties.add(
                                new PropertyProblem(sample.getCode(), property.getKey(),
                                        "Property not in schema"));
                    }

                }
                for (PropertyAssignment propertyAssignment : entityType.getPropertyAssignments())
                {
                    Optional<Serializable>
                            maybePropertyValue = Optional.ofNullable(sample.getProperties()
                            .get(propertyAssignment.getPropertyType().getCode()));
                    if (propertyAssignment == null)
                    {
                        continue;
                    }

                    if (propertyAssignment.isMandatory() && maybePropertyValue.isEmpty())
                    {
                        missingMandatoryProperties.add(
                                new PropertyProblem(sampleType.getCode(),
                                        propertyAssignment.getPropertyType().getCode(),
                                        "Mandatory property missing"));
                        continue;
                    }
                    if (maybePropertyValue.isPresent() && !validateDataType(
                            propertyAssignment.getPropertyType().getDataType(),
                            maybePropertyValue.get()))
                    {
                        currentWrongDataTypes.add(new PropertyProblem(sample.getCode(),
                                propertyAssignment.getPropertyType().getPermId().getPermId(),
                                "Wrong data type"));

                    }

                }

                if (!unknownProperties.isEmpty())
                {
                    entititesToUndefinedProperties.put(sampleType.getCode(), unknownProperties);
                }
                if (!missingMandatoryProperties.isEmpty())
                {
                    entitiesToMissingProperties.put(sampleType.getCode(),
                            missingMandatoryProperties);
                }
                if (!currentWrongDataTypes.isEmpty())
                {
                    wrongDataTypes.put(sampleType.getPermId().getPermId(), currentWrongDataTypes);
                }

            }

        }

        return new ValidationResult(entitiesToMissingProperties, entititesToUndefinedProperties,
                wrongDataTypes);

    }

    private static boolean validateDataType(DataType dataType, Serializable value)
    {
        switch (dataType)
        {
            case INTEGER:
                try
                {
                    Long.parseLong(value.toString());
                    return true;

                } catch (NumberFormatException e)
                {
                    return false;
                }
            case DATE:
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
            case SAMPLE:
                String[] identifiers = value.toString().split(",");
                for (String identifier : identifiers)
                {

                    String[] parts = identifier.toString().split("/");
                    if (parts.length != 3)
                    {
                        return false;
                    }
                    String space = parts[0];
                    String project = parts[1];
                    String code = parts[2];
                }
                return true;
            case BOOLEAN:
                try
                {

                    return Stream.of("true", "false", "0", "1")
                            .anyMatch(x -> x.equals(value.toString()));
                } catch (RuntimeException e)
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
        }
        return false;

    }

}
