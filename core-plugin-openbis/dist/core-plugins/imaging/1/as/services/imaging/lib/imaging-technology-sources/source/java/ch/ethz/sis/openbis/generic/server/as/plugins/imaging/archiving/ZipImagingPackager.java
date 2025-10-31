package ch.ethz.sis.openbis.generic.server.as.plugins.imaging.archiving;

import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;
import de.schlichtherle.util.zip.ZipEntry;
import de.schlichtherle.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ZipImagingPackager extends AbstractImagingPackager
{
    private final boolean compress;

    private ZipOutputStream zipOutputStream;

    public ZipImagingPackager(File archiveFile, boolean compress) {
        super(archiveFile);
        this.compress = compress;
    }

    @Override
    public void addEntry(String entryPath, long lastModified, long size, long checksum,
            InputStream in)
    {
        ZipOutputStream zos = getZipOutputStream();
        try
        {
            ZipEntry zipEntry = new ZipEntry(entryPath.replace('\\', '/'));
            zipEntry.setTime(lastModified);
            zipEntry.setMethod(compress ? ZipEntry.DEFLATED : ZipEntry.STORED);
            if (compress == false)
            {
                zipEntry.setSize(size);
                zipEntry.setCompressedSize(size);
                zipEntry.setCrc(0xffffffffL & checksum);
            }
            zos.putNextEntry(zipEntry);
            int len;
            byte[] buffer = new byte[1024];
            while ((len = in.read(buffer)) > 0)
            {
                zos.write(buffer, 0, len);
            }
        } catch (IOException ex)
        {
            throw new RuntimeException("Error while adding entry " + entryPath, ex);
        } finally
        {
            IOUtils.closeQuietly(in);
            try
            {
                zos.closeEntry();
            } catch (IOException ex)
            {
                throw CheckedExceptionTunnel.wrapIfNecessary(ex);
            }
        }
    }

    @Override
    public void addDirectoryEntry(String entryPath)
    {
        ZipOutputStream zos = getZipOutputStream();
        try
        {
            String path = entryPath.replace('\\', '/');
            if (path.endsWith("/") == false)
            {
                path += "/";
            }
            ZipEntry zipEntry = new ZipEntry(path);
            zos.putNextEntry(zipEntry);
        } catch (IOException ex)
        {
            throw new RuntimeException("Error while adding entry " + entryPath, ex);
        } finally
        {
            try
            {
                zos.closeEntry();
            } catch (IOException ex)
            {
                throw CheckedExceptionTunnel.wrapIfNecessary(ex);
            }
        }
    }

    @Override
    public void close()
    {
        if (zipOutputStream != null)
        {
            try
            {
                zipOutputStream.close();
            } catch (IOException ex)
            {
                throw CheckedExceptionTunnel.wrapIfNecessary(ex);
            }
        }
    }

    private ZipOutputStream getZipOutputStream()
    {
        if (zipOutputStream == null)
        {
            FileOutputStream outputStream = null;
            try
            {
                outputStream = new FileOutputStream(archiveFile);
                zipOutputStream = new ZipOutputStream(outputStream);
            } catch (Exception ex)
            {
                IOUtils.closeQuietly(outputStream);
                throw CheckedExceptionTunnel.wrapIfNecessary(ex);
            }
        }
        return zipOutputStream;
    }
}
