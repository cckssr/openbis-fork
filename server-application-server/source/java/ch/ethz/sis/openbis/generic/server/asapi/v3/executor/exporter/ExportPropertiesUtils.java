package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.exporter;

import ch.ethz.sis.openbis.generic.server.FileServiceServlet;
import ch.systemsx.cisd.common.spring.ExposablePropertyPlaceholderConfigurer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.ethz.sis.openbis.generic.server.FileServiceServlet.DEFAULT_REPO_PATH;
import static ch.ethz.sis.openbis.generic.server.FileServiceServlet.REPO_PATH_KEY;

final class ExportPropertiesUtils
{

    static final String DATA_TAG_START = "<DATA>";

    static final int DATA_TAG_START_LENGTH = DATA_TAG_START.length();

    static final String DATA_TAG_END = "</DATA>";

    static final int DATA_TAG_END_LENGTH = DATA_TAG_END.length();

    private static final String PNG_MEDIA_TYPE = "image/png";

    private static final String JPEG_MEDIA_TYPE = "image/jpeg";

    /** Buffer size for the buffer stream for Base64 encoding. Should be a multiple of 3. */
    static final int BUFFER_SIZE = 3 * 1024;

    private static final Map<String, String> MEDIA_TYPE_BY_EXTENSION = Map.of(
            ".png", PNG_MEDIA_TYPE,
            ".jpg", JPEG_MEDIA_TYPE,
            ".jpeg", JPEG_MEDIA_TYPE,
            ".jfif", JPEG_MEDIA_TYPE,
            ".pjpeg", JPEG_MEDIA_TYPE,
            ".pjp", JPEG_MEDIA_TYPE,
            ".gif", "image/gif",
            ".bmp", "image/bmp",
            ".webp", "image/webp",
            ".tiff", "image/tiff");

    private static final String DEFAULT_MEDIA_TYPE = JPEG_MEDIA_TYPE;

    private static final String DATA_PREFIX_TEMPLATE = "data:%s;base64,";

    private static final Pattern
            FILE_SERVICE_PATTERN = Pattern.compile("/openbis/" + FileServiceServlet.FILE_SERVICE_PATH + "/");


    static String encodeImages(ExposablePropertyPlaceholderConfigurer configurer, final String initialPropertyValue) throws IOException
    {
        final String propertyValue;
        final StringBuilder propertyValueBuilder = new StringBuilder(initialPropertyValue);
        final Document doc = Jsoup.parse(initialPropertyValue);
        final Elements imageElements = doc.select("img");
        for (final Element imageElement : imageElements)
        {
            final String imageSrc = imageElement.attr("src");
            if (!imageSrc.isEmpty())
            {
                replaceAll(propertyValueBuilder, imageSrc, encodeImageContentToString(configurer, imageSrc));
            }
        }
        propertyValue = propertyValueBuilder.toString();
        return propertyValue;
    }

    static void replaceAll(final StringBuilder sb, final String target, final String replacement)
    {
        // Start index for the first search
        int startIndex = sb.indexOf(target);
        while (startIndex != -1)
        {
            final int endIndex = startIndex + target.length();
            sb.replace(startIndex, endIndex, replacement);
            // Update the start index for the next search
            startIndex = sb.indexOf(target, startIndex + replacement.length());
        }
    }

    static String encodeImageContentToString(ExposablePropertyPlaceholderConfigurer configurer, final String imageSrc) throws IOException
    {
        final Base64.Encoder encoder = Base64.getEncoder();
        final int extensionIndex = imageSrc.lastIndexOf('.');

        if (extensionIndex >= 0 && !isAbsoluteUrl(imageSrc))
        {
            final String extension = imageSrc.substring(extensionIndex);
            final String mediaType = MEDIA_TYPE_BY_EXTENSION.getOrDefault(extension, DEFAULT_MEDIA_TYPE);
            final String dataPrefix = String.format(DATA_PREFIX_TEMPLATE, mediaType);

            final String filePath = getFilesRepository(configurer).getCanonicalPath() + "/" + extractFileServicePath(imageSrc);

            final StringBuilder result = new StringBuilder(dataPrefix);
            final FileInputStream fileInputStream = new FileInputStream(filePath);
            try (final BufferedInputStream in = new BufferedInputStream(fileInputStream, BUFFER_SIZE))
            {
                byte[] chunk = new byte[BUFFER_SIZE];
                int len;
                while ((len = in.read(chunk)) == BUFFER_SIZE)
                {
                    result.append(encoder.encodeToString(chunk));
                }

                if (len > 0)
                {
                    chunk = Arrays.copyOf(chunk, len);
                    result.append(encoder.encodeToString(chunk));
                }
            }

            return result.toString();
        } else
        {
            // Invalid image file or the path is absolute. We just return the initial reference.
            return imageSrc;
        }
    }

    static String extractFileServicePath(final String value)
    {
        final Matcher matcher = FILE_SERVICE_PATTERN.matcher(value);
        final boolean found = matcher.find();

        // If not match is found, it would normally mean we are in testing, so we return the value back to make it work - Volkswagen's approach :).
        return found ? value.substring(matcher.end()) : value;
    }

    public static boolean isAbsoluteUrl(final String url) {
        try {
            new URL(url);
            return true;
        } catch (final MalformedURLException e) {
            return false;
        }
    }

    private static File getFilesRepository(ExposablePropertyPlaceholderConfigurer configurer)
    {
        return new File(configurer.getPropertyValue(REPO_PATH_KEY, DEFAULT_REPO_PATH));
    }

    public static Path getFilesRepositoryPath(final ExposablePropertyPlaceholderConfigurer configurer) throws IOException
    {
        return getFilesRepository(configurer).getCanonicalFile().toPath();
    }

    private ExportPropertiesUtils() {}

    public static String referenceImage(final String imageSrc, final ExposablePropertyPlaceholderConfigurer configurer)
            throws IOException
    {
        if (imageSrc.lastIndexOf('.') >= 0 && !isAbsoluteUrl(imageSrc))
        {
            final String relativePath = extractFileServicePath(imageSrc);
            // Resolved already here, so that an invalid or missing image fails the export while the document is built, as it did before.
            ExportImageUtils.resolveImageFile(getFilesRepositoryPath(configurer), relativePath);
            return ExportImageUtils.createImageReference(relativePath);
        } else
        {
            // Invalid image file or the path is absolute. We just return the initial reference.
            return imageSrc;
        }
    }

    public static String referenceImages(final String initialPropertyValue,
                                         final ExposablePropertyPlaceholderConfigurer configurer) throws IOException
    {
        // The envelope has to go before parsing, otherwise the XML declaration ends up as a comment in the body of the parsed document.
        final Document doc = Jsoup.parseBodyFragment(DocumentBuilder.cleanXMLEnvelope(initialPropertyValue));
        final Elements imageElements = doc.select("img");

        if (imageElements.isEmpty())
        {
            // No images, so the value is left untouched to avoid the cost of serialising the document back.
            return initialPropertyValue;
        }

        // XML syntax keeps the image tags self-closed, as the PDF renderer expects. Pretty printing is switched off so that the rich text
        // of the user is handed over unchanged, apart from the image sources.
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).prettyPrint(false);

        for (final Element imageElement : imageElements)
        {
            fixImageSize(imageElement);

            final String imageSrc = imageElement.attr("src");
            if (!imageSrc.isEmpty())
            {
                imageElement.attr("src", referenceImage(imageSrc, configurer));
            }
        }

        return doc.body().html();
    }

    /**
     * Converts the pixel sizes of an image given in its <code>style</code> attribute to the <code>width</code> and <code>height</code> attributes.
     * This is done here, on the property value, rather than on the assembled document.
     *
     * @param imageElement the image element to be fixed in place
     */
    private static void fixImageSize(final Element imageElement)
    {
        final String style = imageElement.attr("style");
        final String[] rules = style.split(";");
        for (final String rule : rules)
        {
            final String[] ruleElements = rule.split(":");
            if (ruleElements.length == 2)
            {
                final String ruleKey = ruleElements[0].trim();
                final String ruleValue = ruleElements[1].trim();
                if ((ruleKey.equalsIgnoreCase("width") || ruleKey.equalsIgnoreCase("height")) && ruleValue.endsWith("px"))
                {
                    imageElement.attr(ruleKey, ruleValue.substring(0, ruleValue.length() - 2));
                }
            }
        }
    }
}
