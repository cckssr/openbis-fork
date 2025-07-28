package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.params;

import jakarta.ws.rs.HeaderParam;

public class ValidateParams
{
    @HeaderParam("api-key")
    private String apiKey;

    @HeaderParam("Content-Type")
    private String contentType;

    public ValidateParams()
    {
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }
}
