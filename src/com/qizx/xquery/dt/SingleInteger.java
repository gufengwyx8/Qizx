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

import com.qizx.xdm.Conversion;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

import java.math.BigDecimal;

/**
 * A scalar Integer value.
 */
public class SingleInteger extends IntegerValue
{
    private long value;

    private boolean started = false;

    public SingleInteger(long value)
    {
        this(value, XQType.INTEGER);
        // System.err.println("SingleInteger("+value+")");new
        // Exception().printStackTrace();
    }

    public SingleInteger(long value, XQItemType subType)
    {
        this.value = value;
        itemType = subType;
        if(subType == null)
            itemType = XQType.INTEGER;
    }

    public boolean next()
    {
        return started ? false : (started = true);
    }

    public XQValue bornAgain()
    {
        return new SingleInteger(value, itemType);
    }

    public long getInteger()
    {
        return value;
    }

    public BigDecimal getDecimal()
        throws XQTypeException
    {
        return Conversion.toDecimal(getInteger());
    }

    public String toString()
    {
        return "SingleInteger " + value + " " + itemType;
    }

    protected long getValue()
    {
        return value;
    }
}
