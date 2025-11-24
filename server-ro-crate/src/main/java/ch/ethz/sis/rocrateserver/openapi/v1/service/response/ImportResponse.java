package ch.ethz.sis.rocrateserver.openapi.v1.service.response;

import java.util.Map;

public class ImportResponse
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
