package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.delegates;

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
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.helper.OpeBISProvider;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.helper.SessionWorkSpace;
import ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service.params.ExportParams;
import ch.openbis.rocrate.app.writer.Writer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ExportDelegate
{

    public OutputStream export(
            ExportParams headers,
            InputStream body) throws Exception
    {
        OpenBIS openBis = OpeBISProvider.createClient(headers.getApiKey());
        String[] identifiers = ExportParams.getIdentifiers(body);
        String[] identifierAnnotations = headers.getIdentifierAnnotations();
        Set<String> identifierAnnotationPropertyTypeCodes = getIdentifierAnnotationPropertyTypes(openBis, identifierAnnotations);

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

        SearchResult<Sample> searchResults = openBis.searchSamples(criteria, new SampleFetchOptions());

        if (searchResults.getTotalCount() < 1) {
            throw new IllegalArgumentException("No results found");
        }

        ExportData exportData = new ExportData();
        List<ExportablePermId> exportablePermIds =
                List.of(new ExportablePermId(ExportableKind.SAMPLE,
                        searchResults.getObjects().get(0).getPermId().toString()));

        exportData.setPermIds(exportablePermIds);
        ExportOptions exportOptions = getExportOptions(headers);

        ExportResult exportResult = openBis.executeExport(exportData, exportOptions);
        String downloadUrl = exportResult.getDownloadURL();
        System.out.println("Download url: " + downloadUrl);

        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        clientConnector.setIdleTimeout(Duration.ofMillis(OpeBISProvider.getTimeOut()));
        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP(clientConnector));
        httpClient.start();

        Request request =
                httpClient.newRequest(
                                downloadUrl)
                        .headers(httpFields -> httpFields.add("sessionToken",
                                openBis.getSessionToken()));
        request.method(HttpMethod.GET);
        ContentResponse response = request.send();
        System.out.println("Got a response!:");
        byte[] content = response.getContent();
        java.nio.file.Path roCrateMetadata = java.nio.file.Path.of("ro-crate-metadata.json");

        SessionWorkSpace.write(headers.getApiKey(), roCrateMetadata, new ByteArrayInputStream(content));
        System.out.println("Wrote excel file!");

        java.nio.file.Path realPath = SessionWorkSpace.getRealPath(headers.getApiKey(), roCrateMetadata);
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, realPath);
        System.out.println("Converted excel to RO-Crate!");

        Writer writer = new Writer();
        java.nio.file.Path roPath =
                java.nio.file.Path.of("result-crate" + UUID.randomUUID() + ".zip");
        java.nio.file.Path roRealPath = SessionWorkSpace.getRealPath(headers.getApiKey(),
                roPath);

        writer.write(openBisModel, roRealPath);

        return SessionWorkSpace.read(headers.getApiKey(), roPath);
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
