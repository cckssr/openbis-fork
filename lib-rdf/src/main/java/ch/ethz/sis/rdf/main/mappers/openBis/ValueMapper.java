package ch.ethz.sis.rdf.main.mappers.openBis;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.rdf.main.model.xlsx.SampleObjectProperty;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.SplitIRI;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ValueMapper
{

    public static final String CANONICAL_DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss Z";

    public static String mapValue(List<String> vocabularyOptionList,
            SampleObjectProperty sampleObjectProperty, PropertyType propertyType, String space,
            String project)
    {

        String value = sampleObjectProperty.value;
        if (propertyType.getDataType() == DataType.SAMPLE)
        {
            if (sampleObjectProperty.valueURI == null)
            {
                return null;
            }

            String referenceValue = SplitIRI.localname(sampleObjectProperty.valueURI);
            return String.join("/", project,
                            OpenBisModel.makeOpenBisCodeCompliant(referenceValue))
                    .toUpperCase(
                            Locale.ROOT);
        }

        //System.out.println("MAPPED: " + sampleObjectProperty + ", CONTAINS: " + vocabularyOptionList.contains(sampleObjectProperty.value) + ", OBJ: " + sampleObjectProperty.value);
        if (vocabularyOptionList.contains(sampleObjectProperty.valueURI))
        {
            return sampleObjectProperty.value.toUpperCase(Locale.ROOT);
        } else
        {
            if (!value.contains("^^"))
            {
                return value;
            } else
            {
                //convertRDFLiteral(property.getObject().replace(RESOURCE_PREFIX, ""), propertyRowValues, idx);
                String rdfLiteral = value;

                int separatorIndex = rdfLiteral.indexOf("^^");

                String lexicalValue = rdfLiteral.substring(0, separatorIndex);
                String datatypeURI = rdfLiteral.substring(separatorIndex + 2);

                Literal literal = ResourceFactory.createTypedLiteral(lexicalValue);

                if (matchUris(XSDDatatype.XSDdateTime.getURI(), datatypeURI))
                {

                    TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(
                            literal.getValue().toString().replaceAll("\"", ""));
                    Instant i = Instant.from(ta);
                    Date d = Date.from(i);
                    DateFormat dateFormat = new SimpleDateFormat(CANONICAL_DATE_FORMAT_PATTERN); // ch.systemsx.cisd.openbis.generic.shared.util.SupportedDateTimePattern.ISO_CANONICAL_DATE_PATTERN
                    String result = dateFormat.format(d);
                    return result;
                } else if (matchUris(XSDDatatype.XSDdouble.getURI(), datatypeURI))
                {
                    String a = literal.getValue().toString().replaceAll("\"", "");
                    double myDouble = Double.parseDouble(a);
                    return Double.toString(myDouble);
                } else if (matchUris(XSDDatatype.XSDint.getURI(), datatypeURI))
                {
                    String a = literal.getValue().toString().replaceAll("\"", "");
                    int myInt = Integer.parseInt(a);
                    return Integer.toString(myInt);
                } else if (matchUris(XSDDatatype.XSDboolean.getURI(), datatypeURI))
                {
                    String a = literal.getValue().toString().replaceAll("\"", "");
                    boolean myBool = Boolean.parseBoolean(a);
                    return Boolean.toString(myBool);
                } else if (matchUris(XSDDatatype.XSDanyURI.getURI(), datatypeURI))
                {
                    return literal.getString().replaceAll("\"", "");
                } else if (matchUris(XSDDatatype.XSDtime.getURI(), datatypeURI))
                {
                    String a = literal.getValue().toString().replaceAll("\"", "");
                    return a;
                } else if (matchUris(XSDDatatype.XSDgYear.getURI(), datatypeURI))
                {
                    String a = literal.getValue().toString().replaceAll("\"", "");
                    return a;
                } else if (matchUris(XSDDatatype.XSDgMonth.getURI(), datatypeURI))
                {
                    String a = literal.getValue().toString().replaceAll("\"", "");
                    return a;
                } else if (matchUris(XSDDatatype.XSDgDay.getURI(), datatypeURI))
                {
                    String a = literal.getValue().toString().replaceAll("\"", "");
                    return a;
                }

                return value;
            }
        }

    }

    private static boolean matchUris(String schemaUri, String datatypeUri)
    {
        if (schemaUri.equals(datatypeUri))
        {
            return true;
        }
        return schemaUri.replace("http://www.w3.org/2001/XMLSchema#", "xsd:").equals(datatypeUri);

    }
}
