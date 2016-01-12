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
import com.qizx.xdm.IQName;
import com.qizx.xdm.XQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleQName;

/**
 *  Implementation of function xs:QName.
 */
public class QName extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("QName", XQType.QNAME, Exec.class)
            .arg("uri", XQType.STRING.opt)
            .arg("qname", XQType.STRING.opt)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {    
        public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
        {
            context.at(this);
            try {
                String uri = args[0].evalAsOptString(focus, context);
                if(uri == null)
                    uri = "";
                String name = args[1].evalAsOptString(focus, context);
                String prefix = IQName.extractPrefix(name);
                String ncname = IQName.extractLocalName(name);
                if(prefix.length() > 0 && uri.length() == 0)
                    context.error("FOCA0002", this, 
                                  "prefix not allowed if no namespace");
                return new SingleQName(XQName.get(uri, ncname, prefix));
            }
            catch (DataModelException e) {
                return context.error("FOCA0002", this, e.getMessage());
            }
        }
    }

}
