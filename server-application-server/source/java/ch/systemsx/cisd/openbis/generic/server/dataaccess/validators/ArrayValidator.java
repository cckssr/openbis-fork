package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

public abstract class ArrayValidator implements IDataTypeValidator {

    private ObjectMapper mapper = new ObjectMapper();

    private DataTypeCode arrayType;

    protected DataTypeCode getArrayType() {
        return arrayType;
    }

    public void setArrayType(DataTypeCode arrayType) {
        this.arrayType = arrayType;
    }

    @Override
    public Serializable validate(Serializable value) throws UserFailureException {
        assert value != null : "Unspecified value.";

        if (value.getClass().isArray()) {
            return value;
        } else if (value instanceof String) {
            try {
                return mapper.readValue((String) value, String[].class);
            } catch (Exception e) {
                throw UserFailureException.fromTemplate(e,
                        "Array value '%s' is not valid. Provided value is a String which could not be parsed to an array.", value);
            }
        } else {
            throw UserFailureException.fromTemplate("Array value '%s' is not valid. "
                    + "Provided value is not an array", value);
        }
    }

}
