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
import com.qizx.xquery.dt.SingleBoolean;

/**
 * Implementation of operator 'castable as'.
 */
public class CastableExpr extends CastExpr
{
    public CastableExpr(Expression expr, XQType targetType)
    {
        super(expr, targetType);
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("type", targetType);
        d.child(expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        super.staticCheck(context, flags);
        type = XQType.BOOLEAN;
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        return new SingleBoolean(evalAsBoolean(focus, context));
    }

    public boolean evalAsBoolean(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue v = expr.eval(focus, context);
        context.at(this);
        if (!v.next())
            return isOptional;
        try {
            targetType.cast(v, context);
            return !v.next(); // must be singleton
        }
        catch (EvaluationException e) {
            return false;
        }
    }
}
