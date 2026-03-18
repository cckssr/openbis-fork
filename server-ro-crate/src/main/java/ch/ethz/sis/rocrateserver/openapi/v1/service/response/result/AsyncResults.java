package ch.ethz.sis.rocrateserver.openapi.v1.service.response.result;

public class AsyncResults
{
    AsyncResult[] jobs;

    public AsyncResult[] getJobs()
    {
        return jobs;
    }

    public void setJobs(AsyncResult[] jobs)
    {
        this.jobs = jobs;
    }
}
