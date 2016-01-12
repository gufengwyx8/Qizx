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
import com.qizx.api.QizxException;
import com.qizx.util.basic.Comparison;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;

public abstract class BooleanValue extends BaseValue
{
    public static SingleBoolean TRUE = new SingleBoolean(true);

    public static SingleBoolean FALSE = new SingleBoolean(false);

    public BooleanValue()
    {
        itemType = XQType.BOOLEAN;
    }

    public abstract boolean getValue();

    public XQItem getItem()
    {
        return getValue() ? TRUE : FALSE;
    }

    public double getDouble()
        throws EvaluationException
    {
        return Conversion.toDouble(getBoolean());
    }

    public float getFloat()
        throws EvaluationException
    {
        return Conversion.toFloat(getBoolean());
    }

    public long getInteger()
        throws EvaluationException
    {
        return Conversion.toInteger(getBoolean());
    }

    public String getString()
        throws EvaluationException
    {
        return Conversion.toString(getBoolean());
    }

    public Object getObject()
        throws QizxException
    {
        return Boolean.valueOf(getValue());
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException
    {
        XQItemType thatType = that.getItemType();
        switch (thatType.quickCode()) {
        case XQType.QT_UNTYPED:
            if ((flags & COMPAR_VALUE) != 0)
                return Comparison.ERROR;
        case XQType.QT_BOOL:
            boolean b = that.getBoolean();
            return (getBoolean() == b) ? Comparison.EQ
                                       : b ? Comparison.LT : Comparison.GT;
        default:
            return Comparison.ERROR;
        }
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof BooleanValue) {
            BooleanValue bob = (BooleanValue) obj;
            return getValue() == bob.getValue();
        }
        return false;
    }

    public int hashCode()
    {
        return getValue() ? 1 : 0;
    }
}
