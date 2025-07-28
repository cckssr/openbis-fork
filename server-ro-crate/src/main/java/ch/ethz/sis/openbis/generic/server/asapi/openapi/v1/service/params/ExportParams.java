package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.params;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.HeaderParam;

import java.io.IOException;
import java.io.InputStream;

public class ExportParams {

    //
    // Header Parameters
    //

    @HeaderParam("api-key")
    private String apiKey;

    @HeaderParam("identifier-annotations")
    private String identifierAnnotations;

    @HeaderParam("openbis.with-Levels-below")
    private String withLevelsBelow; // Include levels below from same space

    @HeaderParam("openbis.with-objects-and-dataSets-parents")
    private String withObjectsAndDataSetsParents; // Include levels below from same space

    @HeaderParam("openbis.with-objects-and-dataSets-other-spaces")
    private String withObjectsAndDataSetsOtherSpaces; // Include Objects and Datasets parents and children from different spaces

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String[] getIdentifierAnnotations() {
        if (identifierAnnotations != null) {
            return identifierAnnotations.split(" ");
        }
        return getDefaultIdentifierAnnotations();
    }

    public void setIdentifierAnnotations(String identifierAnnotations) {
        this.identifierAnnotations = identifierAnnotations;
    }

    public boolean isWithLevelsBelow() {
        return Boolean.parseBoolean(withLevelsBelow);
    }

    public void setWithLevelsBelow(String withLevelsBelow) {
        this.withLevelsBelow = withLevelsBelow;
    }

    public boolean isWithObjectsAndDataSetsParents() {
        return Boolean.parseBoolean(withObjectsAndDataSetsParents);
    }

    public void setWithObjectsAndDataSetsParents(String withObjectsAndDataSetsParents) {
        this.withObjectsAndDataSetsParents = withObjectsAndDataSetsParents;
    }

    public boolean isWithObjectsAndDataSetsOtherSpaces() {
        return Boolean.parseBoolean(withObjectsAndDataSetsOtherSpaces);
    }

    public void setWithObjectsAndDataSetsOtherSpaces(String withObjectsAndDataSetsOtherSpaces) {
        this.withObjectsAndDataSetsOtherSpaces = withObjectsAndDataSetsOtherSpaces;
    }

    //
    // Body Parameters
    //

    public static String[] getIdentifiers(InputStream body) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(body, String[].class);
    }

    //
    // Defaults
    //

    private String[] getDefaultIdentifierAnnotations() {
        return new String[] { "https://schema.org/identifier", "http://datacite.org/schema/kernel-4#doi" };
    }
}
