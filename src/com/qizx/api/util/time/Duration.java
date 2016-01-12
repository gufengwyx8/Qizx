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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Representation and manipulation of a Duration value.
 */
public class Duration
{
    public static DecimalFormat SECOND_FORMAT =
        new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US));

    private boolean yearMonth; // false if dayTime
    private boolean dayTime; // false if yearMonth
    private int months;
    private double seconds;

    private Duration()
    {
    }

    /**
     * General duration.
     * 
     * @param months
     * @param seconds
     */
    public Duration(int months, double seconds)
    {
        if (months * seconds < 0)
            throw new IllegalArgumentException("inconsistent signs");
        this.months = months;
        this.seconds = seconds;
        yearMonth = dayTime = true;
    }

    public static Duration newYearMonth(int months)
    {
        Duration d = new Duration();
        d.yearMonth = true;
        d.months = months;
        return d;
    }

    public static Duration newDayTime(double seconds)
    {
        Duration d = new Duration();
        d.dayTime = true;
        d.seconds = seconds;
        return d;
    }

    public static Duration parseDuration(String s)
        throws DateTimeException
    {
        int charCount = s.length();
        if (charCount <= 1)
            throw new DateTimeException("invalid duration", s);

        Duration dur = new Duration();

        int pos = 0;
        boolean negative = false;
        if (s.charAt(0) == '-') {
            negative = true;
            ++pos;
        }

        // At least P0Y.
        if (pos + 2 >= charCount || s.charAt(pos) != 'P')
            throw new DateTimeException("invalid duration (missing 'P')", s);
        ++pos;

        int timePos = s.indexOf('T');
        if (timePos == charCount - 1)
            throw new DateTimeException("invalid duration (trailing 'T')", s);

        int nextPos = s.indexOf('Y', pos);
        if (nextPos > pos) {
            dur.months =
                12 * DateTimeBase.parseNonNegativeInt(s, pos, nextPos);
            dur.yearMonth = true;
            pos = nextPos + 1;
        }

        nextPos = s.indexOf('M', pos);
        if (nextPos > pos && (timePos < 0 || nextPos < timePos)) {
            dur.months += DateTimeBase.parseNonNegativeInt(s, pos, nextPos);
            dur.yearMonth = true;
            pos = nextPos + 1;
        }

        nextPos = s.indexOf('D', pos);
        if (nextPos > pos) {
            dur.seconds =
                86400.0 * DateTimeBase.parseNonNegativeInt(s, pos, nextPos);
            dur.dayTime = true;
            pos = nextPos + 1;
        }

        if (timePos >= 0) {
            if (timePos == pos) {
                ++pos;

                nextPos = s.indexOf('H', pos);
                if (nextPos > pos) {
                    dur.seconds +=
                        3600 * DateTimeBase.parseNonNegativeInt(s, pos,
                                                                nextPos);
                    dur.dayTime = true;
                    pos = nextPos + 1;
                }

                nextPos = s.indexOf('M', pos);
                if (nextPos > pos) {
                    dur.seconds +=
                        60 * DateTimeBase.parseNonNegativeInt(s, pos, nextPos);
                    dur.dayTime = true;
                    pos = nextPos + 1;
                }

                nextPos = s.indexOf('S', pos);
                if (nextPos > pos) {
                    dur.seconds +=
                        DateTimeBase.parseNonNegativeDouble(s, pos, nextPos);
                    dur.dayTime = true;
                    pos = nextPos + 1;
                }
            }

            if (!dur.dayTime)
                throw new DateTimeException("invalid duration", s);
        }

        if (pos != charCount || !(dur.yearMonth || dur.dayTime))
            throw new DateTimeException("invalid duration", s);

        if (negative) {
            dur.seconds = -dur.seconds;
            dur.months = -dur.months;
        }
        return dur;
    }

    /**
     * Checks the validity of the duration considered as a timezone.
     */
    public boolean checkAsTimezone()
    {
        if (yearMonth || getSeconds() != 0)
            return false;
        final int MAX = 14 * 60;
        int h = getHours() * 60 + getMinutes();
        return h >= -MAX && h <= MAX;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        if (months < 0 || (seconds < 0 && Math.abs(seconds) >= 0.001))
            buffer.append('-');
        buffer.append('P');

        if (yearMonth && (months != 0 || !dayTime)) {
            int year = Math.abs(months) / 12;
            if (year > 0) {
                buffer.append(year);
                buffer.append('Y');
            }
            int mon = Math.abs(months) % 12;
            if (mon > 0 || year == 0) {
                buffer.append(mon);
                buffer.append('M');
            }
        }

        long days = dayTime ? (int) (Math.abs(seconds) / 86400) : 0;
        if (days > 0) {
            buffer.append(days);
            buffer.append('D');
        }

        double time = Math.abs(seconds) - days * 86400;
        if (dayTime) {
            if (time != 0)
                buffer.append('T');
            else if (days == 0) {
                if (months == 0)
                    buffer.append("T0S"); // done
                return buffer.toString();
            }

            int hours = (int) (time / 3600);
            if (hours > 0) {
                buffer.append(hours);
                buffer.append('H');
            }

            int minutes = ((int) (time / 60)) % 60;
            if (minutes > 0) {
                buffer.append(minutes);
                buffer.append('M');
            }

            double secs = time % 60;
            if (secs > 0) {
                buffer.append(SECOND_FORMAT.format(secs));
                buffer.append('S');
            }
        }
        return buffer.toString();
    }

    /**
     * Makes sense if either YM or DT.
     */
    public int compareTo(Duration that)
    {
        int cmp = Comparison.of(this.getTotalMonths(), that.getTotalMonths());
        if (cmp != Comparison.EQ)
            return cmp;
        return Comparison.of(this.getTotalSeconds(), that.getTotalSeconds());
    }

    public Duration multiply(double factor)
    {
        return new Duration((int) Math.round(getTotalMonths() * factor),
                            getTotalSeconds() * factor);
    }

    public int getTotalMonths()
    {
        return months;
    }

    public double getTotalSeconds()
    {
        return seconds;
    }

    public boolean isDayTime()
    {
        return dayTime && months == 0;
    }

    public boolean isYearMonth()
    {
        return yearMonth && seconds == 0.0;
    }

    public Duration copy()
    {
        Duration d = new Duration(months, seconds);
        d.yearMonth = yearMonth;
        d.dayTime = dayTime;
        return d;
    }

    public int getYears()
    {
        return months / 12;
    }

    public int getMonths()
    {
        return months % 12;
    }

    public int getDays()
    {
        return (int) (seconds / 86400);
    }

    public int getHours()
    {
        long h = (long) (seconds / 3600);
        return (int) (h % 24);
    }

    public int getMinutes()
    {
        long m = (long) (seconds / 60);
        return (int) (m % 60);
    }

    public double getSeconds()
    {
        return (seconds % 60);
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (dayTime ? 1231 : 1237);
        result = PRIME * result + months;
        long temp;
        temp = Double.doubleToLongBits(seconds);
        result = PRIME * result + (int) (temp ^ (temp >>> 32));
        result = PRIME * result + (yearMonth ? 1231 : 1237);
        return result;
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Duration other = (Duration) obj;
        if (dayTime != other.dayTime)
            return false;
        if (months != other.months)
            return false;
        if (Double.doubleToLongBits(seconds) // anti NaN
            != Double.doubleToLongBits(other.seconds))
            return false;
        if (yearMonth != other.yearMonth)
            return false;
        return true;
    }

}
