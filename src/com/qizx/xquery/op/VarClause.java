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
import com.qizx.xquery.*;
import com.qizx.xquery.dt.GenericValue;

/**
 * Superclass of Let For Case clauses.
 */
public class VarClause extends Expression
{
    public QName variable;
    public XQType declaredType; // check type
    public Expression expr; // source: in or :=

    public LocalVariable varDecl;
    public Expression owner; // FLWR, quantifiedExpr, typeswitch
    public boolean checked = false;
    public boolean updating;
    
    public VarClause(QName variable)
    {
        this.variable = variable;
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? expr : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("variable", variable);
        d.property("varAddress", varDecl.address);
        d.property("declType", declaredType);
        d.child("expr", expr);
    }

    // dummy sequence used as source for FLWR pipeline
    // Produces ONE dummy item.
    // also root class of For and Let sequences
    public static class SingleDummy extends GenericValue
    {
        XQValue source; // outer clause
        EvalContext context;
        Focus focus;
        boolean done = false;


        public SingleDummy(Focus focus, EvalContext context)
        {
            this.focus = focus;
            this.context = context;
        }

        void setSource(XQValue source)
        {
            this.source = source;
        }

        // single shot sequence
        public boolean next()
            throws EvaluationException
        {
            if (done)
                return false;
            return done = true;
        }

        public XQValue bornAgain()
        {
            return new SingleDummy(focus, context);
        }
    }
}
