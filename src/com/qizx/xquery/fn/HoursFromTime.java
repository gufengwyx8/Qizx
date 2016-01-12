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
import com.qizx.api.util.time.Time;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.dt.MomentValue;
import com.qizx.xquery.impl.EmptyException;

/**
 * Implementation of function fn:hours-from-time.
 */
public class HoursFromTime extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("hours-from-time", XQType.INTEGER.opt, Exec.class).arg("op", XQType.TIME.opt)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.IntegerCall
    {
        public long evalAsInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQItem v = args[0].evalAsOptItem(focus, context);
            if (v == null)
                throw EmptyException.instance();
            if (v instanceof MomentValue) {
                MomentValue mv = (MomentValue) v;
                return mv.getValue().getHour();
            }
            try {
                Time dt = Time.parseTime(v.getString());
                return dt.getHour();
            }
            catch (DateTimeException e) {
                context.error(Conversion.ERR_CAST, this,
                              "cannot cast to xs:time : " + e.getMessage());
                return 0;
            }
        }
    }
}

