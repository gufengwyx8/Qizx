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
package com.qizx.xquery.fn;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleQName;

/**
 *  Implementation of function fn:resolve-QName.
 */
public class ResolveQName extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("resolve-QName", XQType.QNAME, Exec.class)
            .arg("qname", XQType.STRING)
            .arg("element", XQType.ELEMENT)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            try {
                QName result = null;
                String name = args[0].evalAsOptString(focus, context);
                Node element = args[1].evalAsNode(focus, context);
                if (name == null)
                    return XQValue.empty;
                String prefix = IQName.extractPrefix(name);
                String ncname = IQName.extractLocalName(name);
                // if(prefix.length() == 0)
                // return new SingleQName(QName.get(Namespace.NONE, ncname),
                // prefix);
                try {
                    String uri = element.getNamespaceUri(prefix);
                    if (uri != null)
                        result = IQName.get(uri, ncname);
                    else {
                        if (prefix == null || prefix.length() == 0)
                            result = IQName.get(ncname);
                        else
                            context.error("FONS0003", this,
                                         "no namespace found for prefix " + prefix);
                    }
                    return new SingleQName(XQName.get(result, prefix));
                }
                catch (DataModelException e) {
                    return dmError(context, e);
                }
            }
            catch (DataModelException e) {
                context.error(ERR_ARGTYPE, this, e.getMessage());
                return null;
            }
        }
    }
}
