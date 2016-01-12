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

import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.ModuleContext;

public class Unordered extends Expression
{
    public Expression expr;

    public Unordered(Expression expr)
    {
        this.expr = expr;
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? expr : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("name", expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        // bluntly return the argument (ie disappear)
        return context.staticCheck(expr, Expression.UNORDERED);
    }
}
