package ch.ethz.sis.rocrateserver.openapi.v1.service.jobs.importjob.download;

import java.util.concurrent.*;

public class StupidExample
{
    public static void main(String[] args) throws ExecutionException, InterruptedException
    {

        ExecutorService service = Executors.newFixedThreadPool(6);
        CompletionService<String> completionService = new ExecutorCompletionService<>(service);
        completionService.submit(() -> {
            Thread.sleep(3000);
            return "Task 1";
        });
        completionService.submit(() -> {
            Thread.sleep(1000);
            return "Task 2";
        });
        completionService.submit(() -> {
            Thread.sleep(7000);
            return "Task 3";
        });

        Long timeStart = System.currentTimeMillis();
        for (int i = 0; i < 3; i++)
        {
            Future<String> future = completionService.take();
            System.out.println(future.get());
        }

        Long timeEnd = System.currentTimeMillis();

        System.out.println("time taken = " + (timeEnd - timeStart) / 1000 + "s");

        service.shutdown();
    }

}
