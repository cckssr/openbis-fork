package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class AsyncJobRegistry
{

    public static final int N_THREADS = 2;

    private class JobKey
    {
        String userName;

        String jobId;

        public JobKey(String userName, String jobId)
        {
            this.userName = userName;
            this.jobId = jobId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            JobKey jobKey = (JobKey) o;
            return Objects.equals(userName, jobKey.userName) && Objects.equals(jobId,
                    jobKey.jobId);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(userName, jobId);
        }
    }

    ConcurrentHashMap<JobKey, IAsyncJob> jobs = new ConcurrentHashMap<>();

    ConcurrentHashMap<JobKey, Future> results = new ConcurrentHashMap<>();

    ThreadPoolExecutor executor =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(N_THREADS);

    // consider having different states for operations inside openBIS and the RO-Crate microservice

    public enum Status
    {
        PENDING, // Check openapi yaml whether that's the word we use there, RUNNING would be better
        DONE,
        FAILED,
        SCHEDULED;
    }

    public String register(IAsyncJob job)
    {
        UUID uuid = UUID.randomUUID();
        Future<?> submit = executor.submit(job);
        JobKey jobKey = new JobKey(job.getUserId(), uuid.toString());
        jobs.put(jobKey, job);
        results.put(jobKey, submit);
        return uuid.toString();
    }

    //Maybe provide Status and Result together to avoid repeated call
    public Status poll(String username, String jobId)
    {
        JobKey jobKey = new JobKey(username, jobId);
        Future future = results.get(jobKey);
        if (!future.isDone())
        {
            return Status.PENDING;
        }
        return Status.DONE;
    }

}
