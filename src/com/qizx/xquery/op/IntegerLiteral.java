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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;

public class IntegerLiteral extends IntegerExpression
{
    public long value;

    public IntegerLiteral(long value)
    {
        this.value = value;
        type = XQType.INTEGER;
    }

    public Expression child(int rank)
    {
        return null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("value", toString());
    }

    public String toString()
    {
        return "int " + value;
    }

    public int getFlags()
    {
        return CONSTANT;
    }

    public long evalAsOptInteger(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return value;
    }
}
