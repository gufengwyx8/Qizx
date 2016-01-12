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
package com.qizx.api;

import java.text.Collator;

/**
 * Nodes of the Document Data Model, as defined by XQuery1/XPath2, plus
 * extensions.
 * <p>
 * <b>Caution</b>: a node of a Data Model can be represented by several
 * different instances of Node. Therefore Node instances should never be
 * compared with <code>==</code>, but with the <code>equals</code>
 * method, or possibly with the documentOrderCompareTo method.
 * <p>
 * <b><a id="cex">Common exception causes</a></b>: the node belongs to an
 * unreachable Document (deleted or belonging to a closed Library).
 */
public interface Node
    extends Item
{
    /**
     * Returns the node kind as a string: "document", "element", "attribute",
     * "text", "comment", or "processing-instruction".
     * <p>Corresponds to Data Model accessor dm:node-kind().
     * @return a String representing the node kind.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    String getNodeKind()
        throws DataModelException;

    /**
     * Returns the Qualified Name of the node.
     * <p>
     * Corresponds to Data Model accessor dm:node-name().
     * @return the name of the node (for element and attribute), the target for
     *         a processing-instruction, null if the node has no name
     *         (document, text, comment).
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    QName getNodeName()
        throws DataModelException;

    /**
     * Returns the parent node.
     * <p>
     * Corresponds to the XQuery Data Model accessor dm:parent().
     * @return the parent node or null if the node has no parent.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    Node getParent()
        throws DataModelException;

    /**
     * Returns the first child node of an element or a document node.
     * @return null if the node has no first child.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    Node getFirstChild()
        throws DataModelException;

    /**
     * Returns the next sibling Node.
     * @return null if the node has no next sibling.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    Node getNextSibling()
        throws DataModelException;

    /**
     * Returns the next Node in document order. If this node has children,
     * the next node is the first child; otherwise this is the first of the
     * following nodes (in the sense of the XPath axis 'following').
     * @return the next node, or null if the node has no successor.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    Node getNextNode()
        throws DataModelException;

    /**
     * Returns the String Value of this Node.
     * <p>
     * Corresponds to the XQuery Data Model accessor dm:string-value().
     * <p>
     * @return the string value of the node. For an element, it is the
     * concatenation of text nodes contained in the element.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    String getStringValue()
        throws DataModelException;

    /**
     * Returns the String Value of this leaf Node as a char array.
     * <p>
     * Corresponds to the XQuery Data Model accessor dm:string-value().
     * <p>
     * @return the string value of the node.
     * @throws DataModelException <a href="#cex">common causes</a>, Node is not
     * atomic/leaf.
     */
    char[] getCharValue()
        throws DataModelException;

    /**
     * Return the base-URI of this Node.
     * <p>
     * Corresponds to the XQuery Data Model accessor dm:base-uri().
     * @return the base-URI as a String
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    String getBaseURI()
        throws DataModelException;

    /**
     * Returns the URI of the document.
     * @return the document URI as a String
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    String getDocumentURI()
        throws DataModelException;

    /**
     * Returns the document node if any, else the top-level node.
     * @return the topmost ancestor node.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    Node getDocumentNode()
        throws DataModelException;

    // --------------------------------------------------------------
    // Extended accessors
    // --------------------------------------------------------------

    /** Node nature (returned by getNodeNature) for a document node. */
    static int DOCUMENT = 1;

    /** Node nature (returned by getNodeNature) for an Element node. */
    static int ELEMENT = 2;

    /** Node nature (returned by getNodeNature) for an Attribute node. */
    static int ATTRIBUTE = 3;

    /** Node nature (returned by getNodeNature) for a namespace node. */
    static int NAMESPACE = 4;

    /**
     * Node nature (returned by getNodeNature) for a processing instruction
     * node.
     */
    static int PROCESSING_INSTRUCTION = 5;

    /** Node nature (returned by getNodeNature) for a Comment node. */
    static int COMMENT = 6;

    /** Node nature (returned by getNodeNature) for a leaf text node. */
    static int TEXT = 7;

    /**
     * Node nature (returned by getNodeNature) for a typed leaf node holding a
     * boolean value.
     */
    static int ATOM_BOOLEAN = 8;

    /**
     * Node nature (returned by getNodeNature) for a typed integer node,
     * holding a long integer value.
     */
    static int ATOM_LONG = 9;

    /**
     * Node nature (returned by getNodeNature) for a typed leaf node holding a
     * double value.
     */
    static int ATOM_DOUBLE = 10;

    /**
     * Node nature (returned by getNodeNature) for typed leaf node holding a
     * value of any serializable class.
     */
    static int ATOM_ANY = 11;

    /**
     * Node nature (returned by getNodeNature) for typed leaf node holding a
     * date/time value.
     */
    static int ATOM_DATE = 12;

    /**
     * Returns a node kind in numeric form: DOCUMENT, ELEMENT etc.
     * @return an integer representing the node nature
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    int getNodeNature()
        throws DataModelException;

    /**
     * Returns true if the Node is an Element. Convenience method equivalent
     * to: <code>getNodeNature() == Node.ELEMENT</code>.
     * @return true if this node is an XML element
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    boolean isElement()
        throws DataModelException;

    /**
     * Gets an attribute of an element by name.
     * @param name qualified name of the sought attribute
     * @return An attribute node; null if not found or not applied to an
     *         element.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    Node getAttribute(QName name)
        throws DataModelException;

    /**
     * Gets the attributes of an element.
     * @return an array of attribute nodes, or null if the node has no
     *         attributes.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    Node[] getAttributes()
        throws DataModelException;

    /**
     * Returns the number of attributes of an Element. Returns 0 if the node is
     * not an Element.
     * @return an integer which is the number of attributes
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    int getAttributeCount()
        throws DataModelException;

    // ----- Extensions for typed atomic values ------------------------------

    /**
     * Returns the value of an atomic node.
     * @return Generally a String, but for typed atoms it is an object of class
     *         Boolean (ATOM_BOOLEAN), Long (ATOM_LONG), Double (ATOM_DOUBLE)
     *         or any serializable class (ATOM_ANY).
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    Object getAtomValue()
        throws DataModelException;

    /**
     * Returns the node contents as an integer value. Attempts to convert the
     * string-value if the node has not the ATOM_LONG nature.
     * @return a long value
     * @throws DataModelException if not convertible to integer; 
     * <a href="#cex">common causes</a>
     */
    long getLongAtomValue()
        throws DataModelException;

    /**
     * Returns a matching prefix for the Namespace URI by looking up the
     * in-scope namespace definitions.
     * @param nsURI a namespace URI as a String
     * @return the first suitable prefix, otherwise null if none is found.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    String getNamespacePrefix(String nsURI)
        throws DataModelException;

    /**
     * Returns a matching Namespace for the prefix by looking up the in-scope
     * namespace definitions.
     * @param prefix a prefix as a String (canbe the empty string)
     * @return the first suitable namespace URI, otherwise null if none is
     *         found.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    String getNamespaceUri(String prefix)
        throws DataModelException;

    /**
     * Compares the document order of two nodes.
     * @param that other node to compare
     * @return -1 if this node is strictly before that node in document order,
     * 0 if nodes are identical, 1 if after that node. If the two nodes belong
     * to different documents, returns an arbitrary but stable "order of
     * documents".
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    int documentOrderCompareTo(Node that)
        throws DataModelException;

    /**
     * Compares the string values of two nodes, optionally using a collation.
     * @param node other node to compare
     * @param collator an optional collator to compare string values; can be null
     * @return 0 if string values are equal, a negative integer if this string
     * value is before the string value of the other node, a positive value
     * otherwise.
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    int stringValueCompareTo(Node node, Collator collator)
        throws DataModelException;

    /**
     * Deep equality of two subtrees. Implements fn:deep-equal on Node.
     * @param node other node to compare
     * @param collator an optional collator to compare string values; can be null
     * @return true if the two nodes are deeply equal
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    boolean deepEquals(Node node, Collator collator)
        throws DataModelException;

    /**
     * Returns true if this node is an ancestor of the parameter node or the
     * node itself.
     * @param node other node to check for containment
     * @return true if the node argument is a descendant
     * @throws DataModelException <a href="#cex">common causes</a>
     */
    boolean contains(Node node)
        throws DataModelException;
}
