package ch.ethz.sis.rocrateserver.openapi.v1.service.params;

import jakarta.ws.rs.HeaderParam;

public class DownloadParams
{
    @HeaderParam("api-key")
    private String apiKey;

    @HeaderParam("jobid")
    private String jobId;

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
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
