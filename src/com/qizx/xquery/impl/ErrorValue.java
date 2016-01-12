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
package com.qizx.xquery.impl;

import com.qizx.api.EvaluationException;
import com.qizx.api.ItemType;
import com.qizx.api.QizxException;
import com.qizx.api.XMLPullStream;
import com.qizx.api.XMLPushStream;
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;


/**
 *	Special values (always empty).
 *  Used to represent type matching errors.
 */
public class ErrorValue extends BaseValue
{
    // when used as error:
    private String reason;
    
    public ErrorValue(String reason) {
        this.reason = reason;
    }
    
    public ItemType  getType() {
        return XQType.ATOM;
    }
    
    public boolean next() {
        return false;	// by essence
    }
    
    public XQValue  bornAgain() {
        return this;
    }
    
    public boolean  isNode() {
        return false;	// should not be called
    }
    
    public XQItem getItem() {
        return null;	// whatever: should not be called
    }
    
    public XQItem asAtom() {
        return getItem();	// should not be called
    }
    
    public int compareTo( XQItem that, ComparisonContext context, int flags) {
        return Comparison.ERROR;
    }

    public String getReason()
    {
        return reason;
    }

    public void export(XMLPushStream writer)
        throws EvaluationException
    {
         throw new EvaluationException("invalid export");
    }

    public XMLPullStream exportNode()
    {
        return null;
    }

    public Object getObject()
        throws QizxException
    {
        throw new QizxException("invalid value");
    }
} 
