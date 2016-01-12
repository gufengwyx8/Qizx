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
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.op.Expression;


/**
 *  Generic Implementation of functions xs:*().
 */
public class CastFunction extends Function
{    
    private static Prototype[] protos = { 
        Prototype.xs("cast", XQType.ANY, Exec.class)
            .arg("srcval", XQType.ATOM)
    };
    public Prototype[] getProtos() { return protos; }	// dummy
    public XQItemType[] getAllowedTypes() {
        return null;
    }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            Expression expr = args[0];
            XQValue arg = expr.eval(focus, context), result = null;
            context.at(this);
            if( !arg.next() )
                return XQValue.empty;

            result = prototype.returnType.itemType().cast(arg, context);

            if( arg.next() )
                context.error(ERRC_BADTYPE, expr, XQType.ERR_TOO_MANY);
            return result;
        }
    }
}
