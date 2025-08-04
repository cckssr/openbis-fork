package ch.ethz.sis.rocrateserver.openapi.v1.service.delegates;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.OpenBIS;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportFormat;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportMode;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.ethz.sis.rocrateserver.exception.RoCrateExceptions;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.RoCrateSchemaValidation;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.SessionWorkSpaceManager;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.ethz.sis.rocrateserver.openapi.v1.service.params.ImportParams;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ImportDelegate
{

    public class OpenBisImportResult
    {
        List<String> identifiers;

        Map<String, String> externalToOpenBisIdentifiers;

        ValidationResult validationResult;

        public OpenBisImportResult(List<String> identifiers,
                Map<String, String> externalToOpenBisIdentifiers,
                ValidationResult validationResult)
        {
            this.identifiers = identifiers;
            this.externalToOpenBisIdentifiers = externalToOpenBisIdentifiers;
            this.validationResult = validationResult;
        }

        public List<String> getIdentifiers()
        {
            return identifiers;
        }

        public void setIdentifiers(List<String> identifiers)
        {
            this.identifiers = identifiers;
        }

        public Map<String, String> getExternalToOpenBisIdentifiers()
        {
            return externalToOpenBisIdentifiers;
        }

        public void setExternalToOpenBisIdentifiers(
                Map<String, String> externalToOpenBisIdentifiers)
        {
            this.externalToOpenBisIdentifiers = externalToOpenBisIdentifiers;
        }

        public ValidationResult getValidationResult()
        {
            return validationResult;
        }

        public void setValidationResult(
                ValidationResult validationResult)
        {
            this.validationResult = validationResult;
        }
    }

    public OpenBisImportResult import_(
            OpenBIS openBIS,
            ImportParams headers,
            InputStream body,
            boolean validateOnly)
            throws IOException
    {

        // Unpack ro-crate
        java.nio.file.Path roCrateMetadata = java.nio.file.Path.of("ro-crate-metadata.json");
        SessionWorkSpaceManager.write(headers.getApiKey(), roCrateMetadata, body);

        // Reading ro-crate model
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(SessionWorkSpaceManager.getRealPath(headers.getApiKey(), null).toString());

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
        ValidationResult validationResult =
                RoCrateSchemaValidation.validate(conversion);

        // Convert openbis model to openbis excel format for import
        byte[] importExcel = ExcelWriter.convert(ExcelWriter.Format.EXCEL, conversion);
        java.nio.file.Path modelAsExcel = java.nio.file.Path.of(UUID.randomUUID() + ".xlsx");

        if (validateOnly) {
            return new OpenBisImportResult(List.of(), Map.of(), validationResult);
        }
        if (!validationResult.isOkay())
        {
            RoCrateExceptions.throwInstance(RoCrateExceptions.SCHEMA_VALIDATION_FAILED);
        }

        // Import
        SessionWorkSpaceManager.write(openBIS.getSessionToken(), modelAsExcel, new ByteArrayInputStream(importExcel));
        java.nio.file.Path realPath = SessionWorkSpaceManager.getRealPath(openBIS.getSessionToken(), modelAsExcel);
        openBIS.uploadToSessionWorkspace(realPath);

        ImportData importData = new ImportData();
        importData.setSessionWorkspaceFiles(new String[] { modelAsExcel.toString() });
        importData.setFormat(ImportFormat.EXCEL);
        ImportResult apiResult = openBIS.executeImport(importData, getImportOptions(headers));
        return new OpenBisImportResult(
                apiResult.getObjectIds().stream().map(id -> id.toString()).toList(),
                conversion.getExternalToOpenBisIdentifiers(), validationResult);
    }

    private static ImportOptions getImportOptions(ImportParams importParams)
    {
        ImportOptions importOptions = new ImportOptions();
        importOptions.setMode(ImportMode.valueOf(importParams.getImportMode()));
        return importOptions;
    }
}
