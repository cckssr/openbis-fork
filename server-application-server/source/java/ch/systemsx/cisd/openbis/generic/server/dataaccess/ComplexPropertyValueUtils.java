package ch.systemsx.cisd.openbis.generic.server.dataaccess;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;
import ch.systemsx.cisd.openbis.generic.shared.dto.PropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.VocabularyTermPE;
import ch.systemsx.cisd.openbis.generic.shared.util.SupportedDateTimePattern;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class ComplexPropertyValueUtils {

    private ComplexPropertyValueUtils() {
        throw new IllegalStateException("Cannot instantiate a utils class.");
    }

    public static VocabularyTermPE tryGetVocabularyTerm(final Serializable value,
                                                        final PropertyTypePE propertyType) {
        if (propertyType.getType().getCode() != DataTypeCode.CONTROLLEDVOCABULARY) {
            return null; // this is not a property of CONTROLLED VOCABULARY type
        }

        final VocabularyPE vocabulary = propertyType.getVocabulary();
        if (vocabulary == null) {
            return null;
        }
        final VocabularyTermPE term = vocabulary.tryGetVocabularyTerm((String) value);
        if (term != null) {
            return term;
        }
        throw UserFailureException.fromTemplate(
                "Incorrect value '%s' for a controlled vocabulary set '%s'.", value,
                vocabulary.getCode());
    }

    public static Long[] tryGetIntegerArray(final Serializable value, final DataTypeCode dataTypeCode) {
        if (dataTypeCode != DataTypeCode.ARRAY_INTEGER) {
            return null;
        }
        if (value == null || !value.getClass().isArray() || ((Serializable[]) value).length == 0) {
            return null;
        }
        return Arrays.stream((Serializable[]) value)
                .map(x -> Long.parseLong(x.toString().trim()))
                .toArray(Long[]::new);
    }

    public static Double[] tryGetRealArray(final Serializable value, final DataTypeCode dataTypeCode) {
        if (dataTypeCode != DataTypeCode.ARRAY_REAL) {
            return null;
        }
        if (value == null || !value.getClass().isArray() || ((Serializable[]) value).length == 0) {
            return null;
        }
        return Arrays.stream((Serializable[]) value)
                .map(x -> Double.parseDouble(x.toString().trim()))
                .toArray(Double[]::new);
    }

    public static String[] tryGetStringArray(final Serializable value, final DataTypeCode dataTypeCode) {
        if (dataTypeCode != DataTypeCode.ARRAY_STRING) {
            return null;
        }
        if (value == null || !value.getClass().isArray() || ((Serializable[]) value).length == 0) {
            return null;
        }
        return Arrays.stream((Serializable[]) value)
                .map(Serializable::toString)
                .toArray(String[]::new);
    }

    public static Date[] tryGetTimestampArray(final Serializable value, final DataTypeCode dataTypeCode) {
        if (dataTypeCode != DataTypeCode.ARRAY_TIMESTAMP) {
            return null;
        }
        if (value == null || !value.getClass().isArray() || ((Serializable[]) value).length == 0) {
            return null;
        }
        return Arrays.stream((Serializable[]) value)
                .map(x -> parseDateFromString((String) x))
                .toArray(Date[]::new);
    }

    private static Date parseDateFromString(final String dateTime) {
        for (SupportedDateTimePattern format : SupportedDateTimePattern.values()) {
            try {
                final SimpleDateFormat simpleDateFormat =
                        new SimpleDateFormat(format.getPattern());
                return simpleDateFormat.parse(dateTime);
            } catch (final Exception e) {
                // If no format is suitable, the exception will be thrown at the end of this method.
            }
        }
        throw new IllegalArgumentException("Wrong date format:" + dateTime);
    }

    public static String tryGetJsonValue(final Serializable value, final DataTypeCode dataTypeCode) {
        if (dataTypeCode != DataTypeCode.JSON || value == null) {
            return null;
        }
        return value.toString();
    }

}
