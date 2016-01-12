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
package com.qizx.xquery;

import com.qizx.api.*;
import com.qizx.api.util.PullStreamConverter;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Check;
import com.qizx.xdm.*;
import com.qizx.xquery.dt.*;

import org.xml.sax.InputSource;

public class ItemFactoryImpl
    implements ItemFactory
{
    private DocumentParser parser;
    
    public ItemSequence copySequence(ItemSequence sequence)
        throws EvaluationException
    {
        Check.implementation(sequence, ResultSequence.class, ItemSequence.class);
        ResultSequence orig = (ResultSequence) sequence;
        XQValue v = ArraySequence.copy(orig.getValues().bornAgain());
        return new ResultSequence(v);
    }

    public Item createItem(boolean value)
    {
        return new SingleBoolean(value);
    }

    public Item createItem(double value)
    {
        return new SingleDouble(value);
    }

    public Item createItem(float value)
    {
        return new SingleFloat(value);
    }

    public Item createItem(long value, ItemType type)
        throws EvaluationException
    {
        if (type != null && !(type instanceof IntegerType))
            throw new EvaluationException("invalid type: "
                                          + "should derive from xs:integer");
        
        IntegerType itype = (IntegerType) ((type != null)? type : XQType.INTEGER);
        if (value < itype.lowerBound() || value > itype.upperBound())
            throw new XQTypeException("invalid value " + value
                                      + " for type " + itype);

        return new SingleInteger(value, itype);
    }

    public Item createItem(Object object, ItemType type)
        throws EvaluationException
    {
        if(type != null)
            Check.implementation(type, XQItemType.class, ItemType.class);
        return JavaMapping.convertToItem(object, (XQItemType) type);
    }

    public Item createItem(InputSource source)
        throws EvaluationException
    {
        if(parser == null)
            parser = new DocumentParser();
        FONIDocument doc;
        try {
            doc = parser.parseDocument(source);
            FONIDataModel dm = new FONIDataModel(doc);
            return dm.getDocumentNode();
        }
        catch (Exception e) {
            throw new EvaluationException(e.getMessage(), e);
        }
    }

    public Item createItem(XMLPullStream source)
        throws EvaluationException
    {
        try {
            return PullStreamConverter.buildNode(source);
        }
        catch (DataModelException e) {
            BasicNode.wrapDMException(e); 
            return null;
        } 
    }

    public ItemSequence createSequence(Object object, 
                                       com.qizx.api.SequenceType type)
        throws EvaluationException
    {
        return new ResultSequence(JavaMapping.convertToSequence(object,
                                                          (SequenceType) type));
    }

    public QName getQName(String localName)
    {
        return IQName.get(localName);
    }

    public QName getQName(String localName, String namespaceURI)
    {
        return IQName.get(namespaceURI, localName);
    }

    public QName getQName(String localName, String namespaceURI, String prefix)
    {
        return XQName.get(namespaceURI, localName, prefix);
    }

    public ItemType getType(String name)
    {
        if(name.startsWith("xs:"))  // hack
            name = name.substring(3);
        return XQType.findItemType(IQName.get(NamespaceContext.XSD, name));
    }

    public ItemType getNodeType(int nodeKind, QName name)
    {
        switch (nodeKind) {
        case Node.DOCUMENT:
            return name == null ? XQType.DOCUMENT : newNodeType(nodeKind, name);
        case Node.ELEMENT:
            return name == null ? XQType.ELEMENT : newNodeType(nodeKind, name);
        case Node.ATTRIBUTE:
            return name == null ? XQType.ATTRIBUTE : newNodeType(nodeKind, name);
        case Node.TEXT:
            return XQType.TEXT;
        case Node.COMMENT:
            return XQType.COMMENT;
        case Node.PROCESSING_INSTRUCTION:
            return name == null ? XQType.PI: newNodeType(nodeKind, name);
        }
        return null;
    }

    private NodeType newNodeType(int nodeKind, QName name)
    {
        return new NodeType(new BaseNodeFilter(nodeKind,
                                  name.getNamespaceURI(), name.getLocalPart()));
    }
}
