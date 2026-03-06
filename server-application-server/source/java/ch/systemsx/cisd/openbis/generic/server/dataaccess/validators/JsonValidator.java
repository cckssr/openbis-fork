package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

final class JsonValidator implements IDataTypeValidator {
    @Override
    public Serializable validate(Serializable value) throws UserFailureException {
        assert value != null : "Unspecified value.";

        if (value.getClass().isArray()) {
            Serializable[] arrayValues = (Serializable[]) value;
            if (arrayValues.length == 0) {
                return null;
            }
        } else {
            String val = (String) value;
            if (StringUtils.isBlank(val)) {
                return null;
            }
        }
        return value;
    }
}
