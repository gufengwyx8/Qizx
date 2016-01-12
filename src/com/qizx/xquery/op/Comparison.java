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

import com.qizx.api.QizxException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.fn.Function;

/**
 * Superclass of all comparison operators.
 */
public abstract class Comparison extends Expression
{
    public Expression[] operands;

    public Comparison(Expression expr1, Expression expr2)
    {
        operands = new Expression[] { expr1, expr2 };
    }

    public interface Test
    {
        // diff is -1 if lt, 0 if eq, 1 if gt, or COMPARE_FAILS or INCOMPARABLE
        boolean make(int diff);

        Test reverse();

        String getName();
    }

    protected abstract Test getTest();

    public Expression child(int rank)
    {
        return rank < 2 ? operands[rank] : null;
    }

    public abstract static class Exec extends Function.BoolCall
    {
        public Test test;

        public boolean isEq()
        {
            return test == ValueEqOp.TEST;
        }
    }

    public boolean evalAsEffectiveBoolean(Focus focus, EvalContext context)
        throws QizxException
    {
        return evalAsBoolean(focus, context);
    }
}
