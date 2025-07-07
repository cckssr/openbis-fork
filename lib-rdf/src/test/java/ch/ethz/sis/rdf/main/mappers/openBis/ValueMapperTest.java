package ch.ethz.sis.rdf.main.mappers.openBis;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.rdf.main.model.xlsx.SampleObjectProperty;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ValueMapperTest
{

    @Test
    public void testDateConversion()
    {
        List<String> vocabs = new ArrayList<>();
        SampleObjectProperty sampleObjectProperty =
                new SampleObjectProperty(XSDDatatype.XSDdateTime.getURI(), "Datetime",
                        "\"1899-11-02T00:00:00+00:00\"^^xsd:dateTime", "");
        PropertyType propertyType = new PropertyType();
        propertyType.setDataType(DataType.DATE);

        String res = ValueMapper.mapValue(vocabs, sampleObjectProperty, propertyType, "DEFAULT",
                "DEFAULT");
        assertEquals("1899-11-02 01:00:00 +0100", res);

    }

    @Test
    public void testDateConversionTestCase()
    {
        List<String> vocabs = new ArrayList<>();
        SampleObjectProperty sampleObjectProperty =
                new SampleObjectProperty(XSDDatatype.XSDdateTime.getURI(), "Datetime",
                        "\"2024-08-31T09:30:00.000000+00:00\"^^xsd:dateTime", "");
        PropertyType propertyType = new PropertyType();
        propertyType.setDataType(DataType.DATE);

        String res = ValueMapper.mapValue(vocabs, sampleObjectProperty, propertyType, "DEFAULT",
                "DEFAULT");
        assertEquals("2024-08-31 11:30:00 +0200", res);

    }

}