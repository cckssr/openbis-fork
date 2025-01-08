/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.exporter;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.ObjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.*;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetImage;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetPreview;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetPropertyConfig;
import ch.ethz.sis.openbis.generic.server.sharedapi.v3.json.GenericObjectMapper;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.spring.ExposablePropertyPlaceholderConfigurer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

final class ExportImagingUtils {

    private static final String IMAGING_SAMPLE = "IMAGING_SAMPLE";
    private static final String IMAGING_COLLECTION = "IMAGING_COLLECTION";
    private static final String IMAGING_DATA_CONFIG = "IMAGING_DATA_CONFIG";

    private static final List<String> IMAGING_TYPES = Arrays.asList("IMAGING_DATA", "USER_DEFINED_IMAGING_DATA",
            IMAGING_SAMPLE, IMAGING_COLLECTION);
    private static final List<String> IMAGING_RESTRICTED_PROPERTY_NAMES = Arrays.asList(IMAGING_DATA_CONFIG, "DEFAULT_DATASET_VIEW", "DEFAULT_COLLECTION_VIEW");


    private static final String PAGE_BREAK = "<div class=\"pagebreak\"> </div>";
    private static final String IMAGE_FORM = "<figure class=\"image\"><img src=\"data:image/png;base64,%s\"></figure>";
    private static final String DATA_TAG_START = "<DATA>";
    private static final int DATA_TAG_START_LENGTH = DATA_TAG_START.length();
    private static final String DATA_TAG_END = "</DATA>";
    private static final int DATA_TAG_END_LENGTH = DATA_TAG_END.length();

    private ExportImagingUtils() {}

    static boolean isImagingType(IEntityType entityType)
    {
        return entityType != null && entityType.isManagedInternally() && IMAGING_TYPES.contains(entityType.getCode().toUpperCase());
    }

    static boolean isImagingInternalProperty(String propertyName)
    {
        return propertyName != null && IMAGING_RESTRICTED_PROPERTY_NAMES.contains(propertyName.toUpperCase());
    }

    static void buildImagingData(DocumentBuilder documentBuilder, IEntityType typeObj, ICodeHolder propertiesHolder, ExposablePropertyPlaceholderConfigurer configurer)
            throws IOException
    {
        if(!isImagingType(typeObj))
        {
            return;
        }
        boolean dataSetOnly = false;
        List<DataSet> dataSets = new ArrayList<>();
        switch (typeObj.getCode()) {
            case IMAGING_SAMPLE:
                Sample imagingSample = (Sample) propertiesHolder;
                dataSets.addAll(imagingSample.getDataSets());
                break;
            case IMAGING_COLLECTION:
                Experiment imagingCollection = (Experiment) propertiesHolder;
                dataSets.addAll(imagingCollection.getDataSets());
                break;
            default:
                dataSets.add((DataSet) propertiesHolder);
                dataSetOnly = true;
                break;
        }

        buildImagingDataSets(configurer, documentBuilder, dataSets, dataSetOnly);
    }

    private static void buildImagingDataSets(ExposablePropertyPlaceholderConfigurer configurer,
            DocumentBuilder documentBuilder, List<DataSet> inputDataSets, boolean dataSetOnly)
            throws IOException
    {
        List<Map.Entry<DataSet, ImagingDataSetPropertyConfig>> result = new ArrayList<>();
        List<Map.Entry<DataSet, ImagingDataSetPropertyConfig>> allDataSets = new ArrayList<>();
        for(DataSet dataSet : inputDataSets)
        {
            String jsonConfig = dataSet.getJsonProperty(IMAGING_DATA_CONFIG);
            if(jsonConfig != null && !jsonConfig.isEmpty())
            {
                ImagingDataSetPropertyConfig config = readConfig(jsonConfig);
                allDataSets.add(Map.entry(dataSet, config));

                List<ImagingDataSetImage> images = config.getImages();
                if(images != null && !images.isEmpty())
                {
                    for (ImagingDataSetImage image : config.getImages())
                    {
                        List<ImagingDataSetPreview> previews = image.getPreviews();
                        if(previews != null && !previews.isEmpty())
                        {
                            for (ImagingDataSetPreview preview : image.getPreviews())
                            {
                                if (preview.isShow())
                                {
                                    result.add(Map.entry(dataSet, config));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        boolean useShowFlag = true;
        if(result.isEmpty() || dataSetOnly)
        {
            useShowFlag = false;
            result = allDataSets;
        }

        documentBuilder.addHeader("Images", 2);
        for(Map.Entry<DataSet, ImagingDataSetPropertyConfig> pair : result)
        {
            buildImagingDataSet(configurer, documentBuilder, pair.getKey(), pair.getValue(), dataSetOnly, useShowFlag);
        }
    }

    private static void buildImagingDataSet(ExposablePropertyPlaceholderConfigurer configurer,
            DocumentBuilder documentBuilder, DataSet dataSet, ImagingDataSetPropertyConfig config,
            boolean dataSetOnly, boolean useShowFlag)
            throws IOException
    {
        String name = dataSet.getStringProperty("NAME");
        if(name == null)
        {
            documentBuilder.addHeader(String.format("DataSet %s", dataSet.getPermId().getPermId()), 3);
        }
        else
        {
            documentBuilder.addHeader(String.format("DataSet %s (%s)", name, dataSet.getPermId().getPermId()), 3);
        }
        List<ImagingDataSetImage> images = config.getImages();
        if(images != null && !images.isEmpty())
        {
            for(int i=0; i<images.size(); i++)
            {
                documentBuilder.addHeader(String.format("Image %d", i+1), 4);
                ImagingDataSetImage image = images.get(i);
                List<ImagingDataSetPreview> previews = image.getPreviews();
                if(previews != null && !previews.isEmpty())
                {
                    for(ImagingDataSetPreview preview : previews)
                    {
                        if(useShowFlag && !preview.isShow())
                        {
                            continue;
                        }
                        documentBuilder.addParagraph(String.format(IMAGE_FORM, preview.getBytes()));
                        Map<String, Serializable> previewConfig = preview.getConfig();
                        if(previewConfig != null && !previewConfig.isEmpty())
                        {
                            documentBuilder.addHeader("Preview parameters:", 5);
                            List<List<String>> values = new ArrayList<>();
                            for(Map.Entry<String, Serializable> entry : previewConfig.entrySet())
                            {
                                values.add(Arrays.asList(entry.getKey(), getValueAsString(entry.getValue())));
                            }
                            documentBuilder.addTable(Arrays.asList("Parameter", "Value"), values);

                        }
                        String comment = preview.getComment();
                        if(comment != null && !comment.trim().isEmpty())
                        {
                            documentBuilder.addProperty("Comment:", comment);
                        }
                        Map<String, Serializable> metaData = preview.getMetadata();
                        if(metaData != null && !metaData.isEmpty())
                        {
                            documentBuilder.addHeader("Metadata parameters:", 5);
                            for(Map.Entry<String, Serializable> entry : metaData.entrySet())
                            {
                                documentBuilder.addProperty(entry.getKey(), getValueAsString(entry.getValue()));
                            }
                        }

                    }
                }
            }
        }
        if(!dataSetOnly)
        {
            buildPropertiesInfo(documentBuilder, dataSet, configurer);
            buildIdentificationInfo(documentBuilder, dataSet);
            documentBuilder.addParagraph(PAGE_BREAK);
        }

    }

    private static String getValueAsString(Serializable propertyValue)
    {
        if(propertyValue == null) {
            return "";
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

    private static void buildPropertiesInfo(DocumentBuilder documentBuilder, DataSet dataSet, ExposablePropertyPlaceholderConfigurer configurer)
            throws IOException
    {
        documentBuilder.addHeader("Properties", 3);

        for(PropertyAssignment assignment : dataSet.getType().getPropertyAssignments())
        {
            final PropertyType propertyType = assignment.getPropertyType();
            final String propertyTypeCode = propertyType.getCode();
            final Serializable rawPropertyValue = dataSet.getProperties().get(propertyTypeCode);

            if(IMAGING_RESTRICTED_PROPERTY_NAMES.contains(propertyType.getCode().toUpperCase()))
            {
                continue;
            }
            final String initialPropertyValue = String.valueOf(rawPropertyValue instanceof Sample
                    ? ((Sample) rawPropertyValue).getIdentifier().getIdentifier()
                    : getValueAsString(rawPropertyValue));
            String propertyValue;

            if (propertyType.getDataType() == DataType.SAMPLE)
            {
                if (rawPropertyValue instanceof Sample[])
                {
                    propertyValue = Arrays.stream(((Sample[]) rawPropertyValue)).map(sample -> sample.getIdentifier().getIdentifier())
                            .collect(Collectors.joining(", "));
                } else if (rawPropertyValue instanceof Sample)
                {
                    propertyValue = ((Sample) rawPropertyValue).getIdentifier().getIdentifier();
                } else
                {
                    throw new IllegalArgumentException("Sample property value is not of type Sample or Sample[].");
                }
            } else if (propertyType.getDataType() == DataType.MULTILINE_VARCHAR &&
                    Objects.equals(propertyType.getMetaData().get("custom_widget"), "Word Processor"))
            {

                propertyValue = ExportPropertiesUtils.encodeImages(configurer, initialPropertyValue);
            } else if (propertyType.getDataType() == DataType.XML
                    && Objects.equals(propertyType.getMetaData().get("custom_widget"), "Spreadsheet")
                    && initialPropertyValue.toUpperCase().startsWith(DATA_TAG_START) && initialPropertyValue.toUpperCase()
                    .endsWith(DATA_TAG_END))
            {
                final String subString = initialPropertyValue.substring(DATA_TAG_START_LENGTH,
                        initialPropertyValue.length() - DATA_TAG_END_LENGTH);
                final String decodedString = new String(Base64.getDecoder().decode(subString), StandardCharsets.UTF_8);
                final ObjectMapper objectMapper = new ObjectMapper();
                final JsonNode jsonNode = objectMapper.readTree(decodedString);
                propertyValue = ExportPDFUtils.convertJsonToHtml(jsonNode);
            } else if(propertyType.getDataType() == DataType.CONTROLLEDVOCABULARY && rawPropertyValue != null)
            {
                Map<String, String> terms = propertyType.getVocabulary().getTerms().stream()
                        .collect(Collectors.toMap(VocabularyTerm::getCode,
                                x -> x.getLabel() != null ? x.getLabel() : x.getCode()));
                if(rawPropertyValue.getClass().isArray()) {
                    Serializable[] values = (Serializable[]) rawPropertyValue;
                    StringBuilder builder = new StringBuilder("[");
                    for(Serializable value : values) {
                        if(builder.length() > 1) {
                            builder.append(", ");
                        }
                        builder.append(terms.get(value.toString().toUpperCase()));
                    }
                    builder.append("]");
                    propertyValue =  builder.toString();
                } else {
                    propertyValue = terms.get(rawPropertyValue.toString().toUpperCase());
                }
            } else
            {
                propertyValue = initialPropertyValue;
            }

            if (!Objects.equals(propertyValue, "\uFFFD(undefined)"))
            {
                documentBuilder.addProperty(propertyType.getLabel(), propertyValue);
            }
        }

    }

    private static void buildIdentificationInfo(DocumentBuilder documentBuilder, ICodeHolder entityObj)
    {
        documentBuilder.addHeader("Identification Info", 3);

        if (entityObj instanceof Experiment)
        {
            documentBuilder.addProperty("Kind", "Experiment");
        } else if (entityObj instanceof Sample)
        {
            documentBuilder.addProperty("Kind", "Sample");
        } else if (entityObj instanceof DataSet)
        {
            documentBuilder.addProperty("Kind", "DataSet");
        }

            documentBuilder.addProperty("Code", entityObj.getCode());

        if (entityObj instanceof IPermIdHolder)
        {
            documentBuilder.addProperty("Perm ID", ((IPermIdHolder) entityObj).getPermId().toString());
        }

        if (entityObj instanceof IIdentifierHolder)
        {
            final ObjectIdentifier identifier = ((IIdentifierHolder) entityObj).getIdentifier();
            if (identifier != null)
            {
                documentBuilder.addProperty("Identifier", identifier.getIdentifier());
            }
        }

        // Registration / Modification

        if (entityObj instanceof IRegistratorHolder)
        {
            final Person registrator = ((IRegistratorHolder) entityObj).getRegistrator();
            if (registrator != null)
            {
                documentBuilder.addProperty("Registrator", registrator.getUserId());
            }
        }

        if (entityObj instanceof IRegistrationDateHolder)
        {
            final Date registrationDate = ((IRegistrationDateHolder) entityObj).getRegistrationDate();
            if (registrationDate != null)
            {
                documentBuilder.addProperty("Registration Date", String.valueOf(registrationDate));
            }
        }

        if (entityObj instanceof IModifierHolder)
        {
            final Person modifier = ((IModifierHolder) entityObj).getModifier();
            if (modifier != null)
            {
                documentBuilder.addProperty("Modifier", modifier.getUserId());
            }
        }

        if (entityObj instanceof IModificationDateHolder)
        {
            final Date modificationDate = ((IModificationDateHolder) entityObj).getModificationDate();
            if (modificationDate != null)
            {
                documentBuilder.addProperty("Modification Date", String.valueOf(modificationDate));
            }
        }
    }


    private static ImagingDataSetPropertyConfig readConfig(String jsonConfig)
    {
        try
        {
            ObjectMapper objectMapper = new GenericObjectMapper();
            return objectMapper.readValue(new ByteArrayInputStream(jsonConfig.getBytes()),
                    ImagingDataSetPropertyConfig.class);
        } catch (JsonMappingException mappingException)
        {
            throw new UserFailureException(mappingException.toString(), mappingException);
        } catch (Exception e)
        {
            throw new UserFailureException("Could not read the parameters!", e);
        }
    }




}
