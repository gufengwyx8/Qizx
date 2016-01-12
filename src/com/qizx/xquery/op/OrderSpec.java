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
package com.qizx.xquery.op;

import com.qizx.api.EvaluationException;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQItem;

import java.text.Collator;

/**
 * Stores the information for one 'order by' clause.
 */
public class OrderSpec extends Expression 
    implements ComparisonContext
{
    public Expression key;
    public String collation;
    private Collator collator;
    public boolean descending = false;
    public boolean emptyGreatest = false;

    public OrderSpec(Expression key)
    {
        this.key = key;
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? key : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("descending", "" + descending);
        d.property("emptyGreatest", "" + emptyGreatest);
        d.property("collation", collation);
        d.child("key", key);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        key = context.staticCheck(key, 0);
        type = key.getType();
        collator = context.getCollator(collation);
        if (collator == null && collation != null)
            context.error("XQST0076", key, "unknown collation " + collation);
        return this;
    }

    // null key means empty
    int compare(XQItem key1, XQItem key2, EvalContext context)
        throws EvaluationException
    {
        int cmp = 0;
        if (key1 == null)
            cmp = (key2 == null) ? 0 : emptyGreatest ? 1 : -1;
        else if (key2 == null)
            cmp = emptyGreatest ? -1 : 1;
        else
            cmp = key1.compareTo(key2, this, 0);
        // TODO: deal with NaN
        return descending ? -cmp : cmp;
    }

    public Collator getCollator()
    {
        return collator;
    }

    public int getImplicitTimezone()
    {
        return 0;
    }

    public boolean emptyGreatest()
    {
        return emptyGreatest;
    }
}
