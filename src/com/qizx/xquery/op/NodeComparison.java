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
import com.qizx.xquery.SequenceType;
import com.qizx.xquery.XQType;

/**
 * class NodeComparison:
 */
public abstract class NodeComparison extends BooleanExpression
{
    private static final SequenceType[] NODE_TYPES = { XQType.NODE.opt };

    public Expression expr1;

    public Expression expr2;

    public NodeComparison(Expression expr1, Expression expr2)
    {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    public void dump(ExprDisplay d)
    {
        d.binary(this, expr1, expr2);
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? expr1 : (rank == 1) ? expr2 : null;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        expr1 = context.staticCheck(expr1, 0);
        context.staticTyping (expr1, NODE_TYPES, "???", "1st operand");
        
        expr2 = context.staticCheck(expr2, 0);
        context.staticTyping (expr2, NODE_TYPES, "???", "2nd operand");

        type = XQType.BOOLEAN;
        return this;
    }
}
