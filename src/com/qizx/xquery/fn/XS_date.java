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
import com.qizx.api.util.time.Date;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleMoment;

/**
 *  Implementation of function xs:date.
 */
public class XS_date extends CastFunction
{    
    static Prototype[] protos = { 
            // for CastFunction
        Prototype.xs("date", XQType.DATE, Exec.class)
            .arg("srcval", XQType.ANY_ATOMIC_TYPE),
            // normal proto: not for CastFunction
        Prototype.xs("date", XQType.DATE, ExecM.class)
            .arg("year", XQType.INTEGER)
            .arg("month", XQType.INTEGER)
            .arg("day", XQType.INTEGER)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    // not a CastFunction
    public static class ExecM extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            int year = (int) args[0].evalAsInteger(focus, context);
            int month = (int) args[1].evalAsInteger(focus, context);
            int day = (int) args[2].evalAsInteger(focus, context);
            context.at(this);
            try {
                return new SingleMoment(new Date(year, month, day, 0, 0, 0),
                                        XQType.DATE);
            }
            catch (DateTimeException e) {
                return context.error(ERR_ARGTYPE, this, e.getMessage());
            }
        }
    }
}
