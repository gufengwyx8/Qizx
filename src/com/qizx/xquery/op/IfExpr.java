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
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQValue;

public class IfExpr extends Expression
{
    public Expression cond;
    public Expression exprThen, exprElse;

    public IfExpr(Expression cond, Expression exprThen, Expression exprElse)
    {
        this.cond = cond;
        this.exprThen = exprThen;
        this.exprElse = exprElse;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("cond", cond);
        d.child("then", exprThen);
        d.child("else", exprElse);
    }

    public Expression child(int rank)
    {
        switch (rank) {
        case 0:
            return cond;
        case 1:
            return exprThen;
        case 2:
            return exprElse;
        }
        return null;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        cond = context.staticCheck(cond, 0);
        exprThen = context.simpleStaticCheck(exprThen, 0);
        exprElse = context.simpleStaticCheck(exprElse, 0);
        
        type = exprThen.getType().unionWith(exprElse.getType(), true);
        
        if(exprThen.isUpdating() != exprElse.isUpdating())
            if(!(UpdatingExpr.isVacuous(exprThen) ||
                 UpdatingExpr.isVacuous(exprElse)))
                context.error("XUST0001", this,
                          "mix of updating and non-updating expressions in if");            
        return this;
    }

    public boolean isVacuous()
    {
        return UpdatingExpr.isVacuous(exprThen) 
               && UpdatingExpr.isVacuous(exprElse);
    }

    public int getFlags()
    {
        return (UpdatingExpr.isUpdating(exprThen) ||
               UpdatingExpr.isUpdating(exprElse)) ? UPDATING : 0;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        if (cond.evalEffectiveBooleanValue(focus, context)) {
            return exprThen.eval(focus, context);
        }
        else {
            // System.err.println("else");
            return exprElse.eval(focus, context);
        }
    }

    public long evalAsInteger(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        if (cond.evalEffectiveBooleanValue(focus, context)) {
            return exprThen.evalAsInteger(focus, context);
        }
        else {
            return exprElse.evalAsInteger(focus, context);
        }
    }

    public long evalAsOptInteger(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        if (cond.evalEffectiveBooleanValue(focus, context)) {
            return exprThen.evalAsOptInteger(focus, context);
        }
        else {
            return exprElse.evalAsOptInteger(focus, context);
        }
    }

    public double evalAsDouble(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        if (cond.evalEffectiveBooleanValue(focus, context)) {
            return exprThen.evalAsDouble(focus, context);
        }
        else {
            return exprElse.evalAsDouble(focus, context);
        }
    }

    public double evalAsOptDouble(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        if (cond.evalEffectiveBooleanValue(focus, context)) {
            return exprThen.evalAsOptDouble(focus, context);
        }
        else {
            return exprElse.evalAsOptDouble(focus, context);
        }
    }

    public String evalAsString(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        if (cond.evalEffectiveBooleanValue(focus, context)) {
            return exprThen.evalAsString(focus, context);
        }
        else {
            return exprElse.evalAsString(focus, context);
        }
    }

    public XQItem evalAsItem(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        if (cond.evalEffectiveBooleanValue(focus, context)) {
            return exprThen.evalAsItem(focus, context);
        }
        else {
            return exprElse.evalAsItem(focus, context);
        }
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        if (cond.evalEffectiveBooleanValue(focus, context))
            exprThen.evalAsEvents(output, focus, context);
        else
            exprElse.evalAsEvents(output, focus, context);
    }
}
