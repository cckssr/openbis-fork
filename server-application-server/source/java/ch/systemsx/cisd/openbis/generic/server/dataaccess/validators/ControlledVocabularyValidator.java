package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.systemsx.cisd.common.collection.CollectionUtils;
import ch.systemsx.cisd.common.collection.IToStringConverter;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.VocabularyTerm;
import ch.systemsx.cisd.openbis.generic.shared.dto.PropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyTermPE;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;

final class ControlledVocabularyValidator implements IDataTypeValidator {

    private VocabularyPE vocabulary;

    private PropertyTypePE propertyTypePE;

    final void setVocabulary(final PropertyTypePE propertyTypePE) {
        this.propertyTypePE = propertyTypePE;
        this.vocabulary = propertyTypePE.getVocabulary();
    }

    //
    // IDataTypeValidator
    //

    @Override
    public Serializable validate(final Serializable value) throws UserFailureException {
        assert value != null : "Unspecified value.";
        assert vocabulary != null : "Unspecified vocabulary.";

        if (value.getClass().isArray()) {
            Serializable[] arrayValues = (Serializable[]) value;
            if (arrayValues.length == 0) {
                return null;
            }
            return Arrays.stream(arrayValues)
                    .map(x -> validateSingleValue((String) x))
                    .toArray(Serializable[]::new);
        } else {
            String val = value.toString();
            if (value.getClass().equals(VocabularyTerm.class)) {
                val = ((VocabularyTerm) value).getCode();
            }

            if (StringUtils.isBlank(val)) {
                return null;
            }
            return validateSingleValue(val);
        }
    }

    private Serializable validateSingleValue(final String value) {
        String upperCaseValue = value.toUpperCase();
        if (hasTerm(upperCaseValue)) {
            return upperCaseValue;
        }
        throw UserFailureException.fromTemplate("Vocabulary value '%s' of property '%s' is not valid. "
                        + "It must exist in '%s' controlled vocabulary %s", upperCaseValue, propertyTypePE.getCode(),
                vocabulary.getCode(), getVocabularyDetails());
    }

    private boolean hasTerm(String value) {
        vocabulary.tryGetVocabularyTerm(value);
        VocabularyTermPE termOrNull = vocabulary.tryGetVocabularyTerm(value);
        return termOrNull != null;
    }

    /**
     * @return Details about vocabulary dependent on {@link VocabularyPE#isChosenFromList()} value:
     * <ul>
     * <li>for <var>true</var> - returns a list of first few vocabulary terms from it.
     * <li>for <var>false</var> - returns a vocabulary description
     * </ul>
     */
    private final String getVocabularyDetails() {
        if (vocabulary.isChosenFromList()) {
            return CollectionUtils.abbreviate(vocabulary.getTerms(), 10,
                    new IToStringConverter<VocabularyTermPE>() {

                        //
                        // IToStringConverter
                        //

                        @Override
                        public final String toString(final VocabularyTermPE term) {
                            return term.getCode();
                        }
                    });
        } else {
            String descriptionOrNull = vocabulary.getDescription();
            return descriptionOrNull == null ? "" : " - " + descriptionOrNull;
        }
    }

}
