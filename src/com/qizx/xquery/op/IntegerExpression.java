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
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleInteger;
import com.qizx.xquery.impl.EmptyException;

/**
 * Superclass of integer expressions.
 */
public abstract class IntegerExpression extends Expression
{
    // redefined above the abstracted evalAsInteger
    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        return new SingleInteger(evalAsInteger(focus, context));
    }

    public double evalAsDouble(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return evalAsInteger(focus, context);
    }

    public double evalAsOptDouble(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return evalAsOptInteger(focus, context);
    }

    public abstract long evalAsOptInteger(Focus focus, EvalContext context)
        throws EvaluationException;

    public long evalAsInteger(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            return evalAsOptInteger(focus, context);
        }
        catch (EmptyException e) {
            errorEmpty(context);
            return 0;
        }
    }
}
