package ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation;

import ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.MissingDataError;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.result.IResultPayload;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ValidationResult implements IResultPayload
{
    private Map<String, List<PropertyProblem>> entitiesToMissingProperties;

    private Map<String, List<PropertyProblem>> entitiesToUndefinedProperties;

    private Map<String, List<PropertyProblem>> wrongDataTypes;

    private List<String> entities;

    private Map<String, List<MissingDataError>> identifiersWithMissingFiles;

    public ValidationResult()
    {
    }

    public ValidationResult(Map<String, List<PropertyProblem>> entitiesToMissingProperties,
            Map<String, List<PropertyProblem>> entitiesToUndefinedProperties,
            Map<String, List<PropertyProblem>> wrongDataTypes, List<String> entities,
            Map<String, List<MissingDataError>> identifiersWithMissingFiles)
    {
        this.entitiesToMissingProperties = entitiesToMissingProperties;
        this.entitiesToUndefinedProperties = entitiesToUndefinedProperties;
        this.wrongDataTypes = wrongDataTypes;
        this.entities = entities;
        this.identifiersWithMissingFiles = identifiersWithMissingFiles;
    }

    @JsonProperty("isValid")
    public boolean isOkay()
    {
        return entitiesToMissingProperties.isEmpty() && entitiesToUndefinedProperties.isEmpty()
                && wrongDataTypes.isEmpty() && identifiersWithMissingFiles.values().stream()
                .noneMatch(x -> !x.isEmpty());
    }

    public Map<String, List<PropertyProblem>> getEntitiesToMissingProperties()
    {
        return entitiesToMissingProperties;
    }

    public Map<String, List<PropertyProblem>> getEntitiesToUndefinedProperties()
    {
        return entitiesToUndefinedProperties;
    }

    public Map<String, List<PropertyProblem>> getWrongDataTypes()
    {
        return wrongDataTypes;
    }

    public List<String> getEntities()
    {
        return entities;
    }

    public Map<String, List<MissingDataError>> getIdentifiersWithMissingFiles()
    {
        return identifiersWithMissingFiles;
    }

    public void setIdentifiersWithMissingFiles(
            Map<String, List<MissingDataError>> identifiersWithMissingFiles)
    {
        this.identifiersWithMissingFiles = identifiersWithMissingFiles;
    }

    @Override
    public String toString()
    {
        if(isOkay()) {
            return "ValidationResult:[]";
        }
        return String.format("ValidationResult:[entities=%s, identifiersWithMissingFiles=%s, wrongDataTypes=%s, entitiesToMissingProperties=%s, entitiesToUndefinedProperties=%s]",
                entities, identifiersWithMissingFiles,  wrongDataTypes, entitiesToMissingProperties, entitiesToUndefinedProperties);
    }
}