package ch.ethz.sis.openbis.generic.excel.v3.model;

import java.io.IOException;
import java.io.InputStream;

public interface IFileInfo
{
    String objectIdentifier();

    String filePath();

    InputStream getInputStream() throws IOException;

    String originalPath();

}
