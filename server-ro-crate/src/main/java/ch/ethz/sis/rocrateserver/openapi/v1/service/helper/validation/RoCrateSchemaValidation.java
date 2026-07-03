package ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.MissingDataError;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.reader.helper.DataTypeMatcher;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This checks a schema for _internal_ consistency. This is meant as a quick feedback mechanism for
 * problems that do not need further context, e.g. knowledge of the database.
 */
public class RoCrateSchemaValidation
{

    private RoCrateSchemaValidation()
    {
    }

    public static ValidationResult validate(RdfToModel.ConversionResult conversionResult)

    {
        OpenBisModel openBisModel = conversionResult.openBisModel();

        Map<String, List<PropertyProblem>> entitiesToMissingProperties = new LinkedHashMap<>();
        Map<String, List<PropertyProblem>> entititesToUndefinedProperties = new LinkedHashMap<>();
        Map<String, List<PropertyProblem>> wrongDataTypes = new LinkedHashMap<>();
        Map<String, List<MissingDataError>> missingFiles = new LinkedHashMap<>();

        for (RdfToModel.MissingReferenceValue missingReference : conversionResult.missingReferenceValues())
        {
            String key = missingReference.sample().getCode();
            List<PropertyProblem> res =
                    wrongDataTypes.getOrDefault(key,
                            new ArrayList<>());

            PropertyProblem referenceNotFoundInCrate =
                    new PropertyProblem(missingReference.oldIdentifier(), missingReference.key(),
                            "Reference not found in crate");
            res.add(referenceNotFoundInCrate);
            wrongDataTypes.put(key, res);
        }


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
                            matchPropertyAssignment(codeToPropertyAssignment, property.getKey(),
                                    property.getValue());

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

        for (Map.Entry<String, List<RdfToModel.FileProblem>> keyVal : conversionResult.identfiersOfMissingFiles()
                .entrySet())
        {
            List<MissingDataError> newVals = keyVal.getValue().stream()
                    .map(x -> new MissingDataError(x.type(), x.path()))
                    .toList();

            List<MissingDataError> mapVals =
                    missingFiles.getOrDefault(keyVal.getKey(), new ArrayList<>());
            mapVals.addAll(newVals);
            missingFiles.put(keyVal.getKey(), mapVals);

        }


        return new ValidationResult(entitiesToMissingProperties, entititesToUndefinedProperties,
                wrongDataTypes,
                new ArrayList<>(openBisModel.getExternalToOpenBisIdentifiers().keySet()),
                missingFiles);

    }

    private static boolean validateDataType(DataType dataType, Serializable value)
    {
        return DataTypeMatcher.matches(value, dataType);
    }

    private static PropertyAssignment matchPropertyAssignment(
            Map<String, PropertyAssignment> codeToPropertyAssignment, String key,
            Serializable value)
    {
        return DataTypeMatcher.matchPropertyAssignment(codeToPropertyAssignment, key, value);

    }

}
