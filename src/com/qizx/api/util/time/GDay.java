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
package com.qizx.api.util.time;

/**
 * Representation of a GDay value.
 */
public class GDay extends DateTimeBase
{
    public GDay(int day, int tzSign, int tzHour, int tzMinute)
        throws DateTimeException
    {
        super(EPOCH, 1, day, 0, 0, 0, tzSign, tzHour, tzMinute);
    }

    public GDay(DateTimeBase dt) throws DateTimeException
    {
        this(dt.day, dt.tzSign, dt.tzHour, dt.tzMinute);
    }

    public DateTimeBase parse(String that)
        throws DateTimeException
    {
        return parseGDay(that);
    }

    public static GDay parseGDay(String s)
        throws DateTimeException
    {
        int day;
        int[] tz = new int[3];

        if (!s.startsWith("---"))
            throw new DateTimeException("invalid gDay syntax", s);
        int pos = 3;

        int tzPos = parseTimeZone(s, pos, tz);
        if (tzPos != pos + 2)
            throw new DateTimeException("invalid gDay syntax", s);
        day = parseInt(s, pos, tzPos);

        GDay parsed;
        try {
            parsed = new GDay(day, tz[0], tz[1], tz[2]);
        }
        catch (IllegalArgumentException e) {
            throw new DateTimeException("invalid gDay value", s);
        }
        return parsed;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("---");
        appendPadded(day, 2, buffer);
        appendTimeZone(tzSign, tzHour, tzMinute, buffer);
        return buffer.toString();
    }
}
