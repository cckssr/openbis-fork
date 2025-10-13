package ch.ethz.sis.messages.db;

public interface IMessagesDatabase
{
    void begin();

    IMessagesDAO getMessagesDAO();

    ILastSeenMessagesDAO getLastSeenMessagesDAO();

    void rollback();

    void commit();

}
