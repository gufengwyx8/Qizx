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
import com.qizx.xquery.XQType;

/**
 * Superclass of node sequence expressions (union, intersect, except)
 */
public class NodeExpression extends Expression
{
    Expression expr1, expr2;

    public NodeExpression(Expression expr1, Expression expr2)
    {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? expr1 : (rank == 1) ? expr2 : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("node1", expr1);
        d.child("node2", expr2);
    }

    public int getFlags()
    {
        return DOCUMENT_ORDER;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        expr1 = context.staticCheck(expr1, 0);
        if ((expr1.getFlags() & DOCUMENT_ORDER) == 0)
            expr1 = new NodeSortExpr(expr1);
        expr2 = context.staticCheck(expr2, 0);
        if ((expr2.getFlags() & DOCUMENT_ORDER) == 0)
            expr2 = new NodeSortExpr(expr2);
        type = XQType.NODE.star;
        return this;
    }
}
