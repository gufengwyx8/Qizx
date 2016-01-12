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
package com.qizx.api.util;

import com.qizx.api.DataModelException;
import com.qizx.api.Node;

import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;

/**
 * Allows a Node to be used as a source of SAX events.
 * <p>
 * The parse methods traverse the sub-tree of the node and generate SAX events.
 * The specified input is ignored.
 */
public class NodeXMLReader
    implements XMLReader
{
    static final String FEATURE_NS =
        "http://xml.org/sax/features/namespaces";
    static final String FEATURE_NSPREFIXES =
        "http://xml.org/sax/features/namespace-prefixes";
    static final String PROPERTY_LEXICAL =
        "http://xml.org/sax/properties/lexical-handler";

    private PushStreamToSAX saxout = new PushStreamToSAX();
    private DTDHandler dtdHandler; // not used
    private EntityResolver entityResolver; // not used

    private Node rootNode;

    /**
     * Builds a NodeXMLReader with a Node as a source.
     * @param rootNode a Node of the XQuery Data Model
     */
    public NodeXMLReader(Node rootNode)
    {
        this.rootNode = rootNode;
    }

    private void exec()
        throws SAXException
    {
        try {
            saxout.setNsPrefixes(true);
            saxout.reset();
            saxout.putNodeCopy(rootNode, 0);
            saxout.flush();
        }
        catch (DataModelException e) {
            throw new SAXException("traversal error", e);
        }
    }

    // -------- implementation of SAX XMLReader ------------------------------

    /**
     * Re-implementation of {@link org.xml.sax.XMLReader#parse(InputSource)},
     * ignores the InputSource.
     * @param inputSource ignored
     */ 
    public void parse(InputSource inputSource)
        throws IOException, SAXException
    {
        exec(); 
    }

    /**
     * Re-implementation of {@link org.xml.sax.XMLReader#parse(String)},
     * ignores the systemId.
     *  @param systemId ignored
     */ 
    public void parse(String systemId)
        throws IOException, SAXException
    {
        exec(); 
    }

    public ContentHandler getContentHandler()
    {
        return saxout.getContentHandler();
    }

    public void setContentHandler(ContentHandler handler)
    {
        saxout.setContentHandler(handler);
    }

    public DTDHandler getDTDHandler()
    {
        return dtdHandler;
    }

    public void setDTDHandler(DTDHandler handler)
    {
        dtdHandler = handler;
    }

    public EntityResolver getEntityResolver()
    {
        return entityResolver;
    }

    public void setEntityResolver(EntityResolver resolver)
    {
        entityResolver = resolver;
    }

    public ErrorHandler getErrorHandler()
    {
        return saxout.getErrorHandler();
    }

    public void setErrorHandler(ErrorHandler handler)
    {
        saxout.setErrorHandler(handler);
    }

    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        if (name.equals(FEATURE_NS))
            return true;
        else if (name.equals(FEATURE_NSPREFIXES)) {
            return saxout.getNsPrefixes();
        }
        else {
            // System.err.println("getFeature "+name); return true;
            throw new SAXNotSupportedException(name);
        }
    }

    // @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
    public void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        if (name.equals(FEATURE_NS)) {
            if (!value) // cant help managing NS!
                throw new SAXNotSupportedException("feature " + name
                                                   + " is mandatory");
        }
        else if (name.equals(FEATURE_NSPREFIXES))
            saxout.setNsPrefixes(value);
        else
            throw new SAXNotSupportedException(name);
    }

    public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        if (name.equals(PROPERTY_LEXICAL))
            return saxout.getLexicalHandler();
        throw new SAXNotSupportedException(name);
    }

    public void setProperty(String name, Object value)
        throws SAXNotRecognizedException, SAXNotSupportedException
    {
        if (name.equals(PROPERTY_LEXICAL))
            saxout.setLexicalHandler((LexicalHandler) value);
        throw new SAXNotSupportedException(name);
    }
}
