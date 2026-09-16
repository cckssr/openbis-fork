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

import ch.systemsx.cisd.common.logging.LogCategory;
import ch.systemsx.cisd.common.logging.LogFactory;
import org.apache.log4j.Logger;

import java.util.List;

class DocumentBuilder
{

    private static final Logger LOG = LogFactory.getLogger(LogCategory.OPERATION, DocumentBuilder.class);

    private static final String START_RICH_TEXT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body>";

    private static final String END_RICH_TEXT = "</body></html>";

    private StringBuilder doc = new StringBuilder();

    private boolean closed = false;

    public DocumentBuilder()
    {
        System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        startDoc();
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
            // A builder doubles its capacity whenever it grows, and the unused part would stay allocated for as long as the document is used.
            doc.trimToSize();
            closed = true;
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

    public StringBuilder getHtml()
    {
        if (!closed)
        {
            endDoc();
        }
        return doc;
    }

    /**
     * Strips the XML envelope the rich text editor wraps its values into. Package private, because the values are also parsed by
     * {@link ExportExecutor}, which has to strip the envelope before handing them over to a HTML parser.
     *
     * @param value the property value to be cleaned
     * @return the value without the XML envelope
     */
    static String cleanXMLEnvelope(final String value)
    {
        if (value.startsWith(START_RICH_TEXT) && value.endsWith(END_RICH_TEXT))
        {
            return value.substring(START_RICH_TEXT.length(), value.length() - END_RICH_TEXT.length());
        } else
        {
            return value;
        }
    }

}
