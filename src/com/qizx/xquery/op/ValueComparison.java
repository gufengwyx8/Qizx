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

/**
 * Generic part of all value comparisons. Each particular operator (eq gt ...)
 * has only a simple decision maker.
 */
public abstract class ValueComparison extends Comparison
{
    public ValueComparison(Expression expr1, Expression expr2)
    {
        super(expr1, expr2);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        operands[0] = context.staticCheck(operands[0], 0);
        operands[1] = context.staticCheck(operands[1], 0);
        XQItemType t1 = operands[0].getType().itemType();
        XQItemType t2 = operands[1].getType().itemType();
        // optimizations are for benchmarks...?
        Comparison.Exec rt = null;
        switch (combinedTypes(t1, t2)) {
        case XQType.INT_INT:
            rt = new ExecInt();
            break;
        case XQType.DOUBLE_DOUBLE:
        case XQType.DOUBLE_INT:
        case XQType.INT_DOUBLE:
            rt = new ExecDouble();
            break;
        default:
            rt = new ExecAny();
            break;
        }
        rt.test = getTest();
        return transfer(rt, operands);
    }

    public static class ExecInt extends Comparison.Exec
    {
        public void dump(ExprDisplay d)
        {
            d.header(this);
            d.property("test", test.getName());
            d.child("left", args[0]);
            d.child("right", args[1]);
        }

        public boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            long e1 = args[0].evalAsOptInteger(focus, context);
            long e2 = args[1].evalAsOptInteger(focus, context);
            context.at(this);
            return test.make(BaseValue.compare(e1 - e2));
        }
    }

    public static class ExecDouble extends Comparison.Exec
    {
        public void dump(ExprDisplay d)
        {
            d.header(this);
            d.property("test", test.getName());
            d.child("left", args[0]);
            d.child("right", args[1]);
        }

        public boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            double d1 = args[0].evalAsOptDouble(focus, context);
            double d2 = args[1].evalAsOptDouble(focus, context);
            context.at(this);
            if (d1 != d1 || d2 != d2) // Test NaN
                return test == ValueNeOp.TEST;
            return test.make(BaseValue.compare(d1, d2));
        }
    }

    public static class ExecAny extends Comparison.Exec
    {
        public void dump(ExprDisplay d)
        {
            d.header(this);
            d.property("test", test.getName());
            d.child("left", args[0]);
            d.child("right", args[1]);
        }

        public boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQItem op1 = args[0].evalAsOptItem(focus, context);
            XQItem op2 = args[1].evalAsOptItem(focus, context);
            if (op1 == null || op2 == null)
                throw EmptyException.instance();
            context.at(this);
            int flags = XQItem.COMPAR_VALUE;
            if (test != ValueEqOp.TEST && test != ValueNeOp.TEST)
                flags += XQItem.COMPAR_ORDER;

            int cmp = op1.compareTo(op2, context, flags);
            if (Math.abs(cmp) == com.qizx.util.basic.Comparison.ERROR)
                context.error("XPTY0004", this, "values are not comparable");
            return test.make(cmp);
        }
    }
}
