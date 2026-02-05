package ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation;

import ch.ethz.sis.rocrateserver.openapi.v1.service.response.result.IResultPayload;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ValidationResult implements IResultPayload
{
    private Map<String, List<PropertyProblem>> entitiesToMissingProperties;

    private Map<String, List<PropertyProblem>> entititesToUndefinedProperties;

    private Map<String, List<PropertyProblem>> wrongDataTypes;

    private List<String> entities;

    public ValidationResult()
    {
    }

    public ValidationResult(Map<String, List<PropertyProblem>> entitiesToMissingProperties,
            Map<String, List<PropertyProblem>> entititesToUndefinedProperties,
            Map<String, List<PropertyProblem>> wrongDataTypes, List<String> entities)
    {
        this.entitiesToMissingProperties = entitiesToMissingProperties;
        this.entititesToUndefinedProperties = entititesToUndefinedProperties;
        this.wrongDataTypes = wrongDataTypes;
        this.entities = entities;
    }

    @JsonProperty("isValid")
    public boolean isOkay()
    {
        return entitiesToMissingProperties.isEmpty() && entititesToUndefinedProperties.isEmpty()
                && wrongDataTypes.isEmpty();
    }

    public Map<String, List<PropertyProblem>> getEntitiesToMissingProperties()
    {
        return entitiesToMissingProperties;
    }

    public Map<String, List<PropertyProblem>> getEntititesToUndefinedProperties()
    {
        return entititesToUndefinedProperties;
    }

    public Map<String, List<PropertyProblem>> getWrongDataTypes()
    {
        return wrongDataTypes;
    }

    public List<String> getEntities()
    {
        return entities;
    }

}