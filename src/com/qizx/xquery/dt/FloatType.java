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

public class FloatType extends NumericType
{
    public String getShortName()
    {
        return "float";
    }

    public int quickCode()
    {
        return QT_FLOAT;
    }

    // public boolean accepts( XQType that ) {
    // return that != XQType.DOUBLE && XQType.NUMERIC.accepts(that);
    // }

    public boolean encompasses(XQItemType that)
    {
        // encompasses all numeric types but double:
        return that != XQType.DOUBLE && XQType.NUMERIC.encompasses(that);
    }

    public boolean promotable(XQItemType type)
    {
        return (type == UNTYPED_ATOMIC || type.isSubTypeOf(DECIMAL));
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws EvaluationException
    {
        XQItemType type = value.getItemType();
        switch (type.quickCode()) {
        case XQType.QT_YMDUR:
        case XQType.QT_DTDUR:
            if (context.sObs())
                invalidCast(type);
            // GO ON
        case XQType.QT_STRING:
        case XQType.QT_UNTYPED:
        case XQType.QT_DEC:
        case XQType.QT_INT:
        case XQType.QT_FLOAT:
        case XQType.QT_DOUBLE: // extension
        case XQType.QT_BOOL:
            return new SingleFloat(value.getFloat());

        default:
            return invalidCast(type);
        }
    }

    public XQValue convertFromObject(Object object) throws XQTypeException
    {
        float value = 0;
        if (object instanceof Float)
            value = ((Float) object).floatValue();
        else if (object instanceof Long)
            value = ((Long) object).floatValue();
        else if (object instanceof Integer)
            value = ((Integer) object).floatValue();
        else if (object instanceof Short)
            value = ((Short) object).floatValue();
        else if (object instanceof Byte)
            value = ((Byte) object).floatValue();
        else if (object instanceof String)
            value = Conversion.toFloat((String) object);
        else invalidCast(this);
        return new SingleFloat(value);
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        return new Float(expr.evalAsFloat(focus, context));
    }

    public XQValue convertFromArray(Object object)
    {
        float[] result = (float[]) object;
        return new FloatArraySequence(result, result.length);
    }

    public Object convertToArray(XQValue value)
        throws EvaluationException
    {
        return FloatArraySequence.expandFloats(value);
    }
}
