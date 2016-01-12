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
import com.qizx.xquery.op.FilterExpr;

/**
 *  Implementation of function fn:subsequence.
 */
public class Subsequence extends Function
{
    static Prototype[] protos =
        { 
        Prototype.fn("subsequence", XQType.ITEM.star, Exec.class)
            .arg("sourceSeq", XQType.ITEM.star)
            .arg("startingLoc", XQType.DOUBLE),
        Prototype.fn("subsequence", XQType.ITEM.star, Exec.class)
            .arg("sourceSeq", XQType.ITEM.star)
            .arg("startingLoc", XQType.DOUBLE)
            .arg("length", XQType.DOUBLE)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQValue src = args[0].eval(focus, context);
            int start = (int) args[1].evalAsDouble(focus, context), end =
                Integer.MAX_VALUE;
            if (args.length == 3)
                end = start + (int) args[2].evalAsDouble(focus, context) - 1;
            return new FilterExpr.Slice(src, start, end);
        }
    }
}
