package ch.ethz.sis.openbis.generic.imagingapi.v3.dto;

import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonObject("imaging.dto.ImagingInitContainer")
public class ImagingInitContainer extends ImagingDataContainer
{
    @JsonProperty
    private String permId;
    @JsonProperty
    private ImagingDataSetPropertyConfig config = null;

    @JsonIgnore
    public String getPermId()
    {
        return permId;
    }

    public void setPermId(String permId) {
        this.permId = permId;
    }

    @JsonIgnore
    public ImagingDataSetPropertyConfig getConfig()
    {
        return config;
    }

    public void setConfig(ImagingDataSetPropertyConfig config)
    {
        this.config = config;
    }
}
