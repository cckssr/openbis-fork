package ch.ethz.sis.openbis.generic.excel.v3.from.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ImageExtractor

{

    record FileMapping(Path path, Path readPath)
    {
    }

    record UpdatedDocAndReferences(String doc, List<String> refs)
    {
    }

    public static List<Path> findAndUpdateImages(Sample sample)
    {

        Map<String, PropertyType> codeToPropertyType =
                sample.getType().getPropertyAssignments().stream().map(x -> x.getPropertyType())
                        .collect(Collectors.toMap(x -> x.getCode(), x -> x));
        List<String> accum = new ArrayList<>();
        Map<String, String> propertyCodeToNewVal = new LinkedHashMap<>();

        for (Map.Entry<String, Serializable> property : sample.getProperties().entrySet())
        {
            PropertyType propertyType = codeToPropertyType.get(property.getKey());
            List<String> updatedVals = new ArrayList<>();

            if (isRichText(propertyType))
            {
                if (property.getValue() == null)
                {
                    continue;
                }

                Serializable[] vals;
                if (property.getValue() instanceof Serializable[])
                {
                    vals = (Serializable[]) property.getValue();
                } else
                {
                    vals = new Serializable[] { property.getValue() };
                }

                for (Serializable a : vals)
                {
                    try
                    {
                        UpdatedDocAndReferences refsAndUpdate = getWithUpdate(a.toString());
                        accum.addAll(refsAndUpdate.refs());
                        updatedVals.add(refsAndUpdate.doc());
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                Serializable putVal =
                        updatedVals.size() == 1 ? updatedVals.get(0) : updatedVals.toArray();

                sample.getProperties().put(property.getKey(), putVal);

            }
        }
        List<Path> paths = accum.stream().map(Path::of).collect(Collectors.toList());

        return paths;
    }

    static boolean isRichText(PropertyType propertyType)
    {

        return propertyType.getDataType() == DataType.MULTILINE_VARCHAR &&
                Objects.equals(propertyType.getMetaData().get("custom_widget"), "Word Processor");

    }

    static UpdatedDocAndReferences getWithUpdate(final String initialPropertyValue)
            throws IOException
    {
        List<String> images = new ArrayList<>();
        final Document doc = Jsoup.parse(initialPropertyValue);
        final Elements imageElements = doc.select("img");
        for (final Element imageElement : imageElements)
        {
            // Ex: /openbis/openbis/file-service/eln-lims/45/92/b2/4592b240-86a2-4f1a-99d3-598ef288b847/37504e5b-2d89-4129-98d1-1fd4cbe456e8.jpg
            final String imageSrc = imageElement.attr(
                    "src"); // Were the image is stored, this is what should go into the HasPart
            String shortened = shorten(imageSrc).toString();
            imageElement.attr("src", shortened);
            images.add(shortened);
        }
        return new UpdatedDocAndReferences(doc.toString(), images);
    }

    static Path shorten(String a)
    {
        String[] split = a.split("/");

        List<String> parts =
                new ArrayList<>(List.of("xlsx", "miscellaneous", "file-service", "eln-lims"));
        for (int i = 5; i < split.length; i++)
        {
            parts.add(split[i]);
        }

        Path path = Path.of(a);

        return Path.of(parts.stream().collect(Collectors.joining("/")));

    }

}
