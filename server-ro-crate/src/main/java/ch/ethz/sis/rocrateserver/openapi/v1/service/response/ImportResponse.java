package ch.ethz.sis.rocrateserver.openapi.v1.service.response;

import ch.ethz.sis.rocrateserver.openapi.v1.service.response.result.IResultPayload;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")

public class ImportResponse implements IResultPayload
{
    Map<String, String> externalToOpenBisIdentifier;

    public ImportResponse(Map<String, String> externalToOpenBisIdentifier)
    {
        this.externalToOpenBisIdentifier = externalToOpenBisIdentifier;
    }

    public Map<String, String> getExternalToOpenBisIdentifier()
    {
        return externalToOpenBisIdentifier;
    }

    public void setExternalToOpenBisIdentifier(
            Map<String, String> externalToOpenBisIdentifier)
    {
        this.externalToOpenBisIdentifier = externalToOpenBisIdentifier;
    }
}
