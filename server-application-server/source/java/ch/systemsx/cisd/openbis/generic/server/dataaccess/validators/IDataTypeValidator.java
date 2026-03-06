package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.io.Serializable;

interface IDataTypeValidator {
    /**
     * Validates given <var>value</var> according to this data type.
     *
     * @return the validated value. Note that it can differ from the given one.
     * @throws UserFailureException if given <var>value</var> is not valid.
     */
    public Serializable validate(final Serializable value) throws UserFailureException;
}
