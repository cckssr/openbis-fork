package ch.ethz.sis.afs.manager;

import java.nio.file.Path;
import java.nio.file.Paths;

import ch.ethz.sis.afs.startup.AtomicFileSystemParameter;
import ch.ethz.sis.shared.startup.Configuration;

public class TrashRootInOwnerProvider implements TrashRootProvider
{
    private String storageRoot;

    @Override public void init(final Configuration configuration) throws Exception
    {
        storageRoot = configuration.getStringProperty(AtomicFileSystemParameter.storageRoot);
    }

    @Override public String getTrashRoot(final String source)
    {
        Path storageRootPath = Paths.get(storageRoot);
        Path relativePath = storageRootPath.relativize(Paths.get(source));
        String owner = relativePath.getName(0).toString();
        return Path.of(storageRoot, owner, TRASH_FOLDER_NAME).toString();
    }
}
