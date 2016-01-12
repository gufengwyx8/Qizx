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
import com.qizx.util.Binary;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;


public class HexBinaryType extends BinaryType
{    
    public String getShortName() {
        return "hexBinary";
    }
    
    public XQValue cast( XQItem value, EvalContext context )
        throws EvaluationException
    {
        XQItemType type = value.getItemType();
        Binary result = null;
        try {
            if(type instanceof StringType)
                result = Binary.parseHexBinary( value.getString().trim() );
            else if(type instanceof BinaryType)
                result = ((BinaryValue) value).getValue();
            else
                invalidCast(type);
        }
        catch (Binary.Exception e) {
            castException(e);
        }
        return new SingleBinary( result, XQType.HEX_BINARY );
    }

    @Override
    public XQValue convertFromObject(Object object)
        throws XQTypeException
    {
        if(object instanceof byte[])
            return new SingleBinary( new Binary((byte[]) object), XQType.HEX_BINARY );
        if(object instanceof String)
            return new SingleBinary(Binary.parseHexBinary((String) object), XQType.HEX_BINARY );
        return super.convertFromObject(object); // fails
    }
}
