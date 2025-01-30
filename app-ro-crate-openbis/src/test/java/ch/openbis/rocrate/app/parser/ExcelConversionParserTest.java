package ch.openbis.rocrate.app.parser;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.entity.AbstractEntity;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import junit.framework.TestCase;
import org.junit.Test;
import ch.openbis.rocrate.app.parser.results.ParseResult;

import java.io.IOException;

public class ExcelConversionParserTest extends TestCase
{
    static final String PATH = "src/test/resources/metadata.xlsx";

    @Test
    public void testExampleFile() throws IOException
    {
        String[] paths = { PATH };
        ExcelConversionParser excelConversionParser =
                new ExcelConversionParser(paths);
        ParseResult result = excelConversionParser.start();

        assertEquals(1, result.getProjects().size());
        assertEquals(1, result.getSpaceResult().size());
        assertEquals(3, result.getMetadata().size());
        assertEquals(3, result.getSchema().size());
        assertTrue(
                result.getSchema().values().stream().anyMatch(x -> x.getCode().equals("RAW_DATA")));
        assertTrue(result.getSchema().values().stream()
                .anyMatch(x -> x.getCode().equals("COLLECTION")));
        assertTrue(result.getSchema().values().stream().anyMatch(x -> x.getCode().equals("ENTRY")));
        assertEquals(2,
                result.getMetadata().values().stream().filter(x -> x instanceof Sample).count());
        assertEquals(1, result.getMetadata().values().stream().filter(x -> x instanceof Experiment)
                .count());
        AbstractEntity sample1 =
                result.getMetadata().get(new SampleIdentifier("/JOHN/JOHN:ENTRY1"));
        assertEquals("Title A", sample1.getProperties().get("NAME"));
        assertEquals("<h2>Title A</h2><p>Content A</p>", sample1.getProperties().get("DOCUMENT"));

        AbstractEntity sample2 =
                result.getMetadata().get(new SampleIdentifier("/JOHN/JOHN:ENTRY2"));
        assertEquals("Title B", sample2.getProperties().get("NAME"));
        assertEquals("<h2>Title B</h2><p>Content B</p>", sample2.getProperties().get("DOCUMENT"));

        assertTrue(
                result.getMetadata().containsKey(new ExperimentIdentifier("JOHN", "JOHN", "JOHN")));

        {
            IEntityType entryType =
                    result.getSchema().get(new EntityTypePermId("ENTRY", EntityKind.SAMPLE));
            assertTrue(entryType.getPropertyAssignments().stream()
                    .anyMatch(x -> x.getPropertyType().getCode().equals("NAME")));
            assertTrue(entryType.getPropertyAssignments().stream().anyMatch(
                    x -> x.getPropertyType().getCode().equals("SHOW_IN_PROJECT_OVERVIEW")));
            assertTrue(entryType.getPropertyAssignments().stream()
                    .anyMatch(x -> x.getPropertyType().getCode().equals("DOCUMENT")));
            assertTrue(entryType.getPropertyAssignments().stream()
                    .map(PropertyAssignment::getPropertyType)
                    .filter(x -> x.getCode().equals("NAME")).anyMatch(x -> x.getDataType().equals(
                            DataType.VARCHAR)));
            assertTrue(entryType.getPropertyAssignments().stream()
                    .map(PropertyAssignment::getPropertyType)
                    .filter(x -> x.getCode().equals("SHOW_IN_PROJECT_OVERVIEW"))
                    .anyMatch(x -> x.getDataType().equals(
                            DataType.BOOLEAN)));
            assertTrue(entryType.getPropertyAssignments().stream()
                    .map(PropertyAssignment::getPropertyType)
                    .filter(x -> x.getCode().equals("DOCUMENT"))
                    .anyMatch(x -> x.getDataType().equals(
                            DataType.MULTILINE_VARCHAR)));

        }

        {
            IEntityType collectionType = result.getSchema()
                    .get(new EntityTypePermId("COLLECTION", EntityKind.EXPERIMENT));
            assertTrue(collectionType.getPropertyAssignments().stream()
                    .anyMatch(x -> x.getPropertyType().getCode().equals("NAME")));
            assertTrue(collectionType.getPropertyAssignments().stream()
                    .anyMatch(x -> x.getPropertyType().getCode().equals("DEFAULT_OBJECT_TYPE")));
            assertTrue(collectionType.getPropertyAssignments().stream().anyMatch(
                    x -> x.getPropertyType().getCode().equals("DEFAULT_COLLECTION_VIEW")));
            assertTrue(collectionType.getPropertyAssignments().stream()
                    .map(PropertyAssignment::getPropertyType)
                    .filter(x -> x.getCode().equals("NAME")).anyMatch(x -> x.getDataType().equals(
                            DataType.VARCHAR)));
            assertTrue(collectionType.getPropertyAssignments().stream()
                    .map(PropertyAssignment::getPropertyType)
                    .filter(x -> x.getCode().equals("DEFAULT_OBJECT_TYPE"))
                    .anyMatch(x -> x.getDataType().equals(
                            DataType.VARCHAR)));

        }

        {
            IEntityType rawDatType =
                    result.getSchema().get(new EntityTypePermId("RAW_DATA", EntityKind.DATA_SET));
            assertTrue(rawDatType.getPropertyAssignments().stream()
                    .anyMatch(x -> x.getPropertyType().getCode().equals("NAME")));
            assertTrue(rawDatType.getPropertyAssignments().stream()
                    .anyMatch(x -> x.getPropertyType().getCode().equals("NOTES")));
            assertTrue(rawDatType.getPropertyAssignments().stream()
                    .anyMatch(x -> x.getPropertyType().getCode().equals("XMLCOMMENTS")));
            assertTrue(rawDatType.getPropertyAssignments().stream()
                    .map(PropertyAssignment::getPropertyType)
                    .filter(x -> x.getCode().equals("NAME")).anyMatch(x -> x.getDataType().equals(
                            DataType.VARCHAR)));

        }

    }

}