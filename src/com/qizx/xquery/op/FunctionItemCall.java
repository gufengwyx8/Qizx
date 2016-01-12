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
import com.qizx.xquery.*;
import com.qizx.xquery.dt.FunctionItem;
import com.qizx.xquery.dt.FunctionType;
import com.qizx.xquery.fn.Prototype;


/**
 * Call to function as an item: 
 */
public class FunctionItemCall extends Expression
{
    public Expression function;
    public Expression[] args = new Expression[0];

    public FunctionItemCall(Expression functionItem)
    {
        this.function = functionItem;
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
        return rank == 0? function : (rank < args.length) ? args[rank - 1] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("function", function);
        d.children(args);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        function = context.staticCheck(function, 0);
        for (int a = 0; a < args.length; a++)
            args[a] = context.staticCheck(args[a], 0);
        XQType ftype = function.getType();
        if(ftype instanceof FunctionType) {
            Prototype fprot = ((FunctionType) ftype).getSignature();
            if(fprot != null && !fprot.accepts(args.length))
                // surely it is an error
                context.error("XPTY0004", this, "invalid number of arguments");
            type = fprot.returnType;
        }
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQItem fun = function.evalAsItem(focus, context);
        if(!(fun instanceof FunctionItem))
            context.badTypeForArg(XQType.FUNCTION, function, 0, "function item");
        
        FunctionItem funItem = (FunctionItem) fun;
        Prototype proto = funItem.prototype;
        
        return proto.invoke(args, this, funItem.lexicalContext, focus, context);
    }
}
