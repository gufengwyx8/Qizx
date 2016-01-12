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

import com.qizx.api.CompilationException;
import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.util.text.LexicalTokenizer;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

/**
 * Returns a string representing a XQuery script as an element structure where
 * each token is wrapped in an element.
 */
public class XfnSyntaxColoring extends ExtensionFunction
{
    static QName qfname =
        IQName.get(EXTENSION_NS, "syntax-coloring");
    static Prototype[] protos = {
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("query", XQType.STRING)
            .arg("options", XQType.NODE.star)
        };

    public Prototype[] getProtos() {
        return protos;
    }

    final static String[] tokenTypes = {
        null, "tag", "s", "nu", "tx", "op", "nm", "k", "com", "prag", "fun"
    };

    public static class Exec extends Function.TreeCall
    {

        public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                                 EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            String code = args[0].evalAsString(focus, context);
            code = code.replace('\r', ' ');
            Node options = (args.length < 2) ?
                    null : args[1].evalAsOptNode(focus, context);
            LexicalTokenizer scolo = new LexicalTokenizer(code);

            QName wrapperElem = IQName.get("pre");
            QName spanElem = IQName.get("span");
            QName classAttr = IQName.get("class");

            try {
                if (options != null) {
                    Node[] attrs = options.getAttributes();
                    if(attrs != null)
                        for (int a = 0; a < attrs.length; a++) {
                            Node attr = attrs[a];
                            String option = attr.getNodeName().getLocalPart();
                        // TODO specify span element?
                        }
                }
                output.putElementStart(wrapperElem);
                int prevToken = -1;
                for (int token = scolo.nextToken(); token > 0; token =
                    scolo.nextToken()) {
                    String space = scolo.getSpace();
                    if (space != null)
                        output.putText(space);
                    if (token != prevToken) {
                        if (prevToken >= 0)
                            output.putElementEnd(spanElem);
                        output.putElementStart(spanElem);
                        output.putAttribute(classAttr,
                                            "xq_" + tokenTypes[token], null);
                        prevToken = token;
                    }
                    String value = scolo.getTokenValue();
                    output.putText(value);
                }
                output.putElementEnd(spanElem);
                output.putElementEnd(wrapperElem);
            }
            catch (DataModelException e) {
                // should not happen
            }
            catch (CompilationException e) {
                // ignore and stop
            }
        }
    }
}
