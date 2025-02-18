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

package ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.adaptor;

import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetFilter;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetImage;
import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetPreview;
import ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.ImagingService;
import ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.ImagingServiceContext;
import ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.Util;
import ch.ethz.sis.openbis.generic.server.sharedapi.v3.json.GenericObjectMapper;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ImagingDataSetAbstractPythonAdaptor implements IImagingDataSetAdaptor
{
    private static final Logger
            operationLog = LogFactory.getLogger(LogCategory.OPERATION, ImagingDataSetAbstractPythonAdaptor.class);

    protected String pythonPath;

    protected String scriptPath;

    @Override
    public Map<String, Serializable> process(ImagingServiceContext context, File rootFile, String format,
            Map<String, Serializable> imageConfig,
            Map<String, Serializable> imageMetadata,
            Map<String, Serializable> previewConfig,
            Map<String, Serializable> previewMetadata,
            List<ImagingDataSetFilter> filterConfig)
    {
        ProcessBuilder processBuilder = new ProcessBuilder(pythonPath,
                scriptPath, rootFile.getAbsolutePath(), format, convertMapToJson(imageConfig),
                convertMapToJson(imageMetadata), convertMapToJson(previewConfig), convertMapToJson(previewMetadata),
                convertFilterConfig(filterConfig));
        processBuilder.redirectErrorStream(false);

        String fullOutput;
        try
        {
            Process process = processBuilder.start();
            fullOutput =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0)
            {
                String error =
                        new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                logOutput(fullOutput);
                throw new UserFailureException("Script evaluation failed: " + error);
            }
        } catch (IOException | InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        if (fullOutput.trim().isEmpty())
        {
            throw new UserFailureException("Script produced no results!");
        }
        logOutput(fullOutput);
        String[] result = fullOutput.split("\n");
        return convertPythonOutput(result[result.length-1]);
    }

    @Override
    public void computePreview(ImagingServiceContext context, File rootFile,
            ImagingDataSetImage image, ImagingDataSetPreview preview)
    {
        Map<String, Serializable> map = process(context, rootFile, preview.getFormat(),
                image.getImageConfig(), image.getMetadata(),
                preview.getConfig(), preview.getMetadata(), preview.getFilterConfig());

        preview.getMetadata().clear();
        for (Map.Entry<String, Serializable> entry : map.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase("width"))
            {
                Integer value = Integer.valueOf(entry.getValue().toString());
                preview.setWidth(value);
            } else if (entry.getKey().equalsIgnoreCase("height"))
            {
                Integer value = Integer.valueOf(entry.getValue().toString());
                preview.setHeight(value);
            } else if (entry.getKey().equalsIgnoreCase("bytes"))
            {
                preview.setBytes(entry.getValue().toString());
            }  else
            {
                preview.getMetadata().put(entry.getKey(), entry.getValue());
            }
        }
    }

    private String convertMapToJson(Map<String, ?> map)
    {
        Map<String, ?> mapToConvert = map;
        if(map == null) {
            mapToConvert = new HashMap<>();
        }
        try
        {
            ObjectMapper objectMapper = new GenericObjectMapper();
            return objectMapper.writeValueAsString(mapToConvert);
        } catch (Exception e)
        {
            throw new UserFailureException("Couldn't convert map to string", e);
        }
    }

    private String convertFilterConfig(List<ImagingDataSetFilter> list)
    {
        List<ImagingDataSetFilter> listToConvert = list;
        if(listToConvert == null) {
            listToConvert = new ArrayList<>();
        }

        List<Map<String, Map<String, Serializable>>> output = new ArrayList<>();
        for(ImagingDataSetFilter filter : listToConvert) {
            output.add(Map.of(filter.getName(), filter.getParameters()));
        }

        try
        {
            ObjectMapper objectMapper = new GenericObjectMapper();
            return objectMapper.writeValueAsString(output);
        } catch (Exception e)
        {
            throw new UserFailureException("Couldn't convert map to string", e);
        }
    }

    private Map<String, Serializable> convertPythonOutput(String line) {
        try
        {
            return Util.readConfig(line, Map.class);
        } catch (Exception e)
        {
            return Map.of("bytes", line);
        }
    }

    private void logOutput(String output) {
        if(output != null && !output.trim().isEmpty()) {
            String[] lines = output.split("\n");
            for(String line : lines) {
                operationLog.info(String.format("Adapter output: '%s'", line));
            }
        } else {
            operationLog.info("Adapter output: empty");
        }
    }

}
