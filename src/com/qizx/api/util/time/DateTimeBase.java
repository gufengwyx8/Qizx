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
import com.qizx.util.basic.Util;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Superclass of all date & time related item types.
 * 
 * @author Most of this code borrowed and adapted from XMLmind XML Editor
 *         (courtesy H. Shafie).
 */
public abstract class DateTimeBase
    implements Serializable
{
    public static final int LOCAL = Integer.MAX_VALUE;

    public static final int TZMAX = 14 * 60;

    /* package */int year; // Can be negative but not null.
    /* package */byte month;
    /* package */byte day;
    /* package */byte hour;
    /* package */byte minute;
    /* package */double second;
    /* package */byte tzSign; // -1|0|1; 0 means no time zone ==> local time.
    /* package */byte tzHour;
    /* package */byte tzMinute;

    
    public DateTimeBase()
    {
    }

    public DateTimeBase(long timeMillis, int timeZone)
    {
        setSecondsFromEpoch(timeMillis / 1000.0, timeZone);
    }

    public DateTimeBase(DateTime dt) throws DateTimeException
    {
        this(dt.year, dt.month, dt.day, dt.hour, dt.minute, dt.second,
             dt.tzSign, dt.tzHour, dt.tzMinute);
    }

    public DateTimeBase(int year, int month, int day, int hour, int minute,
                        double second, int tzSign, int tzHour, int tzMinute)
        throws DateTimeException
    {

        if (month < 1 || month > 12)
            throw new DateTimeException("invalid month");

        if (day < 1 || day > lastDayOfMonth(month, year))
            throw new DateTimeException("invalid day");

        if (hour < 0 || hour > 24)
            throw new DateTimeException("invalid hour");
        boolean adjustDay = false;
        if (hour == 24) {
            if (minute != 0 || second != 0)
                throw new DateTimeException(
                                            "invalid time: only 24:00:00 is accepted");
            hour = 0;
            adjustDay = true;
        }

        if (minute < 0 || minute > 59)
            throw new DateTimeException("invalid minute");

        if (second < 0 || second >= 60)
            throw new DateTimeException("invalid second");

        if (tzSign < -1 || tzSign > 1)
            throw new DateTimeException("invalid tzSign");

        if (tzHour < 0 || tzHour > 23)
            throw new DateTimeException("invalid tzHour");

        if (tzMinute < 0 || tzMinute > 59)
            throw new DateTimeException("invalid tzMinute");

        if (tzSign == -1 && tzHour == 0 && tzMinute == 0)
            tzSign = 1;

        this.year = year;
        this.month = (byte) month;
        this.day = (byte) day;
        this.hour = (byte) hour;
        this.minute = (byte) minute;
        this.second = second;
        this.tzSign = (byte) tzSign;
        this.tzHour = (byte) tzHour;
        this.tzMinute = (byte) tzMinute;

        if (adjustDay) {
            setSecondsFromEpoch(getSecondsFromEpoch() + 24 * 60 * 60,
                                getTimeZone());
        }
    }

    public int getYear()
    {
        return year;
    }

    public int getMonth()
    {
        return month;
    }

    public int getDay()
    {
        return day;
    }

    public int getHour()
    {
        return hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public double getSecond()
    {
        return second;
    }

    public boolean hasTimeZone()
    {
        return tzSign != 0;
    }

    public int getTimeZoneSign()
    {
        return tzSign;
    }

    public int getTimeZoneHour()
    {
        return tzHour;
    }

    public int getTimeZoneMinute()
    {
        return tzMinute;
    }

    /**
     * Returns the number of seconds from Java epoch (1970-01-01T00:00:00Z).
     */
    public double getSecondsFromEpoch()
    {
        return getMillisecondsFromEpoch() / 1000.0;
    }

    // shared calendar for conversion works:
    // locking is much cheaper than allocating heaps of Calendar objects.
    static Calendar workCal =
        Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * Returns the number of milliseconds from Java epoch
     * (1970-01-01T00:00:00Z).
     */
    public long getMillisecondsFromEpoch()
    {
        synchronized (workCal) {
            int intsec = setCalendar();
            // let the calendar work:
            long millis =
                workCal.getTimeInMillis() + (int) (1000 * (second - intsec) + 0.5);
            return millis;
        }
    }

    private int setCalendar()
    {
        workCal.clear();
        workCal.set(Calendar.YEAR, year);
        workCal.set(Calendar.MONTH, month - 1); // caveat
        workCal.set(Calendar.DATE, day);
        workCal.set(Calendar.HOUR_OF_DAY, hour);
        workCal.set(Calendar.MINUTE, minute);
        int intsec = (int) second;
        workCal.set(Calendar.SECOND, intsec);
        workCal.set(Calendar.MILLISECOND, 0);
        // subtract timezone to go GMT:
        if (tzSign != 0) {
            workCal.add(Calendar.HOUR_OF_DAY, tzSign > 0 ? -tzHour : tzHour);
            workCal.add(Calendar.MINUTE, tzSign > 0 ? -tzMinute : tzMinute);
        }
        else
            workCal.set(Calendar.ZONE_OFFSET, 0);
        return intsec;
    }

    /**
     * Sets up from a time (in seconds from Java epoch) and a timeZone in
     * minutes.
     * 
     * @param seconds from reference epoch, as an UTC/GMT time.
     * @param timeZone in minutes from GMT.
     */
    public void setSecondsFromEpoch(double seconds, int timeZone)
    {
        synchronized (workCal) {
            workCal.clear();
            // feed the calendar with an adjusted TZ:
            // ///seconds += timeZone * 60;

            long millis = (long) (seconds * 1000);
            workCal.setTimeInMillis(millis);
            this.year = workCal.get(Calendar.YEAR);
            int era = workCal.get(Calendar.ERA);
            if (era == 0) // BC
                this.year = -this.year;
            this.month = (byte) (workCal.get(Calendar.MONTH) + 1);
            this.day = (byte) workCal.get(Calendar.DATE);
            this.hour = (byte) workCal.get(Calendar.HOUR_OF_DAY);
            this.minute = (byte) workCal.get(Calendar.MINUTE);
            this.second =
                workCal.get(Calendar.SECOND)
                + workCal.get(Calendar.MILLISECOND) / 1000.0
                + (seconds - millis / 1000.0); // fraction under millis...

            forceTimeZone(timeZone);
        }
    }

    // Takes care of adjusting day of month
    public void addMonths(int totalMonths)
    {
        synchronized (workCal) {
            int intsec = setCalendar();
            workCal.add(Calendar.MONTH, totalMonths);
            // FIX: correct with TZ:
            workCal.add(Calendar.SECOND, getTimeZoneInSeconds());
            this.year = workCal.get(Calendar.YEAR);
            int era = workCal.get(Calendar.ERA);
            if (era == 0) // BC
                this.year = -this.year;
            this.month = (byte) (workCal.get(Calendar.MONTH) + 1);
            this.day = (byte) workCal.get(Calendar.DATE);
        }
        // setMonthsFromEpoch(getMonthsFromEpoch() + totalMonths);
    }

    /**
     * Creates a DateTime from a Java Date (without timezone) and a timezone in
     * minutes.
     */
    public static DateTime fromDate(Date date, int timeZone)
    {
        synchronized (workCal) {

            workCal.clear();
            workCal.setTime(date);

            DateTime val = new DateTime();
            val.year = workCal.get(Calendar.YEAR);
            val.month = (byte) (workCal.get(Calendar.MONTH) + 1);
            val.day = (byte) workCal.get(Calendar.DATE);
            val.hour = (byte) workCal.get(Calendar.HOUR_OF_DAY);
            val.minute = (byte) workCal.get(Calendar.MINUTE);
            val.second =
                (double) workCal.get(Calendar.SECOND)
                + ((double) workCal.get(Calendar.MILLISECOND)) / 1000.0;

            val.forceTimeZone(timeZone);
            return val;
        }
    }

    public abstract DateTimeBase parse(String that)
        throws DateTimeException;

    public int hashCode()
    {
        long D = Double.doubleToRawLongBits(getSecondsFromEpoch());
        return (int) ((D >>> 32) ^ D);
    }

    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof DateTimeBase))
            return false;
        return (this.compareTo((DateTimeBase) other) == 0);
    }

    public int compareTo(DateTimeBase that)
    {
        return Comparison.of(getMillisecondsFromEpoch(), 
                             that.getMillisecondsFromEpoch());
    }

    /**
     * @param tzInMinutes tz or LOCAL
     * @param set if true, sets the TZ without changing the rest
     */
    public DateTimeBase adjustToTimezone(int tzInMinutes, boolean set)
    {
        if (tzSign == 0 || tzInMinutes == LOCAL)
            set = true;
        if (set)
            forceTimeZone(tzInMinutes);
        else {
            double epoc = getSecondsFromEpoch();
            setSecondsFromEpoch(epoc + 60 * tzInMinutes, tzInMinutes);
        }
        return this;
    }

    // ---------- utilities ---------------------------------------

    public static final int EPOCH = 1970; // reference date

    // We do not want scientific notation: 1E4.
    public static final DecimalFormat SECOND_FORMAT =
        new DecimalFormat("0.####################",
                          new DecimalFormatSymbols(Locale.US));
    static {
        SECOND_FORMAT.setParseIntegerOnly(false);
        SECOND_FORMAT.setGroupingUsed(false);
        SECOND_FORMAT.setDecimalSeparatorAlwaysShown(false);
        SECOND_FORMAT.setMinimumFractionDigits(0);
    }

    public static int parseNonNegativeInt(String s, int start, int end)
        throws DateTimeException
    {
        int i = parseInt(s, start, end);
        if (i < 0)
            throw new DateTimeException("syntax error", s);
        return i;
    }

    public static int parseInt(String s, int start, int end)
        throws DateTimeException
    {
        int i;
        try {
            i = Integer.parseInt(s.substring(start, end));
        }
        catch (Exception e) {
            throw new DateTimeException("syntax error", s);
        }
        return i;
    }

    public static double parseNonNegativeDouble(String s, int start, int end)
        throws DateTimeException
    {
        double d = parseDouble(s, start, end);
        if (d < 0)
            throw new DateTimeException("syntax error", s);
        return d;
    }

    public static double parseDouble(String s, int start, int end)
        throws DateTimeException
    {
        double d;

        // JDK1.3: It seems that there is no way to prevent SECOND_FORMAT
        // from parsing 1E-3.

        String chars = s.substring(start, end);
        int charCount = chars.length();
        for (int i = 0; i < charCount; ++i) {
            switch (chars.charAt(i)) {
            case '.':
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                break;
            default:
                throw new DateTimeException("syntax error", s);
            }
        }

        try {
            d = Double.parseDouble(chars);
        }
        catch (Exception e) {
            throw new DateTimeException("syntax error", s);
        }
        return d;
    }

    // Sets the timezone without touching the rest.
    // If minutes == LOCAL, erases the TZ
    public void forceTimeZone(int minutes)
    {
        if (minutes == LOCAL) {
            tzSign = tzHour = tzMinute = 0;
        }
        else {
            tzSign = 1;
            if (minutes < 0) {
                minutes = -minutes;
                tzSign = -1;
            }
            tzHour = (byte) (minutes / 60);
            tzMinute = (byte) (minutes - 60 * tzHour);
        }
    }

    // Returns a signed timezone in minutes, or special value LOCAL
    // 
    public int getTimeZone()
    {
        if (tzSign == 0)
            return LOCAL;
        return tzSign * (tzHour * 60 + tzMinute);
    }

    public int getTimeZoneInSeconds()
    {
        return 60 * (tzSign * (tzHour * 60 + tzMinute));
    }

    public static int parseTimeZone(String s, int pos, int[] tz)
        throws DateTimeException
    {
        int charCount = s.length();
        int tzSign;
        int tzHour;
        int tzMinute;

        boolean parseTZ;
        int tzPos = s.indexOf('Z', pos);
        if (tzPos < 0) {
            tzPos = s.indexOf('+', pos);
            if (tzPos < 0) {
                tzPos = s.indexOf('-', pos);
                if (tzPos < 0) {
                    tzSign = 0;
                    parseTZ = false;
                    pos = tzPos = charCount;
                }
                else {
                    tzSign = -1;
                    parseTZ = true;
                    pos = tzPos + 1;
                }
            }
            else {
                tzSign = 1;
                parseTZ = true;
                pos = tzPos + 1;
            }
        }
        else {
            tzSign = 1;
            parseTZ = false;
            pos = tzPos + 1;
        }

        if (parseTZ) {
            if (pos + 2 >= charCount || s.charAt(pos + 2) != ':')
                throw new DateTimeException("syntax error", s);
            tzHour = parseInt(s, pos, pos + 2);
            pos += 3;

            if (pos + 2 != charCount)
                throw new DateTimeException("syntax error", s);
            tzMinute = parseInt(s, pos, charCount);

            int tza = 60 * tzHour + tzMinute;
            if (tza < -TZMAX || tza > TZMAX)
                throw new DateTimeException(
                                            "invalid timezone value, must be between -PT14H and PT14H",
                                            s);
        }
        else {
            if (pos != charCount)
                throw new DateTimeException("syntax error", s);
            tzHour = tzMinute = 0;
        }

        tz[0] = tzSign;
        tz[1] = tzHour;
        tz[2] = tzMinute;
        return tzPos;
    }

    public static int lastDayOfMonth(int month, int year)
    {
        switch (month) {
        case 1: // January
        case 3: // March
        case 5: // May
        case 7: // July
        case 8: // August
        case 10: // October
        case 12: // December
            return 31;

        case 4: // April
        case 6: // June
        case 9: // September
        case 11: // November
            return 30;

        case 2: // February
            if (year == 0 || isLeapYear(year))
                return 29;
            /* FALLTHROUGH */
        default:
            return 28;
        }
    }

    public static boolean isLeapYear(int year)
    {
        return ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0)));
    }

    public static void appendPadded(int value, int digits, StringBuffer buffer)
    {
        String s = Integer.toString(value);
        int length = s.length();

        while (length < digits) {
            buffer.append('0');
            ++length;
        }

        buffer.append(s);
    }

    public static void appendPadded(double value, int digits,
                                    StringBuffer buffer)
    {
        // We do not want scientific notation: 1E4.
        String s = SECOND_FORMAT.format(value);
        int length = s.indexOf('.');
        if (length < 0)
            length = s.length();

        while (length < digits) {
            buffer.append('0');
            ++length;
        }

        buffer.append(s);
    }

    public static void appendTimeZone(int tzSign, int tzHour, int tzMinute,
                                      StringBuffer buffer)
    {
        if (tzSign == 0)
            return;

        if (tzHour == 0 && tzMinute == 0) {
            buffer.append('Z');
            return;
        }

        buffer.append((tzSign == -1) ? '-' : '+');
        appendPadded(tzHour, 2, buffer);
        buffer.append(':');
        appendPadded(tzMinute, 2, buffer);
    }

    public DateTimeBase add(Duration dur)
        //throws DateTimeException
    {
        DateTimeBase dt = copy();
        if (dur.isDayTime())
            // need to add TZ, otherwise
            dt.setSecondsFromEpoch(getSecondsFromEpoch()
                                   + getTimeZoneInSeconds()
                                   + dur.getTotalSeconds(), getTimeZone());
        else
            dt.addMonths(dur.getTotalMonths());
        return dt;
    }

    public DateTimeBase add(double time)
        //throws DateTimeException
    {
        DateTimeBase dt = copy();
        dt.setSecondsFromEpoch(getSecondsFromEpoch() + getTimeZoneInSeconds()
                               + time, getTimeZone());
        return dt;
    }

    public DateTimeBase subtract(Duration dur)
        //throws DateTimeException
    {
        DateTimeBase dt = copy();
        if (dur.isDayTime()) {
            double secondsFromEpoch = getSecondsFromEpoch();
            dt.setSecondsFromEpoch(secondsFromEpoch + getTimeZoneInSeconds()
                                   - dur.getTotalSeconds(), getTimeZone());
        }
        else
            dt.addMonths(-dur.getTotalMonths());
        return dt;
    }

    public DateTimeBase copy()
    {
        throw Util.unimplemented();
    }
}
