package ch.openbis.rocrate.app.writer.mapping.images;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.util.*;

public class ImageExtractor

{

    public record ValueAndImages(Serializable value, Map<String, String> images)
    {
    }

    public static ValueAndImages findImageAndUpdatePaths(Serializable value)
    {
        Map<String, String> oldToNewImagePaths = new LinkedHashMap<>();
        List<String> images = new ArrayList<>();
        Serializable fieldVal = value;
        if (!value.toString().startsWith("<"))
        {
            return new ValueAndImages(value, new HashMap<>());
        }

        try
        {
            final Document doc = Jsoup.parse(value.toString());
            final Elements imageElements = doc.select("img");
            for (final Element imageElement : imageElements)
            {
                final String imageSrc = imageElement.attr("src");
                String updatedPath = getUpdatedPath(imageSrc);
                imageElement.attr("src", updatedPath);

                oldToNewImagePaths.put(imageSrc, updatedPath);
            }

            fieldVal = doc.toString();

        } catch (Exception e)
        {
            // this means the string is not HTML. This is not an issue.
        }
        return new ValueAndImages(fieldVal, oldToNewImagePaths);

    }

    static String getUpdatedPath(String a)
    {
        String prefix = "xlsx/miscellaneous/file-service/eln-lims/";
        if (a.startsWith(prefix))
        {
            return a;
        }
        return prefix + a;
    }

}
