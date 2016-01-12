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

import com.qizx.api.XMLPushStream;

import javax.xml.transform.sax.SAXResult;

/**
 * An extension of SAXResult that adapts to XMLPushStream.
 * <p>
 * Can be used to store the result of a XSLT transformation into a Document of
 * an XML {@link Library}.
 * <p>
 * To perform this operation, use {@link Library#beginImportDocument(String)},
 * and wrap the returned handler in a PushStreamResult, which is then used as
 * output by the XSLT transformation. After the XSLT transformation, it is
 * compulsory to call endImportDocument:
 * 
 * <pre>
 * Library lib = ...; // a XML Library where the result is stored
 * javax.xml.transform.Transformer transformer = ... ; // XSLT processor
 * javax.xml.transform.Source source = ...; // source to be transformed
 * 
 * PushStreamResult result = new PushStreamResult(lib.beginImportDocument());
 * transformer.transform(source, result);
 * Document doc = lib.endImportDocument();
 * //... doc can be used to set Properties
 * </pre>
 */
public class PushStreamResult extends SAXResult
{
    private SAXToPushStream adapter;

    /**
     * Builds a SAXResult that writes to a XMLPushStream.
     * @param output the XMLPushStream output
     */
    public PushStreamResult(XMLPushStream output)
    {
        adapter = new SAXToPushStream(output);
        setHandler(adapter);
        setLexicalHandler(adapter);
    }
}
