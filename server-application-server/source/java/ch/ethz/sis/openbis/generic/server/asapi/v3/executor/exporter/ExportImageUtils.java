/*
 * Copyright ETH 2026 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.exporter;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import org.apache.commons.io.output.CloseShieldOutputStream;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the content of images out of memory while documents are exported. A document only references its images, and their content is read
 * from the file repository when the document is written: Base64 encoded into the HTML file, or streamed to the PDF renderer.
 */
final class ExportImageUtils
{

    /**
     * Protocol of the image references. It is random, so that user content cannot forge a reference: the only references resolved are the ones
     * created by {@link #createImageReference(String)}.
     */
    static final String IMAGE_REFERENCE_PROTOCOL = "openbis-image-" + UUID.randomUUID();

    private static final String IMAGE_REFERENCE_PREFIX = IMAGE_REFERENCE_PROTOCOL + ":";

    /** The start of the source attribute, as serialised by jsoup. */
    private static final String SOURCE_ATTRIBUTE_START = "src=\"";

    private static final String REFERENCED_SOURCE_ATTRIBUTE_START = SOURCE_ATTRIBUTE_START + IMAGE_REFERENCE_PREFIX;

    private static final String PNG_MEDIA_TYPE = "image/png";

    private static final String JPEG_MEDIA_TYPE = "image/jpeg";

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

    /** Number of characters of the document handed to the writer at once. */
    private static final int CHARACTER_CHUNK_SIZE = 512 * 1024;

    private static final int BUFFER_SIZE = 64 * 1024;

    private ExportImageUtils()
    {
    }

    /**
     * Creates a reference to an image of the file repository.
     *
     * @param relativePath path of the image relative to the file repository
     * @return the reference, to be used as the source of the image
     */
    static String createImageReference(final String relativePath)
    {
        return IMAGE_REFERENCE_PREFIX + URLEncoder.encode(relativePath, StandardCharsets.UTF_8);
    }

    /**
     * Resolves the path of an image in the file repository.
     *
     * @param repositoryPath canonical path of the file repository
     * @param relativePath path of the image relative to the file repository
     * @return the path of the image file
     * @throws FileNotFoundException if the path is invalid, points outside of the file repository, or is not a file
     */
    static Path resolveImageFile(final Path repositoryPath, final String relativePath) throws FileNotFoundException
    {
        final Path imageFile;
        try
        {
            // Leading slashes are dropped, as the path is relative to the repository in any case.
            imageFile = repositoryPath.resolve(relativePath.replaceFirst("^/+", "")).normalize();
        } catch (final InvalidPathException e)
        {
            throw new FileNotFoundException(String.format("Invalid image path '%s'.", relativePath));
        }

        // Checked before the existence of the file, so that nothing is revealed about files outside of the repository.
        if (!imageFile.startsWith(repositoryPath))
        {
            throw new FileNotFoundException(String.format("Image path '%s' points outside of the file repository.", relativePath));
        }

        if (!Files.isRegularFile(imageFile))
        {
            throw new FileNotFoundException(String.format("Image file '%s' does not exist.", relativePath));
        }

        return imageFile;
    }

    /**
     * Writes the HTML document to the file, embedding the referenced images as Base64 encoded data URIs. The content of each image is streamed
     * from the file repository into the file, so it is never held in memory.
     *
     * @param repositoryPath canonical path of the file repository
     */
    static void writeHtml(final Path file, final StringBuilder html, final Path repositoryPath) throws IOException
    {
        final char[] chunk = new char[CHARACTER_CHUNK_SIZE];

        try (
                final OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(file), BUFFER_SIZE);
                final Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        )
        {
            int textStart = 0;
            int attributeStart;
            while ((attributeStart = html.indexOf(REFERENCED_SOURCE_ATTRIBUTE_START, textStart)) >= 0)
            {
                final int referenceStart = attributeStart + SOURCE_ATTRIBUTE_START.length();
                final int referenceEnd = html.indexOf("\"", referenceStart);
                writeCharacters(writer, html, textStart, referenceStart, chunk);

                final String relativePath = decodeImageReference(html.substring(referenceStart, referenceEnd));
                writer.write(String.format(DATA_PREFIX_TEMPLATE, getMediaType(relativePath)));
                // The Base64 content goes to the output stream directly, so what the writer has buffered has to be written out first.
                writer.flush();

                try (
                        final InputStream imageStream = Files.newInputStream(resolveImageFile(repositoryPath, relativePath));
                        // Closing the encoder writes the padding, but must not close the stream of the whole file.
                        final OutputStream base64Stream = Base64.getEncoder().wrap(new CloseShieldOutputStream(outputStream))
                )
                {
                    imageStream.transferTo(base64Stream);
                }

                textStart = referenceEnd;
            }

            writeCharacters(writer, html, textStart, html.length(), chunk);
        }
    }

    /**
     * Creates the stream factory the PDF renderer uses to read the referenced images from the file repository. It has to be registered for
     * {@link #IMAGE_REFERENCE_PROTOCOL}.
     *
     * @param repositoryPath canonical path of the file repository
     */
    static FSStreamFactory createImageStreamFactory(final Path repositoryPath)
    {
        return url -> new FSStream()
        {
            @Override
            public InputStream getStream()
            {
                try
                {
                    return Files.newInputStream(resolveImageFile(repositoryPath, decodeImageReference(url)));
                } catch (final IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public Reader getReader()
            {
                return new InputStreamReader(getStream(), StandardCharsets.UTF_8);
            }
        };
    }

    private static String decodeImageReference(final String reference)
    {
        if (!reference.startsWith(IMAGE_REFERENCE_PREFIX))
        {
            throw new IllegalArgumentException(String.format("'%s' is not an image reference.", reference));
        }

        return URLDecoder.decode(reference.substring(IMAGE_REFERENCE_PREFIX.length()), StandardCharsets.UTF_8);
    }

    private static String getMediaType(final String path)
    {
        final int extensionIndex = path.lastIndexOf('.');
        return extensionIndex >= 0 ? MEDIA_TYPE_BY_EXTENSION.getOrDefault(path.substring(extensionIndex), DEFAULT_MEDIA_TYPE) : DEFAULT_MEDIA_TYPE;
    }

    private static void writeCharacters(final Writer writer, final StringBuilder text, final int start, final int end, final char[] chunk)
            throws IOException
    {
        for (int offset = start; offset < end; offset += chunk.length)
        {
            final int count = Math.min(chunk.length, end - offset);
            text.getChars(offset, offset + count, chunk, 0);
            writer.write(chunk, 0, count);
        }
    }

}
