/*
 * Copyright ETH 2025 Zürich, Scientific IT Services
 *
 * Licensed under the the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.generic.shared.hibernate.type;

import org.hibernate.type.descriptor.java.DateJavaType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class DateArrayJavaType extends AbstractPgArrayJavaType<Date>
{
    public DateArrayJavaType()
    {
        super(DateJavaType.INSTANCE);
    }

    @Override
    protected Date convertElement(String value)
    {
        String val = value;

        // strip optional quotes
        if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) {
            val = val.substring(1, val.length() - 1);
        }

        // Postgres may use a space; normalize to ISO 'T'
        String s = val.replace(' ', 'T');

        // Build formatters that accept +02 and +02:00 (and optional fractional seconds)
        DateTimeFormatter base = new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .toFormatter();

        DateTimeFormatter withOffsetHH = new DateTimeFormatterBuilder()
                .append(base)
                .appendOffset("+HH", "Z")          // accepts +02 or Z
                .toFormatter();

        DateTimeFormatter withOffsetHHMM = new DateTimeFormatterBuilder()
                .append(base)
                .appendOffset("+HH:MM", "Z")       // accepts +02:00 or Z
                .toFormatter();

        // Try parsing as offset date-time first
        Instant instant;
        try {
            instant = OffsetDateTime.parse(s, withOffsetHHMM).toInstant();
        } catch (DateTimeParseException e1) {
            try {
                instant = OffsetDateTime.parse(s, withOffsetHH).toInstant();
            } catch (DateTimeParseException e2) {
                // No offset: treat as local timestamp (choose your policy!)
                LocalDateTime ldt = LocalDateTime.parse(s, base);
                instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
            }
        }

        return Date.from(instant);
    }

    @Override
    protected Date[] createArray(int length)
    {
        return new Date[length];
    }
}
