package ch.openbis.rocrate.app.externalidentifier;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.reader.helper.ExternalIdentifierHelper;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExternalIdentifierTest
{
    @Test
    public void testOpenBisIdentifiers() throws IOException
    {
        String input =
                "src/test/resources/json2excel";
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(input);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();
        openBisModel.getEntityTypes().values().stream().forEach(x -> Assert.assertTrue(
                x.getPropertyAssignments().stream()
                        .anyMatch(y -> y.getPropertyType().getCode().equals(
                                ExternalIdentifierHelper.CODE))));
        AbstractEntityPropertyHolder abstractEntityPropertyHolder = openBisModel.getEntities()
                .get(new SampleIdentifier("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB30"));

        Assert.assertTrue(
                abstractEntityPropertyHolder.getProperties().get(ExternalIdentifierHelper.CODE)
                        .equals("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB30"));

        openBisModel.getEntities();

    }

    @Test
    public void testSciCatIdentifiers() throws IOException
    {

        String input =
                "src/test/resources/scicat/example-export-2025-08-15";
        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(input);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();
        openBisModel.getEntityTypes().values().stream().forEach(x -> Assert.assertTrue(
                x.getPropertyAssignments().stream()
                        .anyMatch(y -> y.getPropertyType().getCode().equals(
                                ExternalIdentifierHelper.CODE))));
        AbstractEntityPropertyHolder abstractEntityPropertyHolder = openBisModel.getEntities()
                .get(new SampleIdentifier(
                        "/DEFAULT/DEFAULT/SCHEMA_CREATIVEWORK_SCICAT_PUBLISHEDDATA_4B55CBAE-AC98-445A-A15E-1534B2A8B01F"));

        Assert.assertTrue(
                abstractEntityPropertyHolder.getProperties().get(ExternalIdentifierHelper.CODE)
                        .equals("https://doi.org/10.16907/4b55cbae-ac98-445a-a15e-1534b2a8b01f"));

        openBisModel.getEntities();
    }

}
