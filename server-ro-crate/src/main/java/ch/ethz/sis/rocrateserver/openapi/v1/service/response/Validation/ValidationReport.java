package ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation;

import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.result.IResultPayload;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationReport implements IResultPayload
{
    boolean isValid;

    List<ValidationError> errors;

    List<String> entities;

    public ValidationReport()
    {
    }

    public ValidationReport(boolean isValid, List<ValidationError> errors, List<String> entities)
    {
        this.isValid = isValid;
        this.errors = errors;
        this.entities = entities;
    }

    public List<ValidationError> getErrors()
    {
        return errors;
    }

    public void setErrors(
            List<ValidationError> errors)
    {
        this.errors = errors;
    }

    public List<String> getEntities()
    {
        return entities;
    }

    @JsonProperty("isValid")
    public boolean isValid()
    {
        return isValid;
    }

    public void setValid(boolean valid)
    {
        isValid = valid;
    }

    public static String serialize(ValidationReport validationReport) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(validationReport);

    }

    public static ValidationReport from(ValidationResult validationResult)
    {

        List<ValidationError> validationErrors = new ArrayList<>();
        validationErrors.addAll(
                validationResult.getEntititesToUndefinedProperties().values().stream().flatMap(
                                Collection::stream)
                        .map(x -> new PropertyError(x.getNode(), x.getProperty(), x.getMessage()))
                        .collect(Collectors.toList()));
        validationErrors.addAll(validationResult.getWrongDataTypes().values().stream().flatMap(
                        Collection::stream)
                .map(x -> new PropertyError(x.getNode(), x.getProperty(), x.getMessage()))
                .collect(Collectors.toList()));
        validationErrors.addAll(
                validationResult.getIdentififersWithMissingFiles().values().stream().flatMap(
                        Collection::stream).map(x -> new MissingDataError(x.type, x.path)).collect(
                        Collectors.toList()));

        return new ValidationReport(validationResult.isOkay(), validationErrors,
                validationResult.getEntities());
    }

}
