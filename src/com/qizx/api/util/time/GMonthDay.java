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
 * Representation of a GMonthDay value.
 */
public class GMonthDay extends DateTimeBase
{
    public GMonthDay(int month, int day, int tzSign, int tzHour, int tzMinute)
        throws DateTimeException
    {
        super(0, month, day, 0, 0, 0, tzSign, tzHour, tzMinute);
    }

    public GMonthDay(DateTimeBase dt) throws DateTimeException
    {
        this(dt.month, dt.day, dt.tzSign, dt.tzHour, dt.tzMinute);
    }

    public DateTimeBase parse(String that)
        throws DateTimeException
    {
        return parseGMonthDay(that);
    }

    public static GMonthDay parseGMonthDay(String s)
        throws DateTimeException
    {
        int month;
        int day;
        int[] tz = new int[3];

        if (!s.startsWith("--"))
            throw new DateTimeException("invalid gMonthDay syntax", s);

        int charCount = s.length();
        int pos = 2;
        if (pos + 2 >= charCount || s.charAt(pos + 2) != '-')
            throw new DateTimeException("invalid gMonthDay syntax", s);
        month = parseInt(s, pos, pos + 2);
        pos += 3;

        int tzPos = parseTimeZone(s, pos, tz);
        if (tzPos != pos + 2)
            throw new DateTimeException("invalid gMonthDay syntax", s);
        day = parseInt(s, pos, tzPos);

        return new GMonthDay(month, day, tz[0], tz[1], tz[2]);
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("--");
        appendPadded(month, 2, buffer);
        buffer.append('-');
        appendPadded(day, 2, buffer);
        appendTimeZone(tzSign, tzHour, tzMinute, buffer);

        return buffer.toString();
    }
}
