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
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

public class Concat extends Function {
    
    static Prototype[] protos = { 
        Prototype.varfn("concat", XQType.STRING, Exec.class)
            .arg("op", XQType.ITEM.opt)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.StringCall
    {
        public String evalAsString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            StringBuffer buf = new StringBuffer();
            if (args.length < 2 && context.sObs()) // for sticklers
                context.error("XPST0017", this,
                              "concat must have at least 2 arguments");
            for (int s = 0; s < args.length; s++) {
                XQValue v = args[s].eval(focus, context);
                context.at(this);
                // this seems to be an extension: each arg can be a sequence of
                // strings
                for (; v.next();)
                    buf.append(v.getString());
            }
            return buf.toString();
        }
    }
}
