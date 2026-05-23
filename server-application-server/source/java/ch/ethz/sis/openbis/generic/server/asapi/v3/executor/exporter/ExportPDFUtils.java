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

public class ExportPDFUtils
{

    static final Pattern hslColorPattern = Pattern.compile("color:hsl\\(.*?\\);");
    static final Pattern hslBackgroundColorPattern = Pattern.compile("background-color:hsl\\(.*?\\);");
    static final String COMMON_STYLE = "border: 1px solid black;";
    static final String TABLE_STYLE = COMMON_STYLE + " border-collapse: collapse;";

    /*
     * This algorithm to replace HSL to Hex colors has the benefit of having a complexity of O(n)
     * It only transverses the source and destination strings once without creating additional copies
     */
    public static String replaceHSLToHex(String html, String cssProperty, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        StringBuilder builder = null;
        while (matcher.find()) {
            if (builder == null) {
                builder = new StringBuilder(html);
            }
            String[] hslParts = html.substring(matcher.start()+10, matcher.end()-2).replace("%", "").split(",");
            String hex = hslToHex(Float.parseFloat(hslParts[0])/360, Float.parseFloat(hslParts[1])/100, Float.parseFloat(hslParts[2])/100);
            String hexColor = cssProperty + ": " + hex + ";";
            int offset = html.length() - builder.length();
            builder.replace((matcher.start() - offset), (matcher.end() - offset), hexColor);
        }
        if (builder != null) {
            html = builder.toString();
        }
        return html;
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

    static String styleCSS = null;
    public static String addStyleHeader(String replacedHtml) throws IOException
    {
        if (styleCSS == null) {
            InputStream is = ExportPDFUtils.class.getResourceAsStream("content-styles-css-2.css");
            styleCSS = new String(readInputStream(is));
        }

        return replacedHtml.replace("<head></head>", "<head><style>" + styleCSS + "</style></head>");
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

    public static String insertPagePagebreak(String html, String before) {
        return html.replace(before, "<div class=\"pagebreak\"> </div>" + before);
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
