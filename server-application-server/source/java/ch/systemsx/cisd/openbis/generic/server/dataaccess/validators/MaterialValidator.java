package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.Material;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.MaterialIdentifier;
import ch.systemsx.cisd.openbis.generic.shared.dto.MaterialTypePE;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

final class MaterialValidator implements IDataTypeValidator {

    private MaterialTypePE materialTypeOrNull;

    public void setMaterialType(MaterialTypePE materialType) {
        this.materialTypeOrNull = materialType;
    }

    //
    // IDataTypeValidator
    //

    @Override
    public final Serializable validate(final Serializable val) throws UserFailureException {
        assert val != null : "Unspecified value.";
        String value;
        if (val.getClass().equals(Material.class)) {
            value = ((Material) val).getIdentifier();
        } else {
            value = val.toString();
        }

        if (StringUtils.isBlank(value)) {
            return null;
        }
        final MaterialIdentifier identifierOrNull =
                MaterialIdentifier.tryParseIdentifier(value);
        if (identifierOrNull == null) {
            if (materialTypeOrNull == null) {
                throw UserFailureException
                        .fromTemplate(
                                "Material specification '%s' has improper format. "
                                        + "Expected format is '<CODE> (<TYPE>)'. Type has to be specified because any type of material can be assigned.",
                                value);
            } else {
                return value;
            }
        }
        if (materialTypeOrNull != null
                && identifierOrNull.getTypeCode()
                .equalsIgnoreCase(materialTypeOrNull.getCode()) == false) {
            throw UserFailureException.fromTemplate(
                    "Material '%s' is of wrong type. Expected: '%s'.", value,
                    materialTypeOrNull.getCode());
        }
        return value;
    }
}
