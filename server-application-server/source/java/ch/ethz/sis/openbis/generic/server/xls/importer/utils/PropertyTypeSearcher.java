/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.xls.importer.utils;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.LongDateFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.ShortDateFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.server.sharedapi.v3.json.GenericObjectMapper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationHelper;
import ch.ethz.sis.openbis.generic.server.xls.importer.helper.semanticannotation.SemanticAnnotationType;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.DateUtil;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertyTypeSearcher
{

    public static final SimpleDateFormat timestampFormatter = new SimpleDateFormat(new LongDateFormat().getFormat());

    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(new ShortDateFormat().getFormat());

    public static final String SAMPLE_DATA_TYPE_PREFIX = "SAMPLE";

    public static final String SAMPLE_DATA_TYPE_MANDATORY_TYPE = ":";

    public static final String VARIABLE_PREFIX = "$";

    public static final String PROPERTY_VALUE_SEPARATOR = ",";

    public static final String SAMPLE_PROPERTY_VALUE_SEPARATOR = "\n";

    /*
     * This regex improves over the original "\n" by supporting also "," and mixing both of them
     */
    public static final String SAMPLE_PROPERTY_VALUE_SEPARATOR_REGEX = ",\\s*\\n|\\n\\s*,|\\s*\\n\\s*|\\s*,\\s*";

    public static final Pattern SAMPLE_PROPERTY_VALUE_SEPARATOR_PATTERN = Pattern.compile(SAMPLE_PROPERTY_VALUE_SEPARATOR_REGEX, Pattern.MULTILINE);

    public static String[] parseSamplePropertyValues(String values) {
        final Matcher matcher = SAMPLE_PROPERTY_VALUE_SEPARATOR_PATTERN.matcher(values);
        return matcher.replaceAll(SAMPLE_PROPERTY_VALUE_SEPARATOR).split(SAMPLE_PROPERTY_VALUE_SEPARATOR);
    }

    private static final ObjectMapper OBJECT_MAPPER = new GenericObjectMapper();

    private Map<String, PropertyType> code2PropertyType;

    private Map<String, PropertyType> label2PropertyType;

    private final SemanticAnnotationHelper annotationCache;

    public PropertyTypeSearcher(List<PropertyAssignment> assignment, SemanticAnnotationHelper annotationCache)
    {
        this.code2PropertyType = new HashMap<>();
        this.label2PropertyType = new HashMap<>();
        this.annotationCache = annotationCache;

        for (PropertyAssignment propertyAssignment : assignment)
        {
            PropertyType propertyType = propertyAssignment.getPropertyType();
            code2PropertyType.put(propertyType.getCode(), propertyType);
            if (label2PropertyType.containsKey(propertyType.getLabel()) == false) // If a label already exists, the next one is ignored
            {
                label2PropertyType.put(propertyType.getLabel(), propertyType);
            }
        }
    }

    public Map<String, PropertyType> getCode2PropertyType() {
        return code2PropertyType;
    }

    public Map<String, PropertyType> getLabel2PropertyType() {
        return label2PropertyType;
    }

    public Set<String> getSemanticallyAnnotatedPropertyTypes() {
        return annotationCache.getCachedPropertyTypes();
    }

    public PropertyType findPropertyType(EntityTypePermId typePermId, String code)
    {
        if (code2PropertyType.containsKey(code))
        {
            return code2PropertyType.get(code);
        }
        if (label2PropertyType.containsKey(code))
        {
            return label2PropertyType.get(code);
        }
        SemanticAnnotation annotation = annotationCache.getCachedSemanticAnnotation(
                SemanticAnnotationType.PropertyType, null, code);
        if(annotation != null) {
            return annotation.getPropertyType();
        }
        annotation = annotationCache.getCachedSemanticAnnotation(
                SemanticAnnotationType.PropertyAssignment, typePermId, code);
        if(annotation != null) {
            return annotation.getPropertyAssignment().getPropertyType();
        }

        throw new UserFailureException("Can't find property with code or label " + code);
    }

    public static Serializable getPropertyValue(PropertyType propertyType, String value)
    {
        if(propertyType.isMultiValue()) {
            if(value == null || value.trim().isEmpty()){
                return parseSinglePropertyValue(propertyType, value);
            }
            if(propertyType.getDataType() == DataType.JSON) {
                List<Serializable> results = new ArrayList<>();
                try {
                    Object[] objects = OBJECT_MAPPER.readValue(
                                            new ByteArrayInputStream(("["+value+"]").getBytes()),
                                            Object[].class);
                    for(Object o : objects) {
                        results.add(OBJECT_MAPPER.writeValueAsString(o));
                    }
                    return results.toArray(Serializable[]::new);
                } catch (Exception e) {
                    throw new UserFailureException(String.format("Multi-value json property '%s' could not be imported!", value));
                }
            } else if (propertyType.getDataType() == DataType.SAMPLE) {
                return parseSamplePropertyValues(value);
            }

            return Stream.of(value.split(PROPERTY_VALUE_SEPARATOR))
                    .map(String::trim)
                    .map(x -> parseSinglePropertyValue(propertyType, x))
                    .toArray(Serializable[]::new);
        } else {
            return parseSinglePropertyValue(propertyType, value);
        }
    }

    private static Serializable parseSinglePropertyValue(PropertyType propertyType, String value)
    {
        if (propertyType.getDataType() == DataType.CONTROLLEDVOCABULARY)
        {
            // First we try to code match, codes have priority
            for (VocabularyTerm term : propertyType.getVocabulary().getTerms())
            {
                if (term.getCode().equals(value))
                {
                    return term.getCode();
                }
            }
            // If we can't match by code we try to match by label
            for (VocabularyTerm term : propertyType.getVocabulary().getTerms())
            {
                if (term.getLabel() != null && term.getLabel().equals(value))
                {
                    return term.getCode();
                }
            }
        } else if (propertyType.getDataType() == DataType.TIMESTAMP) { // Converts native excel timestamps
            if (value != null && isDouble(value)) {
                value = timestampFormatter.format(DateUtil.getJavaDate(Double.parseDouble(value)));
            }
        } else if (propertyType.getDataType() == DataType.DATE) { // Converts native excel dates
            if (value != null && isDouble(value)) {
                value = dateFormatter.format(DateUtil.getJavaDate(Double.parseDouble(value)));
            }
        }
        return value;
    }

    private static boolean isDouble(String string)
    {
        try
        {
            Double.parseDouble(string);
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        return true;
    }

}
