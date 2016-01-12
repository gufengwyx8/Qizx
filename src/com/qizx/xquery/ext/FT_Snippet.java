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

import com.qizx.api.*;
import com.qizx.api.util.fulltext.FullTextSnippetExtractor;
import com.qizx.queries.FullText;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.IntegerLiteral;
import com.qizx.xquery.op.NodeLiteral;
import com.qizx.xquery.op.StringLiteral;

/**
 *  Processes a XML fragment and returns it with "highlighted" occurrences of the
 *  terms of a full-text query.
 */
public class FT_Snippet extends ExtensionFunction
{
    static QName qfname = IQName.get(FULLTEXT_EXT_NS, "snippet");
    static Prototype[] protos = { 
        new Prototype(qfname, XQType.NODE.opt, Exec.class)
            .arg("node", XQType.NODE)
            .arg("query", XQType.STRING)
            .arg("options", XQType.NODE.opt),
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("node", XQType.NODE)
            .arg("query", XQType.STRING)
    };
    public Prototype[] getProtos() { return protos; }


    public static class Exec extends Function.TreeCall
    {
        public void evalAsEvents(XMLPushStreamBase output,
                                 Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            ModuleContext stCtx = context.getStaticContext();
            
            // query:
            FullText.Selection query =
                FT_Contains.compileQueryArgument(args[1], focus, context);

            Node node = args[0].evalAsNode(focus, context);
            
            Node options = (args.length < 3) ?
                               null : args[2].evalAsOptNode(focus, context);
            
            QName snippet = IQName.get("snippet");
            QName wrapperElem = IQName.get("B");
            QName styleAttr = null;
            String pattern = "fterm%";
            Expression funCall = null;
            Expression[] funArgs = null;

            FullTextSnippetExtractor ftx =
                new FullTextSnippetExtractor(query, stCtx.getFulltextFactory());
            
            try {
                if (options != null) {
                    Node[] attrs = options.getAttributes();
                    if(attrs != null)
                    for (int a = 0; a < attrs.length; a++) {
                        Node attr = attrs[a];
                        String option = attr.getNodeName().getLocalPart();
                        if (option.equals("snippet"))
                            snippet = toQName(attr.getStringValue(), options, context);
                        else if (option.equals("word-wrap"))
                            wrapperElem = toQName(attr.getStringValue(), options, context);
                        else if (option.equals("word-style"))
                            styleAttr = toQName(attr.getStringValue(), options, context);
                        else if (option.equals("word-pattern"))
                            pattern = attr.getStringValue();
                        else if (option.equals("word-function")) {
                            QName funName = toQName(attr.getStringValue(),
                                                    options, context);
                            funArgs = new Expression[] {
                                 new StringLiteral("?"), new IntegerLiteral(0),
                                 new NodeLiteral(null)
                            };
                            funCall = checkFunction(funName, funArgs, context);
                        }
                        else if (option.equals("length")) {
                            try {
                                int len = Integer.parseInt(attr.getStringValue());
                                ftx.setMaxSnippetSize(len);
                            }
                            catch (NumberFormatException e) {
                                context.error("XQFT0100", args[2], "invalid length");
                            }
                        }
                        else if (option.equals("work-size")) {
                            try {
                                int len = Integer.parseInt(attr.getStringValue());
                                ftx.setMaxWorkSize(len);
                            }
                            catch (NumberFormatException e) {
                                context.error("XQFT0100", args[2], "invalid work-size");
                            }
                        }
                    }
                }
                 
                ftx.start(node);
                output.putElementStart(snippet);
                for( ; ftx.moveToNextEvent() != XMLPullStream.END; ) {
                    switch(ftx.getCurrentEvent()) {
                    case XMLPullStream.TEXT:
                    case FullTextSnippetExtractor.GAP:
                        output.putText(ftx.getText());
                        break;
                    case FullTextSnippetExtractor.FT_TERM:
                        if(funCall != null) {
                            funArgs[0] = new StringLiteral(ftx.getText());
                            funArgs[1] = new IntegerLiteral(ftx.getTermPosition());
                            funArgs[2] = new NodeLiteral(ftx.getCurrentNode());
                            node = funCall.evalAsOptNode(focus, context);
                            output.putNodeCopy(node, 0);
                        }
                        else {
                            output.putElementStart(wrapperElem);
                            if(styleAttr != null && pattern != null) {
                                String rank = Integer.toString(ftx.getTermPosition());
                                output.putAttribute(styleAttr,
                                                    pattern.replaceAll("%", rank), null);
                            }
                            output.putText(ftx.getText());
                            output.putElementEnd(wrapperElem);
                        }
                        break;
                    }
                }
                output.putElementEnd(snippet);
            }
            catch (CompilationException e) {
                Message[] m = e.getMessages();
                context.error(e.getErrorCode(), this, "runtime eval " + m[0]);
            }
            catch (QizxException e) {
                context.error(e.getErrorCode(), this, e.getMessage());
            }
        }
    }
}
