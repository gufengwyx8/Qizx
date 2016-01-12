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
import com.qizx.xdm.CoreDataModel.CoreNode;
import com.qizx.xdm.CoreDataModel.NSNode;

/**
 * An implementation of XMLPushStream that builds a CoreDataModel tree.
 * <p>
 * Used in XQ node constructors.
 */
public class CorePushBuilder extends XMLPushStreamBase
{
    protected CoreDataModel dm;
    protected CoreNode root;
    protected CoreNode leafRoot; // single leaf node: Attr, Comment, PI, Text

    protected CoreDataModel.Element current;
    protected CoreNode previous;
    protected int orderStamp = 1;

    
    public CorePushBuilder(String baseURI)
    {
        this(baseURI, null);
    }

    /**
     * Extends XMLPushStreamBase only for traverse. Doesnt use its prefix map.
     */
    public CorePushBuilder(String baseURI, NamespaceContext staticNS)
    {
        dm = new CoreDataModel(baseURI);
        contextualMappings = staticNS;
    }

    public BasicNode harvest()
    {
        return root != null? root : leafRoot;
    }

    public String getNSURI(String prefix)
    {
        return (current == null) ? null : current.getNamespaceUri(prefix);
    }

    public void reset()
    {
        super.reset();
        previous = root = leafRoot = current = null;
    }

    public void flush()
    {
    }

    public boolean putDocumentStart()
    {
        spaceNeeded = false;
        if (root != null)
            return false; // no more an error:
        // / throw new DataModelException("document inside document");
        root = current = dm.newDocumentNode();
        previous = null; // should be already
        return true;
    }

    public void putDocumentEnd()
        throws DataModelException
    {
        spaceNeeded = false;
        closeNode();
        spaceNeeded = false;
    }

    public void flushElementStart(boolean empty) throws DataModelException
    {
        spaceNeeded = false;
        // Need to add prefixes for unmapped NS
        completeNameMappings();
        elementStarted = false;
    }

    public void putElementStart(QName name)
        throws DataModelException
    {
        if (maxVolume > 0 && volume > maxVolume)
            throw new DataModelException(VOLUME_LIMIT);
        if (elementStarted)
            flushElementStart(false);
        spaceNeeded = false;
        elementName = IQName.get(name);
        elementStarted = true;
        attrCnt = 0;
        CoreDataModel.Element e = dm.newElement(name);
        openNode(e);
        // marks the namespace stack
        nsContext.newLevel();
    }

    public void putElementEnd(QName name)
        throws DataModelException
    {
        if (maxVolume > 0 && volume > maxVolume)
            throw new DataModelException(VOLUME_LIMIT);
        if (elementStarted)
            flushElementStart(false);
        nsContext.popLevel();
        closeNode();
    }

    public boolean putNamespace(String prefix, String uri)
        throws DataModelException
    {
        if (current == null)
            throw new DataModelException("stray namespace");
        if(uri == null)
            uri = ""; // null not liked by addNSMapping
        if (!addNSMapping(prefix, uri))
            throw new DataModelException("XQST0071",
                                         "duplicate namespace declaration '"
                                         + prefix + "'");
        return true;
    }

    protected boolean addNSMapping(String prefix, String ns)
    {
        nsContext.addMapping(prefix, ns);
        NSNode nsNode = dm.newNSNode(prefix);
        nsNode.strValue = ns;
        if (!current.addNamespace(nsNode))
            return false;
        volume += prefix.length() + ns.length();
        return true;
    }

    public void putAttribute(QName name, String value, String type)
        throws DataModelException
    {
        if (maxVolume > 0 && volume > maxVolume)
            throw new DataModelException(VOLUME_LIMIT);

        CoreDataModel.Attribute attr = dm.newAttribute(name);
        attr.strValue = value;
        attr.order = orderStamp++;

        
        if (current == null) {
            if(root == null && leafRoot == null)
                leafRoot = attr;
            else
                throw new DataModelException("invalid placement for attribute");
        }
        else {
            if (current.getFirstChild() != null)
                throw new DataModelException("XQTY0024", 
                                             "attribute added after contents");
            if (!current.addAttribute(attr))
                throw new DataModelException("XQDY0025", 
                                             "duplicate attribute " + name);
        }
        // used for checking NS:
        rawAddAttribute(name, value, type);
        volume += 10 + value.length();
        
        if (name == IQName.XML_ID) {
            dm.addId(value, current);
        }
    }

    public void putText(String value)
        throws DataModelException
    {
        if (value == null || value.length() == 0)
            return;
        if (maxVolume > 0 && volume > maxVolume)
            throw new DataModelException(VOLUME_LIMIT);
        if (elementStarted)
            flushElementStart(false);
        if (previous != null && previous.getNodeNature() == Node.TEXT) {
            previous.addText(value);
        }
        else {
            addNode(dm.newTextNode(value));
        }
        spaceNeeded = false;
        volume += value.length();
    }

    public void putProcessingInstruction(String target, String value)
        throws DataModelException
    {
        checkPIValue(value);
        if (maxVolume > 0 && volume > maxVolume)
            throw new DataModelException(VOLUME_LIMIT);
        if (elementStarted)
            flushElementStart(false);
        CoreDataModel.PINode pi = dm.newPINode(target, value);
        addNode(pi);
        volume += value.length();
        spaceNeeded = false;
    }

    public void putComment(String value)
        throws DataModelException
    {
        checkCommentValue(value);
        if (maxVolume > 0 && volume > maxVolume)
            throw new DataModelException(VOLUME_LIMIT);
        if (elementStarted)
            flushElementStart(false);
        CoreDataModel.CommentNode c = dm.newCommentNode(value);
        addNode(c);
        volume += value.length();
        spaceNeeded = false;
    }

    // adds a node as child of the current element.
    private void addNode(CoreDataModel.CoreNode n)
        throws DataModelException
    {
        n.parent = current;
        if (previous != null) {
            previous.setNextSibling(n);
            if (current == null) {
                // orphan siblings: add a document node as parent
                root = current = dm.newDocumentNode();
                current.firstChild = previous;
                previous.parent = n.parent = current;
            }
        }
        else {
            if (current == null)
                if (root != null)
                    throw new DataModelException("adding node after root");
                else { // this node becomes the root
                    if(n instanceof CoreDataModel.Element) // or document
                        root = current = (CoreDataModel.Element) n;
                    else if(leafRoot != null)
                        throw new DataModelException("invalid node type for document root");
                    else
                        leafRoot = n;
                }
            else
                current.firstChild = n;
        }
        previous = n;
        // improves order comparisons tremendously :
        n.order = orderStamp++;
    }

    private void openNode(CoreDataModel.Element e)
        throws DataModelException
    {
        addNode(e);
        current = e;
        previous = null;
        volume += 10; // arbitrary
    }

    private void closeNode()
        throws DataModelException
    {
        if (current == null)
            if (maxVolume > 0 && volume > maxVolume)
                return;
            else
                throw new DataModelException("no open element");
        previous = current;
        current = current.parent;
        volume += 10; // arbitrary
        spaceNeeded = false;
    }
}
