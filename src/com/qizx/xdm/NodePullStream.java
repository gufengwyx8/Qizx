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
import com.qizx.util.NamespaceContext;

/**
 * Traverses a Node in pull (streaming) mode.
 */
public class NodePullStream extends XMLPullStreamBase
{
    private Node rootNode;
    private Node curNode;

    public NodePullStream(Node rootNode)
    {
        this.rootNode = rootNode;
        curEvent = START;
    }

    public int moveToNextEvent() throws DataModelException
    {
        Node next;
        switch (curEvent) {
        case START:
            if (rootNode == null)
                return setEvent(END);
            return toNodeStart(rootNode);
        case END:
            return END;
        case DOCUMENT_START:
            next = curNode.getFirstChild();
            if (next == null)
                return setEvent(DOCUMENT_END);
            return toNodeStart(next);
        case DOCUMENT_END:
            return setEvent(END);
        case ELEMENT_START:
            next = curNode.getFirstChild();
            if (next == null)
                return setEvent(ELEMENT_END);
            return toNodeStart(next);
        case ELEMENT_END:
        case TEXT:
        case PROCESSING_INSTRUCTION:
        case COMMENT:
            if (rootNode.equals(curNode))
                return setEvent(END);
            next = curNode.getNextSibling();
            if (next != null)
                return toNodeStart(next);
            next = curNode.getParent();
            if (next == null)
                return setEvent(END);
            curNode = next;
            return setEvent(next.getNodeNature() == Node.ELEMENT ? 
                               ELEMENT_END : DOCUMENT_END);
        default:
            throw new RuntimeException("wrong state " + curEvent);
        }
    }

    // Sets the position to the beginning of a Node
    private int toNodeStart(Node node) throws DataModelException
    {
        // assert(node != null)
        curNode = node;
        switch (node.getNodeNature()) {
        case Node.DOCUMENT:
            return setEvent(DOCUMENT_START);
        case Node.ELEMENT:
            // attributes and NS set in lazy mode:
            attrCount = nsCount = -1;
            return setEvent(ELEMENT_START);
        case Node.TEXT:
            return setEvent(TEXT);
        case Node.COMMENT:
            return setEvent(COMMENT);
        case Node.PROCESSING_INSTRUCTION:
            piTarget = node.getNodeName().getLocalPart();
            return setEvent(PROCESSING_INSTRUCTION);
        default:
            throw new RuntimeException("unimplemented node type "
                                       + node.getNodeNature());
        }
    }

    public QName getName()
    {
        try {
            return (curNode != null) ? curNode.getNodeName() : null;
        }
        catch (DataModelException e) {
            return null;
        }
    }

    public String getText()
    {
        try {
            if(curNode == null)
                return null;
            int nat = curNode.getNodeNature();
            // avoid returning string value on large nodes:
            return (nat == Node.DOCUMENT || nat == Node.ELEMENT)? null
                        : curNode.getStringValue();
        }
        catch (DataModelException e) {
            return null;
        }     
    }

    protected void lazyGetAttrs() 
    {
        attrCount = 0;
        try {
            // lazy evaluation:
            Node[] attributes = curNode.getAttributes();
            attrCount = 0;
            if(attributes != null)
                for (int a = 0, size = attributes.length; a < size; a++) {
                    addAttribute(attributes[a].getNodeName(), 
                                 attributes[a].getStringValue());
                }
        }
        catch (DataModelException e) { ; }
    }

    protected void lazyGetNS()
    {
        BasicNode n = (BasicNode) curNode;
        nsCount = 0;
        try {
            if(n.hasLocalNamespaces()) {
                NamespaceContext ctx = new NamespaceContext();
                n.addNamespacesTo(ctx);
                for (int i = 0, size = ctx.size(); i < size; i++) {
                    addNamespace(ctx.getPrefix(i), ctx.getNamespaceURI(i));
                }
            }
        }
        catch (DataModelException e) { ; }
    }

    public Node getCurrentNode()
    {
        return curNode;
    }
}
