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
import com.qizx.xquery.dt.SingleItem;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

import java.math.BigDecimal;

/**
 * Implementation of operator div.
 */
public class DivOp extends NumericOp
{
    /**
     * Precision of decimal division.
     */
    final static int PRECISION = 18;

    public DivOp(Expression expr1, Expression expr2)
    {
        super(expr1, expr2);
    }

    public Prototype[] getProtos()
    {
        return null;
    } // ZAP

    public Expression staticCheck(ModuleContext context, int flags)
    {
        switch (combinedArgTypes(context)) { // no real control or
                                                // optimization
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
            context.at(this);
            XQItem op1 = args[0].evalAsOptItem(focus, context);
            XQItem op2 = args[1].evalAsOptItem(focus, context);
            if (op1 == null || op2 == null)
                return XQValue.empty;

            switch (combinedArgTypes(op1, op2)) {
            case XQType.INT_INT:
                long i1 = op1.getInteger(),
                i2 = op2.getInteger();
                if (i2 == 0)
                    context.error("FODT0002", args[1], "divide by 0");
                BigDecimal big1 = BigDecimal.valueOf(i1);
                BigDecimal big2 = BigDecimal.valueOf(i2);
                BigDecimal div = Conversion.divide(big1, big2);
                return new SingleDecimal(div);

            case XQType.INT_DEC:
            case XQType.DEC_INT:
            case XQType.DEC_DEC: {

                BigDecimal dec2 = op2.getDecimal();
                try {
                    BigDecimal decRes =
                        Conversion.divide(op1.getDecimal(), dec2);
                    return new SingleDecimal(decRes);
                }
                catch (ArithmeticException e) {
                    context.error("FOAR0002", args[1], e.getMessage());
                }
            }

            case XQType.INT_FLOAT:
            case XQType.FLOAT_INT:
            case XQType.FLOAT_DEC:
            case XQType.DEC_FLOAT:
            case XQType.FLOAT_FLOAT:
                return new SingleFloat(op1.getFloat() / op2.getFloat());

            case XQType.INT_DOUBLE:
            case XQType.DOUBLE_INT:
            case XQType.DOUBLE_DEC:
            case XQType.DEC_DOUBLE:
            case XQType.FLOAT_DOUBLE:
            case XQType.DOUBLE_FLOAT:
            case XQType.DOUBLE_DOUBLE:
                return new SingleDouble(op1.getDouble() / op2.getDouble());

            case XQType.YMDUR_YMDUR: {
                double d1 = op1.getDuration().getTotalMonths();
                double d2 = op2.getDuration().getTotalMonths();
                if (d2 == 0)
                    context.error("FODT0002", args[1], "divide by 0");
                return new SingleDecimal(Conversion.toDecimal(d1 / d2));
            }
            case XQType.YMDUR_DOUBLE:
            case XQType.YMDUR_FLOAT:
            case XQType.YMDUR_DEC:
            case XQType.YMDUR_INT:
                try {
                    double f = op2.getDouble();
                    if (f == 0)
                        context.error("FODT0002", args[1], "divide by 0");
                    if (Double.isNaN(f))
                        context.error("FOCA0005", this, "invalid argument");
                    Duration d = op1.getDuration();
                    return SingleDuration.newYM(Duration.newYearMonth(
                                     (int) Math.round(d.getTotalMonths() / f)));
                }
                catch (DateTimeException e) {
                    context.error("OOPS", this, "OOPS " + e); // cannot
                                                                // happen
                }

            case XQType.DTDUR_DTDUR: {
                double d1 = op1.getDuration().getTotalSeconds();
                double d2 = op2.getDuration().getTotalSeconds();
                if (d2 == 0)
                    context.error("FOAR0001", args[1], "divide by 0");
                return new SingleDecimal(Conversion.toDecimal(d1 / d2));
            }
            case XQType.DTDUR_DOUBLE:
            case XQType.DTDUR_FLOAT:
            case XQType.DTDUR_DEC:
            case XQType.DTDUR_INT:
                try {
                    double f = op2.getDouble();
                    if (f == 0)
                        context.error("FODT0002", args[1], "divide by 0");
                    if (Double.isNaN(f))
                        context.error("FOCA0005", this, "invalid argument");
                    Duration d = op1.getDuration();
                    return SingleDuration.newDT(Duration.newDayTime(d
                        .getTotalSeconds()
                                                                    / f));
                }
                catch (DateTimeException e) {
                    context.error("OOPS", this, "OOPS " + e); // cannot
                                                                // happen
                }

            default:
                context.error(ERRC_BADTYPE, this,
                              "invalid types for operator div");
            }
            return new SingleItem(op1);
        }
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child(operands[0]);
        d.child("expr2", operands[1]);
    }

}
