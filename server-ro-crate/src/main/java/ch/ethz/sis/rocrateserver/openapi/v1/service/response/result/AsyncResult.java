package ch.ethz.sis.rocrateserver.openapi.v1.service.response.result;

import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ImportResponse;
import jakarta.annotation.Nullable;

import java.util.List;

public class AsyncResult
{
    String status;

    List<String> errors;

    ValidationResult validationResult;

    ImportResponse importResponse;


    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public List<String> getErrors()
    {
        return errors;
    }

    public void setErrors(List<String> errors)
    {
        this.errors = errors;
    }

    public AsyncResult()
    {
    }

    public AsyncResult(String status, List<String> errors, @Nullable IResultPayload result)
    {
        this.status = status;
        this.errors = errors;
    }

    public ValidationResult getValidationResult()
    {
        return validationResult;
    }

    public void setValidationResult(
            ValidationResult validationResult)
    {
        this.validationResult = validationResult;
    }

    public ImportResponse getImportResponse()
    {
        return importResponse;
    }

    public void setImportResponse(
            ImportResponse importResponse)
    {
        this.importResponse = importResponse;
    }
}
