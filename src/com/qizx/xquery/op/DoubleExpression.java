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
import com.qizx.xquery.dt.SingleDouble;

/**
 * Superclass of expressions returning double.
 */
public abstract class DoubleExpression extends Expression
{
    // redefined above the abstracted evalAsDouble
    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return new SingleDouble(evalAsDouble(focus, context));
    }

    public abstract double evalAsDouble(Focus focus, EvalContext context)
        throws EvaluationException;
}
