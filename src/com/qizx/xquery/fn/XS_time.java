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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleMoment;

/**
 *  Implementation of function xs:time.
 */
public class XS_time extends CastFunction {
    
    static Prototype[] protos = { 
        Prototype.xs("time", XQType.TIME, Exec.class) .arg("srcval", XQType.ANY_ATOMIC_TYPE),
        Prototype.xs("time", XQType.TIME, ExecM.class)
            .arg("hours", XQType.INTEGER).arg("minutes",XQType.INTEGER).arg("seconds",XQType.INTEGER)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class ExecM extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            int hour = (int) args[0].evalAsInteger(focus, context);
            int minute = (int) args[1].evalAsInteger(focus, context);
            int second = (int) args[2].evalAsInteger(focus, context);
            context.at(this);
            try {
                return new SingleMoment(new Time(hour, minute, second, 0, 0, 0),
                                        XQType.TIME);
            }
            catch (DateTimeException e) {
                context.error(ERR_ARGTYPE, this, e.getMessage());
                return XQValue.empty;
            }
        }
    }
}
