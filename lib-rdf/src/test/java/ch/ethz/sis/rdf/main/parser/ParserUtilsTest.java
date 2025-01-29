package ch.ethz.sis.rdf.main.parser;

import ch.ethz.sis.rdf.main.model.rdf.ModelRDF;
import ch.ethz.sis.rdf.main.model.xlsx.SampleObject;
import ch.ethz.sis.rdf.main.model.xlsx.SampleObjectProperty;
import ch.ethz.sis.rdf.main.model.xlsx.SamplePropertyType;
import ch.ethz.sis.rdf.main.model.xlsx.SampleType;
import junit.framework.TestCase;
import org.junit.Ignore;

import java.util.List;
import java.util.Map;

public class ParserUtilsTest extends TestCase {

    @Ignore
    public void testRemoveObjectsOfUnknownType()
    {
        ModelRDF modelRDF = new ModelRDF();
        String typeCode = "MYTYPE";

        String typeURI = "typeURI";
        modelRDF.sampleTypeList = List.of(new SampleType(typeCode, typeURI));
        modelRDF.subClassChanisMap = Map.of();

        SampleObject object = new SampleObject("code", typeURI,typeCode);

        Map<String, List<SampleObject>> objects = Map.of(typeCode, List.of(object));

        ResourceParsingResult result = ParserUtils.removeObjectsOfUnknownType(modelRDF, objects, Map.of(), null);
        assertEquals(1, result.getDeletedObjects().size());



    }

    @Ignore
    public void testRemoveObjectsOfUnknownTypeWithUnknownType()
    {
        ModelRDF modelRDF = new ModelRDF();
        String typeCode = "MYTYPE";

        String typeURI = "typeURI";
        modelRDF.sampleTypeList = List.of(new SampleType(typeCode, typeURI));
        modelRDF.subClassChanisMap = Map.of();
        String typeCode2 = "MYTYPEBUTDIFFERENT";

        SampleObject object = new SampleObject("code", typeURI,typeCode2);

        Map<String, List<SampleObject>> objects = Map.of(typeCode2, List.of(object));

        ResourceParsingResult result = ParserUtils.removeObjectsOfUnknownType(modelRDF, objects, Map.of(), null);
        assertEquals(0, result.getUnchangedObjects().size());
        assertEquals(1, result.getDeletedObjects().size());

    }

    @Ignore
    public void testRemoveObjectsOfUnknownTypeWithUnknownTypeWithMandatoryProperty()
    {
        ModelRDF modelRDF = new ModelRDF();
        String typeCode = "MYTYPE";

        String typeURI = "typeURI";
        SampleType sampleType = new SampleType(typeCode, typeURI);
        String propertyType = "myProperty";
        SamplePropertyType samplePropertyType = new SamplePropertyType(propertyType, "uri");
        samplePropertyType.setMandatory(1);
        sampleType.properties = List.of(samplePropertyType);
        modelRDF.sampleTypeList = List.of(sampleType);
        modelRDF.subClassChanisMap = Map.of();

        String typeCode2 = "MYTYPEBUTDIFFERENT";

        samplePropertyType.dataType = typeCode2;

        SampleObject object = new SampleObject("code1", typeCode, typeCode);
        SampleObjectProperty sampleObjectProperty =
                new SampleObjectProperty("uri", propertyType, "code2", "valueURI");
        object.properties = List.of(sampleObjectProperty);
        SampleObject objectFromProperty = new SampleObject("code2", typeURI, typeCode2);

        Map<String, List<SampleObject>> objects = Map.of(typeCode, List.of(object), typeCode2, List.of(objectFromProperty));

        ResourceParsingResult result = ParserUtils.removeObjectsOfUnknownType(modelRDF, objects, Map.of(), null);
        assertEquals(0, result.getUnchangedObjects().size());
        assertEquals(1, result.getDeletedObjects().size());
        assertEquals(1, result.getEditedObjects().size());
    }
}