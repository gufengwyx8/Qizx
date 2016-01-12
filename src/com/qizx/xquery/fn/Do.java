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
import com.qizx.xquery.dt.SingleItem;


/**
 * Extension: a function which guarantees that all its arguments 
 * are evaluated in order.
 */
public class Do extends Function {
    
    static Prototype[] protos = { 
        Prototype.varfn("do", XQType.ANY, Exec.class) .arg("op", XQType.ANY)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQItem last = null;
            for (int s = 0; s < args.length; s++) {
                XQValue v = args[s].eval(focus, context);
                context.at(this);
                for (; v.next();)
                    last = v.getItem();
            }
            return last == null ? XQValue.empty : new SingleItem(last);
        }
    }
}
