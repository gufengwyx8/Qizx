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
package com.qizx.xquery.fn;

import com.qizx.api.EvaluationException;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.SingleDecimal;
import com.qizx.xquery.dt.SingleDouble;
import com.qizx.xquery.dt.SingleFloat;
import com.qizx.xquery.dt.SingleItem;
import com.qizx.xquery.op.Expression;

/**
 *  Implementation of function fn:round-half-to-even.
 */
public class RoundHalfToEven extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("round-half-to-even", XQType.NUMERIC.opt, Exec.class)
            .arg("value", XQType.NUMERIC.opt),
        Prototype.fn("round-half-to-even", XQType.NUMERIC.opt, Exec.class)
            .arg("value", XQType.NUMERIC.opt)
            .arg("precision", XQType.INTEGER)
    };
    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQItem arg = args[0].evalAsOptItem(focus, context);
            if (arg == null)
                return XQValue.empty;
            int prec = 0;
            if (args.length == 2)
                prec = (int) args[1].evalAsInteger(focus, context);
            context.at(this);
            switch (arg.getItemType().quickCode()) {
            case XQType.QT_DOUBLE:
            case XQType.QT_UNTYPED:
                context.at(this);
                return new SingleDouble(Conversion.roundHalfToEven(
                                                       arg.getDouble(), prec));
            case XQType.QT_FLOAT:
                return new SingleFloat((float) Conversion.roundHalfToEven(
                                                       arg.getDouble(), prec));
            case XQType.QT_DEC:
                return new SingleDecimal(Conversion.roundHalfToEven(
                                                       arg.getDecimal(), prec));
            case XQType.QT_INT:
                return new SingleItem(arg);
            default:
                context.invalidArgType(args[0], -1, arg.getType(), "numeric");
                return null;
            }
        }
    }

    public Expression staticCheck(ModuleContext ctx, Expression[] args,
                                  Expression loc)
    {
        Exec rt = (Exec) super.staticCheck(getName(), ctx, args, loc);
        if (rt.args.length > 0) {
            // change the result type to the type of the argument
            XQItemType argType = rt.args[0].getType().itemType();
            rt.setType(argType.opt);
        }
        return rt;
    }
}
