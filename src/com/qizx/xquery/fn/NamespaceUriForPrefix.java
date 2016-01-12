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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleString;

/**
 *  Implementation of function fn:get-namespace-uri-for-prefix.
 */
public class NamespaceUriForPrefix extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("namespace-uri-for-prefix", XQType.ANYURI.opt, Exec.class)
            .arg("prefix", XQType.STRING.opt)
            .arg("element", XQType.ELEMENT)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            String prefix = args[0].evalAsOptString(focus, context);
            if (prefix == null)
                prefix = "";
            Node element = args[1].evalAsNode(focus, context);
            try {
                String nsUri = element.getNamespaceUri(prefix);
                return nsUri == null ? XQValue.empty
                                     : new SingleString(nsUri, XQType.ANYURI);
            }
            catch (DataModelException e) {
                return dmError(context, e);
            }
        }
    }
}
