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
import com.qizx.api.EvaluationException;
import com.qizx.api.ItemType;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.XMLPushStream;
import com.qizx.api.util.time.Date;
import com.qizx.api.util.time.DateTime;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.util.NamespaceContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.dt.NodeType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.Collator;

/**
 * Abstract base for actual Node implementations.
 */
public abstract class BasicNode
    implements Node, XQItem, Serializable /* not quite true */
{
    public static final int ATOM_BITSET = 13;

    public String toString()
    {
        try {
            String s = getNodeKind() + "(";
            switch(getNodeNature()) {
            case ELEMENT:
                return "Element(" + getNodeName() + ")";
            case ATTRIBUTE:
                return "Attr(" + getNodeName() + ")= " + getStringValue();
            case PROCESSING_INSTRUCTION:
                return "PI(" + getNodeName() + ")= " + getStringValue() +")";
            default:
                return getNodeKind() + "(" + getStringValue() +")";
            }
        }
        catch (DataModelException e) {
            return "Node(?)";
        }
    }

    public ItemType getType()
        throws EvaluationException
    {
        return getItemType();
    }

    public XQItemType getItemType()
        throws EvaluationException
    {
        try {
            return NodeType.getTypeByKind(getNodeNature());
        }
        catch (DataModelException e) {
            throw wrapDMException(e);
        }
    }

    public boolean getBoolean()
        throws EvaluationException
    {
        return getString().length() != 0; // OPTIM
    }

    public float getFloat()
        throws EvaluationException
    {
        return Conversion.toFloat(getString());
    }

    public double getDouble()
        throws EvaluationException
    {
        return Conversion.toDouble(getString());
    }

    public double getDoubleByRules()    //  redefined in FONIDataModel
    {
        return Double.NaN;  
    }

    public long getInteger()
        throws EvaluationException
    {
        return Conversion.toInteger(getString());
    }

    public BigDecimal getDecimal()
        throws EvaluationException
    {
        return Conversion.toDecimal(getString(), true);
    }

    public QName getQName()
        throws EvaluationException
    {
        throw new EvaluationException("Node not convertible to QName");
    }
    // ATTENTION: wrong
//    public QName getQName()
//        throws EvaluationException
//    {
//        try {
//            return getNodeName();
//        }
//        catch (DataModelException e) {
//            throw wrapDMException(e);
//        }
//    }

    public long getLongAtomValue()
    {
        return -1;
    }

    public Object getAtomValue()
    {
        return null; // not implemented
    }

    public Date getDate()
        throws EvaluationException, DateTimeException
    {
        return Date.parseDate(getString().trim());
    }

    public DateTime getDateTime()
        throws EvaluationException, DateTimeException
    {
        return DateTime.parseDateTime(getString().trim());
    }

    /**
     * Returns an array of 3 strings for DTD-name, system-id and public-id
     * if such info is defined on the document, otherwise returns null.
     * @throws DataModelException 
     */
    public abstract String[] getDTDInfo() throws DataModelException;
    
    public boolean isNode()
    {
        return true;
    }

    public boolean isElement()
        throws DataModelException
    {
        return getNodeNature() == ELEMENT;
    }

    public Node getNode()
    {
        return this;
    }

    public void export(XMLPushStream writer)
        throws DataModelException
    {
        writer.putNodeCopy(this, 0);
    }

    public Node getFirstChild()
        throws DataModelException
    {
        return firstChild();
    }

    public Node getNextSibling()
        throws DataModelException
    {
        return nextSibling();
    }

    public abstract BasicNode firstChild()
        throws DataModelException;

    public abstract BasicNode nextSibling()
        throws DataModelException;

    public Node getNextNode()
        throws DataModelException
    {
        return nodeNext();
    }

    public Node getFollowingNode()
        throws DataModelException
    {
        return nodeAfter();
    }

    public BasicNode nodeNext()
        throws DataModelException
    {
        BasicNode kid = firstChild();
        return kid != null ? kid : nodeAfter();
    }

    public BasicNode nodeAfter()
        throws DataModelException
    {
        BasicNode node = this, nxt;
        while ((nxt = node.nextSibling()) == null) {
            Node parent = node.getParent();
            if (parent == null)
                return null;
            node = (BasicNode) parent;
        }
        return nxt;
    }

    public int stringValueCompareTo(Node that, Collator collator)
        throws DataModelException
    {
        String s1 = this.getStringValue(), s2 = that.getStringValue();
        return collator != null ? collator.compare(s1, s2) : s1.compareTo(s2);
    }

    public abstract NodeSequenceBase getParent(NodeFilter nodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getAncestors(NodeFilter nodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getAncestorsOrSelf(NodeFilter nodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getAttributes(NodeFilter nodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getChildren(NodeFilter NodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getDescendants(NodeFilter NodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getDescendantsOrSelf(NodeFilter NodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getFollowing(NodeFilter NodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getFollowingSiblings(NodeFilter NodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getPreceding(NodeFilter NodeFilter)
        throws DataModelException;

    public abstract NodeSequenceBase getPrecedingSiblings(NodeFilter NodeFilter)
        throws DataModelException;

    public abstract int/*DId*/ docPosition();

    /**
     * Returns the nodes matching an ID/IDREF attribute.
     * @param id value of attribute
     * @param idref true if IDREF attribute
     * @throws DataModelException
     */
    public abstract BasicNode[] getIdMatchingNodes(String id, boolean idref)
        throws DataModelException;

    /**
     * Adds all in-scope namespaces of a node to a context.
     * @param nsContext a context into which namespaces are added locally
     */
    public abstract void addInScopeNamespacesTo(NamespaceContext nsContext)
        throws DataModelException;

    /**
     * Returns true if namespace bindings are defined on this node.
     */
    public abstract boolean hasLocalNamespaces()
        throws DataModelException;

    /**
     * Adds namespaces defined locally on an element to a context.
     * @param nsContext a context into which namespaces are added locally
     * @return the number of namespace declaration actually found
     * @throws DataModelException
     */
    public abstract int addNamespacesTo(NamespaceContext nsContext)
        throws DataModelException;

    /**
     * Adds the namespaces actually used by an element and its attributes to a
     * context.
     * @param nsContext a context into which namespaces are added locally
     * @return the number of namespace declaration actually found
     */
    public abstract int addUsedNamespacesTo(NamespaceContext nsContext)
        throws DataModelException;

    public static EvaluationException wrapDMException(DataModelException e)
    {
        return new EvaluationException(e.getErrorCode(), "data model error: "
                                                         + e.getMessage(), e);
    }
}
