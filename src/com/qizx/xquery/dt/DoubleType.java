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

public class DoubleType extends NumericType
{
    public String getShortName()
    {
        return "double";
    }

    public int quickCode()
    {
        return QT_DOUBLE;
    }

    // public boolean accepts( XQType that ) {
    // // attention: accept all numeric types
    // // It is NOT equivalent to super.accepts() !
    // return XQItemType.isNumeric(that.getItemType());
    // }

    public boolean encompasses(XQItemType valueType)
    {
        // encompasses all numeric types:
        return XQType.NUMERIC.encompasses(valueType);
    }

    public boolean promotable(XQItemType type)
    {
        return (type == UNTYPED_ATOMIC || type == FLOAT ||
                type.isSubTypeOf(DECIMAL));
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws XQTypeException
    {
        try {
            XQItemType type = value.getItemType();
            if(type instanceof BinaryType ||
                    type instanceof QNameType || 
                    type instanceof AnyURIType)
                invalidCast(type);
            if (context.sObs()
                && (type instanceof MomentType || type instanceof DurationType))
                invalidCast(type);
            return new SingleDouble(value.getDouble());
        }
        catch (EvaluationException e) {
            castException(e);
            return null; // dummy
        }
    }

    public XQValue convertFromObject(Object object)
        throws XQTypeException
    {
        double value = 0;
        if (object instanceof Double)
            value = ((Double) object).doubleValue();
        else if (object instanceof Long)
            value = ((Long) object).doubleValue();
        else if (object instanceof Integer)
            value = ((Integer) object).doubleValue();
        else if (object instanceof Short)
            value = ((Short) object).doubleValue();
        else if (object instanceof Byte)
            value = ((Byte) object).doubleValue();
        else if (object instanceof String)
            value = Conversion.toDouble((String) object);
        else
            invalidCast(this);
        return new SingleDouble(value);
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        return new Double(expr.evalAsDouble(focus, context));
    }

    public XQValue convertFromArray(Object object)
    {
        double[] result = (double[]) object;
        return new DoubleArraySequence(result, result.length);
    }

    public Object convertToArray(XQValue value) throws EvaluationException
    {
        return DoubleArraySequence.expandDoubles(value);
    }
}
