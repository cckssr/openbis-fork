/*
 * Copyright ETH 2022 - 2025 Zürich, Scientific IT Services
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
package ch.ethz.sis.shared.startup;

import ch.ethz.sis.shared.log.standard.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Configuration {

    private static final String SYSTEM_PROPERTY_PREFIX_KEY = "system.property.prefix";

    private final Map<Enum, Object> sharables = new ConcurrentHashMap<>();
    private final Properties properties = new Properties();
    private final String systemPropertyPrefix;

    public <E extends Enum<E>> Configuration(List<Class<E>> mandatoryParametersClasses, String pathToConfigurationFile) {
        List<E> parameters = new ArrayList<>();
        for (Class<E> parametersClass : mandatoryParametersClasses) {
            parameters.addAll(Arrays.asList(parametersClass.getEnumConstants()));
        }

        try (InputStream inputStream = new FileInputStream(pathToConfigurationFile)) {
            properties.load(inputStream);
            String propertyPrefix = properties.getProperty(SYSTEM_PROPERTY_PREFIX_KEY, "");
            if(propertyPrefix != null && !propertyPrefix.isBlank()) {
                propertyPrefix = getValue(propertyPrefix + SYSTEM_PROPERTY_PREFIX_KEY, propertyPrefix);
            }
            systemPropertyPrefix = propertyPrefix;

            properties.forEach((key, value) -> {
                String keyStr = (String) key;
                String valueStr = (String) value;
                properties.setProperty(keyStr, getValue(systemPropertyPrefix + keyStr, valueStr));
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        for (E parameter : parameters) {
            String value = getProperty(parameter.toString());
            if (value == null) {
                throw new RuntimeException("Failed to load '" + parameter + "' from config file '" + pathToConfigurationFile + "'");
            }
        }
    }

    public Configuration(Map<Enum, String> values) {

        String propertyPrefix = "";
        for (Enum key:values.keySet())
        {
            String keyStr = key.toString();
            if(keyStr.equals(SYSTEM_PROPERTY_PREFIX_KEY)) {
                propertyPrefix = values.get(key);
                break;
            }
        }
        if(propertyPrefix != null && !propertyPrefix.isBlank()) {
            propertyPrefix = getValue(propertyPrefix + SYSTEM_PROPERTY_PREFIX_KEY, propertyPrefix);
        }
        systemPropertyPrefix = propertyPrefix;


        for (Enum key:values.keySet()) {
            String keyName = key.name();
            String value = values.get(key);
            properties.setProperty(key.name(), getValue(systemPropertyPrefix + keyName, value));
        }
    }

    public Configuration(Properties properties) {
        Enumeration propertyNames = properties.propertyNames();

        String propertyPrefix = properties.getProperty(SYSTEM_PROPERTY_PREFIX_KEY, "");
        if(propertyPrefix != null && !propertyPrefix.isBlank()) {
            propertyPrefix = getValue(propertyPrefix + SYSTEM_PROPERTY_PREFIX_KEY, propertyPrefix);
        }
        systemPropertyPrefix = propertyPrefix;

        while(propertyNames.hasMoreElements()) {
            Object propertyName = propertyNames.nextElement();
            String key = String.valueOf(propertyName);
            String value = properties.getProperty(String.valueOf(propertyName));
            this.properties.setProperty(key, getValue(systemPropertyPrefix + key, value));
        }
    }

    private static String getValue(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        if(value == null) {
            value = defaultValue;
        }
        return value;
    }

    public void logLoadedProperties(Logger logger) {
        logger.info(String.format("Loaded properties count: %s", this.properties.size()));
        this.properties.forEach((propKey, propValue) -> {
            String key = propKey.toString();
            String value = propValue == null ? "" : propValue.toString();
            if(key.toLowerCase().contains("password")) {
                value = "*****";
            }
            logger.info(String.format("Loaded property: ('%s', '%s')", key, value));
        });
    }

    public <E extends Enum<E>> void setProperty(E parameter, String value) {
        properties.setProperty(parameter.toString(), value);
    }

    private <E extends Enum<E>> String getProperty(E parameter) {
        return properties.getProperty(parameter.toString());
    }

    public <E extends Enum<E>> String getStringProperty(E parameter) {
        return getProperty(parameter);
    }

    public <E extends Enum<E>> int getIntegerProperty(E parameter) {
        return Integer.parseInt(getProperty(parameter));
    }

    public <E extends Enum<E>, I> I getSharableInstance(E parameter)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        I sharable = (I) sharables.get(parameter);
        if (sharable == null) {
            String value = getProperty(parameter);
            if (value != null && !value.isBlank()) {
                sharable = (I) Class.forName(value).newInstance();
                sharables.put(parameter, sharable);
            }
        }
        return sharable;
    }

    public <E extends Enum<E>, I> I getInstance(E parameter) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        String value = getProperty(parameter);
        if (value != null && !value.isBlank()) {
            return (I) Class.forName(value).newInstance();
        } else {
            return null;
        }
    }

    public <E extends Enum<E>> Class<?> getInterfaceClass(E parameter) throws ClassNotFoundException {
        String value = getProperty(parameter);
        if (value != null && !value.isBlank()) {
            return Class.forName(value);
        } else {
            return null;
        }
    }

    public Properties getProperties(){
        return this.properties;
    }

    public String getProperty(String key){
        if(properties.containsKey(key)) {
            return properties.getProperty(key);
        }

        String value = getValue(systemPropertyPrefix + key, null);
        if(value != null) {
            properties.put(key,  value);
        }
        return value;
    }
}