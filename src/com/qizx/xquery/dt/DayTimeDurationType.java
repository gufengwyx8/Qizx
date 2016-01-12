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
import com.qizx.api.QName;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.api.util.time.Duration;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

public class DayTimeDurationType extends DurationType
{
    public QName getName()
    {
        return IQName.get(NamespaceContext.XSD, getShortName());
    }

    public String toString()
    {
        return "xs:dayTimeDuration";
    }

    public String getShortName()
    {
        return "dayTimeDuration";
    }

    public int quickCode()
    {
        return QT_DTDUR;
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws EvaluationException
    {
        XQItemType type = value.getItemType();
        try {

            switch (type.quickCode()) {
            case XQType.QT_STRING:
            case XQType.QT_UNTYPED:
                return SingleDuration.newDT(Duration.parseDuration(value
                .getString().trim()));
            case XQType.QT_DUR:
            case XQType.QT_YMDUR:
                double s = value.getDuration().getTotalSeconds();
                return SingleDuration.newDT(Duration.newDayTime(s));
            case XQType.QT_DTDUR:
                return SingleDuration.newDT(value.getDuration().copy());
            case XQType.QT_INT:
            case XQType.QT_DEC:
            case XQType.QT_FLOAT:
            case XQType.QT_DOUBLE: // extension
                if (context != null && !context.sObs())
                    return SingleDuration.newDT(new Duration(0, value
                    .getDouble()));
                // FALL THROUGH:
            default:
                invalidCast(type);
            }
        }
        catch (DateTimeException e) {
            castException(e);
        }
        return null; // dummy
    }
}
