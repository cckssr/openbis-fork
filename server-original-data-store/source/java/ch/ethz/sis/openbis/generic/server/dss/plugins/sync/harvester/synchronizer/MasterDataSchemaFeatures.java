/*
 * Copyright ETH 2018 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.harvester.synchronizer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.MultiKeyMap;

/**
 * Optional source schema fields, keyed by translated codes. Absence means that
 * the source did not supply the field; an empty map or false is an explicit value.
 */
public class MasterDataSchemaFeatures
{
    private final Map<String, Map<String, String>> propertyTypeMetaData = new HashMap<>();

    private final MultiKeyMap<String, Map<String, String>> entityTypeMetaData = new MultiKeyMap<>();

    private final Map<String, Boolean> multiValue = new HashMap<>();

    private final MultiKeyMap<String, Boolean> unique = new MultiKeyMap<>();

    public Map<String, Map<String, String>> getPropertyTypeMetaData()
    {
        return propertyTypeMetaData;
    }

    public MultiKeyMap<String, Map<String, String>> getEntityTypeMetaData()
    {
        return entityTypeMetaData;
    }

    public Map<String, Boolean> getMultiValue()
    {
        return multiValue;
    }

    public MultiKeyMap<String, Boolean> getUnique()
    {
        return unique;
    }
}
