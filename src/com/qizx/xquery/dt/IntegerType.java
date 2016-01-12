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

public class IntegerType extends DecimalType
{

    public String getShortName()
    {
        return "integer";
    }

    public int quickCode()
    {
        return QT_INT;
    }

    public long upperBound()
    {
        return Long.MAX_VALUE;
    }

    public long lowerBound()
    {
        return Long.MIN_VALUE;
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws XQTypeException
    {
        long result = 0;
        try {
            XQItemType type = value.getItemType();
            switch (type.quickCode()) {
            case XQType.QT_STRING:
            case XQType.QT_UNTYPED:
                result = Conversion.toInteger(value.getString());
                break;
            case XQType.QT_INT:
                result = value.getInteger();
                break;
            case XQType.QT_DEC:
            case XQType.QT_FLOAT:
            case XQType.QT_DOUBLE: // extension
                double v = value.getDouble();
                if (v != v)
                    throw new XQTypeException(Conversion.ERR_CAST,
                                              "invalid value " + v);
                else if (Double.isInfinite(v) || v < Long.MIN_VALUE
                         || v > Long.MAX_VALUE)
                    throw new XQTypeException("FOCA0002", "invalid value " + v);
                result = (long) v;
                break;
            case XQType.QT_BOOL:
                result = value.getBoolean() ? 1 : 0;
                break;
            case XQType.QT_YMDUR:
            case XQType.QT_DTDUR:
                if (!context.sObs()) {
                    result = value.getInteger();
                    break;
                }
                // else fall here
            default:
                throw new XQTypeException("invalid type " + type);
            }
        }
        catch (EvaluationException e) {
            throw new XQTypeException(e.getErrorCode(),
                                      "cannot cast to xs:" + getShortName()
                                      + " : " + e.getMessage());
        }
        if (result < lowerBound() || result > upperBound())
            throw new XQTypeException(Conversion.ERR_CAST, 
                                      "invalid value " + result);
        return new SingleInteger(result, this);
    }

    public XQValue convertFromObject(Object object) throws XQTypeException
    {
        long value = 0;
        if (object instanceof String)
            value = Conversion.toInteger(((String) object));
        else if (object instanceof Long)
            value = ((Long) object).longValue();
        else if (object instanceof Integer)
            value = ((Integer) object).longValue();
        else if (object instanceof Short)
            value = ((Short) object).longValue();
        else if (object instanceof Byte)
            value = ((Byte) object).longValue();
        else invalidCast(this);
        if (value < lowerBound() || value > upperBound())
            invalidObject(object);
        return new SingleInteger(value, this);
    }

    protected Object convertToObject(long value)
    {
        return new Long(value);
    }
    
    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        return convertToObject(expr.evalAsInteger(focus, context));
    }

    public XQValue convertFromArray(Object object)
    {
        long[] result = (long[]) object;
        return new IntegerArraySequence(result, result.length);
    }

    public Object convertToArray(XQValue value)
        throws EvaluationException
    {
        return IntegerArraySequence.expandIntegers(value);
    }
}
