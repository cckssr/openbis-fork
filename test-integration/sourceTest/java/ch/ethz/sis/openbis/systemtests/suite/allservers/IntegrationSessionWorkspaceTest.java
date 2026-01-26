package ch.ethz.sis.openbis.systemtests.suite.allservers;

import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.INSTANCE_ADMIN;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.PASSWORD;
import static ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment.environment;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.ExportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.AllFields;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportableKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportablePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.XlsTextFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.systemtests.environment.IntegrationTestFacade;
import ch.ethz.sis.openbis.systemtests.suite.allservers.environment.AllServersIntegrationTestEnvironment;
import ch.systemsx.cisd.common.http.JettyHttpClientFactory;
import ch.systemsx.cisd.openbis.generic.shared.util.TestInstanceHostUtils;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.DataStoreApiUrlUtilities;

public class IntegrationSessionWorkspaceTest
{

    private IntegrationTestFacade facade;

    @BeforeSuite
    public void beforeSuite()
    {
        AllServersIntegrationTestEnvironment.start();
    }

    @AfterSuite
    public void afterSuite()
    {
        AllServersIntegrationTestEnvironment.stop();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception
    {
        facade = new IntegrationTestFacade(environment);
    }

    @Test
    public void testUploadToSessionWorkspace() throws Exception
    {
        final OpenBIS openBIS = facade.createOpenBIS();
        final String sessionToken = openBIS.login(INSTANCE_ADMIN, PASSWORD);

        final Path originalFilePath = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/import-test.zip");

        // Testing upload

        final String uploadId = openBIS.uploadToSessionWorkspace(originalFilePath);

        // Verifying upload ID

        assertTrue(uploadId.endsWith("import-test.zip"));

        // Verifying file info

        final Path uploadedFilePath = Path.of(String.format("targets/sessionWorkspace/%s/import-test.zip", sessionToken));
        final File originalFile = originalFilePath.toFile();
        final File uploadedFile = uploadedFilePath.toFile();

        assertTrue(uploadedFile.exists());
        assertEquals(uploadedFile.length(), originalFile.length());

        // Verifying file content

        final byte[] originalFileContent = Files.readAllBytes(originalFilePath);
        final byte[] uploadedFileContent = Files.readAllBytes(uploadedFilePath);

        assertEquals(uploadedFileContent, originalFileContent);

        openBIS.logout();
    }

    @Test
    public void testImport() throws Exception
    {
        final OpenBIS openBIS = facade.createOpenBIS();
        final String fileName = "import-test.zip";
        final Path originalFilePath = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/" + fileName);

        openBIS.login(INSTANCE_ADMIN, PASSWORD);
        openBIS.uploadToSessionWorkspace(originalFilePath);

        // Execute import

        final List<IObjectId> objectIds = openBIS.executeImport(new ImportData(ImportFormat.EXCEL, fileName),
                new ImportOptions(ImportMode.UPDATE_IF_EXISTS)).getObjectIds();

        // Verify imported sample

        final List<ISampleId> sampleIdentifiers = objectIds.stream().filter(objectId -> objectId instanceof ISampleId)
                .map(objectId -> (ISampleId) objectId).collect(Collectors.toList());

        System.out.println("objectIds: " + objectIds);
        assertEquals(sampleIdentifiers.size(), 1);

        final SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
        sampleFetchOptions.withProperties();
        final Sample sample = openBIS.getSamples(sampleIdentifiers, sampleFetchOptions).values().iterator().next();
        assertEquals(sample.getIdentifier().getIdentifier(), "/DEFAULT/DEFAULT/TEST");

        final String notes = sample.getStringProperty("NOTES");
        assertEquals(notes, "Test");

        // Verify imported sample type

        final List<IEntityTypeId> sampleTypes = objectIds.stream()
                .filter(objectId -> (objectId instanceof EntityTypePermId) && ((EntityTypePermId) objectId).getEntityKind() == EntityKind.SAMPLE)
                .map(objectId -> (IEntityTypeId) objectId).collect(Collectors.toList());

        assertEquals(sampleTypes.size(), 1);

        final SampleTypeFetchOptions sampleTypeFetchOptions = new SampleTypeFetchOptions();
        sampleTypeFetchOptions.withValidationPlugin().withScript();
        final SampleType sampleType = openBIS.getSampleTypes(sampleTypes, sampleTypeFetchOptions).values().iterator().next();
        final Plugin validationPlugin = sampleType.getValidationPlugin();

        assertNotNull(validationPlugin);

        assertEquals(validationPlugin.getName(), "EXPERIMENTAL_STEP.EXPERIMENTAL_STEP.EXPERIMENTAL_STEP.date_range_validation");
        assertTrue(validationPlugin.getScript().contains("\"End date cannot be before start date!\""));

        openBIS.logout();
    }

    @Test
    public void testExport() throws Exception
    {
        final OpenBIS openBIS = facade.createOpenBIS();
        final String sessionId = openBIS.login(INSTANCE_ADMIN, PASSWORD);
        final String sampleIdentifierString = "/DEFAULT/DEFAULT/DEFAULT";

        // Execute export

        final SamplePermId samplePermId = openBIS.getSamples(
                List.of(new SampleIdentifier(sampleIdentifierString)), new SampleFetchOptions()).values().iterator().next().getPermId();

        final ExportResult exportResult = openBIS.executeExport(
                new ExportData(List.of(new ExportablePermId(ExportableKind.SAMPLE, samplePermId.getPermId())), AllFields.INSTANCE),
                new ExportOptions(Set.of(ExportFormat.HTML), XlsTextFormat.PLAIN, true, false, false));

        // Verify result

        final Collection<String> warnings = exportResult.getWarnings();

        assertTrue(warnings.isEmpty());

        final String downloadURL = exportResult.getDownloadURL();

        assertNotNull(downloadURL);
        assertFalse(downloadURL.isBlank());

        final File exportedFile = new File(String.format("targets/sessionWorkspace/%s/%s",
                sessionId, getBareFileName(downloadURL)));

        assertTrue(exportedFile.exists());

        final byte[] exportedFileContents = Files.readAllBytes(exportedFile.toPath());

        assertTrue(exportedFileContents.length > 0);

        final String exportedFileString = new String(exportedFileContents);

        assertTrue(exportedFileString.startsWith("<!DOCTYPE html PUBLIC"));
        assertTrue(exportedFileString.contains(sampleIdentifierString));

        openBIS.logout();
    }

    @Test
    public void testDownloadUrlCompatibility() throws Exception
    {
        final OpenBIS openBIS = facade.createOpenBIS();
        final String sessionId = openBIS.login(INSTANCE_ADMIN, PASSWORD);
        final String sampleIdentifierString = "/DEFAULT/DEFAULT/DEFAULT";

        final SamplePermId samplePermId = openBIS.getSamples(
                        List.of(new SampleIdentifier(sampleIdentifierString)),
                        new SampleFetchOptions())
                .values().iterator().next().getPermId();

        final ExportResult exportResult = openBIS.executeExport(
                new ExportData(
                        List.of(new ExportablePermId(ExportableKind.SAMPLE, samplePermId.getPermId())),
                        AllFields.INSTANCE),
                new ExportOptions(Set.of(ExportFormat.HTML), XlsTextFormat.PLAIN, true, false, false));

        final String downloadURL = exportResult.getDownloadURL();
        assertNotNull(downloadURL);
        assertFalse(downloadURL.isBlank());

        // sanity: file really exists on disk
        final File exportedFile = new File(String.format(
                "targets/sessionWorkspace/%s/%s",
                sessionId, getBareFileName(downloadURL)));
        assertTrue(exportedFile.exists());

        HttpClient client = JettyHttpClientFactory.getHttpClient();

        // --- 1. Normal URL (baseline) ---
        assertDownloadWorks(client, downloadURL, sampleIdentifierString, "normal URL");

        // --- 2. Trailing slash ---
        String trailingSlashUrl = addTrailingSlash(downloadURL);
        assertDownloadWorks(client, trailingSlashUrl, sampleIdentifierString, "trailing slash");

        // --- 3. Double slash ---
        String doubleSlashUrl = downloadURL.replaceFirst("/([^/]+)$", "//$1");
        assertDownloadWorks(client, doubleSlashUrl, sampleIdentifierString, "double slash");

        // --- 4. Encoded slash in last path segment (%2F) ---
        if (downloadURL.contains("/"))
        {
            String encodedSlashUrl = replaceLastSlashWithEncoded(downloadURL);
            assertDownloadWorks(client, encodedSlashUrl, sampleIdentifierString, "encoded slash %2F");
        }

        openBIS.logout();
    }

    @Test
    public void testDssDownloadUrlCompatibility() throws Exception
    {
        final OpenBIS openBIS = facade.createOpenBIS();
        final String sessionId = openBIS.login(INSTANCE_ADMIN, PASSWORD);

        final Path originalFilePath = Path.of("sourceTest/resource/" + getClass().getSimpleName() + "/dss-download-test.txt");
        final String expectedContent = new String(Files.readAllBytes(originalFilePath));

        final String uploadId = openBIS.getDataStoreFacade().uploadToSessionWorkspace(originalFilePath);
        final String filePath = uploadId + "/" + originalFilePath.getFileName();

        final String dssBaseUrl = DataStoreApiUrlUtilities.getDownloadUrlFromDataStoreUrl(
                TestInstanceHostUtils.getDSSUrl());
        final String downloadURL = dssBaseUrl + "/session_workspace_file_download?sessionID=" + sessionId + "&filePath=" + filePath;

        HttpClient client = JettyHttpClientFactory.getHttpClient();

        // --- 1. Normal URL (baseline) ---
        assertDownloadWorks(client, downloadURL, expectedContent, "DSS normal URL");

        // --- 2. Trailing slash ---
        String trailingSlashUrl = addTrailingSlash(downloadURL);
        assertDownloadWorks(client, trailingSlashUrl, expectedContent, "DSS trailing slash");

        // --- 3. Double slash ---
        String doubleSlashUrl = downloadURL.replaceFirst("/([^/]+)$", "//$1");
        assertDownloadWorks(client, doubleSlashUrl, expectedContent, "DSS double slash");

        // --- 4. Encoded slash in last path segment (%2F) ---
        if (downloadURL.contains("/"))
        {
            String encodedSlashUrl = replaceLastSlashWithEncoded(downloadURL);
            assertDownloadWorks(client, encodedSlashUrl, expectedContent, "DSS encoded slash %2F");
        }


        openBIS.logout();
    }


    private String replaceLastSlashWithEncoded(String url)
    {
        int idx = url.lastIndexOf('/');
        if (idx < 0)
        {
            return url;
        }
        return url.substring(0, idx) + "%2F" + url.substring(idx + 1);
    }

    private void assertDownloadWorks(HttpClient client,
            String url,
            String expectedContent,
            String label) throws Exception
    {
        Request request = client.newRequest(url);
        ContentResponse response = request.send();

        Assert.assertEquals(
                response.getStatus(),
                200,
                "Failed for case: " + label + " with URL: " + url);
    }

    private String toggleCase(String url)
    {
        // crude but effective: flip case of path only
        int idx = url.indexOf("://");
        if (idx < 0) return url;

        int pathStart = url.indexOf('/', idx + 3);
        if (pathStart < 0) return url;

        String prefix = url.substring(0, pathStart);
        String path = url.substring(pathStart);

        StringBuilder sb = new StringBuilder();
        for (char c : path.toCharArray())
        {
            if (Character.isUpperCase(c)) sb.append(Character.toLowerCase(c));
            else if (Character.isLowerCase(c)) sb.append(Character.toUpperCase(c));
            else sb.append(c);
        }

        return prefix + sb.toString();
    }


    private static String getBareFileName(final String url)
    {
        return url.substring(url.lastIndexOf("=") + 1);
    }

    private static String addTrailingSlash(final String url)
    {
        int queryIndex = url.indexOf('?');
        if (queryIndex < 0)
        {
            return url.endsWith("/") ? url : url + "/";
        }
        if (queryIndex > 0 && url.charAt(queryIndex - 1) == '/')
        {
            return url;
        }
        return url.substring(0, queryIndex) + "/" + url.substring(queryIndex);
    }

}
