package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IPropertyType;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.RoCrateSchemaValidation;
import ch.ethz.sis.rocrateserver.openapi.v1.service.helper.validation.ValidationResult;
import ch.openbis.rocrate.app.reader.RdfToModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RoCrateSchemaValidationRegressionCasesTest
{

    public ValidationResult getValidationResult(String location) throws JsonProcessingException
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(location).getFile());
        RoCrateReader roCrateReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateReader.readCrate(file.getAbsolutePath());
        SchemaFacade schemaFacade = SchemaFacade.of(crate);
        List<IType> types = schemaFacade.getTypes();
        List<IMetadataEntry> metadataEntries = new ArrayList<>();
        for (IType type : types)
        {
            schemaFacade.getEntries(type.getId());
            metadataEntries.addAll(metadataEntries);
        }
        List<IPropertyType> propertyTypes = schemaFacade.getPropertyTypes();
        OpenBisModel openBisModel =
                RdfToModel.convert(types, propertyTypes, metadataEntries, "DEFAULT", "DEFAULT");
        return RoCrateSchemaValidation.validate(openBisModel);

    }

    @Test
    public void testPsi20251002() throws JsonProcessingException
    {
        String location = "validation/psi.one-publication.zip";
        ValidationResult validationResult = getValidationResult(location);
        Assert.assertTrue(validationResult.isOkay());

    }

    @Test
    void testOpenBis20251002() throws JsonProcessingException
    {
        String location = "validation/openbis.one-publication.zip";
        ValidationResult validationResult = getValidationResult(location);
        Assert.assertTrue(validationResult.isOkay());

    }

}
