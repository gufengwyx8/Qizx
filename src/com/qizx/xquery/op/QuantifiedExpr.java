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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;

/**
 * class QuantifiedExpr:
 */
public class QuantifiedExpr extends BooleanExpression
{

    public boolean every;

    public Expression cond;

    protected Expression[] varClauses = new Expression[0];

    public QuantifiedExpr(boolean every)
    {
        this.every = every;
    }

    public void addVarClause(Expression varClause)
    {
        varClauses = addExpr(varClauses, varClause);
    }

    Expression getVarClause(int rank)
    {
        return rank < 0 || rank >= varClauses.length ? null : varClauses[rank];
    }

    public Expression child(int rank)
    {
        return (rank < varClauses.length)
            ? varClauses[rank] : rank == varClauses.length ? cond : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("every", "" + every);
        d.children("varClauses", varClauses);
        d.child("cond", cond);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        LocalVariable mark = context.latestLocalVariable();
        for (int c = 0; c < varClauses.length; c++)
            context.staticCheck(getVarClause(c), 0);
        cond = context.staticCheck(cond, 0);
        type = XQType.BOOLEAN; // no check: effective bool value
        context.popLocalVariables(mark);
        return this;
    }

    public boolean evalAsBoolean(Focus focus, EvalContext context)
        throws EvaluationException
    {
        // dummy iterator as init stub:
        VarClause.SingleDummy source = new VarClause.SingleDummy(focus, context);
        for (int c = 0; c < varClauses.length; c++) {
            VarClause.SingleDummy newSrc =
                (VarClause.SingleDummy) getVarClause(c).eval(focus, context);
            newSrc.setSource(source);
            source = newSrc;
        }
        if (every)
            for (;;) {
                if (!source.next())
                    return true;
                if (!cond.evalEffectiveBooleanValue(focus, context))
                    return false;
            }
        else
            for (;;) {
                if (!source.next())
                    return false;
                if (cond.evalEffectiveBooleanValue(focus, context))
                    return true;
            }
    }
}
