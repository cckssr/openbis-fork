package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.helper;

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

    public static class ValidationResult
    {
        private final Map<String, List<String>> entitiesToMissingProperties;

        private final Map<String, List<String>> entititesToUndefinedProperties;

        private final Map<String, List<String>> wrongDataTypes;

        public ValidationResult(Map<String, List<String>> entitiesToMissingProperties,
                Map<String, List<String>> entititesToUndefinedProperties,
                Map<String, List<String>> wrongDataTypes)
        {
            this.entitiesToMissingProperties = entitiesToMissingProperties;
            this.entititesToUndefinedProperties = entititesToUndefinedProperties;
            this.wrongDataTypes = wrongDataTypes;
        }

        public boolean isOkay()
        {
            return entitiesToMissingProperties.isEmpty() && entititesToUndefinedProperties.isEmpty()
                    && wrongDataTypes.isEmpty();
        }

        public Map<String, List<String>> getEntitiesToMissingProperties()
        {
            return entitiesToMissingProperties;
        }

        public Map<String, List<String>> getEntititesToUndefinedProperties()
        {
            return entititesToUndefinedProperties;
        }

        public Map<String, List<String>> getWrongDataTypes()
        {
            return wrongDataTypes;
        }
    }

    public static ValidationResult validate(OpenBisModel openBisModel)
    {
        Map<String, List<String>> entitiesToMissingProperties = new LinkedHashMap<>();
        Map<String, List<String>> entititesToUndefinedProperties = new LinkedHashMap<>();
        Map<String, List<String>> wrongDataTypes = new LinkedHashMap<>();

        for (AbstractEntityPropertyHolder entity : openBisModel.getEntities().values())
        {
            if (entity instanceof Sample)
            {
                Sample sample = (Sample) entity;
                IEntityType entityType =
                        openBisModel.getEntityTypes().get(sample.getType().getPermId());
                SampleType sampleType = (SampleType) entityType;
                List<String> unknownProperties = new ArrayList<>();
                List<String> missingMandatoryProperties = new ArrayList<>();
                List<String> currentWrongDataTypes = new ArrayList<>();
                Map<String, PropertyAssignment> codeToPropertyAssignment =
                        entityType.getPropertyAssignments().stream()
                                .distinct()
                                .collect(Collectors.toMap(x -> x.getPropertyType().getCode(),
                                        x -> x));

                for (Map.Entry<String, Serializable> property : sample.getProperties().entrySet())
                {
                    PropertyAssignment propertyAssignment =
                            codeToPropertyAssignment.get(property.getKey());

                    if (propertyAssignment == null)
                    {
                        unknownProperties.add(property.getKey());
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
                                propertyAssignment.getPropertyType().getCode());
                        continue;
                    }
                    if (maybePropertyValue.isPresent() && !validateDataType(
                            propertyAssignment.getPropertyType().getDataType(),
                            maybePropertyValue.get()))
                    {
                        currentWrongDataTypes.add(
                                propertyAssignment.getPropertyType().getPermId().getPermId());

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
                String[] parts = value.toString().split("/");
                if (parts.length != 4)
                {
                    return false;
                }
                String space = parts[1];
                String project = parts[2];
                String code = parts[3];
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
