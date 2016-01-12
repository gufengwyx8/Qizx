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
import com.qizx.api.util.time.Duration;
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.*;
import com.qizx.xquery.impl.BasicCompContext;

import java.math.BigDecimal;
import java.text.Collator;

/**
 *  Implementation of function fn:max.
 */
public class Max extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("max", XQType.ANY_ATOMIC_TYPE.opt, RT.class)
            .arg("op", XQType.ITEM.star), // wider than definition to avoid boring type problems
        Prototype.fn("max", XQType.ANY_ATOMIC_TYPE.opt, RT.class)
            .arg("op", XQType.ITEM.star)  // wider than definition to avoid boring type problems
            .arg("collationLiteral", XQType.STRING)
    };
    public Prototype[] getProtos() { return protos; }
    
//    public Expression staticCheck( StaticContext context, Expression[] arguments,
//                                   Expression subject )
//    {
//        Expression ex = super.staticCheck(context, arguments, subject);
//        RT rt = (RT) ex;
//        // change the result type to the type of the argument (may be empty -> optional)
//        if(rt.args.length > 0) {	// error protection
//            XQItemType argType = rt.args[0].getType().getItemType();
//            if(XQType.NODE.accepts(argType) || argType == XQType.UNTYPED_ATOMIC)
//                argType = XQType.DOUBLE;	// Nov 2003
//            rt.setType( argType.opt );
//        }	
//        return rt;
//    }
    
    public static class RT extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQValue v = args[0].eval(focus, context);
            Collator collator =
                getCollator(args.length < 2 ? null : args[1], focus, context);
            BasicCompContext ctx =
                new BasicCompContext(collator, context.getImplicitTimezone());

            if (!v.next())
                return XQValue.empty;

            XQItem first = v.getItem();
            int cnt = 1;
            double doubleRes = 0;
            float floatRes = 0;
            BigDecimal decRes = null;
            long intRes = 0;
            String stringRes = null;
            XQItem itemRes = null;

            int currentType = first.getItemType().quickCode(), nType = 0;
            switch (currentType) {
            case XQType.QT_DOUBLE:
            case XQType.QT_UNTYPED:
                doubleRes = first.getDouble();
                if (Double.isNaN(doubleRes))
                    return new SingleDouble(doubleRes);
                currentType = XQType.QT_DOUBLE;
                break;
            case XQType.QT_FLOAT:
                floatRes = first.getFloat();
                // if(Float.isNaN(floatRes))
                // return new SingleFloat(floatRes);
                break;
            case XQType.QT_DEC:
                decRes = first.getDecimal();
                break;
            case XQType.QT_INT:
                intRes = first.getInteger();
                break;
            case XQType.QT_YMDUR:
                intRes = first.getDuration().getTotalMonths();
                break;
            case XQType.QT_DTDUR:
                doubleRes = first.getDuration().getTotalSeconds();
                break;
            case XQType.QT_STRING:
            case XQType.QT_ANYURI:
                stringRes = first.getString();
                break;
            default:
                itemRes = first;
                if (!(itemRes instanceof MomentValue))
                    context.invalidArgType(this, 0, first.getItemType(), null);
                break;
            }

            for (;; ++cnt) {
                switch (currentType) {
                case XQType.QT_DOUBLE:
                    if (!v.next())
                        return new SingleDouble(doubleRes);
                    nType = v.getItemType().quickCode();
                    if (XQItemType.isNumeric(nType)
                        || nType == XQType.QT_UNTYPED) {
                        doubleRes = doubleResult(v, doubleRes);
                    }
                    else
                        context.incompatibleType(cnt, "double", this);
                    break;
                case XQType.QT_FLOAT:
                    if (!v.next())
                        return new SingleFloat(floatRes);
                    nType = v.getItemType().quickCode();
                    if (nType == XQType.QT_DOUBLE
                        || nType == XQType.QT_UNTYPED) {
                        currentType = XQType.QT_DOUBLE;
                        doubleRes = doubleResult(v, floatRes);
                    }
                    else if (XQItemType.isNumeric(nType)) {
                        floatRes = floatResult(v, floatRes);
                    }
                    else
                        context.incompatibleType(cnt, "float", this);
                    break;
                case XQType.QT_DEC:
                    if (!v.next())
                        return new SingleDecimal(decRes);
                    nType = v.getItemType().quickCode();
                    switch (nType) {
                    case XQType.QT_UNTYPED:
                    case XQType.QT_DOUBLE:
                        currentType = XQType.QT_DOUBLE;
                        doubleRes = doubleResult(v, decRes.doubleValue());
                        break;
                    case XQType.QT_FLOAT:
                        currentType = nType;
                        floatRes = floatResult(v, decRes.floatValue());
                        break;
                    case XQType.QT_DEC:
                    case XQType.QT_INT:
                        decRes = decimalResult(v, decRes);
                        break;
                    default:
                        context.incompatibleType(cnt, "int", this);
                    }
                    break;

                case XQType.QT_INT:
                    if (!v.next())
                        return new SingleInteger(intRes);
                    nType = v.getItemType().quickCode();
                    switch (nType) {
                    case XQType.QT_UNTYPED:
                    case XQType.QT_DOUBLE:
                        currentType = XQType.QT_DOUBLE;
                        doubleRes = doubleResult(v, intRes);
                        break;
                    case XQType.QT_FLOAT:
                        currentType = nType;
                        floatRes = floatResult(v, intRes);
                        break;
                    case XQType.QT_DEC:
                        currentType = nType;
                        decRes = decimalResult(v, BigDecimal.valueOf(intRes));
                        break;
                    case XQType.QT_INT:
                        intRes = Math.max(intRes, v.getInteger());
                        break;
                    default:
                        context.incompatibleType(cnt, "int", this);
                    }
                    break;

                case XQType.QT_YMDUR:
                    if (!v.next())
                        return new SingleDuration(
                                         Duration.newYearMonth((int) intRes));
                    nType = v.getItemType().quickCode();
                    if (nType == currentType)
                        intRes = Math.max(intRes, v.getDuration().getTotalMonths());
                    else
                        context.incompatibleType(cnt, "yearMonthDuration", this);
                    break;

                case XQType.QT_DTDUR:
                    if (!v.next())
                        return new SingleDuration(
                                         Duration.newDayTime(doubleRes / cnt));
                    nType = v.getItemType().quickCode();
                    if (nType == currentType)
                        doubleRes = Math.max(doubleRes,
                                             v.getDuration().getTotalSeconds());
                    else
                        context.incompatibleType(cnt, "dayTimeDuration", this);
                    break;

                case XQType.QT_STRING:
                case XQType.QT_ANYURI:
                    if (!v.next())
                        return new SingleString(stringRes);
                    nType = v.getItemType().quickCode();
                    if (nType != XQType.QT_STRING && nType != XQType.QT_ANYURI)
                        context.incompatibleType(cnt, "string", this);
                    String s2 = v.getString();
                    if (StringValue.compare(s2, stringRes, collator) > 0)
                        stringRes = s2;
                    break;

                default:
                    if (!v.next())
                        return new SingleItem(itemRes);
                    nType = v.getItemType().quickCode();
                    if (nType != currentType)
                        context.incompatibleType(cnt, "current item", this);
                    XQItem item = v.getItem();
                    int cmp = item.compareTo(itemRes, ctx, XQItem.COMPAR_VALUE);
                    if (cmp == Comparison.ERROR)
                        context.incompatibleType(cnt, "current item", this);
                    if (cmp > 0)
                        itemRes = item;
                    break;
                }
                // e.setContext(context); throw e; // necessary for proper
                // backtrace
            }
        }

        protected BigDecimal decimalResult(XQValue v, BigDecimal decRes)
            throws EvaluationException
        {
            BigDecimal value = v.getDecimal();
            return value.compareTo(decRes) > 0 ? value : decRes;
        }

        protected double doubleResult(XQValue v, double doubleRes)
            throws EvaluationException
        {
            double value = v.getDouble();
            if (Double.isNaN(value))
                return value;
            return (value > doubleRes) ? value : doubleRes;
        }

        protected float floatResult(XQValue v, float floatRes)
            throws EvaluationException
        {
            float value = v.getFloat();
            if (Float.isNaN(value))
                return value;
            return (value > floatRes) ? value : floatRes;
        }
    }
}
