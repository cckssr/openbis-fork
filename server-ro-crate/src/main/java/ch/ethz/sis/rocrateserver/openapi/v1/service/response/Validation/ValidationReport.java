package ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class ValidationReport
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
}
