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
import com.qizx.util.DateTimeParser;

/**
 * Default DateSieve implementation: recognizes ISO date and dateTime.
 * <p>
 * Recognized patterns: YYYY-MM-DD[TZ] and YYYY-MM-DDThh:mm:ss.fff[TZ], where
 * TZ is an optional time-zone specification.
 */
public class ISODateSieve extends DateTimeParser
    implements DateSieve
{
    private String[] parameters;

    /**
     * Returns a number of milliseconds since 1970-01-01 00:00:00 UTC.
     * @param value a possible date to convert.
     */
    public synchronized double convert(String value)
    {
        init(value);
        int len = value.length();

        // Quick pre-tests:
        if (len < 10)
            return fail();
        char c = value.charAt(0);
        if (c < '0' || c > '9' || value.indexOf('-') < 0)
            return fail();

        reset();
        if (!parseDate())
            return fail();

        // optional time:
        if (pick('T')) {
            if (!parseTime())
                return fail();
        }

        // optional timezone
        parseTimezone();
        return getMillisecondsFromEpoch();
    }

    private double fail()
    {
        return Double.NaN;
    }

    // @see com.qizx.api.Indexing.Sieve#getParameters()
    public String[] getParameters()
    {
        return parameters;
    }

    /** 
     * @see com.qizx.api.Indexing.Sieve#setParameters(java.lang.String[]) */
    public void setParameters(String[] parameters)
        throws DataModelException
    {
        this.parameters = parameters;
        if (parameters.length > 0)
            throw new DataModelException("invalid sieve parameter '"
                                         + parameters[0] + "'");
    }

    public String toString()
    {
        return SieveBase.toString(getClass(), parameters);
    }
}
