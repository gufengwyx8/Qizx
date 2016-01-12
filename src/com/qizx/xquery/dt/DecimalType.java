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
package com.qizx.xquery.dt;

import com.qizx.api.EvaluationException;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.*;
import com.qizx.xquery.op.Expression;

import java.math.BigDecimal;
import java.math.BigInteger;

public class DecimalType extends NumericType
{
    public String getShortName()
    {
        return "decimal";
    }

    public int quickCode()
    {
        return QT_DEC;
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws EvaluationException
    {
        XQItemType type = value.getItemType();
        BigDecimal result = null;
        switch (type.quickCode()) {
        case XQType.QT_STRING:
        case XQType.QT_UNTYPED:
            result = Conversion.toDecimal(value.getString().trim(), false);
            break;
        case XQType.QT_DEC:
            result = value.getDecimal();
            break;
        case XQType.QT_INT:
            result = BigDecimal.valueOf(value.getInteger());
            break;
        case XQType.QT_FLOAT:
        case XQType.QT_DOUBLE:
            double d = value.getDouble();
            if (d != d || Double.isInfinite(d))
                throw new XQTypeException("infinite or NaN value");
            result = new BigDecimal(d);
            break;
        case XQType.QT_BOOL:
            result = BigDecimal.valueOf(value.getBoolean() ? 1 : 0);
            break;
        case XQType.QT_YMDUR:
        case XQType.QT_DTDUR:
            if (!context.sObs()) { // extension
                result = value.getDecimal();
                break;
            }
            // else fall here
        default:
            invalidCast(type);
            return null; // dummy
        }

        if (result.scale() < 1)
            result = result.setScale(1);
        return new SingleDecimal(result);
    }

    public XQValue convertFromObject(Object object)
        throws XQTypeException
    {
        if(object instanceof BigInteger)
            object = new BigDecimal((BigInteger) object);
        else if(object instanceof String)
            object = Conversion.toDecimal((String) object, true);
        return new SingleDecimal((BigDecimal) object);
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        XQItem v = expr.evalAsItem(focus, context);
        BigDecimal value = ((DecimalValue) v).getValue();
        return value;
    }
}
