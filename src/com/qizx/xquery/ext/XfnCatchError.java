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
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;
import com.qizx.xquery.op.Expression;

/**
 *  Implementation of function x:catch-error( expr, fallback ).
 *
 */
public class XfnCatchError extends ExtensionFunction
{
    static QName qfname = IQName.get(EXTENSION_NS, "catch-error");
    static Prototype[] protos = { 
            new Prototype(qfname, XQType.ANY, Exec.class)
            .arg("expr", XQType.ANY) .arg("fallback", XQType.ANY)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    public Expression staticCheck(ModuleContext context,
                                  Expression[] arguments, Expression subject)
    {
        Expression res = context.resolve(qfname, getProtos(), arguments, subject);
        if (arguments.length == 2)
            res.setType(arguments[0].getType().unionWith(arguments[1].getType(), true));
        return res;
    }
    
    public static class Exec extends Function.Call {
        
        public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException {
            context.at(this);
            try {
                return args[0].eval( focus, context );
            }
            catch (EvaluationException e) {
                //Log log = context.getStaticContext().getLog();
                //log.trace( module, location, "caught "+ e.getMessage(), null);
                return args[1].eval( focus, context );
            }
        }
    }
}
