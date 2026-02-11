package ch.openbis.rocrate.app.writer.mapping.images;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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
        if (a.startsWith("file-service/eln-lims/"))
        {
            // This means it's an openBIS import, we don't have to rename potential naming collisions
            return prefix + a;
        }

        String[] parts = a.split("/");
        String fileName = parts[parts.length - 1];
        String[] split = fileName.split("\\.");
        parts[parts.length - 1] =
                split[0] + "-" + UUID.randomUUID() + "." + Arrays.stream(split).skip(1)
                        .collect(Collectors.joining("."));
        String b = Arrays.stream(parts).collect(Collectors.joining("/"));

        return prefix + b;
    }

}
