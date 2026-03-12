package ch.openbis.rocrate.app.reader.externalfile.saving;

import ch.openbis.rocrate.app.reader.externalfile.IFileDownloadStrategy;

import java.nio.file.Path;

public class TempDirSaving implements IFileDownloadStrategy
{
    @Override
    public Path getPath(String fileName)
    {

        String property = System.getProperty("java.io.tmpdir");
        return Path.of(property, fileName);
    }
}
