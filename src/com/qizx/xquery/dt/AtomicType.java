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
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

import java.io.ObjectStreamException;

/**
 * Root of all atomic types. Undocumented internal class.
 */
public class AtomicType extends XQItemType
{
    public AtomicType()
    {
    }

    public String getShortName()
    {
        return "anyAtomicType";
    }

    public QName getName()
    {
        return IQName.get(NamespaceContext.XSD, getShortName());
    }

    // Tries to intern on deserialization
    public Object readResolve()
        throws ObjectStreamException
    {
        XQType typ = XQType.findItemType(getName());
        return typ != null ? typ : this;
    }

    public boolean promotable(XQItemType type)
    {
        return type == XQType.UNTYPED_ATOMIC;
    }

    protected void castException(Exception e, String xqueryError)
        throws EvaluationException
    {
        EvaluationException ex =
            new EvaluationException("cannot cast to xs:" + getShortName() + ": "
                                + e.getMessage());
        ex.setErrorCode(ModuleContext.xqueryErrorCode(xqueryError));
        throw ex;
    }
    
    protected XQValue invalidObject(Object object) throws XQTypeException
    {
        throw new XQTypeException("invalid value " + object + " for type " + this);
    }
}
