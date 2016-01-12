/*
 *    Qizx/open 4.1
 *
 * This code is the open-source version of Qizx.
 * Copyright (C) 2004-2009 Axyana Software -- All rights reserved.
 *
 * The contents of this file are subject to the Mozilla Public License 
 *  Version 1.1 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 * The Initial Developer of the Original Code is Xavier Franc - Axyana Software.
 *
 */
package com.qizx.xdm;

import com.qizx.api.DataModelException;
import com.qizx.api.Node;
import com.qizx.api.XMLPushStream;
import com.qizx.api.util.SAXToPushStream;
import com.qizx.util.basic.FileUtil;
import com.qizx.util.basic.PathUtil;

import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 *	Utility for document parsing and data model building.
 * <p>
 * Supports XML catalogs through Norman Walsh's
 * <code>com.sun.resolver.tools.CatalogResolver</code>.
 */
public class DocumentParser
{
    final static String SAXLEX =
        "http://xml.org/sax/properties/lexical-handler";
    final static String XINCLUDE = "http://apache.org/xml/features/xinclude";
    private static final String CHAR_ENTITIES_TXT = "commonCharEntities.txt";
    
    // hack for tests:
    private static boolean WHITESPACE_STRIPPED =
        "yes".equalsIgnoreCase(System.getProperty("qizx.stripws"));
    
    private static CatalogManager catalogManager;
    static {
        resetXMLCatalogs();
    }

    /**
     * Resets the static CatalogManager after change in system properties.
     */
    public static synchronized void resetXMLCatalogs()
    {
        // an absurdity in Crimson:
        System.setProperty("entityExpansionLimit", "1000000000");

        catalogManager = new CatalogManager(null);
        catalogManager.setIgnoreMissingProperties(true);
        catalogManager.setPreferPublic(true);
        catalogManager.setUseStaticCatalog(false);
        catalogManager.setVerbosity(0);
    }

    private synchronized EntityResolver newCatalogResolver()
    {
        CatalogManager catMan =
            localCatalogManager != null? localCatalogManager : catalogManager;

        final CatalogResolver catalogResolver = new CatalogResolver(catMan);
        // try to solve issues with DTD systemIds and Xerces:
        return new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException
            {
                InputSource entity = catalogResolver.resolveEntity(publicId, systemId);
                
                // Kludge: Xerces passes an already expanded systemId, so try
                // with only the basename (anyway needs a proper catalog)
                if(entity == null && systemId != null && publicId == null) {
                    String id2 = PathUtil.getBaseName(systemId);
                    entity = catalogResolver.resolveEntity(publicId, id2);
                }
                if(entity == null && systemId != null) {
                    String path = FileUtil.urlToFileName(systemId);
                    if(path != null && new File(path).exists())
                        return new InputSource(systemId); 
                    if(systemId.endsWith(".dtd") || systemId.endsWith(".DTD")) {
                        // dont fail on a DTD: return a fallback DTD with common char entities
                        URL loc = getClass().getResource(CHAR_ENTITIES_TXT);
                        if(loc != null) {
                            entity = new InputSource(loc.openStream());
                            entity.setSystemId("unresolved.dtd"); // not really used
                        }
                        else System.err.println("no such resource: " + CHAR_ENTITIES_TXT);
                    }
                }
                return entity;
            }
        };
    }

    // ------------- instance part -----------------------------------------
    
    protected SAXParserFactory factory;
    protected boolean stripWhiteSpace = WHITESPACE_STRIPPED;
    // Specific CatalogManager: by default use a global one
    protected CatalogManager localCatalogManager;
    
    
    public DocumentParser()
    {
        init();
    }

    protected void init()
    {
        factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
    }

    /**
     * Returns a SAX parser featured with a proper catalog-based entity resolver.
     * @throws SAXException
     */
    public XMLReader newParser(Map saxProperties)
        throws SAXException
    {
        try {
            if(saxProperties != null) {
                Iterator it = saxProperties.keySet().iterator();
                for (; it.hasNext();) {
                    String feature = (String) it.next();
                    Object value = saxProperties.get(feature);
                   
                    try {
                        factory.setFeature(feature, value == Boolean.TRUE
                                    || value instanceof String &&
                                       "true".equalsIgnoreCase((String) value));
                    }
                    catch (SAXNotRecognizedException e) {
                        throw new SAXException("unrecognized feature " + feature, e);
                    }
                    catch (SAXNotSupportedException e) {
                        throw new SAXException("non-supported feature " + feature, e);
                    }
                }
            }
            // Create a new JAXP SAXParser
            SAXParser saxParser = factory.newSAXParser();
            XMLReader saxReader = saxParser.getXMLReader();
            saxReader.setEntityResolver(newCatalogResolver());
            return saxReader;
        }
        catch (ParserConfigurationException pce) {
            //pce.printStackTrace();
            throw new SAXException("cannot create parser", pce); // can happen?
        }
    }

    /**
     * Returns a SAX parser featured with a proper catalog-based entity resolver.
     * @throws SAXException
     */
    public XMLReader newParser()
        throws SAXException
    {
        return newParser(null);
    }

    public CatalogManager getLocalCatalogManager()
    {
        return localCatalogManager;
    }

    /**
     * Sets a CatalogManager specific to this instance, which has priority
     * over the default static CatalogManager initialized from system properties.
     * @param localCatalogManager
     */
    public void setLocalCatalogManager(CatalogManager catalogManager)
    {
        this.localCatalogManager = catalogManager;
    }

    public void setStripWhiteSpace(boolean stripWhiteSpace)
    {
        this.stripWhiteSpace = stripWhiteSpace;
    }

    public boolean getStripWhiteSpace()
    {
        return stripWhiteSpace;
    }

    /**
     *  Simple document parsing.
     *  @param source the SAX InputSource.
     *  @return the document-node (root) of the parsed document.
     */
    public static Node parse(InputSource source)
        throws SAXException, IOException, DataModelException
    {
        return parse(source, (Map) null);
    }
    
    /**
     *  Simple document parsing.
     *  @param source the SAX InputSource.
     *  @return the document-node (root) of the parsed document.
     */
    static public Node parse(InputSource source, Map saxProperties)
        throws SAXException, IOException, DataModelException
    {
        DocumentParser dparser = new DocumentParser();
        IDocument doc = new IDocument();
        dparser.parseDocument(source, doc, saxProperties);
        FONIDataModel dm = new FONIDataModel(doc);
        return dm.getDocumentNode();
    }

    /**
     *  Simple document parsing.
     *  @param source the SAX InputSource.
     *  @return the document-node (root) of the parsed document.
     */
    static public Node parse(InputSource source, XMLReader parser)
        throws SAXException, IOException, DataModelException
    {
        DocumentParser dparser = new DocumentParser();
        IDocument doc = new IDocument();
        dparser.parseDocument(source, doc, parser);
        FONIDataModel dm = new FONIDataModel(doc);
        return dm.getDocumentNode();
    }

    /**
     * Simple document parsing (no caching). Helper method.
     * @param url the url of a document. Must be correctly escaped (accents,
     *        reserved chars and whitespace)
     * @return a document in memory
     * @throws org.xml.sax.SAXException
     * @throws IOException
     */
    public FONIDocument parseDocument(URL url)
        throws org.xml.sax.SAXException, IOException
    {
        // ATTENTION: intentionally no escaping (preserve HTTP)
        InputSource source = new InputSource(url.toString());

        FONIDocument parsed = null;
        try {
            parsed = parseDocument(source);
        }
        finally {
            //in.close();
        }
        return parsed;
    }


    /**
     * Simple document parsing (no caching). Helper method.
     * @param file a plain file
     * @return a document in memory
     */
    public FONIDocument parseDocument(File file)
        throws org.xml.sax.SAXException, IOException
    {
        // ATTENTION: escaping needed:
        String sid = FileUtil.fileToSystemId(file);
        InputSource source = new InputSource(sid);

        FONIDocument parsed = null;
        try {
            parsed = parseDocument(source);
        }
        finally {
            //in.close();
        }
        return parsed;
    }

    /**
     *	Simple document parsing.
     * @param source a SAX InputSource. If a systemId is used, it should be
     * a correctly escaped URI.
     * @return a document in memory
     */
    public FONIDocument parseDocument(InputSource source)
        throws SAXException, IOException
    {
        return parseDocument(source, (Map) null);
    }
    
    /**
     *  Simple document parsing.
     * @param source a SAX InputSource. If a systemId is used, it should be
     * a correctly escaped URI.
     * @return a document in memory
     */
    public FONIDocument parseDocument(InputSource source, Map saxProperties)
        throws SAXException, IOException
    {
        IDocument doc = new IDocument();
        doc.setWhitespaceStripped(stripWhiteSpace);
        parseDocument(source, doc, saxProperties);
        return doc;
    }

    /**
     *	Simple document parsing, with a specified XML Reader.
     *  <p>The XML Reader is supposed to have a proper Entity resolver.
     * @param input SAX input source
     * @param output an extension of SAX DefaultHandler
     * @param saxReader SAX parser, can be obtained by newParser() featured with
     * a proper catalog-based entity resolver.
     * @throws SAXException
     * @throws IOException
     */
    public void parseDocument(InputSource input, XDefaultHandler output,
                              XMLReader saxReader)
        throws SAXException, IOException
    {
        try {
            saxReader.setProperty(SAXLEX, output);
        }
        catch (Exception lex) {
            System.err.println("*** lexical-handler: " + lex);
        }
        saxReader.setDTDHandler(output);
        saxReader.setContentHandler(output);
        saxReader.setErrorHandler(output);

        
        saxReader.parse(input);
        
    }

    /**
     *  Simple document parsing.
     */
    public void parseDocument(InputSource source, XDefaultHandler output,
                              Map saxProperties)
        throws SAXException, IOException
    {
        parseDocument(source, output, newParser(saxProperties));
    }

    /**
     *  Simple document parsing, with a specified XML Reader.
     *  The XML Reader is supposed to have a proper Entity resolver.
     */
    public void parseDocument(InputSource input, XMLPushStream output,
                              XMLReader saxReader)
        throws SAXException, IOException
    {
        SAXToPushStream adapter = new SAXToPushStream(output);
        adapter.setWhitespaceStripped(stripWhiteSpace);
        
        parseDocument(input, adapter, saxReader);
    }

    /**
     *  Simple document parsing.
     */
    public void parseDocument(InputSource source, XMLPushStream output)
        throws SAXException, IOException
    {
        parseDocument(source, output, newParser());
    }
}
