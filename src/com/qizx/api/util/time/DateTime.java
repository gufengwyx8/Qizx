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
 * Representation of a DateTime value.
 */
public class DateTime extends DateTimeBase
{
    public DateTime(int year, int month, int day, int hour, int minute,
                    double second, int tzSign, int tzHour, int tzMinute)
        throws DateTimeException
    {
        super(year, month, day, hour, minute, second, tzSign, tzHour, tzMinute);
    }

    public DateTime(DateTimeBase dt) throws DateTimeException
    {
        this(dt.year, dt.month, dt.day, dt.hour, dt.minute, dt.second,
             dt.tzSign, dt.tzHour, dt.tzMinute);
    }

    public DateTime(long fromEpoch)
    {
        super(fromEpoch, 0);
    }

    public DateTime(java.util.Date date, int timeZone)
    {
        super(date.getTime(), timeZone);
    }

    public DateTime()
    {
    }

    public DateTimeBase parse(String that)
        throws DateTimeException
    {
        return parseDateTime(that);
    }

    /**
     * No more uses implicitTimeZone (minutes) if not specified explicitly.
     */
    public static DateTime parseDateTime(String s)
        throws DateTimeException
    {
        int year;
        int month;
        int day;
        int hour;
        int minute;
        double second;
        int[] tz = new int[3];

        int charCount = s.length();
        if (charCount == 0)
            throw new DateTimeException("invalid syntax", s);

        int pos = 0;
        boolean negativeYear = false;
        if (s.charAt(pos) == '-') {
            negativeYear = true;
            ++pos;
        }
        int nextPos = s.indexOf('-', pos);
        if (nextPos < pos + 4)
            throw new DateTimeException("invalid year", s);
        year = parseInt(s, pos, nextPos);
        if (year < 10000 && nextPos - pos > 4)
            throw new DateTimeException("invalid year: leading zeroes", s);
        if (negativeYear)
            year = -year;
        pos = nextPos + 1;

        if (pos + 2 >= charCount || s.charAt(pos + 2) != '-')
            throw new DateTimeException("invalid month", s);
        month = parseInt(s, pos, pos + 2);
        pos += 3;

        if (pos + 2 >= charCount || s.charAt(pos + 2) != 'T')
            throw new DateTimeException("invalid day", s);
        day = parseInt(s, pos, pos + 2);
        pos += 3;

        if (pos + 2 >= charCount || s.charAt(pos + 2) != ':')
            throw new DateTimeException("invalid syntax", s);
        hour = parseInt(s, pos, pos + 2);
        pos += 3;

        if (pos + 2 >= charCount || s.charAt(pos + 2) != ':')
            throw new DateTimeException("invalid syntax", s);
        minute = parseInt(s, pos, pos + 2);
        pos += 3;

        nextPos = parseTimeZone(s, pos, tz);
        if (nextPos < pos + 2)
            throw new DateTimeException("invalid syntax", s);

        second = parseDouble(s, pos, nextPos);

        DateTime result =
            new DateTime(year, month, day, hour, minute, second, tz[0], tz[1],
                         tz[2]);

        return result;
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
        buffer.append('-');
        appendPadded(day, 2, buffer);

        buffer.append('T');

        appendPadded(hour, 2, buffer);
        buffer.append(':');
        appendPadded(minute, 2, buffer);
        buffer.append(':');
        appendPadded(second, 2, buffer);

        appendTimeZone(tzSign, tzHour, tzMinute, buffer);

        return buffer.toString();
    }

    public DateTimeBase copy()
    {
        try {
            return new DateTime(year, month, day, hour, minute, second,
                                tzSign, tzHour, tzMinute);
        }
        catch (DateTimeException shouldNotHappen) {
            shouldNotHappen.printStackTrace();
            return this;
        }
    }
}
