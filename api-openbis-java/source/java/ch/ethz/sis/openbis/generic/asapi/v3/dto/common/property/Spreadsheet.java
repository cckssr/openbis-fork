package ch.ethz.sis.openbis.generic.asapi.v3.dto.common.property;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.ObjectToString;
import ch.systemsx.cisd.base.annotation.JsonObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;

@JsonObject("as.dto.common.property.Spreadsheet")
public final class Spreadsheet implements Serializable
{
    @JsonProperty
    private String version = "1";
    @JsonProperty
    private String[] headers;
    @JsonProperty
    private String[][] data;
    @JsonProperty
    private String[][] values;
    @JsonProperty
    private Integer[] width;
    @JsonProperty
    private Map<String, String> style;
    @JsonProperty
    private Map<String, String> meta;

    @JsonIgnore
    public String getVersion()
    {
        return version;
    }

    @JsonIgnore
    public void setVersion(String version)
    {
        this.version = version;
    }

    @JsonIgnore
    public String[] getHeaders()
    {
        return headers;
    }

    @JsonIgnore
    public void setHeaders(String[] headers)
    {
        this.headers = headers;
    }

    @JsonIgnore
    public String[][] getData()
    {
        return data;
    }

    @JsonIgnore
    public void setData(String[][] data)
    {
        this.data = data;
    }

    @JsonIgnore
    public String[][] getValues()
    {
        return values;
    }

    @JsonIgnore
    public void setValues(String[][] values)
    {
        this.values = values;
    }

    @JsonIgnore
    public Integer[] getWidth()
    {
        return width;
    }

    @JsonIgnore
    public void setWidth(Integer[] width)
    {
        this.width = width;
    }

    @JsonIgnore
    public Map<String, String> getStyle()
    {
        return style;
    }

    @JsonIgnore
    public void setStyle(Map<String, String> style)
    {
        this.style = style;
    }

    @JsonIgnore
    public Map<String, String> getMeta()
    {
        return meta;
    }

    @JsonIgnore
    public void setMeta(Map<String, String> meta)
    {
        this.meta = meta;
    }

    @Override
    public String toString()
    {
        return new ObjectToString(this).append("version", version).toString();
    }
}