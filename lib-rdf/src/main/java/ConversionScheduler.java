import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class ConversionScheduler
{

    private static ConcurrentLinkedQueue<String> filesToDo = new ConcurrentLinkedQueue();

    public static Semaphore semaphore = new Semaphore(5);

    public static void main(String[] args) throws InterruptedException
    {
        File dir = new File(".");
        File[] filesList = dir.listFiles();
        for (File file : filesList)
        {
            if (file.getPath().endsWith(".ttl"))
                filesToDo.add(file.getPath());
        }
        System.out.println("Queue: " + filesToDo.stream().collect(Collectors.joining(",")));

        String commandTemplate =
                "java %3$s -i TTL -o ZIP -f %2$s -dangling -f %1$s -r out/%1$s.zip";

        System.out.println("Before sempaphores " + new Date());
        while (!filesToDo.isEmpty())
        {
            semaphore.acquire();
            System.out.println("Semaphore acquired " + new Date());
            String inFile = filesToDo.poll();
            Thread thread = new Thread(
                    new CommandExecutor(commandTemplate,
                            new String[] { inFile, args[0], args[1] }));
            thread.start();
        }

    }

    private static class CommandExecutor implements Runnable
    {

        String commandTemplate;

        String[] parameters;

        public CommandExecutor(String commandTemplate, String[] parameters)
        {
            this.commandTemplate = commandTemplate;
            this.parameters = parameters;
        }

        @Override
        public void run()
        {
            String formattedCommand = String.format(commandTemplate, (Object[]) parameters);
            System.out.println("Started file " + parameters[0] + " " + new Date());

            try
            {
                Runtime.getRuntime().exec(formattedCommand);
                System.out.println("Finished file " + parameters[0] + " " + new Date());

            } catch (IOException e)
            {
                e.printStackTrace();
                System.out.println("Failed file " + parameters[0] + " " + new Date());

                throw new RuntimeException(e);

            } finally
            {
                semaphore.release();
            }

        }
    }

}
