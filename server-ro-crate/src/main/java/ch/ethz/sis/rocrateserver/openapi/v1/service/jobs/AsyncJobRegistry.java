package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class AsyncJobRegistry
{

    public static final int N_THREADS = 2;

    ConcurrentHashMap<String, IAsyncJob> jobs = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, Future> results = new ConcurrentHashMap<>();

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
        jobs.put(uuid.toString(), job);
        results.put(uuid.toString(), submit);
        return uuid.toString();
    }

    //Maybe provide Status and Result together to avoid repeated call
    public Status poll(String jobId)
    {
        if (!results.get(jobId).isDone())
        {
            return Status.PENDING;
        }
        return Status.DONE;
    }

}
