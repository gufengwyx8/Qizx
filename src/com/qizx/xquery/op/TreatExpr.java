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
import com.qizx.xquery.impl.ErrorValue;

public class TreatExpr extends Expression
{
    public Expression expr;

    public XQType seqType;

    public TreatExpr(Expression expr, XQType seqType)
    {
        this.expr = expr;
        this.seqType = seqType;
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? expr : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("seqType", seqType);
        d.child("expr", expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        expr = context.staticCheck(expr, 0);
        // we dont attempt any more to detect static errors

        type = seqType;
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue v = expr.eval(focus, context);
        v = v.checkTypeExpand(seqType, context, false, true);
        if (v instanceof ErrorValue)
            context.error("XPDY0050", this, ((ErrorValue) v).getReason());
        return v;
    }
}
