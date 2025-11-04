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

package ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetFilter;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetImage;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetPreview;
import ch.ethz.sis.openbis.generic.server.as.plugins.imaging.ImagingServiceContext;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContent;
import ch.systemsx.cisd.openbis.common.io.hierarchical_content.api.IHierarchicalContentNode;
import ch.systemsx.cisd.openbis.dss.generic.shared.IHierarchicalContentProvider;
import ch.systemsx.cisd.openbis.dss.generic.shared.ServiceProvider;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;
import ch.systemsx.cisd.openbis.generic.shared.dto.OpenBISSessionHolder;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class NanonisDatAdaptor extends ImagingDataSetAbstractPythonAdaptor
{
    static final String DAT_SCRIPT_PROPERTY = "nanonis.dat-script-path";

    private IHierarchicalContentProvider contentProvider;
    private final String storeRootDss;
    private final String storeRootAfs;

    public NanonisDatAdaptor(Properties properties)
    {

        String scriptProperty = properties.getProperty(DAT_SCRIPT_PROPERTY, "");
        if (scriptProperty.trim().isEmpty())
        {
            throw new IllegalArgumentException(
                    "There is no script path property called '" + DAT_SCRIPT_PROPERTY + "' defined for this adaptor!");
        }
        Path script = Paths.get(scriptProperty);
        if (!Files.exists(script))
        {
            throw new IllegalArgumentException("Script file " + script + " does not exists!");
        }
        this.scriptPath = script.toString();
        this.pythonPath = properties.getProperty("python3-path", "python3");
        this.storeRootDss = properties.getProperty("storageRoot.dss");
        this.storeRootAfs = properties.getProperty("storageRoot.afs");
    }

    @Override
    public void computePreview(ImagingServiceContext context, File rootFile,
            ImagingDataSetImage image, ImagingDataSetPreview preview)
    {
        super.computePreview(context, rootFile, image, preview);
    }

    @Override
    public Map<String, Serializable> process(ImagingServiceContext context, File rootFile, String format,
            Map<String, Serializable> imageConfig,
            Map<String, Serializable> imageMetadata,
            Map<String, Serializable> previewConfig,
            Map<String, Serializable> previewMetadata,
            List<ImagingDataSetFilter> filterConfig)
    {
        if(previewConfig != null)
        {
            if (previewConfig.get("spectraLocator") != null)
            {
                String spectraLocator =
                        previewConfig.get("spectraLocator").toString().trim().toUpperCase();
                if ("TRUE".equals(spectraLocator))
                {
                    String objId = previewConfig.get("objId").toString();
                    String sxmPermId = previewConfig.get("sxmPermId").toString();
                    previewConfig.get("sxmFilePath");

                    if (sxmPermId != null && !sxmPermId.trim().isEmpty())
                    {
                        if (sxmPermId.equals(objId))
                        {
                            previewConfig.put("sxmRootPath", rootFile.getAbsolutePath());
                        } else
                        {
                            File sxmFile = getRootFile(context.getSessionToken(), sxmPermId);
                            previewConfig.put("sxmRootPath", sxmFile.getAbsolutePath());
                        }

                    }
                }
            } else if (previewMetadata != null && previewMetadata.get("spectraLocator") != null)
            {
                String spectraLocator =
                        previewMetadata.get("spectraLocator").toString().trim().toUpperCase();
                if ("TRUE".equals(spectraLocator))
                {
                    Map<String, Serializable> previewConfigCopy = new HashMap<>(previewConfig);
                    previewConfigCopy.put("spectraLocator", "TRUE");
                    String sxmPermId = previewMetadata.get("sxmPermId").toString();
                    previewConfigCopy.put("sxmPermId", sxmPermId);
                    previewConfigCopy.put("sxmFilePath", previewMetadata.get("sxmFilePath"));
                    File sxmFile = getRootFile(context.getSessionToken(), sxmPermId);
                    previewConfigCopy.put("sxmRootPath", sxmFile.getAbsolutePath());
                    Map<String, Serializable> sxmPreviewConfig =
                            convertJsonToMap(previewMetadata.get("sxmConfig").toString());
                    previewConfigCopy.put("sxmPreviewConfig", (Serializable) sxmPreviewConfig);
                    Map<String, Serializable> result =
                            super.process(context, rootFile, format, imageConfig, imageMetadata,
                                    previewConfigCopy, previewMetadata, filterConfig);

                    return result;
                }
            }
        }
        return super.process(context, rootFile, format, imageConfig, imageMetadata, previewConfig, previewMetadata, filterConfig);
    }

    private DataSet getDataSet(String sessionToken, String permId) {

        IDataSetId dataSetId = new DataSetPermId(permId);
        DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
        fetchOptions.withPhysicalData();
        Map<IDataSetId, DataSet> result = CommonServiceProvider.getApplicationServerApi()
                .getDataSets(sessionToken, List.of(dataSetId),  fetchOptions);
        return result.get(dataSetId);
    }

    private File getRootFile(String sessionToken, String permId)
    {
        DataSet dataSet = getDataSet(sessionToken, permId);
        Path path  = Path.of(this.storeRootDss, dataSet.getPhysicalData().getShareId(), dataSet.getPhysicalData().getLocation());
        File file = path.toFile();
        return file;
    }

}
