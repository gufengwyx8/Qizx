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
package com.qizx.xquery.impl;

import com.qizx.api.DataModelException;
import com.qizx.api.XMLPullStream;
import com.qizx.xdm.DocumentParser;
import com.qizx.xdm.FONIDataModel;
import com.qizx.xdm.FONIDocument;
import com.qizx.xdm.NodePullStream;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

public class XQueryXParser
{
    private static final String XQX_NS = "http://www.w3.org/2005/XQueryX";
    private DocumentParser dmParser = new DocumentParser();
    private XMLPullStream in;
    
    public XQueryXParser()
    {
    }

    public static void main(String[] args)
    {
        try {
            for (int a = 0; a < args.length; a++) {
                String arg = args[a];
                if ("-o".equals(arg)) {
                }
                else {
                }
            }

            XQueryXParser xp = new XQueryXParser();
            xp.parse(new File("/home/Qizx/toto.xqx"));
        }
        catch (SAXException e) {
            Throwable cause = e.getException();
            if(cause != null)
                cause.printStackTrace();
            else System.err.println("*** " + e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parse(File file) 
        throws SAXException, IOException, DataModelException
    {
        parse(dmParser.parseDocument(file));
    }

    private void parse(FONIDocument doc) throws DataModelException
    {
        in = new NodePullStream(new FONIDataModel(doc).getDocumentNode());
        if(in.moveToNextEvent() != XMLPullStream.DOCUMENT_START)
            throw new DataModelException("empty XQueryX expression");
        wantStartTag("module");
    }

    private void wantStartTag(String name)
    {
        
    }

    private void parseModule()
    {
         // TODO
    }
}
