package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.ExportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.XlsTextFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Path("/openbis/open-api/ro-crate")
public class RoCrateService {

    @Inject
    OpeBISProvider openBISProvider;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test-echo")
    public String testEcho(@QueryParam(value = "message") String message) {
        return message;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test-openbis-connection")
    public String testOpenbisConnection(@QueryParam(value = "sessionToken") String sessionToken) {
        OpenBIS openBIS = openBISProvider.createClient(sessionToken);
        try {
            return openBIS.getSessionInformation().getUserName();
        } finally {
            openBIS.logout();
        }
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({"application/ld+json", "application/zip"})
    @Path("import")
    public List<String> importRoCrate(
            @HeaderParam(value = "sessionToken") String sessionToken,
//            @HeaderParam(value = "options") Map<String, String> options,
            InputStream inputStream)
            throws IOException
    {
        System.out.println("Import Started");
        List<String> result;
        try
        {
            // Writing the crate to the session workspace

                java.nio.file.Path roCrateMetadata = java.nio.file.Path.of("ro-crate-metadata.json");
                SessionWorkSpace.write(sessionToken, roCrateMetadata, inputStream);
            // Reading ro-crate model
                RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
                RoCrate crate = roCrateFolderReader.readCrate(SessionWorkSpace.getRealPath(sessionToken, null).toString());

                SchemaFacade schemaFacade = SchemaFacade.of(crate);
                List<IType> types = schemaFacade.getTypes();
                List<IPropertyType> propertyTypes = schemaFacade.getPropertyTypes();
                List<IMetadataEntry> entryList = new ArrayList<>();
                for (var type : types)
                {
                    entryList.addAll(schemaFacade.getEntries(type.getId()));
                }
            // Converting ro-crate model to openBIS model
                OpenBisModel conversion = RdfToModel.convert(types, propertyTypes, entryList, "DEFAULT", "DEFAULT");
            if (!RoCrateSchemaValidation.validate(conversion).isOkay())
            {
                return List.of("no good!");
            }

                byte[] importExcel = ExcelWriter.convert(ExcelWriter.Format.EXCEL, conversion);
            // Sending import request to openBIS
                OpenBIS openBIS = openBISProvider.createClient(sessionToken);
                java.nio.file.Path modelAsExcel = java.nio.file.Path.of(
                        UUID.randomUUID() + ".xlsx");
                SessionWorkSpace.write(openBIS.getSessionToken(), modelAsExcel,
                        new ByteArrayInputStream(importExcel));
                java.nio.file.Path realPath =
                        SessionWorkSpace.getRealPath(openBIS.getSessionToken(), modelAsExcel);
                openBIS.uploadToSessionWorkspace(realPath);

                ImportOptions importOptions = new ImportOptions();
                importOptions.setMode(ImportMode.UPDATE_IF_EXISTS);

                ImportData importData = new ImportData();
                importData.setSessionWorkspaceFiles(new String[] { modelAsExcel.toString() });
                importData.setFormat(ImportFormat.EXCEL);
            SessionInformation sessionInformation = openBIS.getSessionInformation();
            System.out.println("Session information: " + sessionInformation);
            ImportResult importResult = openBIS.executeImport(importData, importOptions);
            result = importResult.getObjectIds().stream().map( id -> id.toString()).toList();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        } finally
        {
            SessionWorkSpace.clear(sessionToken);
        }
        System.out.println("Import Finished");
        return result;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes({"application/ld+json", "application/zip"})
    @Path("validate")
    public List<String> validateRoCrate(
            @HeaderParam(value = "sessionToken") String sessionToken,
//            @HeaderParam(value = "options") Map<String, String> options,
            InputStream inputStream)
            throws IOException
    {
        List<String> result = new ArrayList<>();
        try
        {
            // Writing the crate to the session workspace

            java.nio.file.Path roCrateMetadata = java.nio.file.Path.of("ro-crate-metadata.json");
            SessionWorkSpace.write(sessionToken, roCrateMetadata, inputStream);
            // Reading ro-crate model
            RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
            RoCrate crate = roCrateFolderReader.readCrate(
                    SessionWorkSpace.getRealPath(sessionToken, null).toString());

            SchemaFacade schemaFacade = SchemaFacade.of(crate);
            List<IType> types = schemaFacade.getTypes();
            List<IPropertyType> propertyTypes = schemaFacade.getPropertyTypes();
            List<IMetadataEntry> entryList = new ArrayList<>();
            for (var type : types)
            {
                entryList.addAll(schemaFacade.getEntries(type.getId()));
            }
            // Converting ro-crate model to openBIS model
            OpenBisModel conversion =
                    RdfToModel.convert(types, propertyTypes, entryList, "DEFAULT", "DEFAULT");
            if (!RoCrateSchemaValidation.validate(conversion).isOkay())
            {
                return List.of("no good!");
            }
        } catch (Exception e)
        {
            throw new IllegalArgumentException(e);
        } finally
        {
            SessionWorkSpace.clear(sessionToken);
        }
        return result;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes("application/json")
    @Path("export")
    public OutputStream exportRoCrate(
            @HeaderParam(value = "sessionToken") String sessionToken,
            @Context HttpHeaders httpHeaders,
            InputStream inputStream) throws Exception
    {
        ExportArgsDeserializer exportArgsDeserializer = new ExportArgsDeserializer(httpHeaders, inputStream);

//        httpHeaders.getRequestHeaders().entrySet().stream()
//                .forEach(x -> System.out.println(x.getKey() + ": " + x.getValue().stream().collect(
//                        Collectors.joining(","))));
        OpenBIS openBis = openBISProvider.createClient(sessionToken);

        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withType().withCode().thatEquals("CREATIVEWORK_SCICAT_PUBLISHEDDATA");
        //criteria.withStringProperty("PUBLICATION.IDENTIFIER").thatContains("doi");

        IApplicationServerApi
                v3 = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
                "http://localhost:8888/openbis/openbis" + IApplicationServerApi.SERVICE_URL,
                openBISProvider.getTimeOut());
        List<Sample> samples = searchSemanticAnnotations(sessionToken, v3,
                List.of("https://doi.org/10.1038/s41586-020-3010-5"));

        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        SearchResult<Sample>
                searchResults = v3.searchSamples(sessionToken, criteria, sampleFetchOptions);
        ExportData exportData = new ExportData();
        List<ExportablePermId> exportablePermIds =
                List.of(new ExportablePermId(ExportableKind.SAMPLE,
                        searchResults.getObjects().get(0).getPermId().toString()));
        ;
        exportData.setPermIds(exportablePermIds);

        ExportOptions exportOptions =
                getExportOptions();

        ExportResult exportResult = v3.executeExport(sessionToken, exportData, exportOptions);
        String downloadUrl = exportResult.getDownloadURL();
        System.out.println("Download url: " + downloadUrl);

        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        clientConnector.setIdleTimeout(Duration.ofMillis(openBISProvider.getTimeOut()));
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

        SessionWorkSpace.write(sessionToken, roCrateMetadata, new ByteArrayInputStream(content));
        System.out.println("Wrote excel file!");

        java.nio.file.Path realPath = SessionWorkSpace.getRealPath(sessionToken, roCrateMetadata);
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, realPath);
        System.out.println("Converted excel to RO-Crate!");

        Writer writer = new Writer();
        java.nio.file.Path roPath =
                java.nio.file.Path.of("result-crate" + UUID.randomUUID() + ".zip");
        java.nio.file.Path roRealPath = SessionWorkSpace.getRealPath(sessionToken,
                roPath);

        writer.write(openBisModel, roRealPath);

        return SessionWorkSpace.read(sessionToken, roPath);
    }

    private List<Sample> searchSemanticAnnotations(String sessionToken, IApplicationServerApi v3,
            List<String> identifiers)
    {
        SemanticAnnotationFetchOptions semanticAnnotationFetchOptions =
                new SemanticAnnotationFetchOptions();
        SemanticAnnotationSearchCriteria semanticAnnotationSearchCriteria =
                new SemanticAnnotationSearchCriteria();
        semanticAnnotationFetchOptions.withEntityType().withPropertyAssignments()
                .withSemanticAnnotations();
        semanticAnnotationFetchOptions.withEntityType().withPropertyAssignments().withPropertyType()
                .withSemanticAnnotations();

        semanticAnnotationSearchCriteria.withDescriptorAccessionId()
                .thatEquals("https://schema.org/CreativeWork");
        SearchResult<SemanticAnnotation>
                res1 = v3.searchSemanticAnnotations(sessionToken, semanticAnnotationSearchCriteria,
                semanticAnnotationFetchOptions);
        List<IEntityType> entityTypes =
                res1.getObjects().stream().map(x -> x.getEntityType()).collect(Collectors.toList());

        Map<IEntityType, PropertyAssignment> assignmentsToQuery = new LinkedHashMap<>();
        SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
        sampleSearchCriteria.withOrOperator();

        sampleSearchCriteria.withStringProperty("PUBLICATION.IDENTIFIER")
                .thatEquals("https://doi.org/10.1038/s41586-020-3010-5");


        SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProperties();
        sampleFetchOptions.withType();

        SearchResult<Sample> sampleSearchResult =
                v3.searchSamples(sessionToken, sampleSearchCriteria, sampleFetchOptions);

        Set<String> foundIdentifiers = sampleSearchResult.getObjects().stream()
                .map(x -> isIdentifierFoundInProperty(x, assignmentsToQuery, identifiers))
                .collect(Collectors.toSet());

        Set<String> identifiersToSearchViaCOde = Set.of();

        Set<String> identifiersToSearchViaPermId = Set.of();



        return List.of();
    }

    private String isIdentifierFoundInProperty(Sample sample,
            Map<IEntityType, PropertyAssignment> assignmentsToQuery, List<String> identifiers)
    {

        PropertyAssignment assignment = assignmentsToQuery.get(sample.getType());
        String value =
                sample.getProperties().get(assignment.getPropertyType().getCode()).toString();
        if (identifiers.contains(value))
        {
            return value;
        }
        return null;

    }

    private static ExportOptions getExportOptions()
    {
        ExportOptions exportOptions = new ExportOptions();
        exportOptions.setFormats(Set.of(ExportFormat.XLSX));
        exportOptions.setWithImportCompatibility(true);
        exportOptions.setWithLevelsAbove(true);
        exportOptions.setWithReferredTypes(true);
        exportOptions.setWithLevelsBelow(true);
        exportOptions.setXlsTextFormat(XlsTextFormat.PLAIN);
        exportOptions.setZipSingleFiles(false);
        exportOptions.setWithObjectsAndDataSetsParents(true);
        exportOptions.setWithObjectsAndDataSetsOtherSpaces(true);
        return exportOptions;
    }

}
