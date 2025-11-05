package ch.ethz.sis.openbis.generic.server.as.plugins.imaging.archiving;

import java.io.File;
import java.io.InputStream;

public abstract class AbstractImagingPackager
{
    protected final File archiveFile;

    public AbstractImagingPackager(File archiveFile) {
        this.archiveFile = archiveFile;
    }


    public abstract void addEntry(String entryPath, long lastModified, long size, long checksum, InputStream in);

    public abstract void addDirectoryEntry(String entryPath);


    /**
     * Closes the package.
     */
    public abstract void close();
}
