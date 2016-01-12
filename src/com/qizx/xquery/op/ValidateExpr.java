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
import com.qizx.api.QName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

public class ValidateExpr extends Expression
{

    public final static int LAX_MODE = 1, STRICT_MODE = 2, SKIP_MODE = 3;

    public QName schemaContext;
    public Expression expr;
    public int mode;

    public ValidateExpr(int mode, QName type, Expression expr)
    {
        this.mode = mode;
        this.schemaContext = type;
        this.expr = expr;
    }

    public Expression child(int rank)
    {
        return rank == 0 ? expr : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("mode", "" + mode);
        d.property("type", schemaContext);
        d.child("expr", expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        context.error("XQST0075", this, "validate is not supported");
        type = XQType.NODE.star;
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        throw new RuntimeException("ValidateExpr not implemented");
    }
}
