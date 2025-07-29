package ch.ethz.sis.rocrateserver.openapi.v1.service.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ValidationReport
{
    boolean isValid;

    public ValidationReport()
    {
    }

    public ValidationReport(boolean isValid)
    {

        this.isValid = isValid;
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
