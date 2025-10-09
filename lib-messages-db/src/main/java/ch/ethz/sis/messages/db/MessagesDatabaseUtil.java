package ch.ethz.sis.messages.db;

import java.util.function.Supplier;

public class MessagesDatabaseUtil
{

    public static <T> T execute(IMessagesDatabase database, Supplier<T> action)
    {
        try
        {
            database.begin();
            T result = action.get();
            database.commit();
            return result;
        } catch (Exception e)
        {
            database.rollback();
            throw e;
        }
    }

}
