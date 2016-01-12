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
import com.qizx.api.util.time.DateTimeException;
import com.qizx.api.util.time.Duration;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.SingleDecimal;
import com.qizx.xquery.dt.SingleDouble;
import com.qizx.xquery.dt.SingleDuration;
import com.qizx.xquery.dt.SingleFloat;
import com.qizx.xquery.dt.SingleInteger;
import com.qizx.xquery.dt.SingleMoment;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

/**
 * Implementation of operator '+'.
 */
public class PlusOp extends NumericOp
{
    public PlusOp(Expression expr1, Expression expr2)
    {
        super(expr1, expr2);
    }

    public Prototype[] getProtos()
    {
        return null;
    } // ZAP

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("expr1", operands[0]);
        d.child("expr2", operands[1]);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        Expression checked = null;
        switch (combinedArgTypes(context)) {
        case XQType.INT_INT:
            checked = this.transfer(new ExecInt(), operands);
            break;
        case XQType.INT_DOUBLE:
        case XQType.DOUBLE_INT:
        case XQType.DOUBLE_DEC:
        case XQType.DEC_DOUBLE:
        case XQType.FLOAT_DOUBLE:
        case XQType.DOUBLE_FLOAT:
        case XQType.DOUBLE_DOUBLE:
            checked = transfer(new ExecDouble(), operands);
            break;
        default:
            checked = transfer(new ExecAny(), operands);
            break;
        }
        if(operands[0].isConstant() && operands[1].isConstant())
            return context.evalConstantExpr(checked);
        return checked;
    }

    public static class ExecInt extends Function.OptIntegerCall
    {
        public long evalAsOptInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            long e1 = args[0].evalAsOptInteger(focus, context);
            long e2 = args[1].evalAsOptInteger(focus, context);
            context.at(this);
            return e1 + e2;
        }
    }

    public static class ExecDouble extends Function.OptDoubleCall
    {
        public double evalAsOptDouble(Focus focus, EvalContext context)
            throws EvaluationException
        {
            double e1 = args[0].evalAsOptDouble(focus, context);
            double e2 = args[1].evalAsOptDouble(focus, context);
            context.at(this);
            return e1 + e2;
        }
    }

    public static class ExecAny extends Function.NumericCall
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQItem op1 = args[0].evalAsOptItem(focus, context);
            XQItem op2 = args[1].evalAsOptItem(focus, context);
            if (op1 == null || op2 == null)
                return XQValue.empty;
            try {
                switch (combinedArgTypes(op1, op2)) {
                case XQType.INT_INT:
                    long i1 = op1.getInteger(),
                    i2 = op2.getInteger();
                    if (!Conversion.isIntegerRange(i1 + (double) i2))
                        context.error("FOAR0002", this, "integer overflow");
                    return new SingleInteger(i1 + i2);

                case XQType.INT_DEC:
                case XQType.DEC_INT:
                case XQType.DEC_DEC:
                    return new SingleDecimal(op1.getDecimal()
                                             .add(op2.getDecimal()));

                case XQType.INT_FLOAT:
                case XQType.FLOAT_INT:
                case XQType.FLOAT_DEC:
                case XQType.DEC_FLOAT:
                case XQType.FLOAT_FLOAT:
                    return new SingleFloat(op1.getFloat() + op2.getFloat());

                case XQType.INT_DOUBLE:
                case XQType.DOUBLE_INT:
                case XQType.DOUBLE_DEC:
                case XQType.DEC_DOUBLE:
                case XQType.FLOAT_DOUBLE:
                case XQType.DOUBLE_FLOAT:
                case XQType.DOUBLE_DOUBLE:
                    return new SingleDouble(op1.getDouble() + op2.getDouble());

                case XQType.YMDUR_YMDUR:
                    return SingleDuration.newYM(Duration
                        .newYearMonth(op1.getDuration().getTotalMonths()
                                      + op2.getDuration().getTotalMonths()));

                case XQType.DTDUR_DTDUR:
                    return SingleDuration.newDT(Duration
                        .newDayTime(op1.getDuration().getTotalSeconds()
                                    + op2.getDuration().getTotalSeconds()));

                case XQType.DATETIME_YMDUR:
                case XQType.DATETIME_DTDUR:
                    return SingleMoment.dateTime(op1.getMoment()
                        .add(op2.getDuration()));
                case XQType.YMDUR_DATETIME:
                case XQType.DTDUR_DATETIME:
                    return SingleMoment.dateTime(op2.getMoment()
                        .add(op1.getDuration()));
                case XQType.DATETIME_DOUBLE:
                case XQType.DATETIME_INT:
                    return SingleMoment.dateTime(op1.getMoment()
                        .add(op2.getDouble()));
                case XQType.DATE_YMDUR:
                case XQType.DATE_DTDUR:
                    return SingleMoment.date(op1.getMoment()
                        .add(op2.getDuration()));
                case XQType.YMDUR_DATE:
                case XQType.DTDUR_DATE:
                    return SingleMoment.date(op2.getMoment()
                        .add(op1.getDuration()));
                case XQType.DATE_INT:
                    return SingleMoment.date(op1.getMoment()
                        .add(86400 * op2.getInteger()));
                case XQType.TIME_DTDUR:
                    return SingleMoment.time(op1.getMoment()
                        .add(op2.getDuration()));
                case XQType.DTDUR_TIME:
                    return SingleMoment.time(op2.getMoment()
                        .add(op1.getDuration()));
                default:
                    context.error(ERRC_BADTYPE, this,
                                  "invalid types for operator +");
                }
            }
            catch (DateTimeException e) {
                context.error(ERRC_BADTYPE, this, "should not happen: " + e);
            }
            return null;
        }
    }
}
