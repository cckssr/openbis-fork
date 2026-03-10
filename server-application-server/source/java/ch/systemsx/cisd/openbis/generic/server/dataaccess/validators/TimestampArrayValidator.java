package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ComplexPropertyValueUtils;
import ch.systemsx.cisd.openbis.generic.shared.util.SupportedDateTimePattern;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

public final class TimestampArrayValidator extends ArrayValidator {

    @Override
    public Serializable validate(final Serializable value) throws UserFailureException {
        super.validate(value);

        try {
            final Date[] dates = ComplexPropertyValueUtils.tryGetTimestampArray(value, getArrayType());
            return Arrays.stream(dates)
                    .map(date -> DateFormatUtils.format(date,
                            SupportedDateTimePattern.CANONICAL_DATE_PATTERN.getPattern()))
                    .toArray(String[]::new);
        } catch (final IllegalArgumentException e) {
            throw UserFailureException.fromTemplate(e,
                    "Array value '%s' is not valid. At least one element is not a valid timestamp value.", value);
        }
    }

}
