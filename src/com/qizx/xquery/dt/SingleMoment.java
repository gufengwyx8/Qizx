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

import com.qizx.api.util.time.Date;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.Time;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

/**
 * Implementation of a single item of type dateTime, date, time, gDay etc.
 */
public class SingleMoment extends MomentValue
{
    protected DateTimeBase value;

    private boolean started = false;

    public SingleMoment(DateTimeBase value, XQItemType subType)
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
        return new SingleMoment(value, itemType);
    }

    public DateTimeBase getValue()
    {
        return value;
    }

    public static SingleMoment dateTime(DateTimeBase moment)
    {
        return new SingleMoment(moment, XQType.DATE_TIME);
    }

    public static SingleMoment date(DateTimeBase moment)
    {
        Date.normalize(moment);
        return new SingleMoment(moment, XQType.DATE);
    }

    public static SingleMoment time(DateTimeBase moment)
    {
        Time.normalize(moment);
        return new SingleMoment(moment, XQType.TIME);
    }
}
