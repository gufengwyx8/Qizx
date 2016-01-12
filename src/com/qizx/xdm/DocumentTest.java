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
 * Represents the node test document-node(<element-test>).
 */
public class DocumentTest extends BaseNodeFilter
{
    public NodeFilter topTest;

    public DocumentTest(NodeFilter topTest)
    {
        super(Node.DOCUMENT, null, null);
        this.topTest = topTest;
    }

    public String toString()
    {
        return "documentTest(" + topTest + ")";
    }

    public boolean staticallyCheckable()
    {
        return topTest == null;
    }

    public boolean accepts(int nodeKind, QName nodeName)
    {
        return nodeKind == Node.DOCUMENT;
    }

    public boolean needsNode()
    {
        return true;
    }

    public boolean accepts(Node node)
    {
        boolean found = false;

        try {
            if (node.getNodeNature() != Node.DOCUMENT)
                return false;
            if (topTest == null)
                return true;
            // must have exactly 1 element child, matching the topTest
            // predicate
            Node kid = node.getFirstChild();
            for (; kid != null; kid = kid.getNextSibling()) {
                if (kid.getNodeNature() == Node.ELEMENT) {
                    if (found || !topTest.accepts(kid))
                        return false;
                    found = true;
                }
            }
        }
        catch (DataModelException e) {
            return false;
        }
        return found;
    }
}
