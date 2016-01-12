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
import com.qizx.api.QName;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

/**
 *  Evaluates a XQuery expression, returns the execution time.
 *	<p>The result sequence is completely expanded (unless an error occurs).
 */
public class XfnTimedExec extends ExtensionFunction
{
    static QName qfname = IQName.get(EXTENSION_NS, "timed-exec");
    static Prototype[] protos = { 
        new Prototype(qfname, XQType.NODE.star, Exec.class)
            .arg("query", XQType.STRING)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.IntegerCall
    {    
        public long evalAsInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            long T0 = System.currentTimeMillis();
            XQValue query = args[0].eval(focus, context);
            long itemCnt = 0;
            for (; query.next();)
                ++itemCnt;
            return System.currentTimeMillis() - T0;
        }
    }
}
