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
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.impl.ErrorValue;

public class TypeswitchExpr extends Expression
{
    public Expression switche;
    TypeCaseClause[] caseClauses;
    boolean updating;

    public TypeswitchExpr(Expression on)
    {
        this.switche = on;
    }

    public void addCaseClause(TypeCaseClause clause)
    {
        if (caseClauses == null)
            caseClauses = new TypeCaseClause[] { clause };
        else {
            TypeCaseClause[] old = caseClauses;
            caseClauses = new TypeCaseClause[old.length + 1];
            System.arraycopy(old, 0, caseClauses, 0, old.length);
            caseClauses[old.length] = clause;
        }
    }

    TypeCaseClause getCaseClause(int rank)
    {
        return rank < 0 || rank >= caseClauses.length
            ? null : caseClauses[rank];
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? switche : (rank <= caseClauses.length)
            ? caseClauses[rank - 1] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("on", switche);
        d.children(caseClauses);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        int nonUpCount = 0;
        switche = context.staticCheck(switche, 0);
        
        type = null;
        for (int c = 0, C = caseClauses.length; c < C; c++)
        {
            context.simpleStaticCheck(caseClauses[c], 0);
            if (type == null)
                type = caseClauses[c].getType();
            else
                type = type.unionWith(caseClauses[c].getType(), true);
            
            if(caseClauses[c].updating)
                updating = true;
            else if(!UpdatingExpr.isVacuous(caseClauses[c].expr))
                ++ nonUpCount;
        }
        // TODO: detect switch on single item -> optimizable
        // TODO: optimize by hashing, grouping by kind etc...
        
        if(updating && (nonUpCount > 0))
            context.error("XUST0001", this, "mix of updating and non-updating "
                          + "expressions in typeswitch");
        return this;
    }
    
    public boolean isVacuous()
    {
        for (int c = 0, C = caseClauses.length; c < C; c++) {
            if(!UpdatingExpr.isVacuous(caseClauses[c].expr))
                return false;
        }
        return true;
    }

    public int getFlags()
    {
        return updating ? UPDATING : 0;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue value = switche.eval(focus, context);
        context.at(this);
        ArraySequence seq = new ArraySequence(8, value);
        XQItem singleItem = null;
        for (; value.next();)
            seq.addItem(singleItem = value.getItem());
        seq.pack();

        // optimize if exactly one item: the occurence will always match
        boolean oneItem = seq.getSize() == 1;

        // brute force implementation: TODO
        for (int c = 0, C = caseClauses.length; c < C; c++) {
            TypeCaseClause cc = caseClauses[c];
            XQType testType = cc.declaredType;
            boolean ok;
            if (oneItem)
                ok = testType.acceptsItem(singleItem);
            else {
                seq.reset();
                ok = !(seq.checkTypeExpand(testType, context, false, false)
                       instanceof ErrorValue);
            }
            if (ok) {
                seq.reset();
                if (cc.variable != null)
                    context.storeLocal(cc.varDecl.address, seq,
                                       false/* current */, null); // already
                                                                // checked
                return cc.expr.eval(focus, context);
            }
        }
        throw new RuntimeException("Typeswitch default failed"); // should
                                                                    // not
                                                                    // happen
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        XQValue value = switche.eval(focus, context);
        context.at(this);
        ArraySequence seq = new ArraySequence(8, value);
        XQItem singleItem = null;
        for (; value.next();)
            seq.addItem(singleItem = value.getItem());
        seq.pack();

        // optimize if exactly one item: the occurence will always match
        boolean oneItem = seq.getSize() == 1;

        // brute force implementation: TODO
        for (int c = 0, C = caseClauses.length; c < C; c++) {
            TypeCaseClause cc = caseClauses[c];
            XQType testType = cc.declaredType;
            boolean ok;
            if (oneItem)
                ok = testType.acceptsItem(singleItem);
            else {
                seq.reset();
                ok =
                    !(seq.checkTypeExpand(testType, context, false, false) instanceof ErrorValue);
            }
            if (ok) {
                seq.reset();
                if (cc.variable != null)
                    context.storeLocal(cc.varDecl.address, seq,
                                       false/* current */, null); // already
                                                                // checked
                cc.expr.evalAsEvents(output, focus, context);
                break;
            }
        }
    }
}
