package ch.openbis.rocrate.app.openbis.vocabulary;

import ch.eth.sis.rocrate.SchemaFacade;
import ch.eth.sis.rocrate.facade.IMetadataEntry;
import ch.eth.sis.rocrate.facade.IType;
import ch.eth.sis.rocrate.facade.MetadataEntry;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.openbis.rocrate.app.reader.RdfToModel;
import ch.openbis.rocrate.app.writer.Writer;
import ch.openbis.rocrate.app.writer.mapping.Mapper;
import ch.openbis.rocrate.app.writer.mapping.types.MapResult;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.reader.FolderReader;
import edu.kit.datamanager.ro_crate.reader.RoCrateReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenbisVocabularyTest
{
    static final String INPUT =
            "src/test/resources/reference-from-interoperability-0.2-export.xlsx";

    static final String OUTPUT = "out/test/resources/";

    @Test
    public void vocabularyRoundTripTest() throws Exception
    {
        Path path = Paths.get(INPUT);
        OpenBisModel excelModel = ExcelReader.convert(ExcelReader.Format.EXCEL, path);
        // https://sissource.ethz.ch/sispub/ro-crate/-/tree/main/interoperability/0.1.x/lib?ref_type=heads
        Mapper mapper = new Mapper();
        MapResult rocrateModel = mapper.transform(
                excelModel); // <- Our model using only classes by Ro-Crate Profile Official Java library

        MetadataEntry metadataEntry = rocrateModel.getMetaDataEntries().stream()
                .filter(x -> x.getId().equals("/PUBLICATIONS/PUBLIC_REPOSITORIES/PUB25")).collect(
                        Collectors.toList()).stream().findFirst().orElseThrow();
        Serializable[] timeStamp =
                (Serializable[]) metadataEntry.getValues()
                        .get("openBIS:hasPUBLICATION.PUBLICATION_YEAR");

        Timestamp ts = Timestamp.from(Instant.parse(timeStamp[0].toString()));

        // Using official Ro-Crate library to generate the final Ro-Crate JSON
        Writer writer = new Writer();
        writer.write(excelModel, Path.of(OUTPUT));

        RoCrateReader roCrateReader = new RoCrateReader(new FolderReader());
        RoCrate crate = roCrateReader.readCrate(OUTPUT);
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
        Assert.assertFalse(openBisModel.getVocabularyTypes().isEmpty());

        for (Vocabulary vocabulary : excelModel.getVocabularyTypes().values())
        {
            Vocabulary readVocab = openBisModel.getVocabularyTypes().get(vocabulary.getPermId());
            Assert.assertEquals(readVocab.getDescription(), vocabulary.getDescription());

            for (VocabularyTerm vocabularyTerm : vocabulary.getTerms())
            {
                VocabularyTerm vocabularyTerm1 = readVocab.getTerms().stream()
                        .filter(x -> x.getCode().equals(vocabularyTerm.getCode())).findFirst()
                        .orElseThrow();

                Assert.assertEquals(vocabularyTerm.getDescription(),
                        vocabularyTerm1.getDescription());
                Assert.assertEquals(vocabularyTerm.getLabel(), vocabularyTerm1.getLabel());

            }
        }


    }

}
