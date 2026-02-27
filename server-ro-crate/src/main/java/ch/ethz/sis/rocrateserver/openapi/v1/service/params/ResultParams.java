package ch.ethz.sis.rocrateserver.openapi.v1.service.params;

import jakarta.ws.rs.HeaderParam;

public class ResultParams
{
    @HeaderParam("api-key")
    private String apiKey;

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

}
