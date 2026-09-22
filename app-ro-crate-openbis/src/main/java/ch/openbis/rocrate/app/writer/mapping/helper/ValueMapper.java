package ch.openbis.rocrate.app.writer.mapping.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;

import static ch.openbis.rocrate.app.writer.mapping.Mapper.CANONICAL_OPENBIS_DATE_FORMAT_PATTERN;

public class ValueMapper
{
    public final TimeZone timeZone;

    public ValueMapper(TimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    public String mapValue(String val, DataType dataType)
    {
        if (dataType == DataType.DATE)
        {
            LocalDate date = LocalDate.parse(val);

            ZonedDateTime zoned = date.atStartOfDay(timeZone.toZoneId());

            return DateTimeFormatter.ISO_INSTANT.format(zoned.toInstant());

        }

        if (dataType == DataType.TIMESTAMP)
        {

            DateTimeFormatter dateTimeFormatter =
                    DateTimeFormatter.ofPattern(CANONICAL_OPENBIS_DATE_FORMAT_PATTERN);
            TemporalAccessor parsed = dateTimeFormatter.parse(val);
            Instant i = Instant.from(parsed);
            Date d = Date.from(i);
            String format = DateTimeFormatter.ISO_DATE_TIME.format(parsed);
            return format;

        }

        return val;

    }

}
