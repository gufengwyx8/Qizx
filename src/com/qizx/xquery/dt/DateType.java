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
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

import java.util.Calendar;

public class DateType extends MomentType
{

    public String getShortName()
    {
        return "date";
    }

    public int quickCode()
    {
        return QT_DATE;
    }

    public XQValue cast(XQItem item, EvalContext context)
        throws EvaluationException
    {
        XQItemType type = item.getItemType();
        DateTime result = null;
        try {
            switch (type.quickCode()) {
            case XQType.QT_STRING:
            case XQType.QT_UNTYPED:
                return SingleMoment.date(item.getDate());
            case XQType.QT_DATE:
            case XQType.QT_DATETIME:
                return SingleMoment.date(new Date(item.getMoment()));
            case XQType.QT_INT:
            case XQType.QT_DEC:
            case XQType.QT_FLOAT:
            case XQType.QT_DOUBLE: // extension
                if (!context.sObs()) {
                    double s = item.getDouble();
                    return SingleMoment.date(new Date((long) (s * 1000)));
                }
                // FALL THROUGH
            default:
                invalidCast(type);
            }
        }
        catch (DateTimeException e) {
            castException(e);
        }
        return new SingleMoment(result, XQType.DATE);
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
        else try {
            return new SingleMoment(Date.parseDate(object.toString()), this);
        }
        catch (DateTimeException e) {
            return invalidObject(object);
        }
    }
}
