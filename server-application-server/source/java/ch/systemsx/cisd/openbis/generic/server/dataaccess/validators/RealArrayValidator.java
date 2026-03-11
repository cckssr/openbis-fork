package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ComplexPropertyValueUtils;

import java.io.Serializable;

public final class RealArrayValidator extends ArrayValidator {

    @Override
    public Serializable validate(final Serializable value) throws UserFailureException {
        final Serializable[] valueArray = (Serializable[]) super.validate(value);

        try {
            ComplexPropertyValueUtils.tryGetRealArray(value, getArrayType());
        } catch (final NumberFormatException e) {
            throw UserFailureException.fromTemplate(e,
                    "Array value '%s' is not valid. At least one element is not a valid real value.", value);
        }

        return valueArray;
    }

}
