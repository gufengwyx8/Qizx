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
import com.qizx.xdm.XMLPushStreamBase;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An implementation of XMLPushStream that converts to SAX2 events.
 * <p>This class can be used to export or convert a XML tree from Qizx to any
 * handler accepting SAX2 events.
 */
public class PushStreamToSAX extends XMLPushStreamBase
{
    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;
    private ErrorHandler errorHandler;
    // 
    private boolean nsPrefixes = !false;
    private char[] charBuf;

    /**
     * Creates an adapter without handlers.
     * <p>
     * The SAX handlers must then be defined through the set*Handler methods.
     */
    public PushStreamToSAX()
    {
        this(null);
    }

    /**
     * Creates an SAX adapter directly bound to a SAX DefaultHandler.
     * @param handler used as content handler and error handler.
     */
    public PushStreamToSAX(DefaultHandler handler)
    {
        setContentHandler(handler);
        setErrorHandler(handler);
        charBuf = new char[100];
    }

    /**
     * Gets the "NS prefixes" option: controls namespace prefix output.
     * @return true if prefixes are output to SAX
     */
    public boolean getNsPrefixes()
    {
        return nsPrefixes;
    }

    /**
     * Sets the "NS prefixes" option: controls namespace prefix output.
     * @param nsPrefixes if false, do not output prefixes in SAX
     */
    public void setNsPrefixes(boolean nsPrefixes)
    {
        this.nsPrefixes = nsPrefixes;
    }

    /**
     * Returns the current SAX content handler.
     * @return the current SAX content handler
     */
    public ContentHandler getContentHandler()
    {
        return contentHandler;
    }

    /**
     * Sets the current SAX content handler.
     * @param handler SAX content handler used as output (can be null)
     */
    public void setContentHandler(ContentHandler handler)
    {
        contentHandler = handler;
    }

    /**
     * Returns the current SAX lexical handler.
     * @return  the current SAX lexical handler
     */
    public LexicalHandler getLexicalHandler()
    {
        return lexicalHandler;
    }

    /**
     * Sets the current SAX lexical output handler.
     * @param handler SAX lexical handler used as output (can be null)
     */
    public void setLexicalHandler(LexicalHandler handler)
    {
        lexicalHandler = handler;
    }

    /**
     * Returns the current SAX error handler.
     * @return the current SAX error handler
     */
    public ErrorHandler getErrorHandler()
    {
        return errorHandler;
    }

    /**
     * Sets the current SAX error handler.
     * @param handler SAX error handler used (can be null)
     */
    public void setErrorHandler(ErrorHandler handler)
    {
        errorHandler = handler;
    }

    // -----------------------------------------------------------------------

    public boolean putDocumentStart()
        throws DataModelException
    {
        boolean ok = super.putDocumentStart();
        try {
            if (contentHandler != null)
                contentHandler.startDocument();
        }
        catch (SAXException e) {
            convertSaxException(e);
        }
        return ok;
    }

    public void putDocumentEnd()
        throws DataModelException
    {
        try {
            if (contentHandler != null)
                contentHandler.endDocument();
        }
        catch (SAXException e) {
            convertSaxException(e);
        }
        super.putDocumentEnd();
    }

    protected void flushElementStart(boolean empty)
        throws DataModelException
    {
        if (!elementStarted)
            return;
        elementStarted = false;
        try {
            String qname = getNsPrefixes() ? nsContext.prefixedName(elementName) : "";
            if (contentHandler != null)
                contentHandler.startElement(elementName.getNamespaceURI(),
                                            elementName.getLocalPart(),
                                            qname, 
                                            /*attributes*/ this);            
        }
        catch (SAXException e) {
            convertSaxException(e);
        }
    }

    public void putElementEnd(QName name)
        throws DataModelException
    {
        if (elementStarted)
            flushElementStart(true);
        if (contentHandler != null)
            try {
                String qname = // TODO avoid double creation by managing a stack
                    getNsPrefixes() ? nsContext.prefixedName(name) : "";
                contentHandler.endElement(name.getNamespaceURI(),
                                          name.getLocalPart(), qname);
                // remove mappings
                for(int m = 0, cnt = nsContext.getLocalSize(); m < cnt; m++)
                    contentHandler.endPrefixMapping(nsContext.getLocalPrefix(m));
            }
            catch (SAXException e) {
                convertSaxException(e);
            }
        super.putElementEnd(name);
    }

    public boolean putNamespace(String prefix, String namespaceURI)
        throws DataModelException
    {
        boolean needed = super.putNamespace(prefix, namespaceURI);
        try { // OK to emit NS immediately, because tag and attributes are
                // buffered.
            if (contentHandler != null && needed)
                contentHandler.startPrefixMapping(prefix, namespaceURI);
        }
        catch (SAXException e) {
            convertSaxException(e);
        }
        return needed;
    }

    public void putText(String value)
        throws DataModelException
    {
        if (value == null || value.length() == 0)
            return;
        try {
            if (elementStarted)
                flushElementStart(false);

            int vlen = value.length();
            if (charBuf == null || vlen > charBuf.length)
                charBuf = new char[vlen];
            value.getChars(0, vlen, charBuf, 0);
            if (contentHandler != null)
                contentHandler.characters(charBuf, 0, vlen);
            spaceNeeded = false;
        }
        catch (SAXException e) {
            convertSaxException(e);
        }
    }

    public void putProcessingInstruction(String target, String value)
        throws DataModelException
    {
        try {
            if (elementStarted)
                flushElementStart(false);
            if (contentHandler != null)
                contentHandler.processingInstruction(target, value);
        }
        catch (SAXException e) {
            convertSaxException(e);
        }
    }

    public void putComment(String value)
        throws DataModelException
    {
        try {
            if (elementStarted)
                flushElementStart(false);
            int vlen = value.length();
            if (charBuf == null || vlen > charBuf.length)
                charBuf = new char[vlen];
            value.getChars(0, vlen, charBuf, 0);
            if (lexicalHandler != null)
                lexicalHandler.comment(charBuf, 0, vlen);
        }
        catch (SAXException e) {
            convertSaxException(e);
        }
    }

    // ------------------------------------------------------------------------

    private void convertSaxException(SAXException e)
        throws DataModelException
    {
        throw new DataModelException("SAX exception: "+e.getMessage());
    }
}
