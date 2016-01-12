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

/**
 *  Implementation of function fn:string-join.
 */
public class StringJoin extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("string-join", XQType.STRING, Exec.class)
            .arg("op1", XQType.STRING.star) .arg("op2", XQType.STRING)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.StringCall
    {
        public String evalAsString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQValue v = args[0].eval(focus, context);
            String sep = args[1].evalAsString(focus, context);
            StringBuffer buf = new StringBuffer();
            boolean first = true;
            for (; v.next();) {
                if (!first)
                    buf.append(sep);
                first = false;
                buf.append(v.getString());
            }
            return buf.toString();
        }
    }
}
