package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.property.PropertiesDeserializer;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.property.Spreadsheet;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.util.XmlUtils;
import org.w3c.dom.Document;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

final class XmlValidator implements IDataTypeValidator {

    private static final String SPREADSHEET_WIDGET = "SPREADSHEET";

    private String xmlSchema;

    private String propertyTypeLabel;

    private Map<String, String> metaData;

    public void setXmlSchema(String xmlSchema) {
        this.xmlSchema = xmlSchema;
    }

    public void setPropertyTypeLabel(String label) {
        this.propertyTypeLabel = label;
    }

    public void setMetaData(Map<String, String> metaData) {
        this.metaData = metaData;
    }

    //
    // IDataTypeValidator
    //

    @Override
    public final Serializable validate(final Serializable value) throws UserFailureException {
        assert value != null : "Unspecified value.";

        if (value.getClass().isArray()) {
            for (Serializable singleValue : (Serializable[]) value) {
                validateSingleValue(singleValue);
            }
        } else {
            validateSingleValue(value);
        }
        // validated value is valid
        return value;
    }

    private Serializable validateSingleValue(Serializable val) {
        String value = (String) val;
        // parsing checks if the value is a well-formed XML document
        Document document = XmlUtils.parseXmlDocument(value);
        if (xmlSchema != null) {
            // validate against schema
            try {
                XmlUtils.validate(document, xmlSchema);
            } catch (Exception e) {
                // instance document is invalid!
                throw UserFailureException.fromTemplate(
                        "Provided value doesn't validate against schema "
                                + "of property type '%s'. %s", propertyTypeLabel,
                        e.getMessage());
            }
        }
        if (metaData != null) {
            String customWidgetValue = metaData.get("custom_widget");
            if (customWidgetValue != null) {
                if (customWidgetValue.toUpperCase().equals(SPREADSHEET_WIDGET)) {

                    validateSpreadsheet(value);
                }
            }

        }
        // validated value is valid
        return value;
    }

    private void validateSpreadsheet(String value) {
        String rawData = value;
        if (rawData.startsWith("<DATA>")) {
            rawData = rawData.substring("<DATA>".length(), rawData.length() - "</DATA>".length());
        }
        try {
            String jsonString = new String(Base64.getDecoder().decode(rawData), StandardCharsets.UTF_8);
            Spreadsheet result =
                    PropertiesDeserializer.readValue(jsonString, Spreadsheet.class);
        } catch (Exception e) {
            throw UserFailureException.fromTemplate(
                    "Provided spreadsheet could not be validated: %s ",
                    e.getMessage());
        }
    }
}
