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
import com.qizx.api.util.time.Duration;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleMoment;

/**
 *  Implementation of function fn:adjust-to-timezone.
 */
public class AdjustToTimezone extends Function {
    
    private static Prototype[] protos = { 
        Prototype.fn("adjust-to-timezone", XQType.MOMENT.opt, Exec.class)
            .arg("val", XQType.MOMENT.opt)
            .arg("timezone", XQType.DURATION.opt),
        Prototype.fn("adjust-to-timezone", XQType.MOMENT.opt, Exec.class)
            .arg("val", XQType.MOMENT.opt)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {    
        public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
        {
            context.at(this);
            XQItem v1 = args[0].evalAsOptItem(focus, context);
            if(v1 == null)
                return XQValue.empty;
            DateTimeBase moment = v1.getMoment();
            
            boolean set = false;
            int minutes = DateTimeBase.LOCAL;
            XQItem v2 = null;
            if(args.length < 2)
                minutes = context.getImplicitTimezone();
            else {
                v2 = args[1].evalAsOptItem(focus, context);
                if(v2 != null) {
                    Duration tzone = v2.getDuration();
                    if(!tzone.checkAsTimezone())
                        context.error("FODT0003", args[1],
                                      "invalid timezone value");
                    minutes = (int) (tzone.getTotalSeconds() / 60);
                }
                else set = true; // remove TZ
            }

            DateTimeBase res = moment.copy().adjustToTimezone(minutes, set);
            return new SingleMoment( res, v1.getItemType() );
        }
    }
}
