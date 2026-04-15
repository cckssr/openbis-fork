package ch.openbis.rocrate.app.reader.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import org.junit.Assert;
import org.junit.Test;

public class DataTypeMatcherTest
{
    @Test
    public void twoPartIdentifierTest()
    {
        Assert.assertTrue(DataTypeMatcher.matches("MATERIALS/ENTRY1", DataType.SAMPLE));

    }

    @Test
    public void threePartIdentifierTest()
    {
        Assert.assertTrue(DataTypeMatcher.matches("MATERIALS/ENTRY1/A", DataType.SAMPLE));

    }

    @Test
    public void fourPartIdentifierTest()
    {
        Assert.assertTrue(DataTypeMatcher.matches("MATERIALS/ENTRY1/A/A", DataType.SAMPLE));

    }

    @Test
    public void fivePartIdentiferTest()
    {
        Assert.assertFalse(DataTypeMatcher.matches("MATERIALS/ENTRY1/A/A/A", DataType.SAMPLE));

    }

}
