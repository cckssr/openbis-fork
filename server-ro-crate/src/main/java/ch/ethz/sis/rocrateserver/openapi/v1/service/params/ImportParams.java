package ch.ethz.sis.rocrateserver.openapi.v1.service.params;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import jakarta.ws.rs.HeaderParam;

public class ImportParams
{

    @HeaderParam("api-key")
    private String apiKey;

    @HeaderParam("Content-Type")
    private String contentType;

    @HeaderParam("openbis.import-mode")
    private String importMode;

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

    public String getImportMode() {
        return (importMode == null)?ImportMode.UPDATE_IF_EXISTS.toString():importMode;
    }

    public void setImportMode(String importMode) {
        this.importMode = importMode;
    }
}
