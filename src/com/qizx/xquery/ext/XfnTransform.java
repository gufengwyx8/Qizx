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
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.xdm.FONIDataModel;
import com.qizx.xdm.IDocument;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleNode;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

import java.util.Properties;

/**
 *  Implementation of function x:transform().
 *
 * 	x:transform( $node, $templates as string, $parameters as element()?,
 * 	             [ $options as element() ] )
 * 	    as node()?
 */
public class XfnTransform extends ExtensionFunction
{    
    static QName qfname = IQName.get(EXTENSION_NS, "transform");
    static Prototype[] protos = { 
            new Prototype(qfname, XQType.NODE.opt, Exec.class)
              .arg("node", XQType.NODE)
              .arg("templates", XQType.STRING)
              .arg("parameters", XQType.ELEMENT),
            new Prototype(qfname, XQType.NODE.opt, Exec.class)
              .arg("node", XQType.NODE)
              .arg("templates", XQType.STRING)
              .arg("parameters", XQType.ELEMENT)
              .arg("options", XQType.ELEMENT)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            Node source = args[0].evalAsOptNode(focus, context);
            String templates = args[1].evalAsString(focus, context);
            Node paramNode = args[2].evalAsOptNode(focus, context);
            Node optionNode =
                (args.length < 4) ? null : args[3].evalAsNode(focus, context);
            context.at(this);
            try {
                // options (String value always)
                Properties options = new Properties();
                if (optionNode != null) {
                    Node[] opts = optionNode.getAttributes();
                    for (int i = 0; i < opts.length; i++) {
                        Node option = opts[i];
                        options.put(option.getNodeName().toString(),
                                    option.getStringValue());
                    }
                }
                // parameters (String value always)
                Node[] params = paramNode.getAttributes();
                Properties parameters = new Properties();
                if(params != null) {
                    for (int p = 0; p < params.length; p++) {
                        Node param = params[p];
                        parameters.put(param.getNodeName().toString(),
                                       param.getStringValue());
                    }
                }
                
                IDocument doc = context.dynamicContext().xslTransform(
                                      source, templates, parameters, options);
                if(doc == null)
                    return XQValue.empty;
                return new SingleNode(new FONIDataModel(doc).getDocumentNode());
            }
            catch (EvaluationException e) {
                throw e;	// just to avoid the next catch
            }
            catch (Exception e) {
                //e.printStackTrace();
                context.error("FXTR0001", this, 
                              new EvaluationException("XSLT error: " +
                                                      e.toString(), e));
            }
            return null;	// dummy
        }
    }
}
