package ch.ethz.sis.rocrateserver.openapi.v1.service.response.result;

import ch.ethz.sis.rocrateserver.openapi.v1.service.response.ImportResponse;
import ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation.ValidationReport;
import jakarta.annotation.Nullable;

import java.util.List;

public class AsyncResult
{

    String jobId;

    String status;

    List<String> errors;

    ValidationReport validationResult;

    ImportResponse importResponse;

    String downloadUrl;


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

    public AsyncResult(String status, List<String> errors, @Nullable IResultPayload result,
            String jobId)
    {
        this.status = status;
        this.errors = errors;
        this.jobId = jobId;
    }

    public ValidationReport getValidationResult()
    {
        return validationResult;
    }

    public void setValidationResult(
            ValidationReport validationResult)
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

    public void setDownloadUrl(String downloadUrl)
    {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public String getJobId()
    {
        return jobId;
    }

    public void setJobId(String jobId)
    {
        this.jobId = jobId;
    }
}
