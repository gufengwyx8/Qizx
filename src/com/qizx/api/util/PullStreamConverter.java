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
import com.qizx.api.Node;
import com.qizx.api.XMLPullStream;
import com.qizx.api.XMLPushStream;
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xdm.FONIDataModel;
import com.qizx.xdm.IDocument;

/**
 * Utility for conversion of an XMLPullStream input.
 * <ul>
 * <li>Conversion into a push stream: ({@link #convertTree} reads a portion
 * of the input stream that form a complete element tree.
 * <li>Building an in-memory Node representation: ({@link #buildNode}) reads
 * a portion of the input stream that form a complete element and its subtree,
 * or a leaf node, and builds a Node.
 * </ul>
 */
public class PullStreamConverter
{
    /**
     * Reads events from the input stream until a complete well-balanced
     * tree is sent to the output stream.
     * Assumes that the current event points to the beginning of the tree.
     * This method allows for incremental read of sub-trees. 
     * @param in an input stream of XML events
     * @param out an output for XML events
     * @return the code of the event following the tree
     * @throws DataModelException if thrown by the input stream
     */
    public static int convertTree(XMLPullStream in, XMLPushStream out)
        throws DataModelException
    {
        int e = in.getCurrentEvent();
        if(e == XMLPullStream.START)
            e = in.moveToNextEvent();
        switch(e) {
        case XMLPullStream.DOCUMENT_START:
            documentStart(in, out);
            e = in.moveToNextEvent();
            for(; e != XMLPullStream.DOCUMENT_END && e != XMLPullStream.END; )
                convertTree(in, out);
            if(e == XMLPullStream.DOCUMENT_END) {
                out.putDocumentEnd();
            }
            break;

        case XMLPullStream.ELEMENT_START:
            elementStart(in, out);
            e = in.moveToNextEvent();
            for(; e != XMLPullStream.ELEMENT_END && e != XMLPullStream.END; )
                e = convertTree(in, out);
            if(e == XMLPullStream.ELEMENT_END) {
                out.putElementEnd(in.getName());
            }
            break;

        case XMLPullStream.TEXT:
            out.putText(in.getText());
            break;

        case XMLPullStream.PROCESSING_INSTRUCTION:
            out.putProcessingInstruction(in.getTarget(), in.getText());
            break;

        case XMLPullStream.COMMENT:
            out.putComment(in.getText());
            break;
        case XMLPullStream.END:
            return e;

        default:
            
        }
        // swallow something anyway:
        e = in.moveToNextEvent();
        return e;
    }

    /**
     * Directly builds an internal Node representation from a XML input stream.
     * Reads exactly enough events to build a balanced tree.
     * @param in XML input stream
     * @return the Node built
     * @throws DataModelException if thrown by the input stream
     */
    public static Node buildNode(XMLPullStream in) throws DataModelException
    {
        IDocument doc;
        CorePushBuilder builder;
        
        int event = in.getCurrentEvent();
        switch(event) {
        case XMLPullStream.DOCUMENT_START:
            doc = new IDocument();
            convertTree(in, new PushStreamToSAX(doc));
            return new FONIDataModel(doc).getDocumentNode();

        case XMLPullStream.ELEMENT_START:
            doc = new IDocument();
            convertTree(in, new PushStreamToSAX(doc));
            return new FONIDataModel(doc).getDocumentNode();

        case XMLPullStream.TEXT:
            builder = new CorePushBuilder(".");
            builder.putText(in.getText());
            return builder.harvest();

        case XMLPullStream.PROCESSING_INSTRUCTION:
            builder = new CorePushBuilder(".");
            builder.putProcessingInstruction(in.getTarget(), in.getText());
            return builder.harvest();

        case XMLPullStream.COMMENT:
            builder = new CorePushBuilder(".");
            builder.putComment(in.getText());
            return builder.harvest();

        default:
            return null;
        }
    }

    private PullStreamConverter() { }
    
    private static void documentStart(XMLPullStream in, XMLPushStream out)
        throws DataModelException
    {
        out.putDocumentStart();
        String dtd = in.getDTDName();
        if(dtd != null)
            out.putDTD(dtd, in.getDTDPublicId(), in.getDTDSystemId(),
                       in.getInternalSubset());
    }

    private static void elementStart(XMLPullStream in, XMLPushStream out)
        throws DataModelException
    {
        out.putElementStart(in.getName());
        for(int n = 0, cnt = in.getNamespaceCount(); n < cnt; n++) {
            out.putNamespace(in.getNamespacePrefix(n),
                             in.getNamespaceURI(n));
        }
        for(int a = 0, cnt = in.getAttributeCount(); a < cnt; a++) {
            out.putAttribute(in.getAttributeName(a),
                             in.getAttributeValue(a), null);
        }
    }
}
