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

public class BooleanType extends AtomicType
{

    public String getShortName()
    {
        return "boolean";
    }

    public int quickCode()
    {
        return QT_BOOL;
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws EvaluationException
    {
        XQItemType type = value.getItemType();

        switch (type.quickCode()) {
        case XQType.QT_STRING:
        case XQType.QT_UNTYPED:
            return new SingleBoolean(Conversion.toBoolean(value.getString().trim()));
        case XQType.QT_DEC:
        case XQType.QT_INT:
        case XQType.QT_FLOAT:
        case XQType.QT_DOUBLE: // extension
        case XQType.QT_BOOL:
            return new SingleBoolean(value.getBoolean());

        default:
            invalidCast(type);
            return null;
        }
    }

    public XQValue convertFromObject(Object object) throws XQTypeException
    {
        boolean value = false;
        if(object instanceof String)
            value = Conversion.toBoolean((String) object);
        else if(object instanceof Boolean)
            value = ((Boolean) object).booleanValue();
        else
            invalidCast(object);
        return new SingleBoolean(value);
    }

    public XQValue convertFromArray(Object object)
    {
        boolean[] array = (boolean[]) object;
        ArraySequence res = new ArraySequence(array.length, null);
        for (int i = 0, asize = array.length; i < asize; i++) {
            res.addItem(new SingleBoolean(array[i])); // a bit costly TODO
        }
        return res;
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        return Boolean.valueOf(expr.evalAsBoolean(focus, context));
    }
}
