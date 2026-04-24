package ch.openbis.rocrate.app.examples.parentchild;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import edu.kit.datamanager.ro_crate.reader.ZipReader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ParentChildTest
{

    public static final String OUT_CRATE = "/tmp/parentchild.zip";

    public static final String TMP_OUT_2_ZIP = "/tmp/out2.zip";

    @Test
    public void testAParentChild() throws Exception
    {

        String input = "src/test/resources/parentchild/parent-child-simple.xlsx";
        Path path = Path.of(input);
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);
        List<Sample> samples =
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .collect(Collectors.toList());
        Assert.assertEquals(1L, samples.stream().filter(x -> !x.getParents().isEmpty()).count());
        Assert.assertEquals(1L, samples.stream().filter(x -> !x.getChildren().isEmpty()).count());

        Writer writer = new Writer();
        writer.write(openBisModel, Path.of(OUT_CRATE));

        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(OUT_CRATE);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        OpenBisModel
                openBisModel2 =
                RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                        entryList.stream().toList(), "DEFAULT",
                        "DEFAULT", schemaFacade, Map.of()).openBisModel();

        List<Sample> samples2 =
                openBisModel2.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .collect(Collectors.toList());
        Sample child =
                samples2.stream().filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY5"))
                        .findFirst().orElseThrow();

        Sample parent =
                samples2.stream().filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY4"))
                        .findFirst().orElseThrow();

        Assert.assertTrue(child.getParents().contains(parent));
        Assert.assertTrue(child.getChildren().isEmpty());

        Assert.assertTrue(parent.getChildren().contains(child));
        Assert.assertTrue(parent.getParents().isEmpty());

    }

    @Test
    public void testBParentChild() throws Exception
    {

        String input = "src/test/resources/parentchild/parent-child-more.xlsx";
        Path path = Path.of(input);
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);
        List<Sample> samples =
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .collect(Collectors.toList());
        Assert.assertEquals(3L, samples.stream().filter(x -> !x.getParents().isEmpty()).count());
        Assert.assertEquals(3L, samples.stream().filter(x -> !x.getChildren().isEmpty()).count());

        Writer writer = new Writer();
        writer.write(openBisModel, Path.of(TMP_OUT_2_ZIP));
        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(TMP_OUT_2_ZIP);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        {
            OpenBisModel
                    openBisModel2 =
                    RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                            entryList.stream().toList(), "DEFAULT",
                            "DEFAULT", schemaFacade, Map.of()).openBisModel();

            List<Sample> samples2 =
                    openBisModel2.getEntities().values().stream().filter(x -> x instanceof Sample)
                            .map(Sample.class::cast)
                            .collect(Collectors.toList());
            Sample child =
                    samples2.stream()
                            .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY5"))
                            .findFirst().orElseThrow();

            Sample parent =
                    samples2.stream()
                            .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY4"))
                            .findFirst().orElseThrow();

            Assert.assertTrue(child.getParents().contains(parent));
            Assert.assertTrue(child.getChildren().isEmpty());

            Assert.assertTrue(parent.getChildren().contains(child));
            Assert.assertTrue(parent.getParents().isEmpty());

            {
                Sample completelyNothing =
                        samples2.stream()
                                .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY6"))
                                .findFirst().orElseThrow();

                Assert.assertTrue(completelyNothing.getChildren().isEmpty());
                Assert.assertTrue(completelyNothing.getParents().isEmpty());
            }

            Sample tier1 =
                    samples2.stream()
                            .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY7"))
                            .findFirst().orElseThrow();

            Assert.assertTrue(tier1.getParents().isEmpty());


            Sample tier2 =
                    samples2.stream()
                            .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY8"))
                            .findFirst().orElseThrow();

            Sample tier3 =
                    samples2.stream()
                            .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY9"))
                            .findFirst().orElseThrow();
            Assert.assertTrue(tier1.getChildren().contains(tier2));
            Assert.assertTrue(tier2.getParents().contains(tier1));
            Assert.assertTrue(tier2.getChildren().contains(tier3));
            Assert.assertTrue(tier3.getParents().contains(tier2));
            Assert.assertTrue(tier3.getChildren().isEmpty());



        }

    }

    @Test
    public void testMultipleParentChildren() throws Exception
    {

        String input = "src/test/resources/parentchild/parent-child-multiple.xlsx";
        Path path = Path.of(input);
        OpenBisModel openBisModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);
        List<Sample> samples =
                openBisModel.getEntities().values().stream().filter(x -> x instanceof Sample)
                        .map(Sample.class::cast)
                        .collect(Collectors.toList());
        Assert.assertEquals(3L, samples.stream().filter(x -> !x.getParents().isEmpty()).count());
        Assert.assertEquals(3L, samples.stream().filter(x -> !x.getChildren().isEmpty()).count());

        Writer writer = new Writer();
        writer.write(openBisModel, Path.of(TMP_OUT_2_ZIP));
        RoCrateReader roCrateFolderReader = new RoCrateReader(new ZipReader());
        RoCrate crate = roCrateFolderReader.readCrate(TMP_OUT_2_ZIP);
        SchemaFacade schemaFacade = SchemaFacade.of(crate);

        List<IType> types = schemaFacade.getTypes();

        Set<IMetadataEntry> entryList = new LinkedHashSet<>();
        for (IType type : types)
        {
            entryList.addAll(schemaFacade.getEntries(type.getId()));

        }

        {
            OpenBisModel
                    openBisModel2 =
                    RdfToModel.convert(types, schemaFacade.getPropertyTypes(),
                            entryList.stream().toList(), "DEFAULT",
                            "DEFAULT", schemaFacade, Map.of()).openBisModel();

            List<Sample> samples2 =
                    openBisModel2.getEntities().values().stream().filter(x -> x instanceof Sample)
                            .map(Sample.class::cast)
                            .collect(Collectors.toList());

            {
                Sample child =
                        samples2.stream()
                                .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY6"))
                                .findFirst().orElseThrow();

                Sample parent1 =
                        samples2.stream()
                                .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY4"))
                                .findFirst().orElseThrow();

                Sample parent2 =
                        samples2.stream()
                                .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY5"))
                                .findFirst().orElseThrow();

                Assert.assertTrue(child.getParents().contains(parent1));
                Assert.assertTrue(child.getParents().contains(parent2));

                Assert.assertTrue(child.getChildren().isEmpty());

                Assert.assertTrue(parent1.getChildren().contains(child));
                Assert.assertTrue(parent1.getParents().isEmpty());

                Assert.assertTrue(parent1.getChildren().contains(child));
                Assert.assertTrue(parent2.getParents().isEmpty());

            }
            {

                Sample parent =
                        samples2.stream()
                                .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY7"))
                                .findFirst().orElseThrow();

                Sample child1 =
                        samples2.stream()
                                .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY8"))
                                .findFirst().orElseThrow();

                Sample child2 =
                        samples2.stream()
                                .filter(x -> x.getIdentifier().getIdentifier().contains("ENTRY9"))
                                .findFirst().orElseThrow();

                Assert.assertTrue(child1.getParents().contains(parent));
                Assert.assertTrue(child2.getParents().contains(parent));

                Assert.assertTrue(child1.getChildren().isEmpty());
                Assert.assertTrue(child2.getChildren().isEmpty());

                Assert.assertTrue(parent.getChildren().contains(child1));
                Assert.assertTrue(parent.getChildren().contains(child2));

                Assert.assertTrue(parent.getParents().isEmpty());
            }

        }

    }





}
