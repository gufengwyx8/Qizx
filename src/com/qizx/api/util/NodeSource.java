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

import com.qizx.api.Node;

import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.transform.sax.SAXSource;

/**
 * An extension of SAXSource that allows using a Qizx Document or Node as a
 * source for an XSLT transformation.
 */
public class NodeSource extends SAXSource
{
    private Node rootNode;

    /**
     * Creates a NodeSource from a Node.
     * @param node the XML tree to transform by a XSLT processor.
     */
    public NodeSource(Node node)
    {
        super(new NodeXMLReader(node),
              // dummy input: most XSLT engines dont like it null
              new InputSource(new StringReader("<dummy/>")));
        rootNode = node;
    }

    /**
     * Returns the Node used as input by the XSLT transformation.
     * @return the root node set in the constructor
     */
    public Node getRootNode()
    {
        return rootNode;
    }
}
