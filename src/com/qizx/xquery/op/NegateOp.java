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
import com.qizx.xquery.dt.SingleDecimal;
import com.qizx.xquery.dt.SingleDouble;
import com.qizx.xquery.dt.SingleFloat;
import com.qizx.xquery.dt.SingleInteger;
import com.qizx.xquery.fn.Function;

import java.math.BigDecimal;

/**
 * Implementation of unary operator '-'.
 */
public class NegateOp extends Expression
{
    public Expression[] operands;

    public NegateOp(Expression expr)
    {
        operands = new Expression[] { expr };
    }

    public Expression child(int rank)
    {
        return (rank < operands.length) ? operands[rank] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("expr", operands[0]);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        operands[0] = context.staticCheck(operands[0], 0);
        setType(operands[0].getType());
        return transfer(new ExecAny(), operands);
    }

    public static class ExecAny extends Function.NumericCall
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQItem op1 = args[0].evalAsOptItem(focus, context);
            context.at(this);
            if (op1 == null)
                return XQValue.empty;

            switch (op1.getItemType().quickCode()) {
            case XQType.QT_INT:
                long i1 = op1.getInteger();
                return new SingleInteger(-i1);

            case XQType.QT_DEC:
                BigDecimal de1 = op1.getDecimal();
                return new SingleDecimal(de1.negate());

            case XQType.QT_FLOAT:
                return new SingleFloat(-op1.getFloat());

            case XQType.QT_DOUBLE:
            case XQType.QT_UNTYPED:
                return new SingleDouble(-op1.getDouble());

            default:
                context.error(ERRC_BADTYPE, this,
                              "invalid type for unary operator -");
            }
            return null; // dummy
        }
    }
}
