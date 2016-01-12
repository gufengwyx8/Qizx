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
package com.qizx.xquery.fn;
import com.qizx.api.EvaluationException;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleMoment;

/**
 *  Implementation of function fn:dateTime.
 */
public class DateTime extends Function
{
    static Prototype[] protos = {
        Prototype.fn("dateTime", XQType.DATE_TIME, Exec.class)
         .arg("date", XQType.DATE)
         .arg("time", XQType.TIME)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            int year = 0, month = 0, day = 0, hour = 0, minute = 0;
            int tz = DateTimeBase.LOCAL;
            double second = 0;
            if (args.length == 2) {
                // standard:
                XQItem dateItem = args[0].evalAsOptItem(focus, context);
                if(dateItem == null)
                    return XQValue.empty;
                if (dateItem.getType() != XQType.DATE)
                    context.invalidArgType(args[1], 0, XQType.DATE, "date");
                XQItem timeItem = args[1].evalAsOptItem(focus, context);
                if(timeItem == null)
                    return XQValue.empty;
                if (timeItem.getType() != XQType.TIME)
                    context.invalidArgType(args[1], 1, XQType.TIME, "time");
                DateTimeBase date = dateItem.getMoment();
                DateTimeBase time = timeItem.getMoment();
                year = date.getYear();
                month = date.getMonth();
                day = date.getDay();
                hour = time.getHour();
                minute = time.getMinute();
                second = time.getSecond();
                if (date.hasTimeZone()) {
                    tz = date.getTimeZone();
                    if (time.hasTimeZone() && time.getTimeZone() != tz)
                        context.error("FORG0008", this);
                }
                else if (time.hasTimeZone())
                    tz = time.getTimeZone();
            }
            else {
                year = (int) args[0].evalAsInteger(focus, context);
                month = (int) args[1].evalAsInteger(focus, context);
                day = (int) args[2].evalAsInteger(focus, context);
                hour = (int) args[3].evalAsInteger(focus, context);
                minute = (int) args[4].evalAsInteger(focus, context);
                second = args[5].evalAsDouble(focus, context);
                tz =
                    args.length > 6
                        ? (int) (args[6].evalAsDouble(focus, context) * 60)
                        : context.getImplicitTimezone();
            }
            context.at(this);
            try {
                com.qizx.api.util.time.DateTime res = 
                    new com.qizx.api.util.time.DateTime(year, month, day, hour,
                                                        minute, second, 0, 0, 0);
                res.forceTimeZone(tz);
                return new SingleMoment(res, XQType.DATE_TIME);
            }
            catch (DateTimeException e) {
                context.error("FOER0001", this, e.getMessage());
                return XQValue.empty;
            }
        }
    }
}
