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

    private static final String DATA_PREFIX = "xlsx/miscellaneous/file-service/eln-lims/";
    private static final String EMBEDDED_IMAGE_PREFIX = "/openbis/openbis/file-service/eln-lims/";

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
                String imagePath = getImagePath(imageSrc);
                imageElement.attr("src", imagePath);

                oldToNewImagePaths.put(imageSrc, updatedPath);
            }

            fieldVal = doc.toString();

        } catch (Exception e)
        {
            // this means the string is not HTML. This is not an issue.
        }
        return new ValueAndImages(fieldVal, oldToNewImagePaths);

    }

    private static String getImagePath(String path) {
        if (path != null && path.startsWith(DATA_PREFIX))
        {
            return EMBEDDED_IMAGE_PREFIX + path.substring(DATA_PREFIX.length());
        }
        return path;
    }

    static String getUpdatedPath(String imagePath)
    {

        if (imagePath.startsWith(DATA_PREFIX))
        {
            return imagePath;
        }

        if (imagePath.startsWith("file-service/eln-lims/"))
        {
            // This means it's an openBIS import, we don't have to rename potential naming collisions
            return DATA_PREFIX + imagePath;
        }

        String[] parts = imagePath.split("/");
        String fileName = parts[parts.length - 1];
        String[] split = fileName.split("\\.");
        parts[parts.length - 1] =
                split[0] + "-" + UUID.randomUUID() + "." + Arrays.stream(split).skip(1)
                        .collect(Collectors.joining("."));
        String b = Arrays.stream(parts).collect(Collectors.joining("/"));

        return DATA_PREFIX + b;
    }

}
