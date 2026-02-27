package ch.ethz.sis.rocrateserver.openapi.v1.service.response.result;

import java.util.List;

public class StatusResponse
{
    List<AsyncResult> jobs;

    public StatusResponse()
    {
    }

    public StatusResponse(List<AsyncResult> jobs)
    {
        this.jobs = jobs;
    }

    public List<AsyncResult> getJobs()
    {
        return jobs;
    }

    public void setJobs(
            List<AsyncResult> jobs)
    {
        this.jobs = jobs;
    }
}
