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

import com.qizx.util.basic.Util;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

import java.net.URI;
import java.net.URISyntaxException;

public class AnyURIType extends AtomicType
{
    public String getShortName()
    {
        return "anyURI";
    }

    public int quickCode()
    {
        return QT_ANYURI;
    }

    public XQValue cast(XQItem value, EvalContext context)
        throws XQTypeException
    {
        String result = null;
        try {
            XQItemType type = value.getItemType();
            int qtype = type.quickCode();
            if (type != XQType.ANYURI && qtype != XQType.QT_UNTYPED
                && qtype != XQType.QT_STRING)
                throw new Exception("improper type " + type);
            result = convertToURI(value.getString().trim());
        }
        catch (Exception e) {
            throw new XQTypeException("cannot cast to xs:anyURI: "
                                      + e.getMessage());
        }
        return new SingleString(result, XQType.ANYURI); // TODO control value
    }

    // Performs no conversion in general, unless some characters
    // need to be escaped.
    public static String convertToURI(String s)
        throws URISyntaxException
    {
        if (validURI(s))
            return s;
        // try to escape characters:
        String e = Util.encodeForURI(s);
        if (validURI(e))
            return s;
        throw new URISyntaxException(s, "invalid URI");
    }

    public static boolean validURI(String s)
    {
        try {
            new URI(s);
            // accepted by java.net.URI: considered ok
            return true;
        }
        catch (URISyntaxException e) {
            return false;
        }
    }
}
