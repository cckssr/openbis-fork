/*
 * Copyright ETH 2021 - 2023 Zürich, Scientific IT Services
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

import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;

import java.util.List;

class DocumentBuilder
{

    private static final Logger LOG = LogFactory.getLogger(LogCategory.OPERATION, DocumentBuilder.class);

    private static final String START_RICH_TEXT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body>";

    private static final String END_RICH_TEXT = "</body></html>";

    private StringBuffer doc = new StringBuffer();

    private String closedDoc;

    private boolean closed = false;

    public DocumentBuilder()
    {
        System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        startDoc();
    }

    public void setDocument(final String doc)
    {
        this.doc = new StringBuffer(doc);
        closed = true;
    }

    private void startDoc()
    {
        if (!closed)
        {
            doc.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            doc.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            doc.append("<head></head>");
            doc.append("<body>");
        }
    }

    private void endDoc()
    {
        if (!closed)
        {
            doc.append("</body>");
            doc.append("</html>");
            closed = true;
            closedDoc = fixImages(doc);
        }
    }

    public void addProperty(final String key, final String value)
    {
        if (!closed)
        {
            doc.append("<p>").append("<b>").append(key).append(": ").append("</b>").append("</p>");
            addParagraph(value);
        }
    }

    public void addParagraph(final String value)
    {
        if (!closed)
        {
            doc.append("<p>").append(cleanXMLEnvelope(value)).append("</p>");
        }
    }

    public void addHeader(final String header, final int level)
    {
        if (!closed)
        {
            doc.append("<h").append(level).append(">").append(header).append("</h").append(level).append(">");
        }
    }

    public void addTable(final List<String> headers, final List<List<String>> values)
    {
        if (!closed)
        {
            doc.append("<table style=\"border:1px solid black;margin-left:auto;margin-right:auto;\">");
            if(!headers.isEmpty())
            {
                doc.append("<tr style=\"text-align: left; page-break-inside: avoid;\">");
                for(String header : headers)
                {
                    // translateY was used because openhtmltopdf library does not handle 'vertical-align'
                    doc.append("<th>").append("<p style=\" transform: translateY(25%); \">").append(header).append("</p>").append("</th>");
                }
                doc.append("</tr>");
            }
            for(List<String> row : values)
            {
                if(!row.isEmpty())
                {
                    doc.append("<tr style=\"text-align: left; page-break-inside: avoid; \">");
                    for(String value : row)
                    {
                        // translateY was used because openhtmltopdf library does not handle 'vertical-align'
                        doc.append("<td>").append("<p style=\" transform: translateY(25%); \">").append(value).append("</p>").append("</td>");
                    }
                    doc.append("</tr>");
                }
            }
            doc.append("</table>");
        }
    }

    public String getHtml()
    {
        if (!closed)
        {
            endDoc();
        }
        return closedDoc;
    }

    private String cleanXMLEnvelope(final String value)
    {
        if (value.startsWith(START_RICH_TEXT) && value.endsWith(END_RICH_TEXT))
        {
            return value.substring(START_RICH_TEXT.length(), value.length() - END_RICH_TEXT.length());
        } else
        {
            return value;
        }
    }

    private String fixImages(StringBuffer buffer)
    {
        final Document jsoupDoc = Jsoup.parse(buffer.toString());
        jsoupDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        final Elements elements = jsoupDoc.select("img");

        // Fixes images sizes
        for (final Element element : elements)
        {
            final String style = element.attr("style");
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
                        element.attr(ruleKey, ruleValue.substring(0, ruleValue.length() - 2));
                    }
                }
            }
        }

        return jsoupDoc.html();
    }

}
