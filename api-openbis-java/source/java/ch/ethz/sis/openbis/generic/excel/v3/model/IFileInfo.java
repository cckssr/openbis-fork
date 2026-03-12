package ch.ethz.sis.openbis.generic.excel.v3.model;

import java.io.IOException;

public interface IFileInfo
{
    String objectIdentifier();

    String filePath();

    byte[] contents() throws IOException;

    String originalPath();

}
