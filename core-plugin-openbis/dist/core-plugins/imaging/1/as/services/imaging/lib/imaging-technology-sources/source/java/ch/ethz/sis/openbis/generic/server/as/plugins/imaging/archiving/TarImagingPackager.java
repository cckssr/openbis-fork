package ch.ethz.sis.openbis.generic.server.as.plugins.imaging.archiving;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.Log4jSimpleLogger;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import ch.systemsx.cisd.common.filesystem.tar.Tar;
import ch.systemsx.cisd.common.io.MonitoredIOStreamCopier;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class TarImagingPackager extends AbstractImagingPackager
{
    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, TarImagingPackager.class);
    private final Tar tar;

    public TarImagingPackager(File tarFile, int bufferSize, Long maxQueueSizeInBytesOrNull) {
        super(tarFile);
        try
        {
            MonitoredIOStreamCopier
                    copier = new MonitoredIOStreamCopier(bufferSize, maxQueueSizeInBytesOrNull);
            copier.setLogger(new Log4jSimpleLogger(operationLog));
            tar = new Tar(tarFile, copier);
        } catch (FileNotFoundException e)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(e);
        }
    }

    @Override
    public void addEntry(String entryPath, long lastModified, long size, long checksum,
            InputStream in)
    {
        TarArchiveEntry entry = new TarArchiveEntry(entryPath.replace('\\', '/'));
        entry.setSize(size);
        try
        {
            tar.add(entry, in);
        } catch (IOException e)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(e);
        }
    }

    @Override
    public void addDirectoryEntry(String entryPath)
    {
        String path = entryPath.replace('\\', '/');
        if (path.endsWith("/") == false)
        {
            path += "/";
        }
        try
        {
            tar.add(path);
        } catch (IOException e)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(e);
        }
    }

    @Override
    public void close()
    {
        try
        {
            tar.close();
        } catch (IOException e)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(e);
        }
    }
}
