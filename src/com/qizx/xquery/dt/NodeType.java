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
package com.qizx.xquery.dt;

import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.DocumentParser;
import com.qizx.xdm.NodeFilter;
import com.qizx.xquery.*;
import com.qizx.xquery.op.Expression;

import org.xml.sax.InputSource;

import java.io.StringReader;

/**
 * Represents any node type.
 */
public class NodeType extends XQItemType
{
    // details of the node type: can be null if node()
    public NodeFilter nodeFilter;

    public NodeType(NodeFilter nodeFilter)
    {
        this.nodeFilter = nodeFilter;
        parent = XQType.NODE; // unless set by class Type
    }

    public boolean accepts(XQType valueType)
    {
        valueType = valueType.itemType();
        if (!(valueType instanceof NodeType))
            return false;
        // TODO: too lax
        NodeFilter otherTest = ((NodeType) valueType).nodeFilter;
        return nodeFilter == null || otherTest != null
               && nodeFilter.accepts(otherTest.getNodeKind(), null)
               || otherTest == null && nodeFilter.accepts(-1, null);
    }

    public boolean encompasses(XQItemType valueType)
    {
        // System.err.println("ENCOMPASSES?");
        return accepts(valueType);
    }

    public boolean acceptsItem(XQItem item)
    {
        // System.err.println(this+" acceptsItem "+item);
        if (!item.isNode())
            return false;
        try {
            Node node = item.getNode(); // throws exception if not a node
            return nodeFilter == null || nodeFilter.accepts(node);
        }
        catch (EvaluationException e) {
            e.printStackTrace();
            return false; // cannot happen due to check above
        }
    }

    public String toString(BasicStaticContext ctx)
    {
        return toString();
    }

    public String toString()
    {
        int kind = nodeFilter == null ? -1 : nodeFilter.getNodeKind();
        if (kind < 0)
            return "node()";
        // return nodeKind(kind) + "("+ nodeFilter + ")";
        return nodeKind(kind) + "()";
    }

    public String getShortName()
    {
        return "node";
//        int kind = nodeFilter == null ? -1 : nodeFilter.getNodeKind();
//        if (kind < 0)
//            return "node";
//        return nodeKind(kind);
    }

    public int quickCode()
    {
        return QT_UNTYPED;
    }

    public static String nodeKind(int kind)
    {
        switch (kind) {
        case Node.DOCUMENT:
            return "document";
        case Node.ELEMENT:
            return "element";
        case Node.TEXT:
            return "text";
        case Node.COMMENT:
            return "comment";
        case Node.PROCESSING_INSTRUCTION:
            return "processing-instruction";
        case Node.ATTRIBUTE:
            return "attribute";
        case Node.NAMESPACE:
            return "namespace";
        default:
            return "illegal node";
        }
    }

    static public XQItemType getTypeByKind(int kind)
    {
        switch (kind) {
        case Node.DOCUMENT:
            return XQType.DOCUMENT;
        case Node.ELEMENT:
            return XQType.ELEMENT;
        case Node.ATTRIBUTE:
            return XQType.ATTRIBUTE;
        case Node.TEXT:
            return XQType.TEXT;
        case Node.PROCESSING_INSTRUCTION:
            return XQType.PI;
        case Node.COMMENT:
            return XQType.COMMENT;
        case Node.NAMESPACE:
            return XQType.NAMESPACE;
        default:
            throw new RuntimeException("wrong node kind " + kind);
        }
    }

    public XQValue convertFromObject(Object object) throws XQTypeException
    {
        if (object instanceof BasicNode)
            return new SingleNode((BasicNode) object);
        else if (object == null)
            return XQValue.empty;
        else if(object instanceof String) {
            String s = (String) object;
            InputSource input = new InputSource(new StringReader(s));
            try {
                return new SingleNode((BasicNode) DocumentParser.parse(input ));
            }
            catch (Exception e) {
                throw new XQTypeException("string to Node conversion", e);
            }
        }
        else //TODO? convert DOM etc ? or externally?
            return invalidCast(this);
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        return expr.evalAsNode(focus, context);
    }

    public XQValue convertFromArray(Object object) throws XQTypeException
    {
        if(object instanceof BasicNode)
            return new SingleItem((BasicNode) object);
        if(Node[].class.isAssignableFrom(object.getClass())) {
            Node[] result = (Node[]) object;
            return new ArraySequence(result, result.length);
        }
        return invalidCast(this);
    }

    public Object convertToArray(XQValue value)
        throws EvaluationException
    {
        return ArraySequence.expandNodes(value);
    }
}
