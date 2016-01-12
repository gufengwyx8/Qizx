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
 * Comparison on sequences.
 */
public abstract class GeneralComparison extends Comparison
{
    public GeneralComparison(Expression expr1, Expression expr2)
    {
        super(expr1, expr2);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        operands[0] = context.staticCheck(operands[0], 0);
        operands[1] = context.staticCheck(operands[1], 0);
        return transfer(new ExecAny(getTest()), operands);
    }

    public static class ExecAny extends Comparison.Exec
    {
        ExecAny(Test test)
        {
            this.test = test;
        }

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
            XQValue v1 = args[0].eval(focus, context);
            XQValue v2 = null; // lazy
            int flags = (test != ValueEqOp.TEST && test != ValueNeOp.TEST)
                      ? XQItem.COMPAR_ORDER : 0;

            for (; v1.next();) {
                if (v2 == null) {
                    v2 = args[1].eval(focus, context);
                    context.at(this);                   
                }
                else
                    v2 = v2.bornAgain();
                for (; v2.next();) {
                    try {
                        int cmp = v1.compareTo(v2.getItem(), context, flags);
                        if (Math.abs(cmp) == com.qizx.util.basic.Comparison.ERROR)
                            context.error(ERRC_BADTYPE, this,
                                          "values are not comparable ("
                                          + v1.getType() + " and "
                                          + v2.getType() + ")");
                        if (test.make(cmp))
                            return true;
                    }
                    catch (XQTypeException e) {
                        // semantics: ??
                        context.error(e.getErrorCode(), this, e.getMessage());
                    }
                }
            }
            // amazingly   () != item  returns false
            return false;
        }
    }
}
