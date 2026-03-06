package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ComplexPropertyValueUtils;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;

import java.io.Serializable;

public final class IntegerArrayValidator extends ArrayValidator {

    @Override
    public Serializable validate(final Serializable value) throws UserFailureException {
        final String[] valueArray = (String[]) super.validate(value);

        try {
            ComplexPropertyValueUtils.tryGetIntegerArray(value, getArrayType());
        } catch (final NumberFormatException e) {
            throw UserFailureException.fromTemplate(e,
                    "Array value '%s' is not valid. At least one element is not a valid integer value.", value);
        }

        return valueArray;
    }

}
