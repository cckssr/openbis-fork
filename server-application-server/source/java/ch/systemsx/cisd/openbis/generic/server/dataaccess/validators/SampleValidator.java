package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Sample;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

final class SampleValidator implements IDataTypeValidator {
    @Override
    public Serializable validate(Serializable value) throws UserFailureException {
        assert value != null : "Unspecified value.";

        if (value.getClass().isArray()) {
            Serializable[] arrayValues = (Serializable[]) value;
            if (arrayValues.length == 0) {
                return null;
            }
        } else {
            String stringValue;
            if (value.getClass().equals(Sample.class)) {
                stringValue = ((Sample) value).getPermId();
            } else {
                stringValue = value.toString();
            }
            if (StringUtils.isBlank(stringValue)) {
                return null;
            }
            if (stringValue.startsWith("/")) {
                // Is well formed identifier?
            } else {
                // Is well formed permId?
            }
        }
        return value;
    }
}
