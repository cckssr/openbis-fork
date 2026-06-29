package ch.openbis.rocrate.app.reader.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testng.Assert;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class SampleCodeHelperTest
{
    private final String id;

    private final SampleType sampleType;

    private final String expectedCode;

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        SampleType personType = new SampleType();
        personType.setCode("PERSON");

        SampleType messageDatasetType = new SampleType();
        personType.setCode("MESSAGE_DATASET");

        return Arrays.asList(new Object[][] {
                { "person://andreas.meier@ethz.ch", personType, "person___andreas_meier_ethz_ch" },
                { "./6a0ee5d68eb4935f7f530164/", messageDatasetType,
                        "__6a0ee5d68eb4935f7f530164_" },

        });
    }

    public SampleCodeHelperTest(String id, SampleType sampleType, String expectedCode)
    {
        this.id = id;
        this.sampleType = sampleType;
        this.expectedCode = expectedCode;
    }

    @Test
    public void testSciLogIdentifiers()
    {
        String sampleCode = SampleCodeHelper.createSampleCode(sampleType, id);
        Assert.assertEquals(sampleCode, expectedCode);
    }

}
