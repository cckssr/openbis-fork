/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.common.xml;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import ch.systemsx.cisd.base.exceptions.CheckedExceptionTunnel;

/**
 * Helper class providing convenient methods for parsing XML document with and without schema validation.
 * 
 * @author Franz-Josef Elmer
 */
public class XMLInfraStructure
{
    // =========================================================================
    // XML SECURITY FEATURES
    // =========================================================================

    private static final String FEATURE_DISALLOW_DOCTYPE =
            "http://apache.org/xml/features/disallow-doctype-decl";

    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";

    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";

    private static final String FEATURE_LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private static final SchemaFactory SCHEMA_FACTORY =
            createSecureSchemaFactory();

    // =========================================================================
    // SCHEMA FACTORY
    // =========================================================================

    private static SchemaFactory createSecureSchemaFactory()
    {
        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        setFeatureQuietly(factory,
                XMLConstants.FEATURE_SECURE_PROCESSING
        );

        setPropertyQuietly(factory,
                XMLConstants.ACCESS_EXTERNAL_DTD
        );

        setPropertyQuietly(factory,
                XMLConstants.ACCESS_EXTERNAL_SCHEMA
        );

        return factory;
    }

    /**
     * Returns a {@link DocumentBuilderFactory} hardened against XXE attacks: DOCTYPE declarations
     * are disallowed, external entity resolution and external DTD loading are disabled, XInclude
     * is off, and entity-reference expansion is off. Namespace-aware by default.
     */
    public static DocumentBuilderFactory createSecureDocumentBuilderFactory()
    {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        configureCommonXXEProtection(factory);

        try
        {
            factory.setXIncludeAware(false);
        } catch (UnsupportedOperationException ignored)
        {
        }

        try
        {
            factory.setExpandEntityReferences(false);
        } catch (UnsupportedOperationException ignored)
        {
        }

        setAttributeQuietly(factory,
                XMLConstants.ACCESS_EXTERNAL_DTD
        );

        setAttributeQuietly(factory,
                XMLConstants.ACCESS_EXTERNAL_SCHEMA
        );

        return factory;
    }

    /**
     * Returns a {@link SAXParserFactory} hardened against XXE attacks: DOCTYPE declarations are
     * disallowed, external entity resolution and external DTD loading are disabled, XInclude is
     * off. Namespace-aware by default.
     */
    public static SAXParserFactory createSecureSAXParserFactory()
    {
        SAXParserFactory factory =
                SAXParserFactory.newInstance();

        factory.setNamespaceAware(true);

        configureCommonXXEProtection(factory);

        try
        {
            factory.setXIncludeAware(false);
        } catch (UnsupportedOperationException ignored)
        {
        }

        return factory;
    }

    /**
     * Returns a {@link XMLReader} backed by the hardened {@link #createSecureSAXParserFactory()}.
     * Convenience wrapper that unwraps the checked exceptions so callers don't need to declare them.
     */
    public static XMLReader createSecureXMLReader()
    {
        try
        {
            return createSecureSAXParserFactory()
                    .newSAXParser()
                    .getXMLReader();

        } catch (ParserConfigurationException | SAXException e)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(e);
        }
    }

    /**
     * Returns a {@link TransformerFactory} hardened against XSLT-based attacks: secure processing
     * is enabled, and external DTD / stylesheet access is blocked.
     */
    public static TransformerFactory createSecureTransformerFactory()
    {
        TransformerFactory factory =
                TransformerFactory.newInstance();

        try
        {
            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true);

        } catch (TransformerConfigurationException e)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(e);
        }

        setAttributeQuietly(factory,
                XMLConstants.ACCESS_EXTERNAL_DTD
        );

        setAttributeQuietly(factory,
                XMLConstants.ACCESS_EXTERNAL_STYLESHEET
        );

        return factory;
    }

    // =========================================================================
    // COMMON XXE CONFIGURATION
    // =========================================================================

    private static void configureCommonXXEProtection(
            DocumentBuilderFactory factory)
    {
        setFeatureQuietly(factory,
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true);

        setFeatureQuietly(factory,
                FEATURE_DISALLOW_DOCTYPE,
                true);

        setFeatureQuietly(factory,
                FEATURE_EXTERNAL_GENERAL_ENTITIES,
                false);

        setFeatureQuietly(factory,
                FEATURE_EXTERNAL_PARAMETER_ENTITIES,
                false);

        setFeatureQuietly(factory,
                FEATURE_LOAD_EXTERNAL_DTD,
                false);
    }

    private static void configureCommonXXEProtection(
            SAXParserFactory factory)
    {
        setFeatureQuietly(factory,
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true);

        setFeatureQuietly(factory,
                FEATURE_DISALLOW_DOCTYPE,
                true);

        setFeatureQuietly(factory,
                FEATURE_EXTERNAL_GENERAL_ENTITIES,
                false);

        setFeatureQuietly(factory,
                FEATURE_EXTERNAL_PARAMETER_ENTITIES,
                false);

        setFeatureQuietly(factory,
                FEATURE_LOAD_EXTERNAL_DTD,
                false);
    }

    // =========================================================================
    // SAFE SETTERS
    // =========================================================================

    private static void setFeatureQuietly(
            SchemaFactory factory,
            String feature)
    {
        try
        {
            factory.setFeature(feature, true);

        } catch (SAXNotRecognizedException
                | SAXNotSupportedException
                | AbstractMethodError
                | NoSuchMethodError ignored)
        {
        }
    }

    private static void setFeatureQuietly(
            DocumentBuilderFactory factory,
            String feature,
            boolean value)
    {
        try
        {
            factory.setFeature(feature, value);

        } catch (ParserConfigurationException
                | AbstractMethodError
                | NoSuchMethodError ignored)
        {
        }
    }

    private static void setFeatureQuietly(
            SAXParserFactory factory,
            String feature,
            boolean value)
    {
        try
        {
            factory.setFeature(feature, value);

        } catch (ParserConfigurationException
                | SAXNotRecognizedException
                | SAXNotSupportedException
                | AbstractMethodError
                | NoSuchMethodError ignored)
        {
        }
    }

    private static void setPropertyQuietly(
            SchemaFactory factory,
            String property)
    {
        try
        {
            factory.setProperty(property, "");

        } catch (SAXNotRecognizedException
                | SAXNotSupportedException
                | AbstractMethodError
                | NoSuchMethodError ignored)
        {
        }
    }

    private static void configureParserXXEProtection(
            SAXParser parser)
    {
        setPropertyQuietly(parser,
                XMLConstants.ACCESS_EXTERNAL_DTD
        );

        setPropertyQuietly(parser,
                XMLConstants.ACCESS_EXTERNAL_SCHEMA
        );
    }

    private static void setPropertyQuietly(
            SAXParser parser,
            String property)
    {
        try
        {
            parser.setProperty(property, "");

        } catch (SAXNotRecognizedException
                | SAXNotSupportedException
                | AbstractMethodError
                | NoSuchMethodError ignored)
        {
        }
    }

    private static void setAttributeQuietly(
            DocumentBuilderFactory factory,
            String attribute)
    {
        try
        {
            factory.setAttribute(attribute, "");

        } catch (IllegalArgumentException
                | AbstractMethodError
                | NoSuchMethodError ignored)
        {
        }
    }

    private static void setAttributeQuietly(
            TransformerFactory factory,
            String attribute)
    {
        try
        {
            factory.setAttribute(attribute, "");

        } catch (IllegalArgumentException
                | AbstractMethodError
                | NoSuchMethodError ignored)
        {
        }
    }

    /**
     * Creates a Schema from a classpath resource.
     */
    public static Schema createSchema(String schemaAsClasspathResource)
    {
        return createSchema(XMLInfraStructure.class.getResourceAsStream(schemaAsClasspathResource));
    }

    /**
     * Creates a Schema from an input stream
     */
    public static Schema createSchema(InputStream schemaAsInputStream)
    {
        try
        {
            return SCHEMA_FACTORY.newSchema(new StreamSource(schemaAsInputStream));
        } catch (SAXException ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    /**
     * Creates a Schema from a file
     */
    public static Schema createSchema(File schemaFile)
    {
        try
        {
            return SCHEMA_FACTORY.newSchema(new StreamSource(schemaFile));
        } catch (SAXException ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    /**
     * Creates a Schema by a URL
     */
    public static Schema createSchema(URL schemaURL)
    {
        try
        {
            return SCHEMA_FACTORY.newSchema(schemaURL);
        } catch (SAXException ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    /**
     * Creates a Schema from a source
     */
    public static Schema createSchema(Source schemaAsSource)
    {
        try
        {
            return SCHEMA_FACTORY.newSchema(schemaAsSource);
        } catch (SAXException ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    private final SAXParserFactory parserFactory;

    private EntityResolver entityResolver;

    /**
     * Creates a new instance.
     * 
     * @param validating If <code>true</code> Schema validation is enabled.
     */
    public XMLInfraStructure(boolean validating)
    {
        parserFactory = createSecureSAXParserFactory();
        parserFactory.setValidating(validating);
    }

    /**
     * Replaces the default entity resolver by the specified one.
     */
    public void setEntityResolver(EntityResolver entityResolver)
    {
        this.entityResolver = entityResolver;
    }

    /**
     * Parses the specified XML document and deliver all content event to the specified content handler. An exception with detailed error messages is
     * thrown in case of enabled Schema validation.
     */
    public void parse(Reader xmlDocument, ContentHandler contentHandler)
    {
        try
        {
            SAXParser saxParser = parserFactory.newSAXParser();
            configureParserXXEProtection(saxParser);
            if (parserFactory.isValidating())
            {
                if (parserFactory.getSchema() == null)
                {
                    saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                            "http://www.w3.org/2001/XMLSchema");
                }
                XMLReader xmlReader = saxParser.getXMLReader();
                xmlReader.setEntityResolver(entityResolver);
                final List<SAXParseException> exceptions = new ArrayList<SAXParseException>();
                xmlReader.setErrorHandler(new ErrorHandler()
                    {
                        @Override
                        public void warning(SAXParseException exception) throws SAXException
                        {
                        }

                        @Override
                        public void fatalError(SAXParseException exception) throws SAXException
                        {
                            exceptions.add(exception);
                        }

                        @Override
                        public void error(SAXParseException exception) throws SAXException
                        {
                            exceptions.add(exception);
                        }
                    });
                xmlReader.setContentHandler(contentHandler);
                xmlReader.parse(new InputSource(xmlDocument));
                if (exceptions.isEmpty() == false)
                {
                    StringBuilder builder = new StringBuilder();
                    for (SAXParseException exception : exceptions)
                    {
                        builder.append("\n");
                        builder.append("Error in line ").append(exception.getLineNumber());
                        builder.append(" column ").append(exception.getColumnNumber());
                        builder.append(":").append(exception.getMessage());
                    }
                    throw new SAXException("XML validation errors:" + builder);
                }
            } else
            {
                XMLReader xmlReader = saxParser.getXMLReader();
                xmlReader.setContentHandler(contentHandler);
                xmlReader.parse(new InputSource(xmlDocument));
            }
        } catch (Exception ex)
        {
            throw CheckedExceptionTunnel.wrapIfNecessary(ex);
        }
    }

    // for debugging

    public static String getJaxpImplementationInfo()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(getJaxpImplementationInfo("DocumentBuilderFactory", DocumentBuilderFactory
                .newInstance().getClass()));
        sb.append("\n");
        sb.append(getJaxpImplementationInfo("XPathFactory", XPathFactory.newInstance().getClass()));
        sb.append("\n");
        sb.append(getJaxpImplementationInfo("TransformerFactory", TransformerFactory.newInstance()
                .getClass()));
        sb.append("\n");
        sb.append(getJaxpImplementationInfo("SAXParserFactory", SAXParserFactory.newInstance()
                .getClass()));
        sb.append("\n");
        sb.append(getJaxpImplementationInfo("SchemaFactory", SCHEMA_FACTORY.getClass()));
        sb.append("\n");
        return sb.toString();
    }

    private static String getJaxpImplementationInfo(String componentName, Class<?> componentClass)
    {
        CodeSource source = componentClass.getProtectionDomain().getCodeSource();
        Package p = componentClass.getPackage();

        return MessageFormat
                .format(
                        "{0} loaded from: {1},\n\timpl: {2}\n\tpackage: {3},\n\timplVendor: {4},\n\tspecVer: {5},\n\timplVer: {6}",
                        componentName, source == null ? "Java Runtime" : source.getLocation(),
                        componentClass.getName(), p.getName(), p.getImplementationVendor(), p
                                .getSpecificationVersion(), p.getImplementationVersion());
    }

}