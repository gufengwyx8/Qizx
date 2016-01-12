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
import com.qizx.util.Collations;
import com.qizx.util.basic.Comparison;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;

import java.text.Collator;

/**
 *	Base class for String sequences.
 */
public abstract class StringValue extends BaseValue
{
    public StringValue() {
        itemType = XQType.STRING;
    }
    
    public XQItem  getItem() {
        return new SingleString( getValue(), itemType );
    }
    
    protected abstract String getValue();

    public boolean  getBoolean() throws EvaluationException {
        return Conversion.toBoolean(getString());
    }
    
    public long   getInteger() throws EvaluationException {
        return Conversion.toInteger( getString() ); 
    }
    
    public double getDouble() throws EvaluationException {
        return Conversion.toDouble( getString() ); 
    }
    
    public float  getFloat() throws EvaluationException {
        return Conversion.toFloat( getString() ); 
    }
    
    public boolean equals( Object that ) {
        if(! (that instanceof XQItem) )
            return false;
        try {
            return getString().equals( ((XQItem) that).getString() );
        }
        catch (EvaluationException e) { return false; }
    }
    
    public int hashCode() {
        try {
            return getString().hashCode();
        }
        catch (Exception e) { return 0; }	// cannot happen
    }
    
    public static int  compare(String s1, String s2, Collator coll)
    {
        int cmp = (coll == null || coll == Collations.CODEPOINT_COLLATOR)
                ? s1.compareTo(s2)
                : coll.compare(s1, s2);
        return cmp < 0 ? -1 : cmp > 0 ? 1 : 0;
    }
    
    public int compareTo( XQItem that, ComparisonContext context, int flags )
    throws EvaluationException
    {
        XQItemType thatType = (XQItemType) that.getType();
        switch(thatType.quickCode()) {
        case XQType.QT_UNTYPED:
            //return UntypedAtomicType.comparison(this, that, context, flags);
        case XQType.QT_STRING:
        case XQType.QT_ANYURI:
            return compare( getString(), that.getString(),
                            (context != null)? context.getCollator() : null);
        case XQType.QT_INT:
        case XQType.QT_DEC:
        case XQType.QT_FLOAT:
        case XQType.QT_DOUBLE:
        case XQType.QT_BOOL:
            if((flags & COMPAR_VALUE) != 0)
                return Comparison.ERROR;
            return - that.compareTo(this, context, flags);
        default:
            return Comparison.ERROR;
        }
    }
    
} // end of class StringValue

