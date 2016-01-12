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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.dt.DurationType;
import com.qizx.xquery.dt.MomentType;
import com.qizx.xquery.impl.EmptyException;

/**
 *  Implementation of function fn:get-days (short-cut for get-day-from-***).
 */
public class GetDays extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("get-days", XQType.INTEGER.opt, Exec.class)
        .arg("op", XQType.ITEM.opt)
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
            XQItemType typ = v.getItemType();
            if (typ instanceof DurationType) {
                return v.getDuration().getDays();
            }
            else if (typ instanceof MomentType) {
                return v.getMoment().getDay();
            }
            context.error(ERR_ARGTYPE, this, "invalid argument type");
            return 0; // dummy
        }
    }
}
