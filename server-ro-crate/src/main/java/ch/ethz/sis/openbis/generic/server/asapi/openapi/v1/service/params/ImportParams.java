package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.params;

import jakarta.ws.rs.HeaderParam;

public class ImportParams
{

    @HeaderParam("api-key")
    private String apiKey;

    @HeaderParam("Content-Type")
    private String contentType;

}
