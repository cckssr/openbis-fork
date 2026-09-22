package ch.openbis.rocrate.app.writer.mapping.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import org.junit.Test;
import org.testng.Assert;

import java.time.ZoneId;
import java.util.TimeZone;

public class ValueMapperTest
{

    @Test
    public void testDateDifferentTimeZone()
    {
        ValueMapper valueMapper = new ValueMapper(TimeZone.getTimeZone(ZoneId.of("Asia/Yerevan")));
        String s = valueMapper.mapValue("2022-08-29", DataType.DATE);
        Assert.assertEquals(s, "2022-08-28T20:00:00Z");

    }

    @Test
    public void testDateLocalTimeZone()
    {
        ValueMapper valueMapper = new ValueMapper(TimeZone.getTimeZone(ZoneId.of("Europe/Zurich")));
        String s = valueMapper.mapValue("2022-08-29", DataType.DATE);
        Assert.assertEquals(s, "2022-08-28T22:00:00Z");

    }

    @Test
    public void testDateTime()
    {
        ValueMapper valueMapper = new ValueMapper(TimeZone.getTimeZone(ZoneId.of("Europe/Zurich")));
        String s = valueMapper.mapValue("2020-12-09 15:43:20 +0100", DataType.TIMESTAMP);
        Assert.assertEquals(s, "2020-12-09T15:43:20+01:00");

    }

    @Test
    public void testDateTimeDifferentTimeZone()
    {
        ValueMapper valueMapper = new ValueMapper(TimeZone.getTimeZone(ZoneId.of("Asia/Yerevan")));
        String s = valueMapper.mapValue("2020-12-09 15:43:20 +0100", DataType.TIMESTAMP);
        Assert.assertEquals(s, "2020-12-09T15:43:20+01:00");

    }

    @Test
    public void otherDataTypesAreNotEdited()
    {
        ValueMapper valueMapper = new ValueMapper(TimeZone.getTimeZone(ZoneId.of("Asia/Yerevan")));

        String val = "a";
        for (DataType dataType : DataType.values())
        {
            if (dataType == DataType.DATE || dataType == DataType.TIMESTAMP)
            {
                continue;
            }
            Assert.assertEquals(valueMapper.mapValue(val, dataType), val);
        }
    }
}
