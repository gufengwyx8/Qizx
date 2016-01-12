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
package com.qizx.xquery.op;

import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.FunctionItem;
import com.qizx.xquery.dt.FunctionType;
import com.qizx.xquery.fn.Prototype;

public class FunctionLiteral extends Expression
{
    QName name;
    int   arity;
    Prototype proto;
    
    public FunctionLiteral(QName fname, long arity)
    {
        this.name = fname;
        this.arity = (int) arity;
    }

    public Expression child(int rank) {
        return null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("value", toString());
    }

    public String toString()
    {
        return name + "#" + arity + " " + proto;
    }

    public int getFlags() {
        return CONSTANT;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        proto = context.functionLookup(name, arity);
        if (proto == null) {
            context.error("XPST0017", this,
                          "no function '" + context.prefixedName(name)
                          + "' accepting " + arity + " arguments");
            return this;
        }
        setType(new FunctionType(proto));
        return this;
    }

    @Override
    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return new FunctionItem(proto, context);
    }

    @Override
    public XQItem evalAsItem(Focus focus, EvalContext context)
        throws EvaluationException
    {        
        return new FunctionItem(proto, context);
    }
}
