/*
 *  Copyright ETH 2023 - 2024 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.server.dss.plugins.imaging;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.*;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.service.CustomDSSServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.service.id.ICustomDSSServiceId;
import ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.adaptor.IImagingDataSetAdaptor;
import ch.ethz.sis.openbis.generic.dssapi.v3.plugin.service.ICustomDSSServiceExecutor;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.reflection.ClassUtils;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContent;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContentNode;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.dto.OpenBISSessionHolder;
import ch.systemsx.cisd.common.logging.LogFactory;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ImagingService implements ICustomDSSServiceExecutor
{

    private final Properties properties;

    private IHierarchicalContentProvider contentProvider;

    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, ImagingService.class);

    static final String IMAGING_CONFIG_PROPERTY_NAME = "IMAGING_DATA_CONFIG";

    static final String DEFAULT_DATASET_VIEW_PROPERTY_NAME = "DEFAULT_DATASET_VIEW";

    static final String USER_DEFINED_IMAGING_DATA_TYPE = "USER_DEFINED_IMAGING_DATA";

    /** Buffer size for the buffer stream for Base64 encoding. Should be a multiple of 3. */
    static final int BUFFER_SIZE = 3 * 1024;

    static final String PREVIEW_TOTAL_COUNT = "preview-total-count";

    public ImagingService(Properties properties)
    {
        this.properties = properties;
    }

    @Override
    public Serializable executeService(String sessionToken, ICustomDSSServiceId serviceId,
            CustomDSSServiceExecutionOptions options)
    {
        operationLog.info("Executing imaging service:" + serviceId);
        ImagingDataContainer data = getDataFromParams(options.getParameters());
        try
        {
            if (data.getType().equalsIgnoreCase("preview"))
            {
                return processPreviewFlow(sessionToken, (ImagingPreviewContainer) data);
            } else if (data.getType().equalsIgnoreCase("export"))
            {
                return processExportFlow(sessionToken, (ImagingExportContainer) data);
            } else if (data.getType().equalsIgnoreCase("multi-export"))
            {
                return processMultiExportFlow(sessionToken, (ImagingMultiExportContainer) data);
            } else
            {
                throw new UserFailureException("Unknown request type!");
            }
        } catch (Exception e)
        {
            data.setError(e.toString());
        }
        return data;
    }

    private IImagingDataSetAdaptor getAdaptor(ImagingDataSetImage image)
    {
        final String adaptorName = image.getConfig().getAdaptor();
        if (adaptorName == null || adaptorName.trim().isEmpty())
        {
            throw new UserFailureException("Adaptor name is missing from the config!");
        }
        try
        {
            return ClassUtils.create(IImagingDataSetAdaptor.class, adaptorName, properties);
        } catch (Exception e)
        {
            operationLog.error("Failed to load adapter:" + adaptorName, e);
            throw new UserFailureException("Could not load adapter: " + adaptorName, e);
        }
    }

    private File getRootFile(String sessionToken, DataSet dataSet)
    {
        IHierarchicalContent content =
                getHierarchicalContentProvider(sessionToken).asContent(
                        dataSet.getPermId().getPermId());
        IHierarchicalContentNode root = content.getRootNode();
        return root.getFile();
    }

    private void processUserDefinedImagingData(String sessionToken, DataSet dataSet)
    {
        final List<String> supportedFormats = Arrays.asList(".png", ".jpg");
        File rootFile = getRootFile(sessionToken, dataSet);
        LinkedList<File> filesToProces = new LinkedList<>();
        List<File> imageFiles = new ArrayList<>();
        filesToProces.add(rootFile);
        while (!filesToProces.isEmpty())
        {
            File top = filesToProces.poll();
            if (top.isDirectory())
            {
                for (File f : top.listFiles())
                {
                    filesToProces.offer(f);
                }
            } else
            {
                String fileName = top.getName().toLowerCase();
                int pos = fileName.lastIndexOf(".");
                if (pos == -1)
                    continue;
                String suffix = fileName.substring(pos + 1);
                for (String supportedFormat : supportedFormats)
                {
                    if (fileName.endsWith(supportedFormat))
                    {
                        imageFiles.add(top);
                        break;
                    }
                }
            }
        }
        if (!imageFiles.isEmpty())
        {
            List<ImagingDataSetPreview> previews = new ArrayList<>();
            for (int i = 0; i < imageFiles.size(); i++)
            {
                ImagingDataSetPreview preview = new ImagingDataSetPreview();
                preview.setIndex(i);
                File imageFile = imageFiles.get(i);
                preview.setFormat("PNG");
                encodeImageToPreview(imageFile, preview);
                preview.setMetadata(Map.of());
                preview.setConfig(Map.of());
                preview.setComment("");
                preview.setTags(new String[0]);
                previews.add(preview);
            }

            ImagingDataSetPropertyConfig config =
                    Util.readConfig(dataSet.getJsonProperty(IMAGING_CONFIG_PROPERTY_NAME),
                            ImagingDataSetPropertyConfig.class);
            config.getImages().get(0).setPreviews(previews);
            config.getImages().get(0).getMetadata().put("GENERATE", "false");

            DataSetUpdate update = new DataSetUpdate();
            update.setDataSetId(dataSet.getPermId());
            update.setProperty(IMAGING_CONFIG_PROPERTY_NAME, Util.convertConfigToJson(config));
            update.getMetaData().put(PREVIEW_TOTAL_COUNT, Integer.toString(previews.size()));
            getApplicationServerApi().updateDataSets(sessionToken, Arrays.asList(update));
        }
    }

    private void encodeImageToPreview(File file, ImagingDataSetPreview preview)
    {
        try
        {
            final FileInputStream fileInputStream = new FileInputStream(file);
            try (
                    final BufferedInputStream in = new BufferedInputStream(fileInputStream,
                            BUFFER_SIZE);
            )
            {
                final ImageInputStream is = ImageIO.createImageInputStream(in);
                BufferedImage image = ImageIO.read(is);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                preview.setBytes(Base64.getEncoder().encodeToString(baos.toByteArray()));
                preview.setWidth(image.getWidth());
                preview.setHeight(image.getHeight());
            }

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private ImagingPreviewContainer processPreviewFlow(String sessionToken,
            ImagingPreviewContainer data)
    {
        DataSet dataSet = getDataSet(sessionToken, data.getPermId());

        String jsonConfig = dataSet.getJsonProperty(IMAGING_CONFIG_PROPERTY_NAME);
        if (dataSet.getType().getCode().equals(USER_DEFINED_IMAGING_DATA_TYPE))
        {
            processUserDefinedImagingData(sessionToken, dataSet);
            return data;
        }
        if (jsonConfig == null || jsonConfig.isEmpty() || jsonConfig.equals("{}"))
        {
            throw new UserFailureException("Imaging config is empty!");
        } else
        {
            ImagingDataSetPropertyConfig config =
                    Util.readConfig(dataSet.getJsonProperty(IMAGING_CONFIG_PROPERTY_NAME),
                            ImagingDataSetPropertyConfig.class);

            int index = data.getIndex();
            if (config.getImages().size() <= index)
            {
                throw new UserFailureException("There is no image with index:" + index);
            }
            ImagingDataSetImage image = config.getImages().get(index);

            IImagingDataSetAdaptor adaptor = getAdaptor(image);
            File rootFile = getRootFile(sessionToken, dataSet);
            String format = data.getPreview().getFormat();

            if (format == null || format.trim().isEmpty())
            {
                throw new UserFailureException("Format can not be empty!");
            }

            ImagingServiceContext context =
                    new ImagingServiceContext(sessionToken, getApplicationServerApi(),
                            getDataStoreServerApi());

            adaptor.computePreview(context, rootFile, image, data.getPreview());
            return data;
        }
    }

    private Serializable processExportFlow(String sessionToken, ImagingExportContainer data)
    {
        // Get all parameters
        final DataSet dataSet = getDataSet(sessionToken, data.getPermId());
        final ImagingDataSetPropertyConfig config =
                Util.readConfig(dataSet.getJsonProperty(IMAGING_CONFIG_PROPERTY_NAME),
                        ImagingDataSetPropertyConfig.class);

        final File rootFile = getRootFile(sessionToken, dataSet);
        final int index = data.getIndex();
        if (config.getImages().size() <= index)
        {
            throw new UserFailureException("There is no image with index:" + index);
        }

        final ImagingDataSetImage image = config.getImages().get(index);

        ImagingDataSetExportConfig exportConfig = data.getExport().getConfig();
        Validator.validateExportConfig(exportConfig);

        ImagingArchiver archiver;
        // Prepare archiver
        try
        {
            archiver = new ImagingArchiver(sessionToken,
                    exportConfig.getArchiveFormat());
        } catch (IOException exception)
        {
            throw new UserFailureException("Could not export data!", exception);
        }

        // For each export type, perform adequate action
        for (ImagingExportIncludeOptions exportType : exportConfig.getInclude())
        {
            if (exportType == ImagingExportIncludeOptions.IMAGE)
            {
                ImagingServiceContext context =
                        new ImagingServiceContext(sessionToken, getApplicationServerApi(),
                                getDataStoreServerApi());
                IImagingDataSetAdaptor adaptor = getAdaptor(image);
                archiveImage(context, adaptor, image, index, exportConfig, rootFile, "", archiver,
                        dataSet.getPermId().getPermId());
            } else if (exportType == ImagingExportIncludeOptions.RAW_DATA)
            {
                archiveRawData(rootFile, "", archiver, dataSet);
            } else
            {
                throw new UserFailureException("Unknown export type!");
            }

        }

        data.setUrl(archiver.build());
        return data;
    }

    private Serializable processMultiExportFlow(String sessionToken,
            ImagingMultiExportContainer data)
    {
        ImagingArchiver archiver = null;
        if (data.getExports().isEmpty())
        {
            throw new UserFailureException("There are no exports defined!");
        }

        Map<IDataSetId, DataSet> dataSetsMap =
                getDataSets(sessionToken, data.getExports().stream().map(
                        ImagingDataSetMultiExport::getPermId).collect(
                        Collectors.toList()));
        Map<String, List<ImagingDataSetMultiExport>> sortedExports = new HashMap<>();

        for (ImagingDataSetMultiExport export : data.getExports())
        {
            sortedExports.putIfAbsent(export.getPermId(), new ArrayList<>());
            sortedExports.get(export.getPermId()).add(export);

        }

        for (String permId : sortedExports.keySet())
        {
            DataSet dataSet = dataSetsMap.get(new DataSetPermId(permId));
            if (dataSet == null)
            {
                throw new UserFailureException("Could not find data set: " + permId);
            }

            ImagingDataSetPropertyConfig config =
                    Util.readConfig(dataSet.getJsonProperty(IMAGING_CONFIG_PROPERTY_NAME),
                            ImagingDataSetPropertyConfig.class);

            File rootFile = getRootFile(sessionToken, dataSet);
            Map<Integer, Map<String, Object>> imageToMetaDataMap = new HashMap<>();
            for (ImagingDataSetMultiExport export : sortedExports.get(permId))
            {
                final int imageIndex = export.getImageIndex();
                if (config.getImages().size() <= imageIndex)
                {
                    throw new UserFailureException("There is no image with index: " + imageIndex);
                }

                ImagingDataSetImage image = config.getImages().get(imageIndex);

                final int previewIndex = export.getPreviewIndex();
                if (image.getPreviews().size() <= previewIndex)
                {
                    throw new UserFailureException("There is no preview with index: " + previewIndex);
                }

                ImagingDataSetPreview preview = image.getPreviews().get(previewIndex);

                ImagingDataSetExportConfig exportConfig = export.getConfig();
                Validator.validateExportConfig(exportConfig);


                try
                {
                    if (archiver == null)
                    {
                        final String archiveFormat = exportConfig.getArchiveFormat();
                        archiver = new ImagingArchiver(sessionToken, archiveFormat);
                    }
                } catch (IOException exception)
                {
                    throw new UserFailureException("Could not export data!", exception);
                }

                // For each export type, perform adequate action
                for (ImagingExportIncludeOptions exportType : exportConfig.getInclude())
                {
                    if (exportType == ImagingExportIncludeOptions.IMAGE)
                    {
                        ImagingServiceContext context =
                                new ImagingServiceContext(sessionToken, getApplicationServerApi(),
                                        getDataStoreServerApi());
                        IImagingDataSetAdaptor adaptor = getAdaptor(image);
                        archivePreview(context, adaptor, image, imageIndex, previewIndex,
                                exportConfig, rootFile, export.getPermId(), archiver,
                                dataSet.getPermId().getPermId());

                        Map<String, Object> previewMetaData = extractMetaData(preview);
                        if(!previewMetaData.isEmpty())
                        {
                            imageToMetaDataMap.putIfAbsent(imageIndex, new HashMap<>());
                            imageToMetaDataMap.get(imageIndex).put(String.format("preview_%d", previewIndex), previewMetaData);
                        }
                    } else if (exportType == ImagingExportIncludeOptions.RAW_DATA)
                    {
                        archiveRawData(rootFile, export.getPermId(), archiver, dataSet);
                    } else
                    {
                        throw new UserFailureException("Unknown export type!");
                    }
                }
            }

            if (!imageToMetaDataMap.isEmpty())
            {
                for(Integer imageIdx : imageToMetaDataMap.keySet())
                {
                    Map<String, Object> metaData = imageToMetaDataMap.get(imageIdx);
                    byte[] json = Util.mapToJson(metaData).getBytes(StandardCharsets.UTF_8);
                    archiver.addToArchive(permId,
                            String.format("%s_metadata_image_%d.txt", permId, imageIdx), json);
                }
            }

        }

        assert archiver != null;
        data.setUrl(archiver.build());
        return data;
    }



    private void archiveRawData(File rootFile, String rootFolderName,
            ImagingArchiver archiver, DataSet dataSet)
    {
        //Add dataset files to archive
        archiver.addToArchive(rootFolderName, rootFile);
        //Add dataset properties to archive
        Map<String, Serializable> properties = dataSet.getProperties();
        properties.remove(IMAGING_CONFIG_PROPERTY_NAME);
        properties.remove(DEFAULT_DATASET_VIEW_PROPERTY_NAME);
        if (!properties.isEmpty())
        {
            byte[] json = Util.mapToJson(properties).getBytes(StandardCharsets.UTF_8);
            archiver.addToArchive(rootFolderName,
                    String.format("%s_properties.txt", dataSet.getPermId().getPermId()), json);
        }
    }

    private void archiveImage(ImagingServiceContext context, IImagingDataSetAdaptor adaptor,
            ImagingDataSetImage image, int imageIdx, ImagingDataSetExportConfig exportConfig,
            File rootFile, String rootFolderName, ImagingArchiver archiver, String permId)
    {

        int previewIdx = 0;
        Map<String, Object> metaData = new HashMap<>();

        for (ImagingDataSetPreview preview : image.getPreviews())
        {
            archivePreview(context, adaptor, image, imageIdx, previewIdx, exportConfig,
                    rootFile, rootFolderName, archiver, permId);
            Map<String, Object> previewMetaData = extractMetaData(preview);
            if (!previewMetaData.isEmpty())
            {
                metaData.put(String.format("preview_%d", previewIdx), previewMetaData);
            }
            previewIdx++;
        }

        if (!metaData.isEmpty())
        {
            byte[] json = Util.mapToJson(metaData).getBytes(StandardCharsets.UTF_8);
            archiver.addToArchive(rootFolderName,
                    String.format("%s_metadata_image_%d.txt", permId, imageIdx), json);
        }
    }

    private void archivePreview(ImagingServiceContext context, IImagingDataSetAdaptor adaptor,
            ImagingDataSetImage image, int imageIdx, int previewIdx,
            ImagingDataSetExportConfig exportConfig,
            File rootFile, String rootFolderName, ImagingArchiver archiver, String permId)
    {

        String imageFormat = exportConfig.getImageFormat();
        ImagingDataSetPreview preview = image.getPreviews().get(previewIdx);

        String format = imageFormat;
        if (imageFormat.equalsIgnoreCase("original"))
        {
            format = preview.getFormat();
        }

        // check for a new "blank" images
        if (preview.getBytes() != null && !preview.getBytes().trim().isEmpty())
        {
            String imgString;
            Map<String, Serializable> imageConfig = image.getImageConfig();
            Map<String, Serializable> previewConfig = preview.getConfig();

            if ((imageConfig != null && !imageConfig.isEmpty())
                    || (previewConfig != null && !previewConfig.isEmpty()))
            {
                previewConfig.put("resolution", exportConfig.getResolution());
                Map<String, Serializable> img = adaptor.process(context,
                        rootFile, format, imageConfig, image.getMetadata(), previewConfig,
                        preview.getMetadata());
                imgString = img.get("bytes").toString();
            } else
            {
                // uploaded image case
                imgString = preview.getBytes();
                format = preview.getFormat();
            }
            byte[] decoded = Base64.getDecoder().decode(imgString);
            String fileName =
                    String.format("%s_image_%d_preview_%s.%s", permId, imageIdx, previewIdx,
                            format);

            archiver.addToArchive(rootFolderName, fileName, decoded);
        }
    }

    private IHierarchicalContentProvider getHierarchicalContentProvider(String sessionToken)
    {
        if (contentProvider == null)
        {
            contentProvider = ServiceProvider.getHierarchicalContentProvider();
        }
        OpenBISSessionHolder sessionTokenHolder = new OpenBISSessionHolder();
        sessionTokenHolder.setSessionToken(sessionToken);
        return contentProvider.cloneFor(sessionTokenHolder);
    }

    private IApplicationServerApi getApplicationServerApi()
    {
        return ServiceProvider.getV3ApplicationService();
    }

    private IDataStoreServerApi getDataStoreServerApi()
    {
        return ServiceProvider.getV3DataStoreService();
    }

    private DataSet getDataSet(String sessionToken, String permId)
    {
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withType();
        fetchOptions.withDataStore();
        fetchOptions.withPhysicalData();
        Map<IDataSetId, DataSet> result = getApplicationServerApi()
                .getDataSets(sessionToken, Arrays.asList(new DataSetPermId(permId)),
                        fetchOptions);
        if (result.isEmpty())
        {
            throw new UserFailureException("Could not find Dataset:" + permId);
        }
        return result.get(new DataSetPermId(permId));
    }

    private Map<IDataSetId, DataSet> getDataSets(String sessionToken, List<String> permIds)
    {
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withProperties();
        fetchOptions.withType();
        fetchOptions.withDataStore();
        fetchOptions.withPhysicalData();
        Map<IDataSetId, DataSet> result = getApplicationServerApi()
                .getDataSets(sessionToken, permIds.stream()
                                .map(DataSetPermId::new)
                                .collect(Collectors.toList()),
                        fetchOptions);
        return result;
    }

    private ImagingDataContainer getDataFromParams(Map<String, Object> params)
    {
        Validator.validateInputParams(params);

        String type = (String) params.get("type");
        String json = Util.mapToJson(params);

        switch (type.toLowerCase())
        {
            case "preview":
                return Util.readConfig(json, ImagingPreviewContainer.class);
            case "export":
                return Util.readConfig(json, ImagingExportContainer.class);
            case "multi-export":
                return Util.readConfig(json, ImagingMultiExportContainer.class);
            default:
                throw new UserFailureException("Wrong type:" + type);
        }
    }

    private Map<String, Object> extractMetaData(ImagingDataSetPreview preview)
    {
        Map<String, Object> metaData = new HashMap<>();
        if (!preview.getConfig().isEmpty())
        {
            metaData.put("config", preview.getConfig());
        }
        if (preview.getComment() != null && !preview.getComment().trim().isEmpty())
        {
            metaData.put("comment", preview.getComment());
        }
        if (!preview.getMetadata().isEmpty())
        {
            metaData.put("meta_data", preview.getMetadata());
        }
        return metaData;
    }

}
