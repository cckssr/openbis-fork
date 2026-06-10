package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.impl.ConcurrentHashSet;

import java.util.List;
import java.util.Map;
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

    ConcurrentHashSet<JobKey> downloaded = new ConcurrentHashSet<>();

    public record CompletedAndFailedJobs(List<ExportJob> downloaded,
                                         List<ExportJob> notDownloaded,
                                         List<ExportJob> failedExports,
                                         List<ImportJob> failedAndCompletedImportJobs)
    {
    }

    ThreadPoolExecutor executor =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(N_THREADS);

    // consider having different states for operations inside openBIS and the RO-Crate microservice

    public enum Status
    {

        @JsonProperty("RUNINNG")
        RUNNING("RUNNING"),

        @JsonProperty("COMPLETED")
        COMPLETED("COMPLETED"),
        @JsonProperty("FAILED")
        FAILED("FAILED"),
        @JsonProperty("SCHEDULED")
        SCHEDULED("SCHEDULED");

        private final String name;

        Status(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return this.name;
        }

    }

    public String register(IAsyncJob job)
    {
        UUID uuid = job.getJobId();
        Future<?> submit = executor.submit(job);
        JobKey jobKey = new JobKey(job.getUserId(), uuid.toString());
        jobs.put(jobKey, job);
        results.put(jobKey, submit);
        return uuid.toString();
    }

    //Maybe provide Status and Result together to avoid repeated call
    public AsyncStatus poll(String username, String jobId)
    {
        JobKey jobKey = new JobKey(username, jobId);
        Future future = results.get(jobKey);
        IAsyncJob asyncJob = jobs.get(jobKey);

        if (future == null)
        {
            return null;
        }

        if (!future.isDone())
        {
            return new AsyncStatus(Status.RUNNING, jobId, username, asyncJob);
        }
        if (asyncJob.getException() != null)
        {
            return new AsyncStatus(Status.FAILED, jobId, username, asyncJob);
        }

        return new AsyncStatus(Status.COMPLETED, jobId, username, asyncJob);
    }

    public List<AsyncStatus> pollAll(String username)
    {
        return jobs.entrySet().stream().filter(x -> x.getKey().userName.equals(username))
                .map(x -> this.poll(username, x.getKey().jobId))
                .toList();

    }

    public static class AsyncStatus
    {
        Status status;

        String jobId;

        String username;

        IAsyncJob job;

        public AsyncStatus(Status status, String jobId, String username, IAsyncJob job)
        {
            this.status = status;
            this.jobId = jobId;
            this.username = username;
            this.job = job;
        }

        public Status getStatus()
        {

            Status status1 = job.getStatus();
            this.status = status1;
            return status1;
        }

        public IAsyncJob getJob()
        {
            return job;
        }

        public String getJobId()
        {
            return jobId;
        }
    }

    public void setDownloaded(JobKey jobKey)
    {
        this.downloaded.add(jobKey);
    }

    public CompletedAndFailedJobs getCompletedExportJobs()
    {
        List<Map.Entry<JobKey, IAsyncJob>> completedExports =
                jobs.entrySet().stream()
                        .filter(x -> x.getValue() instanceof ExportJob)
                        .filter(x -> x.getValue().getStatus().equals(Status.COMPLETED))
                        .toList();

        List<ExportJob> failedExports =
                jobs.entrySet().stream()
                        .filter(x -> x.getValue() instanceof ExportJob)
                        .filter(x -> x.getValue().getStatus().equals(Status.FAILED))
                        .map(x -> x.getValue())
                        .map(ExportJob.class::cast)
                        .toList();

        List<ExportJob> downloadedExports =
                completedExports.stream().filter(x -> downloaded.contains(x.getKey()))
                        .map(x -> x.getValue())
                        .map(ExportJob.class::cast)
                        .toList();

        List<ExportJob> waitingExports =
                completedExports.stream().filter(x -> !downloaded.contains(x.getKey()))
                        .map(x -> x.getValue())
                        .map(ExportJob.class::cast)
                        .toList();

        List<ImportJob> failedAndCompletedImportJobs =
                jobs.entrySet().stream()
                        .filter(x -> x.getValue() instanceof ImportJob)
                        .filter(x -> x.getValue().getStatus().equals(Status.FAILED) || x.getValue()
                                .getStatus().equals(Status.COMPLETED))
                        .map(x -> x.getValue())
                        .map(ImportJob.class::cast)
                        .toList();

        return new CompletedAndFailedJobs(downloadedExports, waitingExports, failedExports,
                failedAndCompletedImportJobs);
    }

    public void remove(String userId, UUID jobId)
    {
        jobs.remove(new JobKey(userId, jobId.toString()));
        results.remove(new JobKey(userId, jobId.toString()));

    }




}
