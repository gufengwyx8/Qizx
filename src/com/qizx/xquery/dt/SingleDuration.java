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

import com.qizx.api.util.time.DateTimeException;
import com.qizx.api.util.time.Duration;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

/**
 * A scalar Duration value.
 */
public class SingleDuration extends DurationValue
{
    private Duration value;

    private boolean started = false;

    public SingleDuration(Duration value)
    {
        this(value, XQType.DURATION);
    }

    public static SingleDuration newYM(Duration value)
        throws DateTimeException
    {
        if (value == null || !value.isYearMonth())
            throw new DateTimeException("invalid yearMonthDuration value");
        return new SingleDuration(value, XQType.YEAR_MONTH_DURATION);
    }

    public static SingleDuration newDT(Duration value)
        throws DateTimeException
    {
        if (value == null || !value.isDayTime())
            throw new DateTimeException("invalid dayTimeDuration value");
        return new SingleDuration(value, XQType.DAY_TIME_DURATION);
    }

    public SingleDuration(Duration value, XQItemType subType)
    {
        this.value = value;
        itemType = subType;
    }

    public boolean next()
    {
        return started ? false : (started = true);
    }

    public XQValue bornAgain()
    {
        return new SingleDuration(value, itemType);
    }

    public Duration getValue()
    {
        return value;
    }
}
