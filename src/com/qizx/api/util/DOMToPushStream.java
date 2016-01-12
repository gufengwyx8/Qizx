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
import com.qizx.api.ItemFactory;
import com.qizx.api.QName;
import com.qizx.api.XMLPushStream;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.CoreDataModel;
import com.qizx.xdm.IQName;
import com.qizx.xquery.dt.SingleNode;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashSet;

/**
 * Pushes a DOM document or an element to an {@link XMLPushStream} handler.
 * This can be used typically to import a DOM document into an XML Library
 * (see {@link Library#beginImportDocument}), or even to serialize a DOM
 * document using a XMLSerializer as output.
 */
public final class DOMToPushStream
{
    private final ItemFactory itemFactory;
    private XMLPushStream output;
    private HashSet prefixes = new HashSet();

    /**
     * Builds an adapter from DOM to an implementation of XMLPushStream.
     * @param itemFactory used to create QNames
     * @param output an implementation of XMLPushStream
     */
    public DOMToPushStream(ItemFactory itemFactory, XMLPushStream output)
    {
        this.itemFactory = itemFactory;
        this.output = output;
    }

    /**
     * Returns the current output handler.
     * @return the XMLPushStream defined by the constructor or by setOutput.
     */
    public XMLPushStream getOutput()
    {
        return output;
    }

    /**
     * Redefines the current output handler.
     * @param output an implementation of XMLPushStream
     */
    public void setOutput(XMLPushStream output)
    {
        this.output = output;
    }

    /**
     * Pushes a complete document to the output stream.
     * @param document input document to convert
     * @throws DataModelException if thrown by the output handler
     */
    public void putDocument(Document document)
        throws DataModelException
    {
        output.putDocumentStart();

        DocumentType docType = document.getDoctype();
        if (docType != null) {
            output.putDTD(docType.getName(), docType.getPublicId(),
                          docType.getSystemId(), docType.getInternalSubset());
        }

        //dump(document.getDocumentElement());

        putElement(document.getDocumentElement());

        output.putDocumentEnd();
    }

    /**
     * Utility for conversion of DOM to Data Model nodes
     * @param node any DOM node
     * @return a Node of the Data Model
     * @throws DataModelException
     */
    public static com.qizx.api.Node convertNode(Node node)
        throws DataModelException
    {
        switch (node.getNodeType()) {
        case Node.TEXT_NODE:
        case Node.CDATA_SECTION_NODE:
            return new CoreDataModel(null).newTextNode(node.getNodeValue());
        case Node.COMMENT_NODE:
            return new CoreDataModel(null).newCommentNode(node.getNodeValue());
        case Node.PROCESSING_INSTRUCTION_NODE:
            return new CoreDataModel(null).newPINode(node.getNodeName(),
                                                     node.getNodeValue());
        case Node.ELEMENT_NODE: {
            PushNodeBuilder out = new PushNodeBuilder();
            new DOMToPushStream(null, out).putElement((Element) node);
            return out.reap();
        }
        case Node.DOCUMENT_NODE: {
            PushNodeBuilder out = new PushNodeBuilder();
            new DOMToPushStream(null, out).putDocument((Document) node);
            return out.reap();
        }
        default:
            throw new DataModelException("unexpected "
                                         + nodeTypeName(node) + " node");
        }
    }

    /**
     * Pushes an element and all its descendant nodes to the output stream.
     * @param element a DOM element to convert
     * @throws DataModelException
     */
    public void putElement(Element element)
        throws DataModelException
    {
        QName elementName = getQName(element);
        output.putElementStart(elementName);
        
        prefixes.clear();
        putNamespace(element, false);
        
        NamedNodeMap attrMap = element.getAttributes();
        int attrCount = attrMap.getLength();
        for (int i = 0; i < attrCount; ++i) {
            Node attr = attrMap.item(i);

            String attrName = attr.getNodeName();
            if (attrName == null || attrName.startsWith("xmlns"))
                continue;

            output.putAttribute(getQName(attr), attr.getNodeValue(),
                                /*type*/null);
            putNamespace(attr, true);
        }

        Node child = element.getFirstChild();
        while (child != null) {
            switch (child.getNodeType()) {
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                output.putText(child.getNodeValue());
                break;
            case Node.COMMENT_NODE:
                output.putComment(child.getNodeValue());
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                output.putProcessingInstruction(child.getNodeName(),
                                                child.getNodeValue());
                break;
            case Node.ELEMENT_NODE:
                putElement((Element) child);
                break;
            default:
                throw new DataModelException("unexpected "
                                             + nodeTypeName(child) + " node");
            }

            child = child.getNextSibling();
        }

        output.putElementEnd(elementName);
    }

    // Creates a QName with local part and NS uri: prefix is ignored
    //
    private QName getQName(Node node)
        throws DataModelException
    {
        String nsURI = null;
        String localPart = node.getLocalName();
        
        if (localPart == null) {
            localPart = node.getNodeName();
            System.err.println("the name of " + nodeTypeName(node) + " node '"
                               + localPart + "' has no local part");
        }
        else {
            nsURI = node.getNamespaceURI();
        }

        if (localPart == null) {
            throw new DataModelException("the name of " + nodeTypeName(node)
                                         + " node is null");
        }

        if (nsURI == null) {
            return (itemFactory != null)? itemFactory.getQName(localPart)
                                        : IQName.get(localPart);
        }
        else {
            return (itemFactory != null)? itemFactory.getQName(localPart, nsURI)
                                        : IQName.get(nsURI, localPart);
        }
    }

    //  Generates a putNamespace as appropriate for the node name
    //  The DOM does not seem to 
    private void putNamespace(Node node, boolean isAttr) throws DataModelException
    {
        String prefix = node.getPrefix();
        String nsURI = node.getNamespaceURI();

        if ((nsURI == null || nsURI.length() == 0) && isAttr)
            return;     // no need to declare NS
        
        // beware, all kinds of possible behaviors in DOM impls:
        if (prefix == null)
            prefix = "";
        if(nsURI == null)
            nsURI = "";
        // The PushStream implementations are supposed to ignore useless NS decl
        // but they dont like duplicate decls at the same level
        if(!prefixes.contains(prefix)) {
            prefixes.add(prefix);
            output.putNamespace(prefix, nsURI);
        }
    }


    private static String nodeTypeName(Node node)
    {
        int type = node.getNodeType();
        switch (type) {
        case Node.ATTRIBUTE_NODE:
            return "ATTRIBUTE";
        case Node.CDATA_SECTION_NODE:
            return "CDATA_SECTION";
        case Node.COMMENT_NODE:
            return "COMMENT";
        case Node.DOCUMENT_FRAGMENT_NODE:
            return "DOCUMENT_FRAGMENT";
        case Node.DOCUMENT_NODE:
            return "DOCUMENT";
        case Node.DOCUMENT_TYPE_NODE:
            return "DOCUMENT_TYPE";
        case Node.ELEMENT_NODE:
            return "ELEMENT";
        case Node.ENTITY_NODE:
            return "ENTITY";
        case Node.ENTITY_REFERENCE_NODE:
            return "ENTITY_REFERENCE";
        case Node.NOTATION_NODE:
            return "NOTATION";
        case Node.PROCESSING_INSTRUCTION_NODE:
            return "PROCESSING_INSTRUCTION";
        case Node.TEXT_NODE:
            return "TEXT";
        default:
            return Integer.toString(type);
        }
    }

    private void dump(Node node)
    {
        switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE:
            break;
        case Node.ELEMENT_NODE:
            System.err.println("ELEM " + node.getLocalName()
                               + " nsuri=" + node.getNamespaceURI()
                               + " qname=" + node.getNodeName()
                               + " prefix=" + node.getPrefix());

            NamedNodeMap attrs = node.getAttributes();
            for (int i = 0, asize = attrs.getLength(); i < asize; i++) {
                Node attr = attrs.item(i);
                System.err.println("  ATTR " + attr.getLocalName()
                                   + " nsuri=" + attr.getNamespaceURI()
                                   + " qname=" + attr.getNodeName()
                                   + " prefix=" + attr.getPrefix());
            }

            Node kid = node.getFirstChild();
            for (; kid != null; kid = kid.getNextSibling()) {
                dump(kid);
            }
            System.err.println("END");
            break;

        case Node.TEXT_NODE:
            System.err.println("TEXT |" + node.getNodeValue() + "|");
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            System.err.println("PI " + node.getNodeName() + " |" + node.getNodeValue() + "| ");
            break;
        case Node.COMMENT_NODE:
            System.err.println("COMMENT |" + node.getNodeValue() + "|");
            break;
        }
    }
}
