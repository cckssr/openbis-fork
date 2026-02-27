package ch.ethz.sis.rocrateserver.openapi.v1.service.params;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.HeaderParam;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ExportParams {
    public static final String EXPORT_MIME_TYPE_HEADER = "Export";

    //
    // Header Parameters
    //

    @HeaderParam("api-key")
    private String apiKey;

    @HeaderParam("Accept")
    private String accept;

    @HeaderParam(EXPORT_MIME_TYPE_HEADER)
    private String exportMimeTyp;

    @HeaderParam("openbis.identifier-annotations")
    private String identifierAnnotations;

    @HeaderParam("openbis.import-compatible")
    private String importCompatible;

    @HeaderParam("openbis.metadata-pdf")
    private String formatPDF;

    @HeaderParam("openbis.metadata-xlsx")
    private String formatXLSX;

    @HeaderParam("openbis.dataset-data")
    private String importDatasetData;

    @HeaderParam("openbis.afs-data")
    private String importAfsData;

    @HeaderParam("openbis.with-levels-above")
    private String withLevelsAbove;

    @HeaderParam("openbis.with-levels-below")
    private String withLevelsBelow; // Include levels below from same space

    @HeaderParam("openbis.with-objects-and-dataSets-children")
    private String withObjectsAndDataSetsChildren; // Include levels below from same space

    @HeaderParam("openbis.with-objects-and-dataSets-parents")
    private String withObjectsAndDataSetsParents; // Include levels below from same space

    @HeaderParam("openbis.with-objects-and-dataSets-other-spaces")
    private String withObjectsAndDataSetsOtherSpaces; // Include Objects and Datasets parents and children from different spaces

    @HeaderParam("openbis.input-body-format")
    private String inputBodyFormat;

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

    public boolean isWithLevelsAbove() {
        if(withLevelsAbove == null || withLevelsAbove.isBlank()) {
            return true;
        } else {
            return Boolean.parseBoolean(withLevelsAbove);
        }
    }

    public void setWithLevelsAbove(String withLevelsAbove) {
        this.withLevelsAbove = withLevelsAbove;
    }

    public boolean isWithLevelsBelow() {
        return withLevelsBelow != null && Boolean.parseBoolean(withLevelsBelow);
    }

    public void setWithLevelsBelow(String withLevelsBelow) {
        this.withLevelsBelow = withLevelsBelow;
    }

    public boolean isWithObjectsAndDataSetsParents() {
        return withObjectsAndDataSetsParents != null && Boolean.parseBoolean(withObjectsAndDataSetsParents);
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

    public boolean isImportCompatible()
    {
        if(importCompatible == null || importCompatible.isBlank()) {
            return true;
        } else {
            return Boolean.parseBoolean(importCompatible);
        }
    }

    public void setImportCompatible(String importCompatible)
    {
        this.importCompatible = importCompatible;
    }

    public boolean isFormatPDF()
    {
        return formatPDF != null && Boolean.parseBoolean(formatPDF);
    }

    public void setFormatPDF(String formatPDF)
    {
        this.formatPDF = formatPDF;
    }

    public boolean isFormatXLSX()
    {
        return formatXLSX != null && Boolean.parseBoolean(formatXLSX);
    }

    public void setFormatXLSX(String formatXLSX)
    {
        this.formatXLSX = formatXLSX;
    }

    public boolean isImportDatasetData()
    {
        return importDatasetData != null && Boolean.parseBoolean(importDatasetData);
    }

    public void setImportDatasetData(String importDatasetData)
    {
        this.importDatasetData = importDatasetData;
    }

    public boolean isImportAfsData()
    {
        return importAfsData != null && Boolean.parseBoolean(importAfsData);
    }

    public void setImportAfsData(String importAfsData)
    {
        this.importAfsData = importAfsData;
    }

    public boolean isWithObjectsAndDataSetsChildren()
    {
        return withObjectsAndDataSetsChildren != null && Boolean.parseBoolean(withObjectsAndDataSetsChildren);
    }

    public void setWithObjectsAndDataSetsChildren(String withObjectsAndDataSetsChildren)
    {
        this.withObjectsAndDataSetsChildren = withObjectsAndDataSetsChildren;
    }

    public String getInputBodyFormat()
    {
        return inputBodyFormat;
    }

    public void setInputBodyFormat(String inputBodyFormat)
    {
        this.inputBodyFormat = inputBodyFormat;
    }

    //
    // Body Parameters
    //

    public static Map<String,String>[] getIdentifiers(String inputBodyFormat, InputStream body) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        if(inputBodyFormat != null && inputBodyFormat.equalsIgnoreCase("json")) {
            Map<String,String> ids[] = mapper.readValue(body, Map[].class);
            return ids;
        } else {
            String[] identifiers = mapper.readValue(body, String[].class);
            Map<String, String> ids[] = new Map[identifiers.length];

            for(int i=0;i<ids.length;i++) {
                ids[i] = new HashMap<>();
                ids[i].put("permId", identifiers[i]);
                ids[i].put("kind", "SAMPLE");
            }
            return ids;
        }
    }

    //
    // Defaults
    //

    private String[] getDefaultIdentifierAnnotations() {
        return new String[] { "https://schema.org/identifier", "http://datacite.org/schema/kernel-4#doi" };
    }

    public String getAccept()
    {
        return accept;
    }

    public String getExportMimeType()
    {
        return exportMimeTyp;
    }


}
