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

import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.FunctionItem;
import com.qizx.xquery.dt.FunctionType;
import com.qizx.xquery.fn.UserFunction.Signature;


/**
 * Creation of a function item, bound to a context
 */
public class InlineFunction extends Expression
{
    Signature  prototype;

    public InlineFunction(Signature prototype)
    {
        this.prototype = prototype;
    }

    public Expression child(int rank)
    {
        return rank == 0? prototype : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child(prototype);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        // check the body of the function: the "level" of the local context
        // is changed so that var references to closure context can be detected
        context.pushLocalsLevel();
        context.staticCheck(prototype, 0);
        context.popLocalsLevel();
        setType(new FunctionType(prototype));
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
    {
        return new FunctionItem(prototype, context);
    }
}
