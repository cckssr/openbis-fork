/*
 *  Copyright ETH 2026 Zurich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package ch.systemsx.cisd.common.build;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

public class LibsVersionsTomlTest
{
    private static final Path CATALOG = findCatalog();

    private static final String GERONIMO_STAX_API_ALIAS = "org-apache-geronimo-specs-geronimo-stax-api10-spec";

    @Test
    public void catalogEntriesAreAlphabeticallySorted() throws IOException
    {
        Map<String, List<Entry>> entriesBySection = readEntriesBySection();

        for (String section : Arrays.asList("versions", "libraries", "plugins"))
        {
            List<String> names = names(entriesBySection.get(section));
            List<String> sortedNames = new ArrayList<>(names);
            Collections.sort(sortedNames);

            assertEquals(names, sortedNames, "Entries in [" + section + "] must be sorted alphabetically.");
        }
    }

    @Test
    public void catalogEntriesUseKebabCase() throws IOException
    {
        Map<String, List<Entry>> entriesBySection = readEntriesBySection();

        for (String section : Arrays.asList("versions", "libraries", "plugins"))
        {
            for (Entry entry : entriesBySection.get(section))
            {
                assertTrue(isKebabCase(entry.name),
                        "Entry '" + entry.name + "' in [" + section + "] must use kebab-case.");
            }
        }
    }

    @Test
    public void libraryNamesMatchFullQualifiedModuleNames() throws IOException
    {
        for (Entry entry : readEntriesBySection().get("libraries"))
        {
            String module = valueOf(entry.value, "module");
            assertNotNull(module, "Library '" + entry.name + "' must declare a module.");

            String expectedName = normalize(module.replace(':', '-'));
            if (GERONIMO_STAX_API_ALIAS.equals(entry.name))
            {
                continue;
            }
            assertEquals(withoutVersionSuffix(entry.name), expectedName,
                    "Library alias must be the kebab-case form of its full module name.");
        }
    }

    @Test
    public void pluginNamesMatchPluginIds() throws IOException
    {
        for (Entry entry : readEntriesBySection().get("plugins"))
        {
            String pluginId = valueOf(entry.value, "id");
            assertNotNull(pluginId, "Plugin '" + entry.name + "' must declare an id.");
            assertEquals(entry.name, normalize(pluginId),
                    "Plugin alias must be the kebab-case form of its plugin id.");
        }
    }

    @Test
    public void versionReferencesAreDeclaredAndUsed() throws IOException
    {
        Map<String, List<Entry>> entriesBySection = readEntriesBySection();
        Set<String> versions = new HashSet<>(names(entriesBySection.get("versions")));
        Set<String> usedVersions = new HashSet<>();
        Map<String, List<String>> aliasesByVersion = new HashMap<>();

        for (String section : Arrays.asList("libraries", "plugins"))
        {
            for (Entry entry : entriesBySection.get(section))
            {
                String versionRef = valueOf(entry.value, "version.ref");
                assertNotNull(versionRef,
                        "Entry '" + entry.name + "' in [" + section + "] must use version.ref.");

                usedVersions.add(versionRef);
                assertTrue(versions.contains(versionRef),
                        "Entry '" + entry.name + "' references missing version '" + versionRef + "'.");
                aliasesByVersion.computeIfAbsent(versionRef, key -> new ArrayList<>()).add(entry.name);
            }
        }

        for (String version : versions)
        {
            if ("node-runtime".equals(version)){
                continue;
            }
            assertTrue(usedVersions.contains(version), "Version '" + version + "' is not referenced.");

        }
    }

    @Test
    public void versionNamesMatchAReferencedAliasOrSharedAliasPrefix() throws IOException
    {
        Map<String, List<Entry>> entriesBySection = readEntriesBySection();
        Map<String, List<String>> aliasesByVersion = new HashMap<>();

        for (String section : Arrays.asList("libraries", "plugins"))
        {
            for (Entry entry : entriesBySection.get(section))
            {
                String versionRef = valueOf(entry.value, "version.ref");
                assertNotNull(versionRef,
                        "Entry '" + entry.name + "' in [" + section + "] must use version.ref.");
                aliasesByVersion.computeIfAbsent(versionRef, key -> new ArrayList<>()).add(entry.name);
            }
        }

        for (Map.Entry<String, List<String>> versionAliases : aliasesByVersion.entrySet())
        {
            String version = versionAliases.getKey();
            List<String> aliases = versionAliases.getValue();
            if (aliases.size() == 1)
            {
                assertEquals(aliases.getFirst(), version,
                        "Version '" + version + "' must match the referenced alias when used by only one entry.");
            } else
            {
                for (String alias : aliases)
                {
                    assertTrue(alias.startsWith(version),
                            "Alias '" + alias + "' must start with shared version name '" + version + "'.");
                }
            }
        }
    }

    @Test
    public void catalogUsesConsistentAssignmentSpacing() throws IOException
    {
        List<String> invalidLines = new ArrayList<>();
        for (String line : Files.readAllLines(CATALOG))
        {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("["))
            {
                continue;
            }
            if (isEntry(trimmed) && !trimmed.contains(" = "))
            {
                invalidLines.add(trimmed);
            }
        }

        assertTrue(invalidLines.isEmpty(), "Catalog entries must use ' = ': " + invalidLines);
    }

    private static Map<String, List<Entry>> readEntriesBySection() throws IOException
    {
        Map<String, List<Entry>> entriesBySection = new HashMap<>();
        String currentSection = null;
        for (String section : Arrays.asList("versions", "libraries", "plugins"))
        {
            entriesBySection.put(section, new ArrayList<>());
        }

        int lineNumber = 0;
        for (String line : Files.readAllLines(CATALOG))
        {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
            {
                continue;
            }

            if (isSection(trimmed))
            {
                currentSection = trimmed.substring(1, trimmed.length() - 1);
                continue;
            }

            if (entriesBySection.containsKey(currentSection))
            {
                assertTrue(isEntry(trimmed),
                        "Line " + lineNumber + " in [" + currentSection + "] is not a valid catalog entry.");
                int separator = trimmed.indexOf('=');
                entriesBySection.get(currentSection).add(
                        new Entry(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim()));
            }
        }

        for (String section : entriesBySection.keySet())
        {
            assertFalse(entriesBySection.get(section).isEmpty(), "No entries found in [" + section + "].");
        }

        return entriesBySection;
    }

    private static Path findCatalog()
    {
        Path current = Path.of("").toAbsolutePath();
        while (current != null)
        {
            Path catalog = current.resolve("build/libs.versions.toml");
            if (Files.isRegularFile(catalog))
            {
                return catalog;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find build/libs.versions.toml");
    }

    private static List<String> names(List<Entry> entries)
    {
        List<String> names = new ArrayList<>();
        for (Entry entry : entries)
        {
            names.add(entry.name);
        }
        return names;
    }

    private static String normalize(String value)
    {
        StringBuilder normalized = new StringBuilder();
        boolean lastWasSeparator = true;

        for (int i = 0; i < value.length(); i++)
        {
            char character = Character.toLowerCase(value.charAt(i));
            if (isLowerCaseLetterOrDigit(character))
            {
                normalized.append(character);
                lastWasSeparator = false;
            } else if (!lastWasSeparator)
            {
                normalized.append('-');
                lastWasSeparator = true;
            }
        }

        int length = normalized.length();
        if (length > 0 && normalized.charAt(length - 1) == '-')
        {
            normalized.deleteCharAt(length - 1);
        }
        return normalized.toString();
    }

    private static String withoutVersionSuffix(String value)
    {
        int suffixStart = value.lastIndexOf("-v");
        if (suffixStart < 0 || suffixStart + 2 >= value.length() || !Character.isDigit(
                value.charAt(suffixStart + 2)))
        {
            return value;
        }
        for (int i = suffixStart + 3; i < value.length(); i++)
        {
            if (!isLowerCaseLetterOrDigit(value.charAt(i)))
            {
                return value;
            }
        }
        return value.substring(0, suffixStart);
    }

    private static boolean isSection(String value)
    {
        return value.length() > 2 && value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']';
    }

    private static boolean isEntry(String value)
    {
        int separator = value.indexOf('=');
        return separator > 0 && isEntryName(value.substring(0, separator).trim());
    }

    private static boolean isEntryName(String value)
    {
        if (value.isEmpty())
        {
            return false;
        }
        for (int i = 0; i < value.length(); i++)
        {
            char character = value.charAt(i);
            if (!isLowerCaseLetterOrDigit(character) && character != '-' && character != '_')
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isKebabCase(String value)
    {
        if (value.isEmpty() || value.charAt(0) == '-' || value.charAt(value.length() - 1) == '-')
        {
            return false;
        }
        boolean previousWasDash = false;
        for (int i = 0; i < value.length(); i++)
        {
            char character = value.charAt(i);
            if (character == '-')
            {
                if (previousWasDash)
                {
                    return false;
                }
                previousWasDash = true;
            } else if (isLowerCaseLetterOrDigit(character))
            {
                previousWasDash = false;
            } else
            {
                return false;
            }
        }
        return true;
    }

    private static String valueOf(String entryValue, String key)
    {
        String lookup = key + " = \"";
        int keyIndex = entryValue.indexOf(lookup);
        if (keyIndex < 0)
        {
            return null;
        }
        int valueStart = keyIndex + lookup.length();
        int valueEnd = entryValue.indexOf('"', valueStart);
        if (valueEnd < 0)
        {
            return null;
        }
        return entryValue.substring(valueStart, valueEnd);
    }

    private static boolean isLowerCaseLetterOrDigit(char character)
    {
        return (character >= 'a' && character <= 'z') || (character >= '0' && character <= '9');
    }

    private record Entry(String name, String value)
        {
        }
}
