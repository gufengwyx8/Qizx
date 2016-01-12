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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleDecimal;
import com.qizx.xquery.dt.SingleDouble;
import com.qizx.xquery.dt.SingleFloat;
import com.qizx.xquery.dt.SingleInteger;

/**
 * Implementation of function fn:abs.
 */
public class Abs extends Function
{
    static Prototype[] protos = {
            Prototype.fn("abs", XQType.NUMERIC.opt, Exec.class)
                .arg("arg", XQType.NUMERIC.opt)
        };

    public Prototype[] getProtos()
    {
        return protos;
    }


    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQItem arg = args[0].evalAsOptItem(focus, context);
            if (arg == null)
                return XQValue.empty;
            context.at(this);
            switch (arg.getItemType().quickCode()) {
            case XQType.QT_DOUBLE:
            case XQType.QT_UNTYPED:
                return new SingleDouble(Math.abs(arg.getDouble()));
            case XQType.QT_FLOAT:
                return new SingleFloat((float) Math.abs(arg.getFloat()));
            case XQType.QT_DEC:
                return new SingleDecimal(arg.getDecimal().abs());
            case XQType.QT_INT:
                return new SingleInteger(Math.abs(arg.getInteger()));
            default:
                context.badTypeForArg(arg.getItemType(), args[0], -1, "numeric");
                return null;
            }
        }
    }
}
