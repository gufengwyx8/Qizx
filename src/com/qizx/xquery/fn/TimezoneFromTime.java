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
import com.qizx.api.util.time.Duration;
import com.qizx.api.util.time.Time;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.MomentValue;
import com.qizx.xquery.dt.SingleDuration;
import com.qizx.xquery.impl.EmptyException;

/**
 *  Implementation of function fn:timezone-from-time.
 */
public class TimezoneFromTime extends Function
{    
    static Prototype[] protos = { 
            Prototype.fn("timezone-from-time", XQType.DURATION.opt, Exec.class)
                .arg("op", XQType.TIME.opt)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQItem v = args[0].evalAsOptItem(focus, context);
            if (v == null)
                throw EmptyException.instance();
            DateTimeBase dt = null;
            if (v instanceof MomentValue)
                dt = v.getMoment();
            else
                try {
                    dt = Time.parseTime(v.getString());
                }
                catch (DateTimeException e) {
                    return context.error(Conversion.ERR_CAST, this,
                                         "cannot cast to xs:time : "
                                             + e.getMessage());
                }
            try {
                if (!dt.hasTimeZone())
                    return XQValue.empty;
                return SingleDuration.newDT(Duration.newDayTime(dt
                    .getTimeZone() * 60));
            }
            catch (DateTimeException e) {
                return context.error(Conversion.ERR_CAST, this,
                                     "invalid timezone: " + e.getMessage());
            }
        }
    }
}
