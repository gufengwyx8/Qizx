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
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;


/**
 *	Abstract Date/Time value.
 */
public abstract class MomentValue extends BaseValue
{
    public abstract DateTimeBase getValue();
    
    public XQItem getItem()
    {
        return this;
    }

    public String getString()
        throws XQTypeException
    {
        return getValue().toString();
    }

    public double getDouble()
        throws EvaluationException
    {
        DateTimeBase d = getValue();
        return d.getMillisecondsFromEpoch() / 1000.0;
    }

    public float getFloat()
        throws EvaluationException
    {
        return (float) getDouble();
    }

    public DateTimeBase getMoment()
        throws EvaluationException
    {
        return getValue();
    }

    public Object getObject()
        throws QizxException
    {
        return getValue();
    }


    public int hashCode()
    {
        return getValue().hashCode();
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof MomentValue))
            return false;
        return getValue().equals(((MomentValue) that).getValue());
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException
    {
        XQItemType thatType = that.getItemType();
        if (thatType.quickCode() == XQType.QT_UNTYPED)
            that = getItemType().cast(that, null);
        else if (getType() != thatType)
            return Comparison.ERROR;
        return getValue().compareTo(that.getMoment());
    }
}
