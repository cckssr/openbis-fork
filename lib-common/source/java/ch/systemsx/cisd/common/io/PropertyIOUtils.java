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
package ch.systemsx.cisd.common.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import ch.systemsx.cisd.common.exceptions.ConfigurationFailureException;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.properties.PropertyUtils;
import org.springframework.util.PropertyPlaceholderHelper;

import static org.springframework.beans.factory.config.PlaceholderConfigurerSupport.*;

/**
 * A static helper class to help with loading and saving {@link Properties}.
 *
 * @author Bernd Rinn
 */
public class PropertyIOUtils
{

    private static final String PLACEHOLDER_PREFIX = DEFAULT_PLACEHOLDER_PREFIX;

    private static final String PLACEHOLDER_SUFFIX = DEFAULT_PLACEHOLDER_SUFFIX;

    private static final String VALUE_SEPARATOR = DEFAULT_VALUE_SEPARATOR;

    private static final String SYSTEM_PROPERTY_PREFIX_KEY = "system.property.prefix";

    /**
     * Loads properties from the specified file and adds them to the specified properties.
     * <ul>
     * <li>Empty lines and lines starting with a hash symbol '#' are ignored.
     * <li>All other lines should have a equals symbol '='. The trimmed part before/after '=' specify property key and value , respectively.
     * </ul>
     */
    public static void loadAndAppendProperties(Properties properties, File propertiesFile)
    {
        List<String> lines = FileUtilities.loadToStringList(propertiesFile);
        for (int i = 0; i < lines.size(); i++)
        {
            String line = lines.get(i).trim();
            if (line.length() == 0 || line.startsWith("#"))
            {
                continue;
            }
            int indexOfEqualSymbol = line.indexOf('=');
            if (indexOfEqualSymbol < 0)
            {
                throw new UserFailureException("Missing '=' in line " + (i + 1)
                        + " of properties file '" + propertiesFile + "': " + line);
            }
            String key = line.substring(0, indexOfEqualSymbol).trim();
            String value = line.substring(indexOfEqualSymbol + 1).trim();
            properties.setProperty(key, value);
        }
        resolveLoadedProperties(properties);
    }

    /**
     * Loads properties from the specified file. This is a simpler version of {@link PropertyIOUtils#loadProperties(String)} which does not use
     * {@link Properties#load(InputStream)}:
     * <ul>
     * <li>Empty lines and lines starting with a hash symbol '#' are ignored.
     * <li>All other lines should have a equals symbol '='. The trimmed part before/after '=' specify property key and value , respectively.
     * </ul>
     * There is no character escaping as in {@link Properties#load(InputStream)} because this can lead to problems if this syntax isn't known by the
     * creator of a properties file.
     */
    public static Properties loadProperties(File propertiesFile)
    {
        Properties properties = new Properties();
        loadAndAppendProperties(properties, propertiesFile);
        return properties;
    }

    /**
     * Loads and returns {@link Properties} found in given <var>propertiesFilePath</var>.
     * 
     * @throws ConfigurationFailureException If an exception occurs when loading the properties.
     * @return never <code>null</code> but could return empty properties.
     */
    public final static Properties loadProperties(final InputStream is, final String resourceName)
    {
        assert is != null : "No input stream specified";
        final Properties properties = new Properties();
        try
        {
            properties.load(is);
            PropertyUtils.trimProperties(properties);
            resolveLoadedProperties(properties);
            return properties;
        } catch (final Exception ex)
        {
            final String msg =
                    String.format("Could not load the properties from given resource '%s'.",
                            resourceName);
            throw new ConfigurationFailureException(msg, ex);
        } finally
        {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * Loads and returns {@link Properties} found in given <var>propertiesFilePath</var>.
     * 
     * @throws ConfigurationFailureException If an exception occurs when loading the properties.
     * @return never <code>null</code> but could return empty properties.
     */
    public final static Properties loadProperties(final String propertiesFilePath)
    {
        try
        {
            return loadProperties(new FileInputStream(propertiesFilePath), propertiesFilePath);
        } catch (FileNotFoundException ex)
        {
            final String msg = String.format("Properties file '%s' not found.", propertiesFilePath);
            throw new ConfigurationFailureException(msg, ex);
        }
    }

    /**
     * Saves the given <var>properties</var> to the given <var>propertiesFile</var>.
     */
    public static void saveProperties(File propertiesFile, Properties properties)
    {
        final StringBuilder builder = new StringBuilder();
        for (Entry<Object, Object> entry : properties.entrySet())
        {
            final String key = entry.getKey().toString();
            final String value = entry.getValue().toString();
            builder.append(key);
            builder.append(" = ");
            builder.append(value);
            builder.append('\n');
        }
        FileUtilities.writeToFile(propertiesFile, builder.toString());
    }

    public static void resolveLoadedProperties(final Properties properties) {
        String propertyPrefix = properties.getProperty(SYSTEM_PROPERTY_PREFIX_KEY, "");
        if(propertyPrefix != null && !propertyPrefix.isBlank()) {
            propertyPrefix = resolveProperty(propertyPrefix + SYSTEM_PROPERTY_PREFIX_KEY, propertyPrefix);
        }
        final String prefix = propertyPrefix;

        properties.forEach((key,value) -> {
            String keyStr = (String) key;
            String valueStr = (String) value;
            String property = resolveProperty(prefix + keyStr, valueStr);
            String resolved = getResolvedProperty(properties, property, prefix);
            properties.setProperty(keyStr, resolved);
        });
    }

    private static String getResolvedProperty(final Properties props, final String key, final String systemPropertyPrefix)
    {
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(
                PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true);
        return helper.replacePlaceholders(key, new PropertyPlaceholderHelper.PlaceholderResolver()
        {
            @Override
            public String resolvePlaceholder(String placeholderName)
            {
                String defaultValue = props.getProperty(placeholderName);
                return resolveProperty(systemPropertyPrefix + placeholderName, defaultValue);
            }
        });
    }

    private static String resolveProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        if(value == null) {
            value = defaultValue;
        }
        return value;
    }


}
