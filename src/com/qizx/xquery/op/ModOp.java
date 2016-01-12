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
 * Implementation of operator mod.
 */
public class ModOp extends NumericOp
{
    public ModOp(Expression expr1, Expression expr2)
    {
        super(expr1, expr2);
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("expr1", operands[0]);
        d.child("expr2", operands[1]);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        switch (combinedArgTypes(context)) {
        case XQType.INT_INT:
        case XQType.INT_DOUBLE:
        case XQType.DOUBLE_INT:
        case XQType.DOUBLE_DEC:
        case XQType.DEC_DOUBLE:
        case XQType.FLOAT_DOUBLE:
        case XQType.DOUBLE_FLOAT:
        case XQType.DOUBLE_DOUBLE:
        default:
            return transfer(new ExecAny(), operands);
        }
    }

    public static class ExecAny extends Function.NumericCall
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQItem op1 = args[0].evalAsOptItem(focus, context);
            XQItem op2 = args[1].evalAsOptItem(focus, context);
            context.at(this);
            if (op1 == null || op2 == null)
                return XQValue.empty;

            switch (combinedArgTypes(op1, op2)) {
            case XQType.INT_INT:
                long i1 = op1.getInteger(),
                i2 = op2.getInteger();
                if (i2 == 0)
                    context.error("FOAR0001", this, "division by zero");
                return new SingleInteger(i1 % i2);

            case XQType.INT_DEC:
            case XQType.DEC_INT:
            case XQType.DEC_DEC:
                BigDecimal de1 = op1.getDecimal(),
                de2 = op2.getDecimal();
                BigDecimal q = de1.divide(de2, 0, BigDecimal.ROUND_DOWN);
                return new SingleDecimal(de1.subtract(q.multiply(de2)));

            case XQType.INT_FLOAT:
            case XQType.FLOAT_INT:
            case XQType.FLOAT_DEC:
            case XQType.DEC_FLOAT:
            case XQType.FLOAT_FLOAT:
                return new SingleFloat(op1.getFloat() % op2.getFloat());

            case XQType.INT_DOUBLE:
            case XQType.DOUBLE_INT:
            case XQType.DOUBLE_DEC:
            case XQType.DEC_DOUBLE:
            case XQType.FLOAT_DOUBLE:
            case XQType.DOUBLE_FLOAT:
            case XQType.DOUBLE_DOUBLE:
                return new SingleDouble(op1.getDouble() % op2.getDouble());

            default:
                context.error(ERRC_BADTYPE, this,
                              "invalid types for operator mod");
            }
            return null; // dummy
        }
    }
}
