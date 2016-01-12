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

/**
 * Implementation of operator 'cast as'.
 */
public class CastExpr extends Expression
{

    public Expression expr;

    public XQItemType targetType;

    protected boolean isOptional;

    public CastExpr(Expression expr, XQType targetType)
    {
        this.expr = expr;
        this.targetType = targetType.itemType();
        isOptional = targetType instanceof SequenceType; // only
                                                            // (ItemType.opt)
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? expr : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("targetType", targetType);
        d.child("expr", expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        expr = context.staticCheck(expr, 0);
        if (targetType == XQType.ITEM || targetType == XQType.ATOM
            || targetType == XQType.NOTATION)
            context.error("XPST0080", this, "invalid cast target type");
        type = targetType;
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue v = expr.eval(focus, context), result;
        if (!v.next()) {
            if (!isOptional)
                context.error("XPTY0004", this,
                              "empty sequence cannot be cast to type "
                                  + targetType);
            else
                return XQValue.empty;
        }
        try {
            result = targetType.cast(v, context);
        }
        catch (XQTypeException err) {
            return context.error(this, err);
        }
        if (v.next())
            context.error("XPTY0004", this, "singleton expected in cast");
        return result;
    }
}
