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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.op.Expression;

public class ShortType extends IntType
{
    public String getShortName()
    {
        return "short";
    }

    public long upperBound()
    {
        return Short.MAX_VALUE;
    }

    public long lowerBound()
    {
        return Short.MIN_VALUE;
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        return new Short((short) expr.evalAsInteger(focus, context));
    }

    protected Object convertToObject(long value)
    {
        return new Short((short) value);
    }
    
    public XQValue convertFromArray(Object object)
    {
        short[] result = (short[]) object;
        return new IntegerArraySequence(result, result.length);
    }

    public Object convertToArray(XQValue value)
        throws EvaluationException
    {
        return IntegerArraySequence.expandShorts(value);
    }
}
