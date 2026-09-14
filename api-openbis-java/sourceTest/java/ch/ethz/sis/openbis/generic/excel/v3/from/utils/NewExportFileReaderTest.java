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

    @Test
    public void testSpaceSampleIdentifier()
    {
        String result = NewExportFileReader.getIdentifierFromParts(new String[] { "hierarchy", "SPACE", "SAMPLE", "data", "test.zip" });
        assertEquals(result, "/SPACE/SAMPLE");
    }

    @Test
    public void testProjectSampleIdentifier()
    {
        String result = NewExportFileReader.getIdentifierFromParts(new String[] { "hierarchy", "SPACE", "PROJECT", "SAMPLE", "data", "test.zip" });
        assertEquals(result, "/SPACE/PROJECT/SAMPLE");
    }

    @Test
    public void testExperimentSampleIdentifier()
    {
        String result = NewExportFileReader.getIdentifierFromParts(new String[] { "hierarchy", "SPACE", "PROJECT", "EXP", "SAMPLE", "data", "test.zip" });
        assertEquals(result, "/SPACE/PROJECT/SAMPLE");
    }

}