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
import com.qizx.api.util.time.DateTimeException;
import com.qizx.api.util.time.DateTime;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleMoment;

/**
 *  Implementation of function xs:dateTime.
 */
public class XS_dateTime extends CastFunction
{
    static Prototype[] protos = { 
        // Proto for CastFunction
        Prototype.xs("dateTime", XQType.DATE_TIME, Exec.class)
            .arg("src", XQType.ATOM),
        // extensions:
        Prototype.xs("dateTime", XQType.DATE_TIME, ExecEx.class)
            .arg("year", XQType.INTEGER)
            .arg("month", XQType.INTEGER)
            .arg("day", XQType.INTEGER)
            .arg("hours", XQType.INTEGER)
            .arg("minutes",XQType.INTEGER)
            .arg("seconds",XQType.DOUBLE),
        Prototype.xs("dateTime", XQType.DATE_TIME, ExecEx.class)
            .arg("year", XQType.INTEGER)
            .arg("month", XQType.INTEGER)
            .arg("day", XQType.INTEGER)
            .arg("hours", XQType.INTEGER)
            .arg("minutes",XQType.INTEGER)
            .arg("seconds",XQType.DOUBLE)
            .arg("timezone", XQType.INTEGER.opt)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class ExecEx extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            int year = (int) args[0].evalAsInteger(focus, context);
            int month = (int) args[1].evalAsInteger(focus, context);
            int day = (int) args[2].evalAsInteger(focus, context);
            int hour = (int) args[3].evalAsInteger(focus, context);
            int minute = (int) args[4].evalAsInteger(focus, context);
            double second = args[5].evalAsDouble(focus, context);
            // TODO empty TZ -> local time
            int tz = args.length > 6
                        ? (int) (args[6].evalAsInteger(focus, context) * 60)
                        : context.getImplicitTimezone();
            context.at(this);
            try {
                return SingleMoment.dateTime(new DateTime(
                                      year, month, day, hour, minute, second,
                                      tz < 0 ? -1 : 1,
                                      Math.abs(tz) / 60, Math.abs(tz) % 60));
            }
            catch (DateTimeException e) {
                context.error(ERR_ARGTYPE, this, e.getMessage());
                return XQValue.empty;
            }
        }
    }
}
