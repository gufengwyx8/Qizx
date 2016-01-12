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
import com.qizx.xquery.*;
import com.qizx.xquery.op.Expression;

public class StringType extends AtomicType
{
    public String getShortName()
    {
        return "string";
    }

    public int quickCode()
    {
        return QT_STRING;
    }

    protected boolean checkValue(String value)
    {
        return true;
    }

    public boolean accepts(XQType valueType)
    {
        XQItemType itemType = valueType.itemType();
        if (super.accepts(itemType))
            return true;
        // special case: always accepted (converted to string by toString() )
        return (itemType instanceof WrappedObjectType);
    }

    public boolean promotable(XQItemType type)
    {
        return (type == UNTYPED_ATOMIC || type == ANYURI);
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws XQTypeException
    {
        // XQItemType type = value.getType();
        String result = null;
        try {
            result = value.getString();
        }
        catch (EvaluationException e) {
            throw new XQTypeException("cannot cast to xs:string: "
                                      + e.getMessage());
        }
        if (!checkValue(result))
            throw new XQTypeException("cannot cast to xs:" + getShortName()
                                      + ": value does not conform to facets");
        return new SingleString(result, this);
    }

    public XQValue convertFromObject(Object object)
        throws XQTypeException
    {
        return (object == null) ? XQValue.empty
                                : new SingleString(object.toString(), this);
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        return expr.evalAsString(focus, context);
    }

    public XQValue convertFromArray(Object object)
    {
        String[] result = (String[]) object;
        return new StringArraySequence(result, result.length);
    }

    public Object convertToArray(XQValue value)
        throws EvaluationException
    {
        return StringArraySequence.expand(value, false);
    }
}
