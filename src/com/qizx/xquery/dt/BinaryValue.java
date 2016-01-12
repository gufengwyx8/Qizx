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

import com.qizx.api.QizxException;
import com.qizx.util.Binary;
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;

/**
 * Abstract Binary value (hex & base64).
 */
public abstract class BinaryValue extends BaseValue
{
    public XQItem getItem()
    {
        return this;
    }

    public abstract Binary getValue();

    public String getString()
        throws XQTypeException
    {
        return itemType == XQType.HEX_BINARY
                            ? getValue().toHexString()
                            : getValue().toBase64String();
    }

    public Object getObject()
        throws QizxException
    {
        return getValue();
    }

    public boolean equals(Object that)
    {
        if (!(that instanceof BinaryValue))
            return false;
        return getValue().equals(((BinaryValue) that).getValue());
    }

    public int hashCode()
    {
        return getValue().hashCode();
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
    {
        if (!(that instanceof BinaryValue))
            return Comparison.ERROR;
        // no order:
        return getValue().equals(((BinaryValue) that).getValue())
                             ? Comparison.EQ
                             : Comparison.FAIL;
    }
}
