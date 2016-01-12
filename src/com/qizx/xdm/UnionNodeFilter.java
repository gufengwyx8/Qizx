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
import com.qizx.api.Node;
import com.qizx.api.QName;

/**
 * Logical OR of NodeTests.
 */
public class UnionNodeFilter
    implements NodeFilter
{
    public NodeFilter[] filters;

    public UnionNodeFilter(NodeFilter t1, NodeFilter t2)
    {
        int size1 = unionSize(t1);
        int size = size1 + unionSize(t2);
        this.filters = new NodeFilter[size];
        expandUnion(t1, 0);
        expandUnion(t2, size1);
    }

    private void expandUnion(NodeFilter f, int pos)
    {
        if(f instanceof UnionNodeFilter) {
            UnionNodeFilter u = (UnionNodeFilter) f;
            System.arraycopy(u.filters, 0, filters, pos, u.filters.length);
        }
        else
            filters[pos] = f;
    }

    private int unionSize(NodeFilter f)
    {
        if(f instanceof UnionNodeFilter) {
            UnionNodeFilter u2 = (UnionNodeFilter) f;
            return u2.filters.length;
        }
        return 1;
    }

    public void add(NodeFilter f)
    {
        NodeFilter[] old = filters;
        int len = old.length;
        filters = new NodeFilter[len + 1];
        System.arraycopy(old, 0, filters, 0, len);
        filters[len] = f;
    }
    
    public String toString()
    {
        StringBuffer buf = new StringBuffer("UnionTest( ");
        for (int i = 0; i < filters.length; i++) {
            if(i > 0)
                buf.append(", ");
            buf.append(filters[i]);
        }
        buf.append(")");
        return buf.toString();
    }

    public int getNodeKind()
    {
        int k1 = filters[0].getNodeKind();
        for (int i = 1; i < filters.length; i++) {
            if(filters[i].getNodeKind() != k1)
                return -1;
        }
        return k1;
    }

    public boolean staticallyCheckable()
    {
        return false;
    }

    public boolean accepts(int nodeKind, QName nodeName)
    {
        for (int i = 0; i < filters.length; i++) {
            if(filters[i].accepts(nodeKind, nodeName))
                return true;
        }
        return false;
    }

    public boolean needsNode()
    {
        return false;
    }

    public boolean accepts(Node node)
    {
        try {
            return accepts(node.getNodeNature(), node.getNodeName());
        }
        catch (DataModelException e) {
            return false;
        }
    }
}
