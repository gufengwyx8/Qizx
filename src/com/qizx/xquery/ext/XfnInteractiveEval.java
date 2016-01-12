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

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.EvaluationStackTrace;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.util.XMLSerializer;
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExpressionImpl;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *  Parses and evaluates a XQuery expression, returns the results in diverse
 *  forms according to the options.
 *	<p>Options:<ul>
 *	<li>escaped: if true, each item is wrapped into a redefinable element, 
 *  otherwise it is simply serialized to XML.
 *	<li>output-max: maximum size of output in bytes.
 *	<li>time-max: maximum execution time in milliseconds.
 *	<li>item-max: maximum number of displayed items.
 *	</ul>
 */
public class XfnInteractiveEval extends ExtensionFunction
{
    static QName LENGTH_LIMIT = IQName.get("LENGTH_LIMIT");
    static QName SIZE_LIMIT = IQName.get("SIZE_LIMIT");

    static QName qfname = IQName.get(EXTENSION_NS,
    "interactive-eval");
    static Prototype[] protos = { 
        new Prototype(qfname, XQType.NODE.star, Exec.class)
            .arg("query", XQType.STRING)
            .arg("options", XQType.NODE.star),
        new Prototype(qfname, XQType.NODE.star, Exec.class)
            .arg("query", XQType.STRING)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {    
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            String querySrc = args[0].evalAsString(focus, context);
            querySrc = querySrc.replace('\r', ' ');
            Node options = (args.length < 2)? null
                                : args[1].evalAsOptNode(focus, context);
            
            QName headerElem = IQName.get("pre"), itemElem = IQName.get("pre");
            QName classAttr = IQName.get("class");
            int maxTime = -1, maxItems = -1;
            int maxOutSize = -1;
            boolean escaped = true, traceExceptions = false;
            
            Node[] attrs = null;
            try {
                if(options != null && (attrs = options.getAttributes()) != null) {  
                    for (int a = 0; a < attrs.length; a++) {
                        Node attr = attrs[a];
                        String option = attr.getNodeName().getLocalPart();
                        if(option.equals("escaped"))
                            escaped = attr.getStringValue().equals("true");
                        else if(option.equals("output-max"))
                            maxOutSize = Integer.parseInt(attr.getStringValue());
                        else if(option.equals("time-max"))
                            maxTime = Integer.parseInt(attr.getStringValue());
                        else if(option.equals("item-max"))
                            maxItems = Integer.parseInt(attr.getStringValue());
                        else
                            context.error(Function.ERR_ARGTYPE, 
                                          this, "invalid option " + option);
                    }
                }
            }
            catch (NumberFormatException e1) {
                context.error(Function.ERR_ARGTYPE, this, 
                              "invalid option value: " + e1.getMessage());
            }
            catch (DataModelException e1) {
                context.error(this, new EvaluationException(e1.getMessage()));
            } 

            StringWriter out = null;
            PrintWriter pwout = null;
            CorePushBuilder builder =
                new CorePushBuilder(context.getStaticContext().getBaseURI());
            
            ArraySequence rseq = new ArraySequence(4, null);
            try {
                out = new StringWriter();
                pwout = new PrintWriter(out);
                
                ExpressionImpl exp =
                    context.dynamicContext().compileExpression(querySrc);
                //exp.defineFocus(focus);
                if(maxTime > 0)
                    exp.setTimeOut(maxTime);
                if(maxOutSize > 0)
                    builder.setMaxVolume(maxOutSize);
                
                try { // execution
                    long T0 = System.currentTimeMillis();
                    // execution time includes compilation
                    XQValue result = exp.rawEval();
                    
                    int count = 0;
                    for(; result.next(); )
                        ++ count;
                    long T1 = System.currentTimeMillis();

                    //builder.startDocument();
                    builder.putElementStart(headerElem);
                    builder.putAttribute(classAttr, "header", null);
                    builder.putText("Query executed in "+(T1-T0)+" milliseconds, returns ");
                    
                    if (count == 0)
                        builder.putText("empty sequence");
                    else
                        builder.putText(count + " item"
                                        + ((count > 1) ? "s" : "") + ":");
                    builder.putElementEnd(headerElem);
                    //builder.endDocument();
                    rseq.addItem( builder.harvest() );
                    
                    XMLSerializer serialDisplay = new XMLSerializer();
                    if(maxOutSize > 0)
                        serialDisplay.setMaxVolume(maxOutSize);
                    //serialDisplay.defineContextMappings( exp.getInScopeNS() );
                    serialDisplay.setOption(XMLSerializer.OMIT_XML_DECLARATION, "yes");
                    serialDisplay.setOption(XMLSerializer.INDENT, "yes");
                    if(escaped) {
                        builder.reset();
                        builder.putElementStart(itemElem);
                        builder.putAttribute(classAttr, "items", null);
                    }
                    result = result.bornAgain();
                    for(int it = 1; result.next(); ++it) {
                        if(maxItems > 0 && it > maxItems)
                            context.error(LENGTH_LIMIT, this, "too many items");
                        XQItem item = result.getItem();
                        Node node = null;
                        if (item.isNode()
                            && !escaped
                            && (node = item.getNode()).getNodeNature() != Node.TEXT) {
                            rseq.addItem((XQItem) node);
                        }
                        else {	// serialize each item inside an 'item' element:
                            if(item.isNode()) {	// escaped
                                out = new StringWriter();
                                pwout = new PrintWriter(out);
                                serialDisplay.setOutput(pwout);
                                serialDisplay.reset();
                                serialDisplay.putNodeCopy(item.getNode(), 0);
                                serialDisplay.flush();
                                builder.putText(trimString(out.getBuffer()));
                            }
                            else builder.putAtomText( result.getString() );
                        }
                    }
                    if(escaped) {
                        builder.putElementEnd(itemElem);
                        builder.flush();
                        rseq.addItem(builder.harvest());
                    }
                }
                catch (EvaluationException ee) {
                    QName errCode = ee.getErrorCode();
                    if (errCode == EvaluationException.TIME_LIMIT ||
                        errCode == LENGTH_LIMIT ||
                        errCode == SIZE_LIMIT)
                    {
                        if(escaped) {
                            builder.setMaxVolume(-1);
                            //builder.putElementEnd(itemElem);
                            //builder.flush();
                            rseq.addItem(builder.harvest());
                        }
                        pwout.println("** limit reached: "+ ee.getMessage() +" **");
                    }
                    else {
                        pwout.println("*** execution error: " + ee.getMessage());
                        if(ee.getCause() != null && traceExceptions) {
                            pwout.println("  caused by: " + ee.getCause());
                        }
                        EvaluationStackTrace[] stack = ee.getStack();
                        for (int i = 0; i < stack.length; i++) {
                            pwout.println(stack[i].getSignature());

                        }
                    }

                    // generate <header class='runtime-error'>
                    builder.putElementStart(headerElem);
                    builder.putAttribute(classAttr, "runtime-error", null);
                    builder.putText(out.toString());
                    builder.putElementEnd(headerElem);
                    rseq.addItem(builder.harvest());
                }
                finally {
                    // retrieve possible compilation messages
                    //context.messages(exp.getMessages());
                }
            }
            catch (Exception e) {
                try {
                    // generate <header class='error'>
                    builder.putElementStart(headerElem);
                    builder.putAttribute(classAttr, "error", null);
                    pwout.println("*** "+e.getMessage());
                    pwout.flush();
                    builder.putText(out.toString());
                    builder.putElementEnd(headerElem);
                    rseq.addItem(builder.harvest());
                }
                catch (DataModelException de) {
                    context.error("FOSE0000", this,
                                  "serialization error: " + de.getMessage());
                }
            }
            return rseq;
        }
    }
    
    // annoying problem with \r generated by PrintWriter on Windows
    private static String trimString(StringBuffer buf)
    {
        int out = 0;
        for(int i = 0, L = buf.length(); i < L; i++)
            if(buf.charAt(i) != '\r')
                buf.setCharAt(out++, buf.charAt(i));
        return buf.substring(0, out);
    }
} 
