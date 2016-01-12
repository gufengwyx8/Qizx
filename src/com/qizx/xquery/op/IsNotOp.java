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

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleBoolean;

/**
 * Operator 'isnot'.
 */
public class IsNotOp extends NodeComparison
{
    public IsNotOp(Expression expr1, Expression expr2)
    {
        super(expr1, expr2);
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue n1 = expr1.eval(focus, context);
        XQValue n2 = expr2.eval(focus, context);
        try {
            return (!n1.next() || !n2.next())
                ? XQValue.empty
                : new SingleBoolean(n1.getNode().documentOrderCompareTo(n2.getNode()) != 0);
        }
        catch (DataModelException e) {
            throw BasicNode.wrapDMException(e);
        }
    }

    public boolean evalAsBoolean(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue n1 = expr1.eval(focus, context);
        XQValue n2 = expr2.eval(focus, context);
        try {
            return (!n1.next() || !n2.next()) ? false
                : n1.getNode().documentOrderCompareTo(n2.getNode()) != 0;
        }
        catch (DataModelException e) {
            throw BasicNode.wrapDMException(e);
        }
    }
}
