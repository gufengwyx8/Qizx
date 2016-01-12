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
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Prototype;

/**
 * Lambda expression bound to a lexical context.
 */
public class FunctionItem extends BaseValue
{
    public Prototype prototype;
    public EvalContext lexicalContext;
    
    private boolean started = false;    // single item

    public FunctionItem(Prototype prototype, EvalContext context)
    {
        this.prototype = prototype;
        this.lexicalContext = context;
        itemType = new FunctionType(prototype);
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
    {
        return Comparison.ERROR;
    }

    public XQValue bornAgain()
    {
        return new FunctionItem(prototype, lexicalContext);
    }

    public boolean next()
        throws EvaluationException
    {
        return started ? false : (started = true);
    }

    public XQItem getItem()
    {
        return this;
    }
    
    @Override public String getString()
    {
        return toString();
    }
    
    @Override
    public String toString()
    {
        return prototype == null? "function(*)" : prototype.toString();
    }
}
