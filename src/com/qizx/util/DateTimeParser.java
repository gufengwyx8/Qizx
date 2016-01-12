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
package com.qizx.util;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Date Time parser superclass.
 * TODO Use in util.time.
 */
public class DateTimeParser extends StringParser
    implements java.io.Serializable
{
    // shared calendar for conversion works: 
    // locking is much cheaper than allocating heaps of Calendar objects.
    static Calendar workCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /*package*/ int year;   // Can be negative but not null.
    /*package*/ byte month;
    /*package*/ byte day;
    /*package*/ byte hour;
    /*package*/ byte minute;
    /*package*/ double second;
    /*package*/ byte tzSign;    // -1|0|1; 0 means no time zone ==> local time.
    /*package*/ byte tzHour;
    /*package*/ byte tzMinute;

    public void reset()
    {
        year = 0;
        month = day = hour = minute = 0;
        second = 0;
        tzSign = tzHour = tzMinute;
    }

    // Parses a sequence YYYY-MM-DD from current position.
    public boolean parseDate()
    {
        if (!parseDec(4))
            return false;
        year = latestInt;
        if (!pick('-') || !parseDec(2))
            return false;
        month = (byte) latestInt;
        if (!pick('-') || !parseDec(2))
            return false;
        day = (byte) latestInt;
        return true;
    }
    
    public boolean parseTime()
    {
        if (!parseDec(2))
            return false;
        hour = (byte) latestInt;
        if (!pick(':') || !parseDec(2))
            return false;
        minute = (byte) latestInt;
        if (!pick(':') || !parseDec(2))
            return false;
        second = latestInt;
        return true;
    }

    public boolean parseTimezone()
    {
        tzSign = 0;
        if (pick('Z'))
            return true;
        if (pick('-'))
            tzSign = -1;
        else if (pick('+'))
            tzSign = 1;
        else
            return false;
        if (!parseDec(2))
            return false;
        tzHour = (byte) latestInt;
        if (!pick(':') || !parseDec(2))
            return false;
        tzMinute = (byte) latestInt;
        return true;
    }

    /**
     * Returns the number of milliseconds from Java epoch
     * (1970-01-01T00:00:00Z).
     */
    public long getMillisecondsFromEpoch()
    {
        synchronized (workCal) {
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
                workCal.add(Calendar.HOUR_OF_DAY,
                            tzSign > 0 ? -tzHour : tzHour);
                workCal.add(Calendar.MINUTE,
                            tzSign > 0 ? -tzMinute : tzMinute);
            }
            else
                workCal.set(Calendar.ZONE_OFFSET, 0);
            // let the calendar work:
            long millis =
                workCal.getTimeInMillis() + (int) (1000 * (second - intsec));
            return millis;
        }
    }

}
