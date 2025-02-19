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

package ch.ethz.sis.openbis.generic.imagingapi.v3.dto;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.property.PropertiesDeserializer;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonObject("imaging.dto.ImagingDataSetPreview")
public class ImagingDataSetPreview implements Serializable
{
    private static final long serialVersionUID = 1L;

    @JsonProperty
    @JsonDeserialize(contentUsing = PropertiesDeserializer.class)
    private Map<String, Serializable> config;

    @JsonProperty
    private List<ImagingDataSetFilter> filterConfig;

    @JsonProperty
    private String format;

    @JsonProperty
    private String bytes;

    @JsonProperty
    private Integer width;

    @JsonProperty
    private Integer height;

    @JsonProperty
    private Integer index;

    @JsonProperty
    private boolean show;

    @JsonProperty
    @JsonDeserialize(contentUsing = PropertiesDeserializer.class)
    private Map<String, Serializable> metadata;

    @JsonProperty
    private String comment;

    @JsonProperty
    private String[] tags;


    @JsonIgnore
    public Map<String, Serializable> getConfig()
    {
        if(config == null)
        {
            config = new HashMap<>();
        }
        return config;
    }

    public void setConfig(Map<String, Serializable> config)
    {
        this.config = config;
    }

    @JsonIgnore
    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    @JsonIgnore
    public String getBytes()
    {
        return bytes;
    }

    public void setBytes(String bytes)
    {
        this.bytes = bytes;
    }

    @JsonIgnore
    public boolean isShow()
    {
        return show;
    }

    public void setShow(boolean show)
    {
        this.show = show;
    }

    @JsonIgnore
    public Integer getWidth()
    {
        return width;
    }

    public void setWidth(Integer width)
    {
        this.width = width;
    }

    @JsonIgnore
    public Integer getHeight()
    {
        return height;
    }

    public void setHeight(Integer height)
    {
        this.height = height;
    }

    @JsonIgnore
    public Integer getIndex()
    {
        return index;
    }

    public void setIndex(Integer index)
    {
        this.index = index;
    }

    @JsonIgnore
    public Map<String, Serializable> getMetadata()
    {
        if(metadata == null)
        {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Serializable> metadata)
    {
        this.metadata = metadata;
    }

    @JsonIgnore
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @JsonIgnore
    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    @JsonIgnore
    public List<ImagingDataSetFilter> getFilterConfig()
    {
        return filterConfig;
    }

    public void setFilters(List<ImagingDataSetFilter> filterConfig)
    {
        this.filterConfig = filterConfig;
    }

    @Override
    public String toString()
    {
        return "ImagingDataSetPreview";
    }

}
