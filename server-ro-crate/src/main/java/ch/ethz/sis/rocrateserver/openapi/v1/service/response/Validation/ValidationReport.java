package ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class ValidationReport
{
    boolean isValid;

    List<ValidationError> validationErrors;

    public ValidationReport()
    {
    }

    public ValidationReport(boolean isValid, List<ValidationError> validationErrors)
    {
        this.isValid = isValid;
        this.validationErrors = validationErrors;
    }

    public List<ValidationError> getValidationErrors()
    {
        return validationErrors;
    }

    public void setValidationErrors(
            List<ValidationError> validationErrors)
    {
        this.validationErrors = validationErrors;
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
