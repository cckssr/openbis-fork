package ch.ethz.sis.afs.manager;

import ch.ethz.sis.shared.startup.Configuration;

public interface TrashRootProvider
{

    String TRASH_FOLDER_NAME = ".trash";

    void init(Configuration configuration) throws Exception;

    String getTrashRoot(String source);

}
