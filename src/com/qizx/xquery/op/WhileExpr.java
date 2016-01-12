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
import com.qizx.xquery.XQValue;

public class WhileExpr extends Expression
{
    public Expression cond;
    public Expression block;

    public WhileExpr(Expression cond, Expression block)
    {
        this.cond = cond;
        this.block = block;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child(cond);
        d.child(block);
    }

    public Expression child(int rank)
    {
        switch (rank) {
        case 0:
            return cond;
        case 1:
            return block;
        }
        return null;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        cond = context.staticCheck(cond, 0);
        block = context.simpleStaticCheck(block, 0);
        // TODO
        return this;
    }

    public boolean isVacuous()
    {
        return UpdatingExpr.isVacuous(block);
    }

    public int getFlags()
    {
        return UpdatingExpr.isUpdating(block) ? UPDATING : 0;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        while (cond.evalEffectiveBooleanValue(focus, context)) {
            block.eval(focus, context);
        }
        return XQValue.empty;
    }
}
