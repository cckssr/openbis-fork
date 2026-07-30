package ch.openbis.rocrate.app.examples.psi;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntityPropertyHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SciCatExportTest
{
    static final String INPUT = "src/test/resources/scicat/example-export-2025-08-15";

    public static final EntityTypePermId
            PUBLICATION_TYPE_PERMID =
            new EntityTypePermId("SCICAT_PUBLISHEDDATA", EntityKind.SAMPLE);

    public static final EntityTypePermId PUBLICATION_INTERSECTION_TYPE =
            new EntityTypePermId("SCHEMA_CREATIVEWORK_SCICAT_PUBLISHEDDATA", EntityKind.SAMPLE);

    @Test
    public void testSciCatCrate20250815() throws IOException
    {


        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(INPUT);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();
        Optional<IEntityType>
                maybePublicatioNType =
                openBisModel.getEntityTypes().values().stream().filter(x -> x.getPermId().equals(
                        PUBLICATION_TYPE_PERMID)).findFirst();
        List<String> publicationCodes = List.of("scicatUser", "relatedPublications",
                "numberOfFiles",
                "sizeOfArchive");
        List<Sample> samples =
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .toList();
        assertEquals(6, samples.size());

        assertEquals(10, openBisModel.getEntities().size());
        assertTrue(maybePublicatioNType.isPresent());
        assertEquals(1, openBisModel.getSpaces().size());
        assertEquals(1, openBisModel.getProjects().size());
        for (String propertyCode : publicationCodes)
        {
            assertTrue(maybePublicatioNType.filter(x -> x.getPropertyAssignments().stream()
                            .anyMatch(y -> y.getPropertyType().getCode().equals(propertyCode)))
                    .isPresent());
        }
        AbstractEntityPropertyHolder

                entity =
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .filter(x -> x.getCode().contains(
                                "schema_CreativeWork_scicat_PublishedData_4b55cbae-ac98-445a-a15e-1534b2a8b01f"))
                        .findFirst().orElseThrow();
        assertEquals("0.0", entity.getProperties().get("numberOfFiles").toString());
        assertEquals("4000.0", entity.getProperties().get("sizeOfArchive").toString());

        Optional<IEntityType>
                maybePublicationIntersectionType =
                openBisModel.getEntityTypes().values().stream().filter(x -> x.getPermId().equals(
                        PUBLICATION_INTERSECTION_TYPE)).findFirst();
        assertTrue(maybePublicatioNType.isPresent());
        IEntityType intersectionEntitytype = maybePublicationIntersectionType.get();
        assertTrue(intersectionEntitytype.getPropertyAssignments().stream()
                .map(x -> x.getPropertyType())
                .filter(x -> x.getCode().contains("abstract")).findFirst().isPresent());

    }

    @Test
    public void testSciCatCrate20250815WithMissingFiles() throws IOException
    {

        RoCrateReader roCrateFolderReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateFolderReader.readCrate(INPUT + "-missing-files");
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (var type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        RdfToModel.ConversionResult convert =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of());

        Assert.assertEquals(2,
                convert.identfiersOfMissingFiles().values().stream().filter(x -> x.size() > 0)
                        .count());
        ;
    }

}
