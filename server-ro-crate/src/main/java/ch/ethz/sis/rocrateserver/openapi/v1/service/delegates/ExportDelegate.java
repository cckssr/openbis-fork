package ch.ethz.sis.rocrateserver.openapi.v1.service.delegates;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.ExportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.XlsTextFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.SessionWorkSpaceManager;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ExportParams;
import ch.ethz.sis.rocrateserver.startup.RoCrateServerParameter;
import ch.ethz.sis.rocrateserver.startup.StartupMain;
import ch.openbis.rocrate.app.writer.Writer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.HttpMethod;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@ApplicationScoped
public class ExportDelegate
{

    public InputStream export(
            OpenBIS openBIS,
            ExportParams headers,
            InputStream body) throws Exception
    {
        String[] identifiers = ExportParams.getIdentifiers(body);
        String[] identifierAnnotations = headers.getIdentifierAnnotations();

        // Obtain openBIS Properties annotated with semantic annotations used to hold identifiers
        Set<String> identifierAnnotationPropertyTypeCodes = getIdentifierAnnotationPropertyTypes(openBIS, identifierAnnotations);

        // Search for any openBIS samples holding identifiers on their properties or matching openBIS identifiers or permIds
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withOrOperator();

        for (String identifier:identifiers) {

            // Semantic Annotation Matching to properties
            for (String propertyTypeCode:identifierAnnotationPropertyTypeCodes) {
                criteria.withStringProperty(propertyTypeCode)
                        .withoutWildcards()
                        .thatEquals(identifier);
            }

            // Could it be an openBIS permId ?
            if (identifier.contains("-") && !identifier.contains("/")) {
                criteria.withPermId().withoutWildcards().thatEquals(identifier);
            }

            // Could it be an openBIS identifier ?
            if (identifier.contains("/")) {
                criteria.withIdentifier().withoutWildcards().thatEquals(identifier);
            }

        }

        SearchResult<Sample> searchResults = openBIS.searchSamples(criteria, new SampleFetchOptions());

        if (searchResults.getTotalCount() < 1) {
            RoCrateExceptions.throwInstance(RoCrateExceptions.NO_RESULTS_FOUND);
        }

        // Request openBIS export for found samples
        ExportData exportData = new ExportData();
        List<ExportablePermId> exportablePermIds =
                List.of(new ExportablePermId(ExportableKind.SAMPLE,
                        searchResults.getObjects().get(0).getPermId().toString()));

        exportData.setPermIds(exportablePermIds);
        ExportOptions exportOptions = getExportOptions(headers);

        ExportResult exportResult = openBIS.executeExport(exportData, exportOptions);

        // Download of openBIS export
        // TODO: Extract this download to a separate method and deal with data as streams not as arrays
        String downloadUrl = exportResult.getDownloadURL();
        System.out.println("Download url: " + downloadUrl);

        java.nio.file.Path realPathToExcel = downloadExcel(openBIS, headers, downloadUrl);

        // Convert openBIS Excel to Ro-Crate
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, realPathToExcel);

        Writer writer = new Writer();
        java.nio.file.Path tempRoCratePath =
                java.nio.file.Path.of("result-crate" + UUID.randomUUID() + ".zip");
        java.nio.file.Path realTempRoCratePath =
                SessionWorkSpaceManager.getRealPath(headers.getApiKey(), tempRoCratePath);
        writer.write(openBisModel, realTempRoCratePath);

        if (headers.getAccept().equals("application/ld+json"))
        {
            ZipFile zipFile = new ZipFile(realTempRoCratePath.toFile());
            ZipEntry zipEntry = zipFile.getEntry("ro-crate-metadata.json");
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            return inputStream;

        }


        return SessionWorkSpaceManager.read(headers.getApiKey(), tempRoCratePath);
    }

    private static java.nio.file.Path downloadExcel(OpenBIS openBIS, ExportParams headers,
            String downloadUrl) throws Exception
    {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        clientConnector.setIdleTimeout(Duration.ofMillis(StartupMain.getConfiguration()
                .getIntegerProperty(RoCrateServerParameter.openBISTimeout)));
        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP(clientConnector));
        httpClient.start();

        InputStreamResponseListener listener = new InputStreamResponseListener();

        Request request =
                httpClient.newRequest(
                                downloadUrl)
                        .headers(httpFields -> httpFields.add("sessionToken",
                                openBIS.getSessionToken()));
        request.method(HttpMethod.GET);
        request.send(listener);
        System.out.println("Got a response!:");


        // Write openBIS export to disk
        // TODO: Extract this part with previous
        java.nio.file.Path pathToExcel = java.nio.file.Path.of("metadata.xlsx");
        SessionWorkSpaceManager.write(headers.getApiKey(), pathToExcel, listener.getInputStream());
        java.nio.file.Path realPathToExcel = SessionWorkSpaceManager.getRealPath(headers.getApiKey(), pathToExcel);
        return realPathToExcel;

    }

    private Set<String> getIdentifierAnnotationPropertyTypes(
            OpenBIS v3,
            String[] identifierAnnotations) {
        SemanticAnnotationSearchCriteria criteria = new SemanticAnnotationSearchCriteria();
        criteria.withOrOperator();
        for(String identifierAnnotation:identifierAnnotations) {
            criteria.withPredicateAccessionId().thatEquals(identifierAnnotation);
        }

        SemanticAnnotationFetchOptions options = new SemanticAnnotationFetchOptions();
        options.withPropertyAssignment().withPropertyType();
        options.withPropertyType();

        SearchResult<SemanticAnnotation> apiResults = v3.searchSemanticAnnotations(criteria, options);
        Set<String> results = new HashSet<>(apiResults.getTotalCount());
        for (SemanticAnnotation annotation: apiResults.getObjects()) {
            if (annotation.getPropertyAssignment() != null && annotation.getPropertyAssignment().getPropertyType() != null) {
                results.add(annotation.getPropertyAssignment().getPropertyType().getCode());
            } else if (annotation.getPropertyType() != null) {
                results.add(annotation.getPropertyType().getCode());
            }
        }
        return results;
    }

    private static ExportOptions getExportOptions(ExportParams exportParams)
    {
        ExportOptions exportOptions = new ExportOptions();

        // Mandatory, non-optional for ro-crate exports
        exportOptions.setWithImportCompatibility(true);
        exportOptions.setWithReferredTypes(true);
        exportOptions.setXlsTextFormat(XlsTextFormat.RICH);
        exportOptions.setWithLevelsAbove(true);
        exportOptions.setFormats(Set.of(ExportFormat.XLSX));
        exportOptions.setZipSingleFiles(false);

        // Defaults, could be overridden with options
        exportOptions.setWithLevelsBelow(exportParams.isWithLevelsBelow());
        exportOptions.setWithObjectsAndDataSetsParents(exportParams.isWithObjectsAndDataSetsParents());
        exportOptions.setWithObjectsAndDataSetsOtherSpaces(exportParams.isWithObjectsAndDataSetsOtherSpaces());

        return exportOptions;
    }
}
