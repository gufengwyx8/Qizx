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
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.op.Expression;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

/**
 * External (Java) object wrapped as extension type xdt:object.
 */
public class WrappedObjectType extends AtomicType
{
    Class wrappedClass;

    public WrappedObjectType(Class wrappedClass)
    {
        this.wrappedClass = wrappedClass;
        parent = XQType.ATOM;
    }

    public QName getName()
    {
        return IQName.get(NamespaceContext.XDT, getShortName());
    }

    public String toString()
    {
        String cname = wrappedClass.getName();
        return "xdt:object[" + cname.substring(cname.lastIndexOf('.') + 1)
               + "]";
    }

    public String getShortName()
    {
        return "object";
    }

    // public boolean accepts( XQType valueType )
    // {
    // XQItemType type = valueType.getItemType();
    // if(!(type instanceof WrappedObjectType) || type == XQType.OBJECT)
    // return true;
    // WrappedObjectType wotype = (WrappedObjectType) type;
    // return classe.isAssignableFrom(wotype.classe);
    // }

    public XQValue convertFromObject(Object object)
    {
        if (object instanceof XQItem)
            return new SingleItem((XQItem) object);
        return (object == null) ? XQValue.empty
                                : new SingleWrappedObject(object, itemType());
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        XQItem item = expr.evalAsItem(focus, context);
        if (item instanceof StringValue)
            return item.getString();
        if (item instanceof WrappedObjectValue)
            return ((WrappedObjectValue) item).getObject();
        return item;
    }

    public XQValue convertFromArray(Object object)
    {
        if (object == null)
            return XQValue.empty;
        if (object instanceof Enumeration)
            return new ObjectArraySequence((Enumeration) object, this);
        if (object instanceof ArrayList)
            return new ObjectArraySequence((ArrayList) object, this);
        if (object instanceof Vector)
            return new ObjectArraySequence((Vector) object, this);
        if (object instanceof Object[]) {
            Object[] result = (Object[]) object;
            return new ObjectArraySequence(result, result.length, this);
        }
        return convertFromObject(object);
    }

    public Object convertToArray(XQValue value)
        throws EvaluationException
    {
        return ObjectArraySequence.expand(value, wrappedClass);
    }

    public Class getWrappedClass()
    {
        return wrappedClass == null? Object.class : wrappedClass;
    }
}
