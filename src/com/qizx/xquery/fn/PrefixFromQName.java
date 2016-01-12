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

import com.qizx.api.EvaluationException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleString;

/**
 *  Implementation of function fn:get-namespace-from-QName.
 */
public class PrefixFromQName extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("prefix-from-QName", XQType.STRING.opt, Exec.class)
            .arg("qname", XQType.QNAME.opt)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQItem item;
            try {
                item = args[0].evalAsOptItem(focus, context);
            }
            catch (EvaluationException e) {
                e.substituteCode("XPTY0004", "FONS0004");
                throw e;
            }
            if (item == null)
                return XQValue.empty;
            context.checkArgType(item, this, 0, XQType.QNAME);
            com.qizx.api.QName qname = item.getQName();
            String prefix = qname.getPrefix();
            return (prefix == null) ? XQValue.empty : new SingleString(prefix);
        }
    }
}
