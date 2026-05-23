package ch.openbis.rocrate.app.reader.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;

import java.net.MalformedURLException;
import java.net.URL;

public class SampleCodeHelper
{
    public static String createSampleCode(SampleType sampleType, String identifier)
    {
        boolean isUrl = false;
        try
        {
            URL url = new URL(identifier);
            isUrl = true;

        } catch (MalformedURLException ignored)
        {
        }
        if (isUrl)
        {
            String[] parts = identifier.split("/");
            return OpenBisModel.makeOpenBisCodeCompliant(
                    sampleType.getCode() + "_" + parts[parts.length - 1]);
        }
        if (DataTypeMatcher.matches(identifier, DataType.SAMPLE))
        {
            String[] parts = identifier.split("/");
            return parts[parts.length - 1];
        }

        return OpenBisModel.makeOpenBisCodeCompliant(identifier);

    }
}
