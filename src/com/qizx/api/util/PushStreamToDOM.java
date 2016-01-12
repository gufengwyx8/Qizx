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
package com.qizx.api.util;

import com.qizx.api.DataModelException;
import com.qizx.api.QName;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.XMLPushStreamBase;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * An implementation of XMLPushStream which creates W3C DOM documents or
 * nodes.
 * <p>
 * This class can be used to export or convert a XML tree from Qizx to any
 * system accepting DOM representation.
 * <p>
 * Caution: leading and trailing Comments or Processing-Instructions (i.e.
 * preceding or following the topmost Element) are discarded. This feature is
 * in fact not really supported by W3C DOM.
 */
public class PushStreamToDOM extends XMLPushStreamBase
{
    // Bootstrapping:
    private static DocumentBuilderFactory factory =
        DocumentBuilderFactory.newInstance();
    private DOMImplementation implementation;
    
    private Document document;
    private Element rootElement;
    private Element current;  //
    private Text latestText;

    /**
     * Creates a DOM output using the default DOM implementation.
     * @throws DataModelException wraps a ParserConfigurationException
     */
    public PushStreamToDOM() throws DataModelException
    {
        try {
            this.implementation =
                factory.newDocumentBuilder().getDOMImplementation();
        }
        catch (ParserConfigurationException e) {
            throw new DataModelException("DOM factory error: "
                                         + e.getMessage(), e);
        }
    }

    /**
     * Creates a DOM output using a specified DOM implementation.
     * @param implem DOM implementation
     */
    public PushStreamToDOM(DOMImplementation implem)
    {
        this.implementation = implem;
    }

    /**
     * Converts a node of the XDM into a W3C DOM node.
     * @param node a Node of the XQuery Data Model.
     * @return a DOM Node, according to the type of the argument.
     * @throws DataModelException not thrown
     */
    public Node exportNode(com.qizx.api.Node node) throws DataModelException
    {
        QName name = node.getNodeName();
        switch(node.getNodeNature()) { // atoms not handled by XMLPushStream
        case BasicNode.DOCUMENT:
            putNodeCopy(node, 0);
            return getResultDocument();
        case BasicNode.ELEMENT:
            putNodeCopy(node, 0);
            return getResultDocument().getDocumentElement();
            
        case BasicNode.ATTRIBUTE:
            Attr attr = initDoc().createAttributeNS(name.getNamespaceURI(),
                                                    name.getLocalPart());
            attr.setNodeValue(node.getStringValue());
            return attr;
        case BasicNode.COMMENT:
            return initDoc().createComment(node.getStringValue());
        case BasicNode.PROCESSING_INSTRUCTION:
            return initDoc().createProcessingInstruction(name.getLocalPart(),
                                                         node.getStringValue());
        case BasicNode.TEXT:
        default:
            return initDoc().createTextNode(node.getStringValue());
        }
    }
    
    /**
     * Returns the DOM document built. Attention: non null result only if the
     * source Node is a document or an element.
     * @return DOM document built
     */
    public Document getResultDocument()
    {
        return document;
    }

    // ---------- implementation of XMLPushStream -----------------------------

    public boolean putDocumentStart() throws DataModelException
    {
        if(rootElement != null)
            return false;  // no more an error:
            /// throw new DataModelException("document inside document");
        boolean ok = super.putDocumentStart();
        initDoc();
        return ok;
    }

    private Document initDoc()
    {
        rootElement = current = null;
        document = implementation.createDocument("", "Dummy", null);
        document.removeChild(document.getFirstChild());
        return document;
    }

//    public void putDocumentEnd() throws DataModelException
//    {
//        super.putDocumentEnd();
//    }

    public void putElementStart(QName name) throws DataModelException
    {
        super.putElementStart(name);
        spaceNeeded = false;
        // no openNode here: wait until a flushElementStart
    }

    public void putElementEnd(QName name) throws DataModelException
    {
        if (elementStarted)
            flushElementStart(false);
        else
            flushText();
        if(current == null)
            throw new DataModelException("unbalanced end of element: " + name);
        Node parent = current.getParentNode();
        if(parent == null || parent.getNodeType() == Node.DOCUMENT_NODE)
            current = null; // complete
        else
            current = (Element) parent;

        super.putElementEnd(name);
    }

    protected void flushElementStart(boolean isEmpty)
        throws DataModelException
    {
        flushText();
        completeNameMappings();
        Element e;
        String nsURI = elementName.getNamespaceURI();
        // Fix: Always with NS
        e = document.createElementNS(nsURI, qualifiedName(elementName));
        
        openElement(e);
        
        for (int a = 0; a < attrCnt; a++) {
            QName aname = attrNames[a];
            String ns = aname.getNamespaceURI();
            Attr attr = document.createAttributeNS(ns, qualifiedName(aname));
            attr.setValue(attrValues[a]);
            current.setAttributeNode(attr);
        }

        elementStarted = false;
    }

    private void flushText()
    {
        latestText = null;
    }

//    public void putAttribute(QName name, String value, String type)
//        throws DataModelException
//    {
//        if(current == null)
//            throw new DataModelException("stray attribute");
//        String ns = name.getNamespaceURI();
//        Attr attr = (ns == NamespaceContext.EMPTY) 
//                      ? document.createAttribute(name.getLocalPart())
//                      : document.createAttributeNS(ns, qualifiedName(name));
//        attr.setValue(value);
//        current.setAttributeNode(attr);
//
//    }

    public void putText(String text)
        throws DataModelException
    {
        if (text == null || text.length() == 0)
            return;
        if (elementStarted)
            flushElementStart(false);
        if (latestText != null) {
            latestText.appendData(text);
        }
        else {
            addAtom(latestText = document.createTextNode(text));
        }
    }

    public void putComment(String contents)
        throws DataModelException
    {
        flushText();
        super.putComment(contents);
        Comment c = document.createComment(contents);
        addAtom(c);
    }

    public void putProcessingInstruction(String target, String contents)
        throws DataModelException
    {
        flushText();
        super.putProcessingInstruction(target, contents);
        Node pi = document.createProcessingInstruction(target, contents);
        addAtom(pi);
    }

    public void reset()
    {
        super.reset();
        flushText();
    }
    
    // Adds a prefix to the local name
    private String qualifiedName(QName name)
    {
        String ns = name.getNamespaceURI();
        if(ns == NamespaceContext.XML) {       // needs not be declared
            return "xml:" + name.getLocalPart();
        }
        String prefix = nsContext.getPrefix(ns);

        if(prefix == null && ns.length() > 0)
            throw new RuntimeException("cannot map NS " + ns);
        
        if(prefix != null && prefix.length() > 0)
            return prefix + ':' + name.getLocalPart();
        return name.getLocalPart();
    }

    private void addAtom(Node atomic) throws DataModelException
    {
        if(current == null) {
            // no enclosing element: siblings of root element
            // not really supported by this DOM? discard it
            return;
        }
        current.appendChild(atomic);
    }
    
    // adds a node as child of the current element. Returns that node.
    private void openElement(Element e)
        throws DataModelException
    {
        if(current == null) {
            if(rootElement != null)
                throw new DataModelException("adding node after root");
            else {
                rootElement = e;
                if(rootElement.getNodeType() == Node.ELEMENT_NODE)
                        current = (Element) rootElement;
                document.appendChild(e);
            }
        }
        else
            current.appendChild(e);
        current = e;
        spaceNeeded = false;
    }

}
