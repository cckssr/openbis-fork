package ch.ethz.sis.openbis.generic.excel.v3.from.utils;

import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NewExportFileReader
{
    public static OpenBisModel.FileInfo readFiles(ZipEntry zipEntry, ZipInputStream zipInputStream,
            ExcelReader.FileMode fileMode)
            throws IOException
    {
        String[] parts = zipEntry.getName().split("/");
        String fileName = parts[parts.length - 1];

        List<String> identifierParts = new ArrayList<>();
        for (int i = 1; i < parts.length - 3; i++)
        {
            identifierParts.add(parts[i]);

        }
        identifierParts.add(getObjectCode(parts[parts.length - 3]));

        String identifier = "/" + identifierParts.stream().collect(Collectors.joining("/"));
        byte[] content = fileMode != ExcelReader.FileMode.DUMMY ?
                zipInputStream.readAllBytes() :
                new byte[] { 0x00 };
        String fileIdentifier = makeFileIdentifierRoCrateCompatible(identifier + "/" + fileName);

        return new OpenBisModel.FileInfo(identifier, fileIdentifier, content, zipEntry.getName());

    }

    static String makeFileIdentifierRoCrateCompatible(String a)
    {
        if (a.startsWith("/"))
        {
            return "hierarchy/" + a.substring(1);
        }
        return "hierarchy/" + a;

    }

    record IdentifierWithFile(String identifier, String fileName, byte[] file)
    {
    }

    @VisibleForTesting
    public static String getObjectCode(String stuff)
    {
        if (!stuff.endsWith(")"))
        {
            return stuff;
        }
        int pos = stuff.length() - 2;
        while (stuff.charAt(pos) != '(')
        {

            pos--;
        }
        return stuff.substring(pos + 1, stuff.length() - 1);
    }

}
