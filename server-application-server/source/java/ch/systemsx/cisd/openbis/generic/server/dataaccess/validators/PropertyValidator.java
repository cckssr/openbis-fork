/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.openbis.generic.server.dataaccess.validators;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

import ch.systemsx.cisd.openbis.generic.server.dataaccess.IPropertyValueValidator;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.openbis.generic.shared.basic.BasicConstant;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;
import ch.systemsx.cisd.openbis.generic.shared.dto.PropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.util.SimplePropertyValidator;

/**
 * The default {@link IPropertyValueValidator} implementation.
 *
 * @author Christian Ribeaud
 */
public final class PropertyValidator implements IPropertyValueValidator
{
    private final static SimplePropertyValidator simplePropertyValidator =
            new SimplePropertyValidator();

    private final static Map<DataTypeCode, IDataTypeValidator> createDataTypeValidators()
    {
        final Map<DataTypeCode, IDataTypeValidator> map =
                new EnumMap<DataTypeCode, IDataTypeValidator>(DataTypeCode.class);
        map.put(DataTypeCode.CONTROLLEDVOCABULARY, new ControlledVocabularyValidator());
        map.put(DataTypeCode.XML, new XmlValidator());
        map.put(DataTypeCode.SAMPLE, new SampleValidator());
        map.put(DataTypeCode.JSON, new JsonValidator());
        map.put(DataTypeCode.ARRAY_STRING, new StringArrayValidator());
        map.put(DataTypeCode.ARRAY_INTEGER, new IntegerArrayValidator());
        map.put(DataTypeCode.ARRAY_REAL, new RealArrayValidator());
        map.put(DataTypeCode.ARRAY_TIMESTAMP, new TimestampArrayValidator());
        return map;
    }

    @Override
    public final Serializable validatePropertyValue(final PropertyTypePE propertyType, final Serializable value)
            throws UserFailureException
    {
        assert propertyType != null : "Unspecified property type.";
        assert value != null : "Unspecified value.";

        // don't validate error messages and placeholders
        if (value.getClass().equals(String.class) && ((String) value).startsWith(BasicConstant.ERROR_PROPERTY_PREFIX))
        {
            return value;
        }
        final DataTypeCode entityDataType = propertyType.getType().getCode();
        // If the simplePropertyValidator can handle this, use it.
        if (simplePropertyValidator.canValidate(entityDataType))
        {
            return simplePropertyValidator.validatePropertyValue(entityDataType, value);
        }
        final IDataTypeValidator dataTypeValidator = createDataTypeValidators().get(entityDataType);
        assert dataTypeValidator != null : String.format("No IDataTypeValidator implementation "
                + "specified for '%s'.", entityDataType);
        switch (entityDataType)
        {
            case CONTROLLEDVOCABULARY:
                ((ControlledVocabularyValidator) dataTypeValidator).setVocabulary(propertyType);
                break;
            case XML:
                ((XmlValidator) dataTypeValidator).setXmlSchema(propertyType.getSchema());
                ((XmlValidator) dataTypeValidator).setPropertyTypeLabel(propertyType.getLabel());
                ((XmlValidator) dataTypeValidator).setMetaData(propertyType.getMetaData());
                break;
            case ARRAY_STRING:
            case ARRAY_INTEGER:
            case ARRAY_REAL:
            case ARRAY_TIMESTAMP:
                ((ArrayValidator) dataTypeValidator).setArrayType(entityDataType);
                break;
            default:
                break;
        }
        return dataTypeValidator.validate(value);
    }

}
