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

import com.qizx.api.QName;
import com.qizx.api.XMLPullStream;

public abstract class XMLPullStreamBase
    implements XMLPullStream
{
    protected int curEvent;

    protected String encoding;
    protected String dtdName;
    protected String dtdPublicId;
    protected String dtdSystemId;
    protected String dtdInternalSubset;

    protected int attrCount;
    private QName[] attrNames = new QName[2];
    private String[] attrValues = new String[2];

    protected char[] charBuffer;
    protected int charCount;

    protected String piTarget;

    protected int nsCount;
    private QName[] namespaces; // uses prefix and uri of QNames

    public int getCurrentEvent()
    {
        return curEvent;
    }

    protected int setEvent(int event)
    {
        return curEvent = event;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public String getDTDName()
    {
        return dtdName;
    }

    public String getDTDPublicId()
    {
        return dtdPublicId;
    }

    public String getDTDSystemId()
    {
        return dtdSystemId;
    }

    public String getInternalSubset()
    {
        return dtdInternalSubset;
    }

    public int getAttributeCount()
    {
        if (attrCount < 0)
            lazyGetAttrs();
        return attrCount;
    }

    public QName getAttributeName(int index)
    {
        if (attrCount < 0)
            lazyGetAttrs();
        return attrNames[index];
    }

    public String getAttributeValue(int index)
    {
        if (attrCount < 0)
            lazyGetAttrs();
        return attrValues[index];
    }

    /**
     * To be overriden if appropriate
     */
    protected void lazyGetAttrs()
    {
    }

    protected void addAttribute(QName name, String value)
    {
        if (attrCount >= attrNames.length) {
            QName[] old = attrNames;
            attrNames = new QName[old.length * 2];
            System.arraycopy(old, 0, attrNames, 0, old.length);
            String[] oldValues = attrValues;
            attrValues = new String[attrNames.length];
            System.arraycopy(oldValues, 0, attrValues, 0, oldValues.length);
        }
        attrNames[attrCount] = name;
        attrValues[attrCount++] = value;
    }

    public int getNamespaceCount()
    {
        if (nsCount < 0)
            lazyGetNS();
        return nsCount;
    }

    public String getNamespacePrefix(int index)
    {
        if (nsCount < 0)
            lazyGetNS();
        return namespaces[index].getLocalPart();
    }

    public String getNamespaceURI(int index)
    {
        if (nsCount < 0)
            lazyGetNS();
        return namespaces[index].getNamespaceURI();
    }

    /**
     * To be overridden if appropriate
     */
    protected void lazyGetNS() {
    }

    protected void addNamespace(String prefix, String namespaceURI)
    {
        QName ns = IQName.get(namespaceURI, prefix);
        if (nsCount >= namespaces.length) {
            QName[] old = namespaces;
            namespaces = new QName[old.length * 2];
            System.arraycopy(old, 0, namespaces, 0, old.length);
        }
        namespaces[nsCount++] = ns;
    }

    public String getText()
    {
        if(charBuffer == null)
            return null;
        return new String(charBuffer, 0, charCount);
    }

    public int getTextLength()
    {
        return charCount;
    }
    
    protected void clearText()
    {
        charCount = 0;
    }
    
    protected void storeText(String s)
    {
        int osize = (charBuffer == null)? 0 : charBuffer.length;
        int nsize = s.length() + charCount;
        if(nsize > osize) {
            char[] old = charBuffer;
            charBuffer = new char[nsize + nsize / 4];
            if(osize > 0)
                System.arraycopy(old, 0, charBuffer, 0, osize);
        }
        s.getChars(0, s.length(), charBuffer, charCount);
        charCount += s.length();
    }

    protected void storeText(char[] s, int len)
    {
        int osize = (charBuffer == null)? 0 : charBuffer.length;
        int nsize = len + charCount;
        if(nsize > osize) {
            char[] old = charBuffer;
            charBuffer = new char[nsize + nsize / 4];
            if(osize > 0)
                System.arraycopy(old, 0, charBuffer, 0, osize);
        }
        System.arraycopy(s, 0, charBuffer, charCount, len);
        charCount += len;
    }

    public String getTarget()
    {
        return piTarget;
    }
}
