package ch.ethz.sis.openbis.generic.excel.v3.from.utils;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class NewExportFileReaderTest
{

    @Test
    public void testBasicExample()
    {
        String objectCode = NewExportFileReader.getObjectCode("New Title (ENTRY1)");
        assertEquals(objectCode, "ENTRY1");
    }

}