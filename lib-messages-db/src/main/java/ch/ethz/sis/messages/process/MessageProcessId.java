package ch.ethz.sis.messages.process;

import java.util.UUID;

public class MessageProcessId
{

    private static final ThreadLocal<String> processIds = new ThreadLocal<>();

    public static void setCurrent(String processId)
    {
        synchronized (MessageProcessId.class)
        {
            processIds.set(processId);
        }
    }

    public static String getCurrentOrGenerateNew()
    {
        synchronized (MessageProcessId.class)
        {
            String processId = processIds.get();
            return processId != null ? processId : UUID.randomUUID().toString();
        }
    }

}
