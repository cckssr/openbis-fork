package ch.ethz.sis.rocrateserver.openapi.v1.service.params;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.HeaderParam;

import java.io.IOException;
import java.io.InputStream;

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

    @HeaderParam("openbis.with-Levels-below")
    private String withLevelsBelow; // Include levels below from same space

    @HeaderParam("openbis.with-objects-and-dataSets-children")
    private String withObjectsAndDataSetsChildren; // Include levels below from same space

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

    public String getAccept()
    {
        return accept;
    }

    public String getWithLevelsBelow()
    {
        return withLevelsBelow;
    }

    public String getWithObjectsAndDataSetsParents()
    {
        return withObjectsAndDataSetsParents;
    }

    public String getWithObjectsAndDataSetsOtherSpaces()
    {
        return withObjectsAndDataSetsOtherSpaces;
    }

    public String getExportMimeType()
    {
        return exportMimeTyp;
    }

    public boolean isImportCompatible()
    {
        return importCompatible != null &&  Boolean.parseBoolean(importCompatible);
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

    public boolean isWithObjectsAndDataSetsChildren()
    {
        return withObjectsAndDataSetsChildren != null && Boolean.parseBoolean(withObjectsAndDataSetsChildren);
    }

    public void setWithObjectsAndDataSetsChildren(String withObjectsAndDataSetsChildren)
    {
        this.withObjectsAndDataSetsChildren = withObjectsAndDataSetsChildren;
    }

    public void setImportAfsData(String importAfsData)
    {
        this.importAfsData = importAfsData;
    }
}
