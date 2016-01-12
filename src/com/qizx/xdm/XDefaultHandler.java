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

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

/**
 * An extension of SAX2 DefaultHandler that also implements LexicalHandler.
 * <p>The EntityResolver interface should be provided separately,
 * though a support is provided by DefaultHandler.
 */
public abstract class XDefaultHandler extends DefaultHandler
    implements LexicalHandler
{
    protected int/*DId*/ docId;    // set internally
    protected Locator currentLocator;
    protected String baseURI;
    protected String dtdName = "";
    protected String dtdPublicId = "";
    protected String dtdSystemid = "";
    protected boolean takeComment = true;
    protected boolean whitespaceStripped;
    //protected ProgressHandler progressHandler;
    
    /**
     * Terminates creation of a document (required).
     */
    public void close()
        throws IOException, SAXException
    {
    }
    
    /**
     *  Returns an identifier associated with the document.
     */
    public int/*DId*/ getDocumentId() {
        return docId;
    }
    
    /**
     *  Associates an identifier with the document.
     */
    public void setDocumentId(int/*DId*/ value) {
        docId = value;
    }

    public String getBaseURI()
    {
        return baseURI;
    }

    public void setBaseURI(String baseURI)
    {
        this.baseURI = baseURI;
    }

    public String getDtdName() {
        return dtdName;
    }

    public String getDtdPublicId() {
        return dtdPublicId;
    }

    public String getDtdSystemid() {
        return dtdSystemid;
    }

    public boolean isWhitespaceStripped()
    {
        return whitespaceStripped;
    }

    public void setWhitespaceStripped(boolean stripWhitespace)
    {
        this.whitespaceStripped = stripWhitespace;
    }
    
    // normally not called
//    public InputSource resolveEntity(String publicId, String systemId)
//        throws SAXException
//    {
//        InputSource src = super.resolveEntity(publicId, systemId);
//        return src;
//    }

    // ------------- SAX2 handlers --------------------------------

    public void setDocumentLocator(Locator locator) {
        currentLocator = locator;
    }
    
    public void startDocument() throws SAXException {
        if(currentLocator != null)
            baseURI = currentLocator.getSystemId();
    }
    
    public void startDTD(String name, String publicId, String systemId)
        throws SAXException
    {
        takeComment = false;
        if (name != null)
            dtdName = name;
        if (publicId != null)
            dtdPublicId = publicId;
        if (systemId != null)
            dtdSystemid = systemId;
    }

    public void endDTD()
        throws SAXException
    {
        takeComment = true;
    }

    public abstract void characters(String chars) throws SAXException;
    
    public void atom(Object value)
        throws SAXException
    {
        throw new SAXException("not implemented");
    }
    
    public void notationDecl(String name, String publicId, String systemId)
        throws SAXException
    {
        // ignored
    }

    public void unparsedEntityDecl(String name, String publicId,
                                   String systemId, String notationName)
        throws SAXException {
        // ignored
    }

    public void skippedEntity(String name) throws SAXException {
        // ignored
    }

    public void startCDATA() throws SAXException {
        // ignored
    }

    public void endCDATA() throws SAXException {
        // ignored
    }

    public void startEntity(String name) throws SAXException {
        // ignored
    }

    public void endEntity(String name) throws SAXException {
        // ignored
    }

    protected void saxWrap (Exception e) throws SAXException {
        throw new SAXException(e);
    }
}
