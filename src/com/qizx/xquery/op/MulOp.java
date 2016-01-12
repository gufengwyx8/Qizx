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
import com.qizx.xquery.dt.SingleItem;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

/**
 * Implementation of operator '*'.
 */
public class MulOp extends NumericOp
{
    public MulOp(Expression expr1, Expression expr2)
    {
        super(expr1, expr2);
    }

    public Prototype[] getProtos()
    {
        return null;
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
            return this.transfer(new ExecInt(), operands);
        case XQType.INT_DOUBLE:
        case XQType.DOUBLE_INT:
        case XQType.DOUBLE_DEC:
        case XQType.DEC_DOUBLE:
        case XQType.FLOAT_DOUBLE:
        case XQType.DOUBLE_FLOAT:
        case XQType.DOUBLE_DOUBLE:
            return transfer(new ExecDouble(), operands);
        default:
            return transfer(new ExecAny(), operands);
        }
    }

    public static class ExecInt extends Function.OptIntegerCall
    {

        public long evalAsOptInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            long e1 = args[0].evalAsOptInteger(focus, context);
            long e2 = args[1].evalAsOptInteger(focus, context);
            context.at(this);
            if (!Conversion.isIntegerRange(e1 * (double) e2))
                context.error("FOAR0002", this, "integer overflow");
            return e1 * e2;
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
            // System.out.println("add "+e1+" to "+e2);
            return e1 * e2;
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
                if (!Conversion.isIntegerRange(i1 * (double) i2))
                    context.error("FOAR0002", this, "integer overflow");
                return new SingleInteger(i1 * i2);

            case XQType.INT_DEC:
            case XQType.DEC_INT:
            case XQType.DEC_DEC:
                return new SingleDecimal(op1.getDecimal()
                    .multiply(op2.getDecimal()));

            case XQType.INT_FLOAT:
            case XQType.FLOAT_INT:
            case XQType.FLOAT_DEC:
            case XQType.DEC_FLOAT:
            case XQType.FLOAT_FLOAT:
                return new SingleFloat(op1.getFloat() * op2.getFloat());

            case XQType.INT_DOUBLE:
            case XQType.DOUBLE_INT:
            case XQType.DOUBLE_DEC:
            case XQType.DEC_DOUBLE:
            case XQType.FLOAT_DOUBLE:
            case XQType.DOUBLE_FLOAT:
            case XQType.DOUBLE_DOUBLE:
                return new SingleDouble(op1.getDouble() * op2.getDouble());

            case XQType.YMDUR_DOUBLE:
            case XQType.YMDUR_FLOAT:
            case XQType.YMDUR_DEC:
            case XQType.YMDUR_INT:
                return mulYM(op1.getDuration(), op2.getDouble(), context);

            case XQType.DOUBLE_YMDUR:
            case XQType.FLOAT_YMDUR:
            case XQType.DEC_YMDUR:
            case XQType.INT_YMDUR:
                return mulYM(op2.getDuration(), op1.getDouble(), context);

            case XQType.DTDUR_DOUBLE:
            case XQType.DTDUR_FLOAT:
            case XQType.DTDUR_DEC:
            case XQType.DTDUR_INT:
                return mulDT(op1.getDuration(), op2.getDouble(), context);

            case XQType.DOUBLE_DTDUR:
            case XQType.FLOAT_DTDUR:
            case XQType.DEC_DTDUR:
            case XQType.INT_DTDUR:
                return mulDT(op2.getDuration(), op1.getDouble(), context);

            default:
                context.error(ERRC_BADTYPE, this,
                              "invalid types for operator *");
            }
            return new SingleItem(op1);
        }

        private XQValue mulDT(Duration duration, double sc, EvalContext context)
            throws EvaluationException
        {
            try {
                if (Double.isNaN(sc))
                    context.error("FOCA0005", this, "invalid argument");
                if (Double.isInfinite(sc))
                    context.error("FODT0002", this, "invalid argument");
                return SingleDuration.newDT(Duration.newDayTime(duration
                    .getTotalSeconds()
                                                                * sc));
            }
            catch (DateTimeException e) {
                context.error(ERRC_BADTYPE, this, "OOPS " + e);
                return null;
            }

        }

        private XQValue mulYM(Duration duration, double sc, EvalContext context)
            throws EvaluationException
        {
            try {
                if (Double.isNaN(sc))
                    context.error("FOCA0005", this, "invalid argument");
                if (Double.isInfinite(sc))
                    context.error("FODT0002", this, "invalid argument");
                return SingleDuration.newYM(Duration.newYearMonth((int) Math
                    .round(duration.getTotalMonths() * sc)));
            }
            catch (DateTimeException e) {
                context.error(ERRC_BADTYPE, this, "OOPS " + e);
                return null;
            }

        }
    }
}
