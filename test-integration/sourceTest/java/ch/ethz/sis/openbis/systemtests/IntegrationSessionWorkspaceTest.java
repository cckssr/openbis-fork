package ch.ethz.sis.openbis.systemtests;

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
import ch.ethz.sis.openbis.systemtests.common.AbstractIntegrationTest;

public class IntegrationSessionWorkspaceTest extends AbstractIntegrationTest
{

    @Test
    public void testUploadToSessionWorkspace() throws Exception
    {
        final OpenBIS openBIS = createOpenBIS();
        final String sessionToken = openBIS.login(INSTANCE_ADMIN, PASSWORD);

        final Path originalFilePath = Path.of("sourceTest/java/tests.xml");

        // Testing upload

        final String uploadId = openBIS.uploadToSessionWorkspace(originalFilePath);

        // Verifying upload ID

        assertTrue(uploadId.endsWith("tests.xml"));

        // Verifying file info

        final Path uploadedFilePath = Path.of(String.format("targets/sessionWorkspace/%s/tests.xml", sessionToken));
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
        final OpenBIS openBIS = createOpenBIS();
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
        final OpenBIS openBIS = createOpenBIS();
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

    private static String getBareFileName(final String url)
    {
        return url.substring(url.lastIndexOf("=") + 1);
    }

}
