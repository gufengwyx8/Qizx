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

import com.qizx.api.QName;
import com.qizx.api.QizxException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Function;


/**
 * Call to Function: temporarily built by the parser.
 *  After static check, it is resolved as predefined function runtime,
 *  UserFunction.Call, JavaFunction.Call etc
 *  @see FunctionItemCall
 */
public class FunctionCall extends Expression
{
    public QName name;
    Expression[] args = new Expression[0];

    public FunctionCall(QName name)
    {
        this.name = name;
    }

    public FunctionCall(QName name, Expression[] args)
    {
        this.name = name;
        this.args = args;
    }

    public void addArgument(Expression arg)
    {
        args = addExpr(args, arg);
    }

    public int getArgCount()
    {
        return args.length;
    }

    public Expression child(int rank)
    {
        return (rank < args.length) ? args[rank] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("name", name);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        // TODO special functions may want to check their arguments themselves

        for (int a = 0; a < args.length; a++)
            args[a] = context.staticCheck(args[a], 0);
        // lookup simply by name: overloaded functions hold several prototypes
        // selected by Function.staticCheck(context, arg)
        // Note: staticCheck happens after all functions declarations
        // have been processed
        Function fun = context.functionLookup(name);
        if (fun == null) {
            context.error("XPST0017", this, "unknown function '" +
                          context.prefixedName(name)+ "'");
            return this;
        }
        return fun.staticCheck(name, context, args, this);
    }

    public XQValue eval(Focus focus, EvalContext context)
    {
        throw new RuntimeException("BUG: FunctionCall.eval(" + name + ") : should not happen");
    }
}
