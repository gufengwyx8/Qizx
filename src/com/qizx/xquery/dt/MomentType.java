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
package com.qizx.xquery.dt;

import com.qizx.api.EvaluationException;
import com.qizx.api.util.time.Date;
import com.qizx.api.util.time.DateTime;
import com.qizx.api.util.time.Time;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.op.Expression;

import java.util.Calendar;

public class MomentType extends AtomicType
{
    public String getShortName()
    {
        return "moment";
    }

    public XQValue convertFromObject(Object object) throws XQTypeException
    {
        if(object instanceof Calendar) {
            object = ((Calendar) object).getTime();
        }
        if(object instanceof java.util.Date) {
            java.util.Date date = (java.util.Date) object;
            return new SingleMoment(DateTimeBase.fromDate(date, 0), this);
        }
        else if(object instanceof DateTimeBase) {
            DateTimeBase date = (DateTimeBase) object;
            return new SingleMoment(date, this);
        }
        try {   // parse
            String sv = object.toString();
            if(this instanceof DateType)
                return new SingleMoment(Date.parseDate(sv), this);
            else if(this instanceof TimeType)
                return new SingleMoment(Time.parseTime(sv), this);
            else
                return new SingleMoment(DateTime.parseDateTime(sv), this);
        }
        catch (DateTimeException e) {
            return invalidObject(object);
        }
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        XQItem v = expr.evalAsItem(focus, context);
        DateTimeBase dt = ((MomentValue) v).getValue();
        return new Date((long) (1000 * dt.getSecondsFromEpoch()));
    }
}
