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
import com.qizx.api.util.time.Duration;
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.*;

import java.math.BigDecimal;

/**
 * Abstract Duration value.
 */
public abstract class DurationValue extends BaseValue
{
    public abstract Duration getValue();

    public XQItem getItem()
    {
        return this;
    }

    public double getDouble()
        throws EvaluationException
    {
        Duration d = getValue();
        switch (getItemType().quickCode()) {
        case XQType.QT_YMDUR:
            return d.getTotalMonths();
        case XQType.QT_DTDUR:
            return d.getTotalSeconds();
        default:
            return super.getDouble(); // failure
        }
    }

    public BigDecimal getDecimal()
        throws EvaluationException
    {
        long v = (long) (getDouble() * 1000);
        return BigDecimal.valueOf(v, 3);
    }

    public float getFloat()
        throws EvaluationException
    {
        return (float) getDouble();
    }

    public long getInteger()
        throws EvaluationException
    {
        return (long) getDouble();
    }

    public String getString()
        throws XQTypeException
    {
        return getValue().toString();
    }

    public Duration getDuration()
    {
        return getValue();
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof DurationValue))
            return false;
        return getValue().equals(((DurationValue) that).getValue());
    }

    public int hashCode()
    {
        return getValue().hashCode();
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException
    {
        XQItemType t1 = getItemType(), t2 = that.getItemType();
        if ((flags & COMPAR_ORDER) != 0)
            if (t1 != t2 || t1 == XQType.DURATION)
                return Comparison.ERROR;

        Duration d1 = getValue(), d2 = that.getDuration();
        return d1.compareTo(d2);
    }

} // end of class DurationValue

