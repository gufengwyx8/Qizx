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
import com.qizx.api.QName;
import com.qizx.util.NamespaceContext;

import org.xml.sax.Attributes;

/**
 * Manages a list of attributes in the way of SAX,
 */
public class AttributeList
    implements Attributes
{
    protected IQName[] attrNames;   // must be interned
    protected String[] attrValues;
    protected String[] attrTypes;
    protected int attrCnt;
    protected NamespaceContext nsContext = new NamespaceContext();

    public AttributeList()
    {
    }

    public void reset()
    {
        attrCnt = 0;
    }

    public boolean addAttribute(QName name, String value, String type)
    {
        // check duplicates
        for (int a = 0; a < attrCnt; a++)
            if (name.equals(attrNames[a])) {    // not == : can be a XQName
                attrValues[a] = value;
                return false; // not added
            }
        rawAddAttribute(name, value, type);
        return true;
    }

    protected void rawAddAttribute(QName name, String value, String type)
    {
        if (attrNames == null)
            return;
        if (attrCnt >= attrNames.length) {
            QName[] oldn = attrNames;
            attrNames = new IQName[oldn.length * 2];
            System.arraycopy(oldn, 0, attrNames, 0, oldn.length);
            String[] oldv = attrValues;
            attrValues = new String[attrNames.length];
            System.arraycopy(oldv, 0, attrValues, 0, oldv.length);
            oldv = attrTypes;
            attrTypes = new String[attrNames.length];
            System.arraycopy(oldv, 0, attrTypes, 0, oldv.length);
        }
        attrNames[attrCnt] = IQName.get(name);
        attrValues[attrCnt] = value;
        attrTypes[attrCnt] = type;
        ++attrCnt;
    }

    // --------------- implement sax Attributes -------------------------------

    public int getIndex(QName qName)
    {
        for (int i = attrCnt; --i >= 0;)
            if (attrNames[i] == qName)
                return i;
        return -1;
    }

    public int getIndex(String qName)
    {
        try {
            return getIndex(nsContext.expandName(qName));
        }
        catch (DataModelException e) {
            return -1; // should not happen?
        }
    }

    public int getIndex(String uri, String localName)
    {
        return getIndex(IQName.get(uri, localName));
    }

    public int getLength()
    {
        return attrCnt;
    }

    public int getAttributeCount()
    {
        return attrCnt;
    }

    public String getLocalName(int index)
    {
        return (index < 0 || index >= attrCnt) ? 
                    null : attrNames[index].getLocalPart();
    }

    public String getURI(int index)
    {
        return (index < 0 || index >= attrCnt) ?
                null : attrNames[index].getNamespaceURI();
    }

    public String getQName(int index)
    {
        return (index < 0 || index >= attrCnt) ?
                null : nsContext.prefixedName(attrNames[index]);
    }

    public String getType(int index)
    {
        String type = (attrTypes == null || index < 0 || index >= attrCnt) ?
                null : attrTypes[index];
        return type == null? "CDATA" : type;
    }

    public String getType(String qName)
    {
        return "CDATA";
    }

    public String getType(String uri, String localName)
    {
        return "CDATA";
    }

    public String getValue(int index)
    {
        return (index < 0 || index >= attrCnt) ? null : attrValues[index];
    }

    public String getValue(String qName)
    {
        return getValue(getIndex(qName));
    }

    public String getValue(String uri, String localName)
    {
        return getValue(getIndex(uri, localName));
    }

    // ---------------------------------------------------------------------

    public int getNamespaceCount()
    {
        return nsContext.getLocalSize();
    }

    public String getNamespacePrefix(int index)
    {
        return nsContext.getLocalPrefix(index);
    }

    public String getNamespaceURI(int index)
    {
        return nsContext.getLocalNamespaceURI(index);
    }

    public NamespaceContext getNamespaceContext()
    {
        return nsContext;
    }
}
