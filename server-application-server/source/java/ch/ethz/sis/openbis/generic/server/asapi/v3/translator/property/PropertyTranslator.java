/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.translator.property;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyFetchOptions;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.AbstractCachingTranslator;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.TranslationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.common.ObjectHolder;
import ch.ethz.sis.openbis.generic.server.asapi.v3.translator.sample.ISampleAuthorizationValidator;

/**
 * @author pkupczyk
 */
public abstract class PropertyTranslator extends
        AbstractCachingTranslator<Long, ObjectHolder<Map<String, Serializable>>, PropertyFetchOptions>
        implements IPropertyTranslator
{
    @Autowired
    private ISampleAuthorizationValidator sampleAuthorizationValidator;

    @Override
    protected ObjectHolder<Map<String, Serializable>> createObject(TranslationContext context,
            Long objectId, PropertyFetchOptions fetchOptions)
    {
        return new ObjectHolder<Map<String, Serializable>>();
    }

    @Override
    protected Object getObjectsRelations(TranslationContext context, Collection<Long> objectIds,
            PropertyFetchOptions fetchOptions)
    {
        List<PropertyRecord> records = loadProperties(objectIds);
        Set<Long> visibleSamples =
                sampleAuthorizationValidator.validate(context.getSession().tryGetPerson(),
                        records.stream().filter(r -> r.sample_id != null).map(r -> r.sample_id)
                                .collect(Collectors.toSet()));

        Map<Long, Map<String, Serializable>> properties =
                new HashMap<Long, Map<String, Serializable>>();
        for (PropertyRecord record : records)
        {
            Map<String, Serializable> objectProperties = properties.get(record.objectId);

            if (objectProperties == null)
            {
                objectProperties = new HashMap<>();
                properties.put(record.objectId, objectProperties);
            }

            boolean isMultiValue = Boolean.TRUE.equals(record.isMultiValue);

            if (record.propertyValue != null)
            {
                updateObjectProperty(objectProperties, record.propertyCode, record.propertyValue,
                        isMultiValue);
            } else if (record.vocabularyPropertyValue != null)
            {
                updateObjectProperty(objectProperties, record.propertyCode,
                        record.vocabularyPropertyValue, isMultiValue);
            } else if (record.sample_perm_id != null)
            {
                if (visibleSamples.contains(record.sample_id))
                {
                    updateObjectProperty(objectProperties, record.propertyCode,
                            record.sample_perm_id, isMultiValue);
                }
            } else if (record.integerArrayPropertyValue != null)
            {
                updateArrayObjectProperty(objectProperties, record.propertyCode,
                        record.integerArrayPropertyValue, isMultiValue);
            } else if (record.realArrayPropertyValue != null)
            {
                updateArrayObjectProperty(objectProperties, record.propertyCode,
                        record.realArrayPropertyValue, isMultiValue);
            } else if (record.stringArrayPropertyValue != null)
            {
                updateArrayObjectProperty(objectProperties, record.propertyCode,
                        record.stringArrayPropertyValue, isMultiValue);
            } else if (record.timestampArrayPropertyValue != null)
            {
                updateArrayObjectProperty(objectProperties, record.propertyCode,
                        record.timestampArrayPropertyValue, isMultiValue);
            } else if (record.jsonPropertyValue != null)
            {
                updateObjectProperty(objectProperties, record.propertyCode,
                        record.jsonPropertyValue, isMultiValue);
            } else
            {
                // SAMPLE property with deleted sample. Thus, nothing is put to objectProperties
            }
        }

        return properties;
    }

    private void updateObjectProperty(Map<String, Serializable> objectProperties,
            String propertyCode, Serializable propertyValue, boolean isMultiValue)
    {
        if (objectProperties.containsKey(propertyCode))
        {
            Serializable current = objectProperties.get(propertyCode);
            Serializable newValue = composeMultiValueProperty(current, propertyValue);
            objectProperties.put(propertyCode, newValue);
        } else if (isMultiValue)
        {
            // Always represent a multi-value property as an array, even when only a single
            // value/row currently exists, so it doesn't collapse into a bare scalar.
            objectProperties.put(propertyCode, new Serializable[] { propertyValue });
        } else
        {
            objectProperties.put(propertyCode, propertyValue);
        }
    }

    private void updateArrayObjectProperty(Map<String, Serializable> objectProperties,
            String propertyCode, Serializable[] propertyValue, boolean isMultiValue)
    {
        if (objectProperties.containsKey(propertyCode))
        {
            Serializable[] current = (Serializable[]) objectProperties.get(propertyCode);
            Serializable[] result;
            if (current.length > 0)
            {
                if (current[0].getClass().isArray())
                {
                    result = new Serializable[current.length + 1];
                    System.arraycopy(current, 0, result, 0, current.length);
                    result[current.length] = propertyValue;
                } else
                {
                    result = new Serializable[] { current, propertyValue };
                }
            } else
            {
                result = propertyValue;
            }
            objectProperties.put(propertyCode, result);
        } else if (isMultiValue)
        {
            // Always represent a multi-value property as an array of arrays, even when only a
            // single value/row currently exists, so it doesn't collapse into a bare array.
            objectProperties.put(propertyCode, new Serializable[] { propertyValue });
        } else
        {
            objectProperties.put(propertyCode, propertyValue);
        }
    }

    private Serializable composeMultiValueProperty(Serializable current, Serializable newValue)
    {
        Serializable[] result;
        if (current.getClass().isArray())
        {
            Serializable[] values = (Serializable[]) current;
            result = new Serializable[values.length + 1];
            System.arraycopy(values, 0, result, 0, values.length);
            result[values.length] = newValue;
        } else
        {
            result = new Serializable[] { current, newValue };
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void updateObject(TranslationContext context, Long objectId,
            ObjectHolder<Map<String, Serializable>> result, Object relations,
            PropertyFetchOptions fetchOptions)
    {
        Map<Long, Map<String, Serializable>> properties =
                (Map<Long, Map<String, Serializable>>) relations;
        Map<String, Serializable> objectProperties = properties.get(objectId);

        if (objectProperties == null)
        {
            objectProperties = new HashMap<String, Serializable>();
        }

        result.setObject(objectProperties);
    }

    protected abstract List<PropertyRecord> loadProperties(Collection<Long> entityIds);

}
