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
import com.qizx.xquery.XQValue;

/**
 * The FLOWER expression. Features detection and optimization of joins.
 */
public class BlockExpr extends Expression
{
    public Expression body;     // return expr
    private Expression[] clauses = new Expression[0];

    public BlockExpr()
    {
    }

    public VarClause addClause(LetClause clause)
    {
        clauses = addExpr(clauses, clause);
        return clause;
    }

    LetClause getClause(int rank)
    {
        return rank < 0 || rank >= clauses.length ? null
                : (LetClause) clauses[rank];
    }

    public Expression child(int rank)
    {
        if (rank < clauses.length)
            return clauses[rank];
        rank -= clauses.length;
        if (rank == 0)
            return body;
        return null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.children(clauses);
        d.child("body", body);
    }

    public int getFlags()
    {
        return body.getFlags(); 
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        LocalVariable mark = context.latestLocalVariable();
        
        // for and let clauses, declare local variables
        for (int c = 0; c < clauses.length; c++) {
            context.staticCheck(clauses[c], 0);
        }

        // returned expression:
        body = context.simpleStaticCheck(body, 0);
        type = body.getType().itemType().star;

        context.popLocalVariables(mark);
        return this;
    }

    // -----------------------------------------------------------------------------

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        for (int c = 0; c < clauses.length; c++) {
            LetClause init = getClause(c);
            if(init.expr != null)
                context.storeLocal(init.varDecl, init.expr, null, false,
                                   focus, context);
            else
                context.storeLocal(init.varDecl.address,
                                   XQValue.empty, false, null);
        }

        return body.eval(focus, context);
    }

}
