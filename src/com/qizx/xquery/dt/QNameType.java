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

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

public class QNameType extends AtomicType
{
    public String getShortName()
    {
        return "QName";
    }

    public int quickCode()
    {
        return XQType.QT_QNAME;
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws XQTypeException
    {
        QName result = null;
        String name = null, prefix = null;
        try {
            XQItemType type = value.getItemType();
            switch (type.quickCode()) {
            case XQType.QT_STRING:
                // case XQType.QT_UNTYPED:
                name = value.getString().trim();
                // in constructors: NS must be visible here
                result = context.getInScopeNS().expandName(name);
                prefix = IQName.extractPrefix(name);
                if ("".equals(prefix))
                    prefix = null;
                break;
            case XQType.QT_QNAME:
                QName qv = value.getQName();
                result = qv;
                prefix = qv.getPrefix();
                break;
            default:
                invalidCast(this);
            }
        }
        catch (DataModelException e) {
            throw new XQTypeException("cannot cast to xs:QName: "
                                      + e.getMessage());
        }
        catch (EvaluationException e) {
            castException(e);
        }
        if (result == null)
            throw new XQTypeException("cannot cast " + name
                                      + " to xs:QName : unknown prefix " + prefix);
        return new SingleQName(prefix == null? (QName) result : XQName.get(result, prefix));
    }

    public XQValue convertFromObject(Object object)
        throws XQTypeException
    {
        if(object instanceof QName)
            return new SingleQName((QName) object);
        if(object == null)
            return XQValue.empty; // oops?
        String s = object.toString();
        if(!XMLUtil.isNCName(s))
            invalidCast(this);
        return new SingleQName(IQName.get(s));
    }
    
    
}
