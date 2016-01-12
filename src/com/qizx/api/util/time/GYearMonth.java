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
 * Representation of a GYearMonth value.
 */
public class GYearMonth extends DateTimeBase
{
    public GYearMonth(int year, int month, int tzSign, int tzHour, int tzMinute)
        throws DateTimeException
    {
        super(year, month, 1, 0, 0, 0, tzSign, tzHour, tzMinute);
    }

    public GYearMonth(DateTimeBase dt) throws DateTimeException
    {
        this(dt.year, dt.month, dt.tzSign, dt.tzHour, dt.tzMinute);
    }

    public DateTimeBase parse(String that)
        throws DateTimeException
    {
        return parseGYearMonth(that);
    }

    public static GYearMonth parseGYearMonth(String s)
        throws DateTimeException
    {
        int year;
        int month;
        int[] tz = new int[3];

        int charCount = s.length();
        if (charCount == 0)
            throw new DateTimeException("invalid gYearMonth syntax", s);

        int pos = 0;
        boolean negativeYear = false;
        if (s.charAt(pos) == '-') {
            negativeYear = true;
            ++pos;
        }
        int nextPos = s.indexOf('-', pos);
        if (nextPos < pos + 4)
            throw new DateTimeException("invalid gYearMonth syntax", s);
        year = parseInt(s, pos, nextPos);
        if (year < 10000 && nextPos - pos > 4)
            throw new DateTimeException("invalid year: leading zeroes", s);
        if (negativeYear)
            year = -year;
        pos = nextPos + 1;

        nextPos = parseTimeZone(s, pos, tz);
        if (nextPos != pos + 2)
            throw new DateTimeException("invalid gYearMonth syntax", s);
        month = parseInt(s, pos, nextPos);

        return new GYearMonth(year, month, tz[0], tz[1], tz[2]);
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        if (year < 0) {
            buffer.append('-');
            appendPadded(-year, 4, buffer);
        }
        else {
            appendPadded(year, 4, buffer);
        }
        buffer.append('-');
        appendPadded(month, 2, buffer);

        appendTimeZone(tzSign, tzHour, tzMinute, buffer);

        return buffer.toString();
    }
}
