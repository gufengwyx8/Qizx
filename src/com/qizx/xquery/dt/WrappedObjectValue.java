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
import com.qizx.util.basic.Comparison;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;

/**
 * Base class for Wrapped Java Object.
 */
public abstract class WrappedObjectValue extends BaseValue
{
    public WrappedObjectValue()
    {
        itemType = XQType.WRAPPED_OBJECT;
    }

    public abstract Object getObject();

    public XQItem getItem()
    {
        return this;
    }

    public String getString()
        throws XQTypeException
    {
        return getObject().toString();
    }

    public boolean getBoolean()
        throws XQTypeException
    {
        String d = getString();
        return d.length() != 0;
    }

    public long getInteger()
        throws EvaluationException
    {
        return Conversion.toInteger(getString());
    }

    public double getDouble()
        throws EvaluationException
    {
        return Conversion.toDouble(getString());
    }

    public float getFloat()
        throws EvaluationException
    {
        return (float) getDouble();
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof WrappedObjectValue))
            return false;
        return getObject().equals(((WrappedObjectValue) that).getObject());
    }

    public int hashCode()
    {
        return getObject().hashCode();
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
    {
        return Comparison.ERROR;
    }

    /**
     * Unwraps object if it matches the class, otherwise return null
     */
    public static Object as(Class classe, XQItem item)
    {
        if (item instanceof WrappedObjectValue) {
            Object object = ((WrappedObjectValue) item).getObject();
            if (classe.isAssignableFrom(object.getClass()))
                return object;
        }
        return null;
    }
}
