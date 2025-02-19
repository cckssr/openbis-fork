/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.asapi.v3.dto.common.property;

import ch.systemsx.cisd.common.exceptions.UserFailureException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PropertiesDeserializer extends JsonDeserializer<Serializable>
{
    @Override
    public Serializable deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
    {
        JsonNode node = p.readValueAsTree();
        if(node.isArray()) {
            ArrayList<String> list = new ArrayList<>();
            node.forEach(value -> list.add(value.asText()));
            return list.toArray(new String[0]);
        } else if(node.isObject()) {
            return readValue(node.toString(), HashMap.class);
        } else {
            return node.asText();
        }
    }

    public static String getPropertyAsString(Serializable propertyValue) {
        if(propertyValue == null) {
            return null;
        } else {
            if(propertyValue.getClass().isArray()) {
                Serializable[] values = (Serializable[]) propertyValue;
                StringBuilder builder = new StringBuilder("[");
                for(Serializable value : values) {
                    if(builder.length() > 1) {
                        builder.append(", ");
                    }
                    builder.append(value);
                }
                builder.append("]");
                return builder.toString();
            } else {
                return (String) propertyValue;
            }
        }
    }

    public static <T> T readValue(String val, Class<T> clazz)
    {
        try
        {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(new ByteArrayInputStream(val.getBytes()),
                    clazz);
        } catch (JsonMappingException mappingException)
        {
            throw new UserFailureException(mappingException.toString(), mappingException);
        } catch (Exception e)
        {
            throw new UserFailureException("Could not read the parameters!", e);
        }
    }


    public static String writeValue(Object value)
    {
        try
        {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(value);
        } catch (JsonMappingException mappingException)
        {
            throw new UserFailureException(mappingException.toString(), mappingException);
        } catch (Exception e)
        {
            throw new UserFailureException("Could not read the parameters!", e);
        }
    }

}
