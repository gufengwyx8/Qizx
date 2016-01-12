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
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.impl.EmptyException;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.NodeSortExpr;

/**
 *  Implementation of function fn:count.
 */
public class Count extends Function {
    
    static Prototype[] protos = { 
        Prototype.fn("count", XQType.INTEGER, Exec.class)
            .arg("src", XQType.ITEM.star),
    };
    public Prototype[] getProtos() { return protos; }
    
    public Expression staticCheck(ModuleContext context,
                                  Expression[] arguments, Expression subject)
    {
        Expression ex = super.staticCheck(getName(), context, arguments, subject);
        Exec rt = (Exec) ex;
        // FIXME? is it correct ? what if duplicate nodes?
        if (rt.args.length > 0 && // error protection
            rt.args[0] instanceof NodeSortExpr) {
            NodeSortExpr nodso = (NodeSortExpr) rt.args[0];
            rt.args[0] = nodso.expr;
        }
        return rt;
    }


    public static class Exec extends Function.IntegerCall
    {
        public long evalAsInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            try {
                XQValue v = args[0].eval(focus, context);
                context.at(this);
                return v.quickCount(context);
            }
            catch (EmptyException e) {
                return 0;
            }
        }
    }
}
