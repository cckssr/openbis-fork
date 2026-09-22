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

import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.*;
import ch.ethz.sis.openbis.generic.server.as.plugins.imaging.ImagingServiceContext;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.util.*;

public final class ImagingDataSetExampleAdaptor implements IImagingDataSetAdaptor
{

    private static final int WIDTH = 640;
    private static final int HEIGHT = 640;

    public ImagingDataSetExampleAdaptor(Properties properties) {
    }

    int getParameter(Map<String, Serializable> previewConfig, String name) {
        Serializable value = previewConfig.get(name);
        if(value == null) {
            throw new UserFailureException("Parameter: '"+name+"' is not correct!");
        }
        if (value instanceof Integer intValue) {
            return intValue;
        } else if (value instanceof String stringVal) {
            if(!stringVal.isEmpty() && stringVal.matches("[0-9]+")) {
                return Integer.parseInt(stringVal);
            } else {
                throw new UserFailureException("Parameter: '"+name+"' is not correct!");
            }
        } else if (value instanceof String[] array) {
            if(array.length != 1) {
                throw new UserFailureException("Parameter: '"+name+"' is not correct!");
            }
            String stringVal = array[0];
            if(!stringVal.isEmpty() && stringVal.matches("[0-9]+")) {
                return Integer.parseInt(stringVal);
            } else {
                throw new UserFailureException("Parameter: '"+name+"' is not correct!");
            }
        }
        throw new UserFailureException("Parameter: '"+name+"' is not correct!");
    }

    @Override
    public Map<String, Serializable> process(ImagingServiceContext context, File rootFile, String format,
            Map<String, Serializable> imageConfig,
            Map<String, Serializable> imageMetadata,
            Map<String, Serializable> previewConfig,
            Map<String, Serializable> previewMetadata,
            List<ImagingDataSetFilter> filterConfig)
    {
        int width = getParameter(previewConfig, "width");
        int height = getParameter(previewConfig, "height");

        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<HEIGHT; y+=height)
        {
            for (int x = 0; x < WIDTH; x+=width)
            {
                int a = (int)(Math.random()*256);
                int r = (int)(Math.random()*256);
                int g = (int)(Math.random()*256);
                int b = (int)(Math.random()*256);

                //pixel
                int p = (a<<24) | (r<<16) | (g<<8) | b;
                for(int i = 0; i < height; i++)
                {
                    if(y+i >= HEIGHT) {
                        break;
                    }
                    for (int j = 0; j < width; j++)
                    {
                        if(x+j >= WIDTH) {
                            break;
                        }
                        img.setRGB(x+j, y+i, p);
                    }
                }
            }
        }
        if(filterConfig != null && !filterConfig.isEmpty()) {
            for(ImagingDataSetFilter filter : filterConfig) {
                Map<String, Serializable> params = filter.getParameters();
                switch (filter.getName()) {
                    case "Gaussian":
                        int radius = Integer.parseInt((String)params.get("Radius"));
                        img = gaussianBlur(img, radius);
                        break;
                    default:
                        break;
                }
            }
        }

        try
        {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(img, format, byteArrayOutputStream);
            byteArrayOutputStream.flush();
            String bytes = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
            HashMap<String, Serializable> map = new HashMap<>();
            map.put("width", WIDTH);
            map.put("height", HEIGHT);
            map.put("bytes", bytes);
            map.put("adapter comment", "randomized image");
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createConfig(ImagingServiceContext context, File rootFile, ImagingDataSetPropertyConfig propertyConfig) {
        ImagingDataSetConfig config = new ImagingDataSetConfig();
        config.setAdaptor("ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor.ImagingDataSetExampleAdaptor");
        ImagingDataSetControl widthControl = new  ImagingDataSetControl();
        widthControl.setLabel("width");
        widthControl.setSection("Block size");
        widthControl.setRange(List.of("1", "640", "1"));
        widthControl.setType("Slider");
        widthControl.setUnit("px");

        ImagingDataSetControl heightControl = new  ImagingDataSetControl();
        heightControl.setLabel("height");
        heightControl.setSection("Block size");
        heightControl.setRange(List.of("1", "640", "1"));
        heightControl.setType("Slider");
        heightControl.setUnit("px");

        config.setInputs(List.of(widthControl, heightControl));

        ImagingDataSetControl gaussianControl = new  ImagingDataSetControl();
        gaussianControl.setLabel("Radius");
        gaussianControl.setRange(List.of("1", "100", "1"));
        gaussianControl.setType("Slider");
        gaussianControl.setUnit("px");

        config.setFilters(Map.of("Gaussian", List.of(gaussianControl)));

        ImagingDataSetControl include = new ImagingDataSetControl();
        include.setLabel("include");
        include.setType("Dropdown");
        include.setMultiselect(true);
        include.setValues(Arrays.asList("image", "raw data"));

        ImagingDataSetControl archiveFormat = new ImagingDataSetControl();
        archiveFormat.setLabel("archive-format");
        archiveFormat.setType("Dropdown");
        archiveFormat.setMultiselect(false);
        archiveFormat.setValues(Arrays.asList("zip", "tar"));

        ImagingDataSetControl imageFormat = new ImagingDataSetControl();
        imageFormat.setLabel("image-format");
        imageFormat.setType("Dropdown");
        imageFormat.setMultiselect(false);
        imageFormat.setValues(Arrays.asList("png"));

        ImagingDataSetControl resolution = new ImagingDataSetControl();
        resolution.setLabel("resolution");
        resolution.setType("Dropdown");
        resolution.setMultiselect(false);
        resolution.setValues(Arrays.asList("original"));

        config.setExports(Arrays.asList(include, imageFormat, archiveFormat, resolution));
        config.setFilterSemanticAnnotation(Map.of());
        config.setMetadata(Map.of());
        config.setResolutions(List.of("original"));

        ImagingDataSetImage image = new ImagingDataSetImage();
        image.setIndex(0);
        image.setImageConfig(Map.of());
        image.setMetadata(Map.of());

        ImagingDataSetPreview preview = new ImagingDataSetPreview();
        preview.setIndex(0);
        preview.setFormat("png");
        preview.setConfig(Map.of("width", "1", "height", "1"));
        preview.setMetadata(Map.of());
        preview.setTags(new String[0]);
        preview.setComment("");
        preview.setHeight(HEIGHT);
        preview.setWidth(WIDTH);

        image.setPreviews(List.of(preview));
        image.setConfig(config);
        propertyConfig.setImages(List.of(image));
        propertyConfig.setMetadata(Map.of("preview-total-count", "1", "comment", "autogenerated config"));

    }

    public static BufferedImage gaussianBlur(BufferedImage src, int radius) {
        float[] kernel = createGaussianKernel(radius);

        // Horizontal pass
        BufferedImage horizontal = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Kernel hKernel = new Kernel(kernel.length, 1, kernel);
        new ConvolveOp(hKernel, ConvolveOp.EDGE_NO_OP, null).filter(src, horizontal);

        // Vertical pass
        BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Kernel vKernel = new Kernel(1, kernel.length, kernel);
        new ConvolveOp(vKernel, ConvolveOp.EDGE_NO_OP, null).filter(horizontal, result);

        return result;
    }

    private static float[] createGaussianKernel(int radius) {
        int size = radius * 2 + 1;
        float[] data = new float[size];
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0f;

        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int idx = i + radius;
            data[idx] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += data[idx];
        }

        // Normalize so weights sum to 1
        for (int i = 0; i < size; i++) {
            data[i] /= total;
        }

        return data;
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
            } else
            {
                preview.getMetadata().put(entry.getKey(), entry.getValue());
            }
        }
    }

}
