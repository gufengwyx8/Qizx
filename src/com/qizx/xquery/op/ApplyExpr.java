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
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.ArraySequence;

/**
 * Sequence of expressions followed by ';'
 */
public class ApplyExpr extends Expression
{
    Expression[] exprs = new Expression[0];
    boolean updating;
    
    public ApplyExpr()
    {
    }

    public void addExpr(Expression expr)
    {
        exprs = addExpr(exprs, expr);
    }

    Expression getExpr(int rank)
    {
        return rank < 0 || rank >= exprs.length ? null : exprs[rank];
    }

    public Expression child(int rank)
    {
        return (rank < exprs.length) ? exprs[rank] : null;
    }

    public int getFlags()
    {
        return updating ? UPDATING : 0;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.children("exprs", exprs);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        int nupCount = 0;
        type = exprs.length == 0 ? XQType.NONE.opt : null;
        for (int e = 0, E = exprs.length; e < E; e++) {
            Expression subx = exprs[e] = context.simpleStaticCheck(exprs[e], 0);
            if (type == null) {
                type = subx.getType();
            }
            else {
                // System.err.println("union "+type+" avec
                // "+exprs[e].getType());
                type = type.unionWith(subx.getType(), false);
                // System.err.println("donne "+type);
            }
            if (type == null)
                context.error("(BUG)", subx, "no union type found");
            
            if(UpdatingExpr.isUpdating(subx))
                updating = true;
            else if(!UpdatingExpr.isVacuous(subx))
                ++ nupCount;
        }
        return this;
    }

    public boolean isVacuous()
    {
        if(exprs == null || exprs.length == 0)
            return true;
        for (int c = 0, C = exprs.length; c < C; c++) {
            if(!UpdatingExpr.isVacuous(exprs[c]))
                return false;
        }
        return true;
    }
    
    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue last = null;    // at least one subexpr anyway
        for (int c = 0, C = exprs.length; c < C; c++) {
            last = exprs[c].eval(focus, context);
            // actually expand the sequence here
            ArraySequence expanded = new ArraySequence(4, last);
            for (; last.next();)
                expanded.addItem(last.getItem());
            last = expanded;
            // TODO apply updates
        }
        return last;
    }
}
