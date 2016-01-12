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
import com.qizx.xquery.dt.IntegerValue;
import com.qizx.xquery.dt.SingleInteger;
import com.qizx.xquery.impl.EmptyException;

/**
 * integer range x to y.
 */
public class RangeExpr extends Expression
{
    public Expression lower;
    public Expression upper;

    public RangeExpr(Expression expr1, Expression expr2)
    {
        this.lower = expr1;
        this.upper = expr2;
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? lower : (rank == 1) ? upper : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("lower", lower);
        d.child("upper", upper);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        lower = context.staticCheck(lower, 0);
        upper = context.staticCheck(upper, 0);
        type = XQType.INTEGER.star;
        // type = context.check(proto, new Expression[] { expr1, expr2 });
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        try {
            return new Sequence(lower.evalAsOptInteger(focus, context),
                                upper.evalAsOptInteger(focus, context));
        }
        catch (EmptyException e) {
            return XQValue.empty;
        }
    }

    /**
     * Evaluate as a pair of int, not as a sequence
     */
    public int[] evaluate(Focus focus, EvalContext context)
        throws EvaluationException
    {
        if(lower != null && !XQType.INTEGER.accepts(lower.getType()))
            context.invalidArgType(this, 0, lower.getType(), "integer");
        if(upper != null && !XQType.INTEGER.accepts(upper.getType()))
            context.invalidArgType(this, 0, upper.getType(), "integer");
        
        long lo = (lower == null)? -1 : lower.evalAsInteger(focus, context);
        // for 'exactly' range in FT, the two expressions are identical:
        long up = (upper == lower)? lo
                  : (upper == null) ? Long.MAX_VALUE
                  : upper.evalAsInteger(focus, context);
        return new int[] { (int) lo, (int) up };
    }

    static class Sequence extends IntegerValue
    {
        long start, end;

        long current;

        Sequence(long start, long end)
        {
            // System.out.println(" range "+start+" : "+end);
            this.start = start;
            this.end = end;
            if (start <= end)
                current = start - 1;
            else
                current = start + 1;
        }

        public boolean next()
        {
            // NOV 2003
            // if(start <= end)
            return (++current <= end);
            // else return (-- current >= end);
        }

        public long quickCount(EvalContext context)
            throws EvaluationException
        {
            return Math.max(0, end - start + 1);
        }

        public boolean hasQuickIndex()
        {
            return true;
        }

        public XQItem quickIndex(long index)
        {
            if (index < 1 || index > end - start + 1)
                return null;
            return new SingleInteger(current = start + index - 1);
        }

        public XQValue bornAgain()
        {
            return new Sequence(start, end);
        }

        public boolean worthExpanding()
        {
            return false;
        }

        public long getInteger()
        {
            return current;
        }

        public long getValue()
        {
            return current;
        }
    }
}
