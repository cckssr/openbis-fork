package ch.openbis.rocrate.app.reader.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import org.junit.Assert;
import org.junit.Test;

public class DataTypeMatcherTest
{
    @Test
    public void onePartIdentifierTest()
    {
        Assert.assertFalse(DataTypeMatcher.matches("/SHARED_SAMPLE_NOT_ALLOWED", DataType.SAMPLE));
    }

    @Test
    public void twoPartIdentifierTest()
    {
        Assert.assertTrue(DataTypeMatcher.matches("/SPACE_CODE/SPACE_SAMPLE_CODE", DataType.SAMPLE));
    }

    @Test
    public void threePartIdentifierTest()
    {
        Assert.assertTrue(DataTypeMatcher.matches("/SPACE_CODE/PROJECT_CODE/SAMPLE_CODE", DataType.SAMPLE));
    }

    @Test
    public void threePartIdentifierWithContainerTest()
    {
        Assert.assertTrue(DataTypeMatcher.matches("/SPACE_CODE/PROJECT_CODE/CONTAINER_SAMPLE_CODE:CONTAINED_SAMPLE_CODE", DataType.SAMPLE));
    }

    @Test
    public void fourPartIdentiferTest()
    {
        Assert.assertFalse(DataTypeMatcher.matches("/SPACE_CODE/PROJECT_CODE/SAMPLE_CODE/DOESNT_EXIT", DataType.SAMPLE));
    }

}
