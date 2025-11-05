package ch.ethz.sis.openbis.afsserver.server.messages;

import java.util.List;

import ch.ethz.sis.messages.db.Message;

public interface IMessagesDatabaseFacade
{

    void create(Message message);

    List<Message> listByTypesNotConsumed(List<String> messageTypes);

}
