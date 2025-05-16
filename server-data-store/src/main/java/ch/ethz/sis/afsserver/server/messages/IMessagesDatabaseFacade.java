package ch.ethz.sis.afsserver.server.messages;

import ch.ethz.sis.messages.db.Message;

public interface IMessagesDatabaseFacade
{

    void create(Message message);

}
