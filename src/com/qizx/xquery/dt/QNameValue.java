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
import com.qizx.api.QizxException;
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;

/**
 * Abstract QName value.
 */
public abstract class QNameValue extends BaseValue
{
    public QNameValue()
    {
        itemType = XQType.QNAME;
    }

    public abstract QName getValue();

    public XQItem getItem()
    {
        return this;
    }

    public QName getQName()
    {
        return getValue();
    }

    public String getString()
    {
        QName name = getValue();
        String prefix = name.getPrefix();
        if (prefix == null)
            return name.toString();
        return prefix + ":" + name.getLocalPart();
    }


    public Object getObject()
        throws QizxException
    {
        return getValue();
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof QNameValue))
            return false;
        return getValue() == ((QNameValue) that).getValue();
    }

    public int hashCode()
    {
        return getValue().hashCode();
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
    {
        try {
            if ((flags & COMPAR_ORDER) != 0)
                return Comparison.ERROR;
            QName q1 = getQName(), q2 = that.getQName();
            return (q1.equals(q2)) ? Comparison.EQ : Comparison.FAIL;
        }
        catch (EvaluationException e) {
            return Comparison.ERROR;
        }
    }
}
