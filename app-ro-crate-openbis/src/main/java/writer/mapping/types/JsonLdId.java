package writer.mapping.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class JsonLdId
{

    @JsonProperty("@id")
    private final String id;

    public JsonLdId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass())
            return false;
        JsonLdId jsonLdId = (JsonLdId) o;
        return Objects.equals(id, jsonLdId.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(id);
    }
}
