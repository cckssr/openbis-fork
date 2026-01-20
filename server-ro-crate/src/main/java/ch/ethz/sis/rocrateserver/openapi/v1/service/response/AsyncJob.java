package ch.ethz.sis.rocrateserver.openapi.v1.service.response;

public class AsyncJob
{
    String jobId;

    public AsyncJob(String jobId)
    {
        this.jobId = jobId;
    }

    public AsyncJob()
    {
    }

    public String getJobId()
    {
        return jobId;
    }
}
