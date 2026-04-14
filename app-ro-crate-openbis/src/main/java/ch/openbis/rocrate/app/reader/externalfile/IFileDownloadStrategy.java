package ch.openbis.rocrate.app.reader.externalfile;

import java.io.IOException;
import java.nio.file.Path;

public interface IFileDownloadStrategy
{

    Path getPath(String fileName) throws IOException;

}
