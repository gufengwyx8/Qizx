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
import com.qizx.xdm.Conversion;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleDecimal;
import com.qizx.xquery.dt.SingleDouble;
import com.qizx.xquery.dt.SingleDuration;
import com.qizx.xquery.dt.SingleFloat;

import java.math.BigDecimal;

/**
 *  Implementation of function fn:avg.
 */
public class Avg extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("avg", XQType.ATOM.opt, Exec.class)
            .arg("seq", XQType.ANY)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call {
        
        public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
        {
            context.at(this);
            XQValue v = args[0].eval(focus, context);
            
            if(!v.next())
                return XQValue.empty;
            XQItem first = v.getItem();
            
            int cnt = 1;
            double doubleSum = 0;
            float floatSum = 0;
            BigDecimal decSum = null;
            long intSum = 0;
            
            int currentType = first.getItemType().quickCode(), nType = 0;
            switch(currentType) {
            case XQType.QT_DOUBLE:
            case XQType.QT_UNTYPED:
                doubleSum = first.getDouble();
                currentType = XQType.QT_DOUBLE;
                break;
            case XQType.QT_FLOAT:
                floatSum = first.getFloat();
                break;
            case XQType.QT_DEC:
                decSum = first.getDecimal();
                break;
            case XQType.QT_INT:
                intSum = first.getInteger();
                break;
            case XQType.QT_YMDUR:
                doubleSum = first.getDuration().getTotalMonths();
                break;
            case XQType.QT_DTDUR:
                doubleSum = first.getDuration().getTotalSeconds();
                break;
            default:
                context.invalidArgType(this, 0, first.getItemType(), null); // 
            }
            
            for( ; ; ++ cnt) {
                switch(currentType) {
                case XQType.QT_DOUBLE:
                    if(!v.next())
                        return new SingleDouble( doubleSum / cnt);
                    nType = v.getItemType().quickCode();
                    if(XQItemType.isNumeric(nType) || nType == XQType.QT_UNTYPED)
                        doubleSum += v.getDouble();
                    else
                        context.incompatibleType(cnt, "double", this);
                    break;
                case XQType.QT_FLOAT:
                    if(!v.next())
                        return new SingleFloat( floatSum / cnt);
                    nType = v.getItemType().quickCode();
                    if(nType == XQType.QT_DOUBLE || nType == XQType.QT_UNTYPED) {
                        currentType = XQType.QT_DOUBLE;
                        doubleSum = floatSum + v.getDouble();
                    }
                    else if(XQItemType.isNumeric(nType))
                        floatSum += v.getFloat();
                    else
                        context.incompatibleType(cnt, "float", this);
                    break;
                case XQType.QT_DEC:
                    if(!v.next())
                      return new SingleDecimal(Conversion.divide(decSum, cnt));
                    nType = v.getItemType().quickCode();
                    switch(nType) {
                    case XQType.QT_UNTYPED:
                    case XQType.QT_DOUBLE:
                        currentType = XQType.QT_DOUBLE;
                        doubleSum = decSum.doubleValue() + v.getDouble();
                        break;
                    case XQType.QT_FLOAT:
                        currentType = nType;
                        floatSum = decSum.floatValue() + v.getFloat();
                        break;
                    case XQType.QT_DEC:
                    case XQType.QT_INT:
                        decSum = decSum.add(v.getDecimal());
                        break;
                    default:
                        context.incompatibleType(cnt, "int", this);
                    }
                    break;
                    
                case XQType.QT_INT:
                    if(!v.next())
                        return new SingleDecimal(
                           Conversion.divide(BigDecimal.valueOf(intSum), cnt));                    
                    nType = v.getItemType().quickCode();
                    switch(nType) {
                    case XQType.QT_UNTYPED:
                    case XQType.QT_DOUBLE:
                        currentType = XQType.QT_DOUBLE;
                        doubleSum = intSum + v.getDouble();
                        break;
                    case XQType.QT_FLOAT:
                        currentType = nType;
                        floatSum = intSum + v.getFloat();
                        break;
                    case XQType.QT_DEC:
                        currentType = nType;
                        decSum = BigDecimal.valueOf(intSum).add(v.getDecimal());
                        break;
                    case XQType.QT_INT:
                        intSum += v.getInteger();
                        break;
                    default:
                        context.incompatibleType(cnt, "int", this);
                    }
                    break;
                    
                case XQType.QT_YMDUR:
                    if(!v.next())
                        return new SingleDuration(
                           Duration.newYearMonth((int) Math.round(doubleSum / cnt)));
                    nType = v.getItemType().quickCode();
                    if(nType == currentType)
                        doubleSum += v.getDuration().getTotalMonths();
                    else
                        context.incompatibleType(cnt, "yearMonthDuration", this);
                    break;
                case XQType.QT_DTDUR:
                    if(!v.next())
                        return new SingleDuration(Duration.newDayTime(doubleSum / cnt));
                    nType = v.getItemType().quickCode();
                    if(nType == currentType)
                        doubleSum += v.getDuration().getTotalSeconds();
                    else
                        context.incompatibleType(cnt, "dayTimeDuration", this);
                    break;

                default:
                    context.invalidArgType(this, cnt, v.getItemType(), null); // 
                }
                // e.setContext(context); throw e;	// necessary for proper backtrace
            }
        }
    }
}
