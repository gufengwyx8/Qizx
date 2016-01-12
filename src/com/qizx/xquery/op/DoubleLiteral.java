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
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;

/**
 * class DoubleLiteral:
 */
public class DoubleLiteral extends DoubleExpression
{
    public double value;

    public DoubleLiteral(double value)
    {
        this.value = value;
        type = XQType.DOUBLE;
    }

    public Expression child(int rank)
    {
        return null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("value", "double " + value);
    }

    public int getFlags()
    {
        return CONSTANT;
    }

    public double evalAsDouble(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return value;
    }
}
