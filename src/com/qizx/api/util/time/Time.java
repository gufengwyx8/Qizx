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

import com.qizx.util.basic.Comparison;

/**
 * Representation of a Time value.
 * 
 * @author Xavier Franc. Most of this code borrowed from XMLmind XML Editor
 *         (courtesy of H. Shafie), and adapted (or botched-up).
 */
public class Time extends DateTimeBase
{
    private static final int DAY = 86400;

    public Time(int hour, int minute, double second, int tzSign, int tzHour,
                int tzMinute) throws DateTimeException
    {
        super(EPOCH + 2, 12, 31, hour, minute, second, tzSign, tzHour,
              tzMinute);
    }

    public Time(DateTimeBase dt) throws DateTimeException
    {
        this(dt.hour, dt.minute, dt.second, dt.tzSign, dt.tzHour, dt.tzMinute);
    }

    public Time(long epoch, int timezone)
    {
        super(epoch % (DAY * 1000), timezone);
    }

    public DateTimeBase parse(String that)
        throws DateTimeException
    {
        return parseTime(that);
    }

    public static Time parseTime(String s)
        throws DateTimeException
    {
        int hour;
        int minute;
        double second;
        int[] tz = new int[3];

        int charCount = s.length();
        int pos = 0;
        if (pos + 2 >= charCount || s.charAt(pos + 2) != ':')
            throw new DateTimeException("invalid time syntax", s);
        hour = parseInt(s, pos, pos + 2);
        pos += 3;

        if (pos + 2 >= charCount || s.charAt(pos + 2) != ':')
            throw new DateTimeException("invalid time syntax", s);
        minute = parseInt(s, pos, pos + 2);
        pos += 3;

        int tzPos = parseTimeZone(s, pos, tz);
        if (tzPos < pos + 2)
            throw new DateTimeException("invalid time syntax", s);

        second = parseDouble(s, pos, tzPos);

        Time result = new Time(hour, minute, second, tz[0], tz[1], tz[2]);
        return result;
    }

    public int compareTo(DateTimeBase that)
    {
        double s1 = getSecondsFromEpoch(), s2 = that.getSecondsFromEpoch();
        if (s1 < 0)
            s1 += DAY;
        else if (s1 > DAY)
            s1 -= DAY;
        if (s2 < 0)
            s2 += DAY;
        else if (s2 > DAY)
            s2 -= DAY;
        return Comparison.of(s1, s2);
    }

    public DateTimeBase copy()
    {
        try {
            return new Time(hour, minute, second, tzSign, tzHour, tzMinute);
        }
        catch (DateTimeException cannotHappen) {
            return null;
        }
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        appendPadded(hour, 2, buffer);
        buffer.append(':');
        appendPadded(minute, 2, buffer);
        buffer.append(':');
        appendPadded(second, 2, buffer);

        appendTimeZone(tzSign, tzHour, tzMinute, buffer);

        return buffer.toString();
    }

    public DateTimeBase adjustToTimezone(int tzInMinutes, boolean set)
    {
        DateTimeBase result = super.adjustToTimezone(tzInMinutes, set);
        normalize(result);
        return result;
    }

    public static void normalize(DateTimeBase dt)
    {
        dt.year = 1972;
        dt.month = 12;
        dt.day = 31;
    }
}
