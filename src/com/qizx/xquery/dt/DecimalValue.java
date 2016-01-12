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
import com.qizx.xquery.XQTypeException;

import java.math.BigDecimal;

/**
 * Abstract decimal value.
 */
public abstract class DecimalValue extends BaseValue
{
    public DecimalValue()
    {
        itemType = XQType.DECIMAL;
    }

    public abstract BigDecimal getValue();

    public XQItem getItem()
    {
        return this;
    }

    public String getString()
        throws XQTypeException
    {
        return Conversion.toString(getValue());
    }

    public double getDouble()
        throws XQTypeException
    {
        return getValue().doubleValue();
    }

    public float getFloat()
        throws XQTypeException
    {
        return getValue().floatValue();
    }

    public boolean getBoolean()
        throws EvaluationException
    {
        BigDecimal d = getValue();
        return d.signum() != 0; // not obvious, equals(ZERO) wont work
    }

    public Object getObject()
        throws QizxException
    {
        return getValue();
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof DecimalValue))
            return false;
        return getValue().equals(((DecimalValue) that).getValue());
    }

    public int hashCode()
    {
        return Util.hashDouble(getValue().doubleValue());
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException
    {
        double d;
        switch (that.getItemType().quickCode()) {
        case XQType.QT_UNTYPED:
            if ((flags & COMPAR_VALUE) != 0)
                return Comparison.ERROR;
        case XQType.QT_FLOAT:
            float f = that.getFloat();
            if (context == null || (flags & COMPAR_VALUE) != 0)
                return compare(getFloat(), f);
            return compare(getFloat(), f, context.emptyGreatest());
        case XQType.QT_DOUBLE:
            d = that.getDouble();
            if (context == null)
                return compare(getDouble(), d);
            return compare(getDouble(), d, context.emptyGreatest());
        case XQType.QT_DEC:
            return getValue().compareTo(that.getDecimal());
        case XQType.QT_INT:
            int cmp =
                getValue().compareTo(BigDecimal.valueOf(that.getInteger()));
            return cmp;
        default:
            return Comparison.ERROR;
        }
    }
}
