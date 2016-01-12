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
import com.qizx.api.ItemType;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.util.NamespaceContext;
import com.qizx.xquery.op.SchemaContext;

public class BaseNodeFilter
    implements NodeFilter
{
    public int srcLocation;

    /**
     * Tested kind (ELEMENT TEXT PI COMMENT..., or -1 for node())
     */
    public int kind; // 

    /**
     * Namespace test: null if not specified (* or *:NCName)
     */
    public String namespace;

    /**
     * Localname test: null if not specified (* or ns:*)
     */
    public String name;

    /**
     * Accelerator, non-null if non-null namespace and name.
     */
    public QName qname;
    
    /**
     * Basetype for value: accepts only untypedAtomic in absence of Schema.
     */
    public SchemaContext schemaType;

    /**
     * Builds a node test specifying the node-kind and optionally the name of
     * nodes to match.
     * 
     * @param kind the node-kind as defined in Node: ELEMENT, ATTRIBUTE, TEXT
     *        etc.
     * @param namespaceURI of the node name. If null, represents the wildcard
     *        *:NCName.
     * @param name local part of node name. If null, represents the wildcard
     *        ns:* or *.
     */
    public BaseNodeFilter(int kind, String namespaceURI, String name)
    {
        this.kind = kind;
        this.namespace = (namespaceURI == null)? null
                         : NamespaceContext.unique(namespaceURI);
        if (name != null)
            name = NamespaceContext.unique(name);
        this.name = name;
        if (namespace != null && name != null)
            qname = IQName.get(namespace, name);
    }

    public BaseNodeFilter(int kind, String nsURI, String name,
                          SchemaContext schemaType)
    {
        this(kind, nsURI, name);
        this.schemaType = schemaType;
    }

    public String toString()
    {
        return "baseNodeTest(" + kind + ", " + namespace + ":" + name + ")";
    }

    public int getNodeKind()
    {
        return kind;
    }

    public boolean staticallyCheckable()
    {
        return namespace == null && name == null;   // ie no name or wildcard
    }

    public boolean needsNode()
    {
        return false;
    }

    public boolean accepts(int nodeKind, QName nodeName)
    {
        if (kind > 0 && kind != nodeKind)
            return false;
        if (nodeName == null) // contract of the method
            return true;
        if (namespace == null && name == null) // dont care about name
            return true;
        if (qname != null) // simple name
            return nodeName.equals(qname);
        // nodeName cannot be null
        if (namespace != null) {
            String nsuri = nodeName.getNamespaceURI();
            return namespace == nsuri || namespace.equals(nsuri);
        }
        else
            // Use of == is intentional:
            return name == nodeName.getLocalPart();
    }

    public boolean accepts(Node node)
    {
        try {
            if (!accepts(node.getNodeNature(), node.getNodeName()))
                return false;
            return schemaType == null || schemaType.acceptsNode(node);
        }
        catch (DataModelException e) {
            return false;
        }
    }
    
}
