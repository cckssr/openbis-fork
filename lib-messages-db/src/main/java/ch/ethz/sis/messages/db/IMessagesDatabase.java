package ch.ethz.sis.messages.db;

import java.util.function.Supplier;

public interface IMessagesDatabase
{
    void begin();

    IMessagesDAO getMessagesDAO();

    ILastSeenMessagesDAO getLastSeenMessagesDAO();

    void rollback();

    void commit();

    default <T> T execute(Supplier<T> action)
    {
        try
        {
            begin();
            T result = action.get();
            commit();
            return result;
        } finally
        {
            rollback();
        }
    }
}
