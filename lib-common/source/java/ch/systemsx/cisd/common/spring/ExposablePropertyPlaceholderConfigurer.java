/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.common.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.Constants;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringUtils;

import ch.systemsx.cisd.common.properties.ExtendedProperties;

/**
 * Bean that should be used instead of the {@link PropertyPlaceholderConfigurer} if you want to have access to the resolved properties not obligatory
 * from the Spring context. e.g. from JSP or so.
 * 
 * @author Christian Ribeaud
 */
public class ExposablePropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer
{
    /** Standard bean name in an application context file. */
    public static final String PROPERTY_CONFIGURER_BEAN_NAME = "propertyConfigurer";

    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            ExposablePropertyPlaceholderConfigurer.class);

    private Properties resolvedProps;

    private int systemPropertiesMode = PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK;

    private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

    /** Returns the resolved properties. */
    public final Properties getResolvedProps()
    {
        return resolvedProps;
    }

    public final String getPropertyValue(String key) {
        return getPropertyValue(key, null);
    }

    public final String getPropertyValue(String key, String defaultValue) {
        String result = resolvedProps.getProperty(key);
        if(result != null) {
            return result;
        }
        operationLog.info(String.format("Property '%s' not found in the resolved properties, checking system properties", key));

        result = System.getProperty(key);

        if(result == null) {
            operationLog.info(String.format("Property '%s' not found in the system properties, checking environment variables", key));
            result = System.getenv(key);
        }

        if(result != null) {
            operationLog.info(String.format("Property '%s' found. Adding its value to resolved properties.", key));
            resolvedProps.put(key, result);
        } else {
            operationLog.info(String.format("Property '%s' not found. Using default value", key));
            result = defaultValue;
        }
        return result;
    }

    //
    // PropertyPlaceholderConfigurer
    //

    @Override
    protected final String convertPropertyValue(final String originalValue)
    {
        // Can handle null value
        return StringUtils.trimWhitespace(originalValue);
    }

    @Override
    public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException
    {
        setSystemPropertiesMode(constants.asNumber(constantName).intValue());
    }

    @Override
    public void setSystemPropertiesMode(int systemPropertiesMode)
    {
        this.systemPropertiesMode = systemPropertiesMode;
        super.setSystemPropertiesMode(systemPropertiesMode);
    }

    public Map<String, String> getDefaultValuesForMissingProperties()
    {
        return new HashMap<String, String>();
    }

    @Override
    protected final void processProperties(
            final ConfigurableListableBeanFactory beanFactoryToProcess, final Properties props)
            throws BeansException
    {
        if(operationLog.isDebugEnabled())
        {
            for(Map.Entry<Object, Object> prop : props.entrySet())
            {
                String key = String.valueOf(prop.getKey());
                String value = String.valueOf(prop.getValue());
                if(key.contains("password")) {
                    value =  "*****";
                }
                operationLog.debug(String.format("Passed property: ('%s', '%s')", key, value));
            }
        }
        resolvedProps = new ExtendedProperties();
        for (final Object key : props.keySet())
        {
            final String keyStr = key.toString();
            resolvedProps.setProperty(keyStr, getResolvedProperty(props, keyStr));
        }

        Map<String, String> defaultValues = getDefaultValuesForMissingProperties();
        for (String key : defaultValues.keySet())
        {
            if (resolvedProps.containsKey(key) == false)
            {
                resolvedProps.setProperty(key, defaultValues.get(key));
            }
        }

        injectPropertiesInto(resolvedProps);
        super.processProperties(beanFactoryToProcess, resolvedProps);

        operationLog.info("Service properties loaded. Properties count:" + resolvedProps.size());
        resolvedProps.forEach((propKey, propValue) -> {
            String key = propKey.toString();
            String value = propValue == null ? "" : propValue.toString();
            if(key.contains("password")) {
                value = "*****";
            }
            operationLog.info(String.format("Loaded property: ('%s', '%s')", key, value));
        });
    }

    protected void injectPropertiesInto(Properties properties)
    {
    }

    private String getResolvedProperty(final Properties props, final String key)
    {
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(
                placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
        String resolvedPlaceholder = resolvePlaceholder(key, props, systemPropertiesMode);
        return helper.replacePlaceholders(resolvedPlaceholder, new PlaceholderResolver()
            {
                @Override
                public String resolvePlaceholder(String placeholderName)
                {
                    return ExposablePropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName, props, systemPropertiesMode);
                }
            });        
    }
}