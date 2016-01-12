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
import com.qizx.util.basic.Util;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;

import java.math.BigDecimal;

/**
 * Abstract Integer value.
 */
public abstract class IntegerValue extends BaseValue
{
    public IntegerValue()
    {
        itemType = XQType.INTEGER;
    }

    public XQItem getItem()
    {
        return new SingleInteger(getValue(), itemType);
    }

    protected abstract long getValue();

    public boolean getBoolean()
        throws EvaluationException
    {
        return getInteger() != 0;
    }

    public double getDouble()
        throws EvaluationException
    {
        return getInteger(); // simple promotion
    }

    public float getFloat()
        throws EvaluationException
    {
        return getInteger(); // simple promotion
    }

    public String getString()
        throws EvaluationException
    {
        return Conversion.toString(getInteger());
    }

    public Object getObject()
        throws QizxException
    {
        return ((IntegerType) itemType).convertToObject(getValue());
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof XQItem))
            return false;
        try {
            return getInteger() == ((XQItem) that).getInteger();
        }
        catch (EvaluationException e) {
            return false;
        }
    }

    public int hashCode()
    {
        try {
            return Util.hashDouble(getInteger());
        }
        catch (Exception e) {
            return 0;
        } // cannot happen
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException
    {
        switch (that.getItemType().quickCode()) {
        case XQType.QT_UNTYPED:
            if ((flags & COMPAR_VALUE) != 0)
                return Comparison.ERROR;
            // GO ON
        case XQType.QT_FLOAT:
        case XQType.QT_DOUBLE:
            if (context == null || (flags & COMPAR_VALUE) != 0)
                return compare(getDouble(), that.getDouble());
            return compare(getDouble(), that.getDouble(),
                           context.emptyGreatest());
        case XQType.QT_DEC:
            return BigDecimal.valueOf(getInteger()).compareTo(that.getDecimal());
        case XQType.QT_INT:
            return compare(getInteger(), that.getInteger());
        default:
            return Comparison.ERROR;
        }
    }
}
