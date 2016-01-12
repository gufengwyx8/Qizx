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

/**
 * Fully-Ordered Node Identification Document. A representation of XML Documents
 * where nodes are accessed by integer handles whose values are guaranteed to be
 * in document order (i.e. node N1 before node N2 <=> handle(N1) < handle(N2) )
 * <p>
 * Such a representation is suitable for documents that are built in one
 * operation such as parsing and never modified.
 * <p>
 * The interface is very close to the XPath/XQuery/XSLT Data Model.
 * <p>
 * The handle value 0 is reserved and represents a null or absent node.
 */
public interface FONIDocument
{
    /**
     * Returns the handle of the document node.
     */
    int/*NId*/  getRootNode()
        throws DataModelException;

    /**
     * Returns one of the kinds DOCUMENT, ELEMENT, TEXT, PROCESSING_INSTRUCTION,
     * COMMENT, ATTRIBUTE, NAMESPACE, ATOM_XX.
     * @throws DataModelException 
     */
    int getKind(int/*NId*/  nodeId)
        throws DataModelException;

    /**
     * Gets the name of a Node.
     */
    IQName getName(int/*NId*/  nodeId)
        throws DataModelException;

    /**
     * Gets the name of a pseudo Node Attribute / Namespace.
     */
    IQName pnGetName(int/*NId*/  nodeId)
        throws DataModelException;

    /**
     * Gets the name-id of a real Node. All names are represented by an index in
     * a name table.
     */
    int getNameId(int/*NId*/  nodeId)
        throws DataModelException;

    /**
     * Gets the name-id of a pseudo-node Attribute / Namespace.
     */
    int pnGetNameId(int/*NId*/  nodeId)
        throws DataModelException;

    int/*NId*/  getParent(int/*NId*/  nodeId)
        throws DataModelException;

    int/*NId*/  getNextSibling(int/*NId*/  nodeId)
        throws DataModelException;

    int/*NId*/  getNodeSpan(int/*NId*/ nodeId)
        throws DataModelException;

    /**
     * Gets the node that is next in document order. (first child if non-atomic,
     * otherwise returns getNodeAfter()).
     */
    int/*NId*/ getNodeNext(int/*NId*/ nodeId)
        throws DataModelException;

    /**
     * Gets the node that is next in document order but not contained. It is the
     * following sibling if any, else the parent's following sibling,
     * recursively.
     */
    int/*NId*/getNodeAfter(int/*NId*/nodeId)
        throws DataModelException;

    int/*NId*/getFirstChild(int/*NId*/nodeId)
        throws DataModelException;

    /**
     * Returns the number of attributes of an element (not guaranteed to be
     * efficient).
     */
    int getAttrCount(int/*NId*/ nodeId)
        throws DataModelException;

    /**
     * Gets an attribute node by name. If nameId < 0, returns the 1st attribute.
     */
    int/*NId*/getAttribute(int/*NId*/nodeId, int nameId)
        throws DataModelException;

    /**
     * Returns the next pseudo-node (attribute or NS).
     */
    int/*NId*/pnGetNext(int/*NId*/nodeId)
        throws DataModelException;

    /**
     * Gets the string value for any node but Attributes and Namespaces.
     */
    String getStringValue(int/*NId*/ nodeId)
    throws DataModelException;

    /**
     * Gets the string value for pseudo-nodes Attributes and Namespaces.
     */
    String pnGetStringValue(int/*NId*/ nodeId)
    throws DataModelException;

    /**
     * Specially meant for indexing: gets the string value of a text node.
     * Returns the value into a char array, reserving chars (at head if reserve >
     * 0).
     */
    char[] getCharValue(int/*NId*/nodeId, int reserve)
        throws DataModelException;

    /**
     * Specially meant for indexing: gets the string value of a "pseudo-node"
     * (attribute or NS). Returns the value into a char array, possibly
     * reserving leading chars.
     */
    char[] pnGetCharValue(int/*NId*/nodeId, int reserve)
        throws DataModelException;

    /**
     * Gets an atomic value from a leaf node.
     */
    Object getValue(int/*NId*/nodeId)
        throws DataModelException;

    /**
     * Gets an integer value from an atome of type ATOM_INT.
     */
    long getIntegerValue(int/*NId*/nodeId)
        throws DataModelException;

    /**
     * Returns the number of NS defined on this node.
     */
    int getDefinedNSCount(int/*NId*/nodeId)
        throws DataModelException;

    /**
     * Returns the first namespace mapping of an element. Use pnGetNext to
     * iterate.
     */
    int/*NId*/getFirstNSNode(int/*NId*/nodeId)
        throws DataModelException;

    /**
     * Returns nodes that match this ID or IDREF attribute.
     * 
     * @param id
     *            key value
     * @param idref
     *            if true returns nodes that bear an IDREF attributes with the
     *            key value. If false, returns the unique node that bears the ID
     *            attribute.
     * @return an array of matching node ids.
     */
    public int/*NId*/ [] getIdMatchingNodes(String id, boolean idref)
        throws DataModelException;

    /**
     * Gets the total number of element names.
     */
    int getElementNameCount()
        throws DataModelException;

    /**
     * Gets the QName of an element node by the internal id.
     */
    IQName getElementName(int nameId)
        throws DataModelException;

    /**
     * Gets the internal id of an element name.
     */
    int internElementName(QName name)
        throws DataModelException;

    /**
     * Gets the total number of non-element node names.
     */
    int getOtherNameCount()
        throws DataModelException;

    /**
     * Gets the QName of a non-element node by the internal id.
     */
    IQName getOtherName(int nameId)
        throws DataModelException;

    /**
     * Gets the internal id of a non-element node name.
     */
    int internOtherName(QName name)
        throws DataModelException;

    /**
     * Cache management.
     */
    int/*NId*/ estimateMemorySize()
        throws DataModelException;
    
    /**
     * Estimated document size in bytes.
     */
    int/*NId*/ virtualSize()
        throws DataModelException;

    
    /**
     * Returns the internal document identifier.
     */
    int getDocumentId();

    void setDocumentId(int/*DId*/ docId);

    /**
     * Returns the base-uri of the document (defined by owner or set explicitly).
     */
    String getBaseURI()
        throws DataModelException;

    /**
     * Returns the URI of the document.
     */
    void setBaseURI(String uri);

    // --------------- Relate document to an owner object -------------
    
    String[] getDTDInfo();

    void setOwner(Owner owner);
    
    Owner getOwner();
    
    /**
     * Owner of a document: normally an XML Library if not null.
     */
    public interface Owner
    {
        boolean isDeleted(FONIDocument doc);
        
        // looks up the base-uri in owner (Library):
        String getBaseURI(FONIDocument doc);

        DataConversion getDataConversion();
    }
}
