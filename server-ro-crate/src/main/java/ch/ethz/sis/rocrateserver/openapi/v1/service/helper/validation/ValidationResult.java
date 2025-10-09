package ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation;

import java.util.List;
import java.util.Map;

public class ValidationResult
{
    private final Map<String, List<PropertyProblem>> entitiesToMissingProperties;

    private final Map<String, List<PropertyProblem>> entititesToUndefinedProperties;

    private final Map<String, List<PropertyProblem>> wrongDataTypes;

    private final List<String> foundIdentifiers;

    public ValidationResult(Map<String, List<PropertyProblem>> entitiesToMissingProperties,
            Map<String, List<PropertyProblem>> entititesToUndefinedProperties,
            Map<String, List<PropertyProblem>> wrongDataTypes, List<String> foundIdentifiers)
    {
        this.entitiesToMissingProperties = entitiesToMissingProperties;
        this.entititesToUndefinedProperties = entititesToUndefinedProperties;
        this.wrongDataTypes = wrongDataTypes;
        this.foundIdentifiers = foundIdentifiers;
    }

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

    public List<String> getFoundIdentifiers()
    {
        return foundIdentifiers;
    }
}