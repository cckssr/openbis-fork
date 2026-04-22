package ch.ethz.sis.afs.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.sis.shared.io.IOUtils;
import lombok.Data;

public class TransactionFileSystemIO
{

    private final String storageRoot;

    private final TrashRootProvider trashRootProvider;

    private final Map<String, PathState> pathStateCache;

    public TransactionFileSystemIO(String storageRoot, TrashRootProvider trashRootProvider)
    {
        this.storageRoot = storageRoot;
        this.trashRootProvider = trashRootProvider;
        this.pathStateCache = new HashMap<>();
    }

    public String getStorageRoot()
    {
        return storageRoot;
    }

    public String getTrashRoot(final String source)
    {
        return trashRootProvider.getTrashRoot(source);
    }

    public void setCreated(final String source, final boolean directory) throws Exception
    {
        PathState pathState = getCachedPathState(source);
        List<String> parentSubPaths = PathLockFinder.getParentSubPaths(source);

        for (String parentSubPath : parentSubPaths)
        {
            PathState parentSubPathState = getCachedPathState(parentSubPath);
            parentSubPathState.setExists(true);
            parentSubPathState.setDeleted(false);
            if (parentSubPathState == pathState)
            {
                parentSubPathState.setWritten(true);
                parentSubPathState.setDirectory(directory);
            } else
            {
                parentSubPathState.setDirectory(true);
            }
        }
    }

    public void setWritten(final String source) throws Exception
    {
        PathState pathState = getCachedPathState(source);
        List<String> parentSubPaths = PathLockFinder.getParentSubPaths(source);

        for (String parentSubPath : parentSubPaths)
        {
            PathState parentSubPathState = getCachedPathState(parentSubPath);
            parentSubPathState.setExists(true);
            parentSubPathState.setDeleted(false);
            if (parentSubPathState == pathState)
            {
                parentSubPathState.setWritten(true);
                parentSubPathState.setDirectory(false);
            } else
            {
                parentSubPathState.setDirectory(true);
            }
        }
    }

    public void setMoved(final String source)
    {
        for (Map.Entry<String, PathState> pathStateEntry : pathStateCache.entrySet())
        {
            if (pathStateEntry.getKey().startsWith(source))
            {
                pathStateEntry.getValue().setExists(false);
                pathStateEntry.getValue().setMoved(true);
            }
        }
    }

    public void setCopied(final String source)
    {
        for (Map.Entry<String, PathState> pathStateEntry : pathStateCache.entrySet())
        {
            if (pathStateEntry.getKey().startsWith(source))
            {
                pathStateEntry.getValue().setExists(true);
                pathStateEntry.getValue().setCopied(true);
            }
        }
    }

    public void setDeleted(final String source)
    {
        for (Map.Entry<String, PathState> pathStateEntry : pathStateCache.entrySet())
        {
            if (pathStateEntry.getKey().startsWith(source))
            {
                pathStateEntry.getValue().setExists(false);
                pathStateEntry.getValue().setDeleted(true);
            }
        }
    }

    public boolean exists(final String source) throws Exception
    {
        PathState pathState = getCachedPathState(source);
        return pathState.isExists();
    }

    public boolean isDirectory(final String source) throws Exception
    {
        PathState pathState = getCachedPathState(source);
        return pathState.isExists() && pathState.isDirectory();
    }

    public boolean isWritten(final String source) throws Exception
    {
        PathState pathState = getCachedPathState(source);
        return pathState.isWritten();
    }

    public boolean isMoved(final String source) throws Exception
    {
        PathState pathState = getCachedPathState(source);
        return pathState.isMoved();
    }

    public boolean isCopied(final String source) throws Exception
    {
        PathState pathState = getCachedPathState(source);
        return pathState.isCopied();
    }

    public boolean isDeleted(final String source) throws Exception
    {
        PathState pathState = getCachedPathState(source);
        return pathState.isDeleted();
    }

    private PathState getCachedPathState(String source) throws Exception
    {
        PathState pathState = pathStateCache.get(source);

        if (pathState != null)
        {
            return pathState;
        }

        pathState = new PathState();
        pathState.setExists(IOUtils.exists(source));
        pathState.setDirectory(IOUtils.exists(source) ? IOUtils.getFile(source).getDirectory() : false);

        pathStateCache.put(source, pathState);

        return pathState;
    }

    @Data
    private static class PathState
    {
        boolean exists;

        boolean isDirectory;

        boolean written;

        boolean moved;

        boolean copied;

        boolean deleted;
    }

}
