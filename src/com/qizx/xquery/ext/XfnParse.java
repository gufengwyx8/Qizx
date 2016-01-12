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
package com.qizx.xquery.ext;

import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.xdm.DocumentParser;
import com.qizx.xdm.FONIDataModel;
import com.qizx.xdm.FONIDocument;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleNode;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

import org.xml.sax.InputSource;

import java.io.StringReader;

/**
 *  Parses a string that represents a well-formed XML document,
 *  and return the root node.
 */
public class XfnParse extends ExtensionFunction
{
    static QName qfname = IQName.get(EXTENSION_NS, "parse");
    static Prototype[] protos = { 
        new Prototype(qfname, XQType.NODE.opt, Exec.class)
                .arg("xml", XQType.STRING)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call {
        
        public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException {
            
            context.at(this);
            String src = args[0].evalAsOptString(focus, context);
            if(src == null)
                return XQValue.empty;

            DocumentParser parser = new DocumentParser();
            try {
                FONIDocument doc = 
                  parser.parseDocument(new InputSource(new StringReader(src)));
                FONIDataModel dm = new FONIDataModel(doc);
                return new SingleNode(dm.getDocumentNode());
            }
            catch (Exception e) {
                context.error("FXPA0001", this, 
                              new EvaluationException(e.getMessage(), e));
            }
            return null; // dummy
        }
   }
} 
