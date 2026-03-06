package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.ComplexPropertyValueUtils;

import java.io.Serializable;

public final class StringArrayValidator extends ArrayValidator {

    @Override
    public Serializable validate(final Serializable value) throws UserFailureException {
        // ComplexPropertyValueUtils.tryGetStringArray() is called for redundancy,
        // to make sure that the value can be converted to an array.
        return ComplexPropertyValueUtils.tryGetStringArray(super.validate(value), getArrayType());
    }

}
