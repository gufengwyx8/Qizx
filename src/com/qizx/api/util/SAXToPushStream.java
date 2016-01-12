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
import com.qizx.api.QName;
import com.qizx.api.XMLPushStream;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XDefaultHandler;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Adapter from SAX2 to XMLPushStream.
 */
public class SAXToPushStream 
    // implements DTDHandler, ContentHandler, ErrorHandler, LexicalHandler, EntityResolver
    extends XDefaultHandler
{
    private XMLPushStream out;
    private String[] nsPrefix;
    private String[] nsURI;
    private int nsCount;
    private boolean discardComments;
    protected Locator locator;
   
    /**
     * Builds a SAX to XMLPushStream adapter.
     * @param output the XMLPushStream output
     */
    public SAXToPushStream(XMLPushStream output)
    {
        this.out = output;
        nsPrefix = new String[4];
        nsURI = new String[4];
    }

    // ------------ SAX implementation ---------------------------------------
    
    /** @see com.qizx.xdm.XDefaultHandler#startDocument() */
    public void startDocument() throws SAXException
    {
        discardComments = false;
        try {
            if(out != null)
                out.putDocumentStart();
        }
        catch (Exception e) {
           saxException(e);
        }
    }

    public void endDocument() throws SAXException
    {
        try {
            if(out != null)
                out.putDocumentEnd();
        }
        catch (Exception e) {
           saxException(e);
        }
    }

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
        throws SAXException
    {
        try {
            if(out == null)
                return;
            out.putElementStart(convertName(namespaceURI, localName, qName));
            if(atts != null) {
                int attrCount = atts.getLength();
                for (int a = 0; a < attrCount; a++) {
                    QName attrName = convertName(atts.getURI(a), 
                                                 atts.getLocalName(a),
                                                 atts.getQName(a));
                    out.putAttribute(attrName, 
                                     atts.getValue(a), atts.getType(a));
                }
            }
            for(int n = 0; n < nsCount; n++)
                out.putNamespace(nsPrefix[n], nsURI[n]);
            nsCount = 0;
        }
        catch (Exception e) {
            //e.printStackTrace();
            saxException(e);
        }
    }

    public void startPrefixMapping(String prefix, String uri)
        throws SAXException
    {
        // called before elementStart: need to store namespace
        if(nsCount >= nsPrefix.length) {
            String[] old = nsPrefix;
            nsPrefix = new String[old.length + 8];
            System.arraycopy(old, 0, nsPrefix, 0, old.length);
            old = nsURI;
            nsURI = new String[nsPrefix.length];
            System.arraycopy(old, 0, nsURI, 0, old.length);
        }
        nsURI[nsCount] = uri;
        nsPrefix[nsCount] = prefix;
        ++ nsCount;
    }

    public void endPrefixMapping(String prefix) throws SAXException
    {
    }

    public void endElement(String namespaceURI, String localName, String qName)
        throws SAXException
    {
        try {   // could be more efficient by remembering QNames in a stack
            if(out != null)
                out.putElementEnd(convertName(namespaceURI, localName, qName));
        }
        catch (Exception e) {
            e.printStackTrace();
            saxException(e);
        }
    }

    public void characters(char[] chars, int start, int length)
        throws SAXException
    {
        try {
            if(out != null &&
               !(whitespaceStripped && XMLUtil.isXMLSpace(chars, start, length)))
                out.putChars(chars, start, length);
        }
        catch (DataModelException e) {
            saxException(e);
        }
    }

    public void characters(String chars)
        throws SAXException
    {
        try {
            if(out != null &&
               !(whitespaceStripped && XMLUtil.isXMLSpace(chars)))
                out.putText(chars);
        }
        catch (DataModelException e) {
            saxException(e);
        }
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException
    {
        // ignored indeed
    }

    public void processingInstruction(String target, String data)
        throws SAXException
    {
        try {
            if(out != null)
                out.putProcessingInstruction(target, data);
        }
        catch (DataModelException e) {
            saxException(e);
        }
    }

    public void comment(char[] ch, int start, int length) throws SAXException
    {
        try {
            if(out != null && !discardComments)
                out.putComment(new String(ch, start, length));
        }
        catch (DataModelException e) {
            saxException(e);
        }
    }

    public void startDTD(String name, String publicId, String systemId)
        throws SAXException
    {
        try {
            if(out != null)
                out.putDTD(name, publicId, systemId, null);
            discardComments = true;
        }
        catch (DataModelException e) {
            saxException(e);
        }
    }

    public void endDTD() throws SAXException
    {
        discardComments = false;
    }


    public void setDocumentLocator(Locator locator)
    {
        this.locator = locator;
    }

    public void fatalError(SAXParseException exception) throws SAXException
    {
        throw exception;
    }

    public void error(SAXParseException exception) throws SAXException
    {
        throw exception;
    }

    public void warning(SAXParseException exception) throws SAXException
    {
        // ignored
    }

    // ------------------------ internals ------------------------------------
    
    private QName convertName(String namespaceURI, String localName,
                              String prefixedName)
    {
        // parser not NS aware:
        if(localName == null || localName.length() == 0) {
            namespaceURI = "";
            localName = prefixedName;
        }
        return IQName.get(namespaceURI, localName);
    }

    private void saxException(Exception e) throws SAXException
    {
        //e.printStackTrace();
        throw new SAXException(e.getMessage(), e);
    }
}
