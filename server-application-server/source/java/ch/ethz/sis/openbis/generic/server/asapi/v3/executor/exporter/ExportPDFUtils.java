package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.exporter;


import ch.ethz.sis.openbis.generic.server.xls.export.helper.AbstractXLSExportHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ExportPDFUtils
{

    static final Pattern HSL_COLOR_PATTERN = Pattern.compile("color:hsl\\(.*?\\);");
    private static final Pattern hslBackgroundColorPattern = Pattern.compile("background-color:hsl\\(.*?\\);");
    private static String styleCSS = null;
    static final String COMMON_STYLE = "border: 1px solid black;";
    static final String TABLE_STYLE = COMMON_STYLE + " border-collapse: collapse;";

    /*
     * Replaces the HSL colors found by the pattern with their Hex representation in place, without creating a copy of the document.
     * The builder is compacted in a single pass: the text following a replacement is moved left by the number of characters saved so far,
     * which keeps the complexity at O(n) instead of shifting the whole remainder of a potentially very large document on every match.
     */
    public static void replaceHSLToHex(final StringBuilder html, final String cssProperty, final Pattern pattern)
    {
        final Matcher matcher = pattern.matcher(html);
        // Everything before readIndex has been processed, and the result of it occupies everything before writeIndex.
        int readIndex = 0;
        int writeIndex = 0;

        while (matcher.find(readIndex)) {
            final int matchStart = matcher.start();
            final int matchEnd = matcher.end();

            final String[] hslParts = html.substring(matchStart + 10, matchEnd - 2).replace("%", "").split(",");
            final String hex = hslToHex(Float.parseFloat(hslParts[0]) / 360, Float.parseFloat(hslParts[1]) / 100,
                    Float.parseFloat(hslParts[2]) / 100);
            final String hexColor = cssProperty + ": " + hex + ";";

            writeIndex = moveCharacters(html, readIndex, matchStart, writeIndex);

            if (writeIndex + hexColor.length() <= matchEnd) {
                for (int i = 0; i < hexColor.length(); i++) {
                    html.setCharAt(writeIndex++, hexColor.charAt(i));
                }
                readIndex = matchEnd;
            } else {
                // The replacement does not fit into the space freed so far, so the unprocessed text has to be shifted right.
                // (Matcher.find(int) resets the matcher, so it picks up the new length of the builder.)
                html.replace(writeIndex, matchEnd, hexColor);
                writeIndex += hexColor.length();
                readIndex = writeIndex;
            }
        }

        writeIndex = moveCharacters(html, readIndex, html.length(), writeIndex);
        html.setLength(writeIndex);
    }

    /**
     * Moves the characters in the range [from, to) of the builder to the given destination, which must not be after <code>from</code>.
     *
     * @return the index right after the last moved character
     */
    private static int moveCharacters(final StringBuilder builder, final int from, final int to, final int destination)
    {
        if (destination == from) {
            return to;
        }

        int writeIndex = destination;
        for (int readIndex = from; readIndex < to; readIndex++) {
            builder.setCharAt(writeIndex++, builder.charAt(readIndex));
        }
        return writeIndex;
    }

    public static String hslToHex(double hue, double saturation, double lightness) {
        // Convert HSL to RGB
        int rgb = Color.HSBtoRGB((float) hue, (float) saturation, (float) lightness);

        // Get the RGB components
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        // Convert RGB to Hex
        String hex = String.format("#%02X%02X%02X", red, green, blue);

        return hex;
    }

    public static void addStyleHeader(final Document document) throws IOException
    {
        if (styleCSS == null) {
            InputStream is = ExportPDFUtils.class.getResourceAsStream("content-styles-css-2.css");
            // The style sheet contains HSL colors too, so they are converted like the ones of the document.
            final StringBuilder css = new StringBuilder(new String(readInputStream(is)));
            replaceHSLToHex(css, "color", HSL_COLOR_PATTERN);
            styleCSS = css.toString();
        }

        // A data node, so that the style sheet is not escaped.
        document.head().appendElement("style").appendChild(new DataNode(styleCSS));
    }

    public static byte[] readInputStream(InputStream inputStream) throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024]; // or any other buffer size you prefer
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }

    /**
     * Inserts a page break before the first second level header with the given text. If there is no such header, the document is not changed.
     *
     * @param document HTML document where the page break will be inserted
     * @param headerText text of the header before which the page break is to be added
     */
    public static void insertPageBreak(final Document document, final String headerText)
    {
        for (final Element header : document.getElementsByTag("h2"))
        {
            if (header.text().equals(headerText))
            {
                header.before("<div class=\"pagebreak\"> </div>");
                return;
            }
        }
    }

    public static String convertJsonToHtml(final JsonNode node)
    {
        JsonNode data = node.get("values");
        if (data == null) {
            // backwards compatibility
            data = node.get("data");
        }

        final JsonNode styles = node.get("style");

        final StringBuilder tableBody = new StringBuilder();
        for (int i = 0; i < data.size(); i++)
        {
            final JsonNode dataRow = data.get(i);
            tableBody.append("<tr>\n");
            for (int j = 0; j < dataRow.size(); j++)
            {
                final String stylesKey = AbstractXLSExportHelper.convertNumericToAlphanumeric(i, j);
                final String stylesValue;
                if (styles == null || styles.get(stylesKey) == null) {
                    // backwards compatibility
                    stylesValue = "";
                } else {
                    stylesValue = styles.get(stylesKey).asText();
                }
                final JsonNode cell = dataRow.get(j);
                tableBody.append("  <td style='").append(COMMON_STYLE).append(" ").append(stylesValue).append("'> ").append(cell.asText())
                        .append(" </td>\n");
            }
            tableBody.append("</tr>\n");
        }
        return String.format("<table style='%s'>\n%s\n%s", TABLE_STYLE, tableBody, "</table>");
    }

    private ExportPDFUtils()
    {
    }
}
