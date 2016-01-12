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
package com.qizx.api.util.text;

import com.qizx.api.DataModelException;
import com.qizx.api.Indexing.DateSieve;
import com.qizx.util.basic.IntSet;
import com.qizx.util.basic.Util;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * DateSieve implementation: uses a DateFormat (by default the short format of
 * the default locale.)
 */
public class FormatDateSieve extends SieveBase
    implements DateSieve
{    
    /**
     * Name of the parameter used for specifying the format of recognized date
     * values. 
     * The value of the parameter must be accepted by {@link java.text.DateFormat}.
     */
    public static final String FORMAT = "format";
    /**
     * Name of the parameter used for specifying whether the parsing of date values
     * is lenient.
     * The value of the parameter must be "true" or "yes" or "false" or "no"
     */
    public static final String LENIENT = "lenient";
    /**
     * Name of the parameter used for specifying the default time-zone of parsed
     * dates. The value must be suitable for {@link TimeZone#getTimeZone(String)}.
     */
    public static final String TIMEZONE = "timezone";
    /**
     * Name of the parameter used for specifying the locale of parsed
     * dates. The value of this parameter can be of different forms (the dash 
     * separator can also be replaced by an underscore):<ul>
     * <li><em>language</em> (ISO 639, accepted by {@link Locale})
     * <li><em>language-country</em>, for example en-US
     * <li><em>language-country-variant</em>, for example en-US-POSIX. The 
     * variant value is generally "WIN", "MAC" or "POSIX".
     * </ul>
     */
    public static final String LOCALE = "locale";

    
    private String formatSrc; // debug
    private DateFormat format;

    private transient ParsePosition position = new ParsePosition(0);
    private transient IntSet badFirstChars = null; // accelerator
    private boolean lenient;

    /**
     * Constructs a sieve that uses the short format of the default locale.
     */
    public FormatDateSieve()
    {
        this.format = DateFormat.getDateInstance(DateFormat.SHORT);
        formatSrc = "<default>";
        this.format.setLenient(false);
        // System.err.println("format "+format.getTimeZone()+"
        // "+format.format(new Date()));
    }

    public void setParameters(String[] parameters)
        throws DataModelException
    {
        this.parameters = parameters;
        TimeZone tz = null;
        Locale locale = null;
        for (int i = 0; i < parameters.length; i += 2) {
            String paramName = parameters[i];
            String value = parameters[i + 1];
            if (FORMAT.equals(paramName)) {
                formatSrc = value;
            }
            else if (TIMEZONE.equals(paramName)) {
                tz = TimeZone.getTimeZone(value);
            }
            else if (LOCALE.equals(paramName)) {
                locale = Util.getLocale(value);
            }
            else if (LENIENT.equals(paramName)) {
                lenient = ("true".equalsIgnoreCase(value)
                          || "yes".equalsIgnoreCase(value));
            }
            else
                throw new DataModelException("invalid parameter " + paramName);
        }
        
        if(formatSrc == null)
            throw new DataModelException("format parameter required");
        setFormat(formatSrc, locale);
        if (tz != null)
            format.setTimeZone(tz);
        format.setLenient(lenient);
    }

    /**
     * Sets the format used. Convenience method equivalent to
     * setting parameter "format".
     * @param dateFormat a format accepted by {@link SimpleDateFormat}
     * @param locale if not null, the format is adapted to this locale
     */
    public void setFormat(String dateFormat, Locale locale)
    {
        formatSrc = dateFormat;
        format = (locale == null) ? new SimpleDateFormat(formatSrc)
                                  : new SimpleDateFormat(formatSrc, locale);
        this.format.setLenient(false);
        addParameter("format", dateFormat);
    }

    /**
     * Returns the currently used Date Format.
     * @return the currently used Date Format.
     */
    public String getFormat()
    {
        return (format instanceof SimpleDateFormat)?
                ((SimpleDateFormat) format).toPattern() : "";
    }
    
    public synchronized double convert(String value)
    {
        // boost: reject if first char is in a set
        if (badFirstChars != null && badFirstChars.test(value.charAt(0)))
            return Double.NaN;
        position.setIndex(0);
        Date d = format.parse(value, position);
        if (d == null || position.getIndex() == 0) {
            if (position.getErrorIndex() == 0) { // rejected at
                if (badFirstChars == null)
                    badFirstChars = new IntSet();
                badFirstChars.add(value.charAt(0));
            }
            return Double.NaN;
        }
        else if (!lenient && position.getIndex() != value.length())
            return Double.NaN;
        return d.getTime();
    }

}
