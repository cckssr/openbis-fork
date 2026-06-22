package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ComplexPropertyValueUtils;
import ch.systemsx.cisd.openbis.generic.shared.util.SupportedDateTimePattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

public final class TimestampArrayValidator extends ArrayValidator {

    @Override
    public Serializable validate(final Serializable value) throws UserFailureException {
        super.validate(value);

        try {
            // A special check if the value is an array in a string form, e.g. "[\"2026-01-30 10:18:30\"]".
            final Serializable finalValue = value instanceof String
                    ? CommonServiceProvider.getObjectMapper().readValue((String) value, String[].class) : value;

            final Date[] dates = ComplexPropertyValueUtils.tryGetTimestampArray(finalValue, getArrayType());
            if (dates == null) {
                return null;
            } else {
                return Arrays.stream(dates)
                        .map(date -> date == null ? null : DateFormatUtils.format(date,
                                SupportedDateTimePattern.CANONICAL_DATE_PATTERN.getPattern()))
                        .toArray(String[]::new);
            }
        } catch (final IllegalArgumentException e) {
            throw UserFailureException.fromTemplate(e, e.getMessage(), value);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
