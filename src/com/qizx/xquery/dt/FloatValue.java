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
import com.qizx.xdm.FloatConversion;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;

/**
 *	
 */
public abstract class FloatValue extends BaseValue
{
    public FloatValue()
    {
        itemType = XQType.FLOAT;
    }

    public XQItem getItem()
    {
        return new SingleFloat(getValue());
    }

    protected abstract float getValue();

    public boolean getBoolean()
        throws EvaluationException
    {
        float d = getFloat();
        return d == d && d != 0; // d==d tests NaN
    }

    public long getInteger()
        throws EvaluationException
    {
        return (long) Math.round(getFloat());
    }

    public double getDouble()
        throws EvaluationException
    {
        return getFloat();
    }

    public String getString()
        throws EvaluationException
    {
        return FloatConversion.toString(getFloat());
    }


    public Object getObject()
        throws QizxException
    {
        return new Float(getValue());
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof XQItem))
            return false;
        try {
            XQItem thatItem = (XQItem) that;
            return thatItem.getType() == XQType.FLOAT
                   && getFloat() == thatItem.getFloat();
        }
        catch (EvaluationException e) {
            return false;
        }
    }

    public int hashCode()
    {
        try {
            return Util.hashDouble(getFloat()); // compatibility with
                                                // int/dec/float...
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
        case XQType.QT_DOUBLE:
        case XQType.QT_DEC:
        case XQType.QT_FLOAT:
        case XQType.QT_INT:
            if (context == null || (flags & COMPAR_VALUE) != 0)
                return compare(getDouble(), that.getDouble());
            return compare(getDouble(), that.getDouble(), 
                           context.emptyGreatest());
        default:
            return Comparison.ERROR;
        }
    }
}
