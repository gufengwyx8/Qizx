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

import com.qizx.util.basic.XMLUtil;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

public class NCNameType extends NameType
{    
    public String getShortName() {
        return "NCName";
    }
    
    protected boolean checkValue( String value )
    {
        return XMLUtil.isNCName(value);
    }

    public XQValue convertFromObject(Object object)
        throws XQTypeException
    {
        if (object == null)
            return XQValue.empty;
        String s = object.toString();
        if(XMLUtil.isNCName(s))
            return new SingleString(s, this);
        throw new XQTypeException("invalid value for NCName");
    }
}
