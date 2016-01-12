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
import com.qizx.xquery.impl.EmptyException;
import com.qizx.xquery.impl.ErrorValue;

public class InstanceofExpr extends BooleanExpression
{
    private Expression expr;

    private XQType testedType; // hides member of super class

    public InstanceofExpr(Expression expr, XQType testedType)
    {
        this.expr = expr;
        this.testedType = testedType;
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? expr : null;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        expr = context.staticCheck(expr, 0);
        type = XQType.BOOLEAN;
        return this;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("testedType", testedType);
        d.child("expr", expr);
    }

    public boolean evalAsBoolean(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            XQValue v = expr.eval(focus, context);
            v = v.checkTypeExpand(testedType, context, false, false);
            if (v instanceof ErrorValue)
                return false;
            return true;
        }
        catch (EmptyException e) { // the trick
            return testedType.isOptional();
        }
    }
}
