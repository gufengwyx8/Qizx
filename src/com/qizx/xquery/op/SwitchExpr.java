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
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.impl.ErrorValue;

public class SwitchExpr extends Expression
{
    public Expression switche;
    protected Case[] cases;
    protected boolean updating;

    public SwitchExpr(Expression on)
    {
        this.switche = on;
    }

    public static class Case extends VarClause
    {
        public Expression key;
        
        public Case()
        {
            super(null);
        }

        public void dump(ExprDisplay d)
        {
            d.header(key == null? "default" : "case");
            d.child("key", key);
            d.child("expr", expr);
        }
    }
    
    public void addCase(Case clause)
    {
        if (cases == null)
            cases = new Case[] { clause };
        else {
            Case[] old = cases;
            cases = new Case[old.length + 1];
            System.arraycopy(old, 0, cases, 0, old.length);
            cases[old.length] = clause;
        }
    }

    Case getCaseClause(int rank)
    {
        return rank < 0 || rank >= cases.length
            ? null : cases[rank];
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? switche 
               : (rank <= cases.length)? cases[rank - 1] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("on", switche);
        d.children(cases);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        int nonUpCount = 0;
        switche = context.staticCheck(switche, 0);
        
        type = null;
        for (int c = 0, C = cases.length; c < C; c++)
        {
            context.simpleStaticCheck(cases[c], 0);
            if (type == null)
                type = cases[c].getType();
            else
                type = type.unionWith(cases[c].getType(), true);
            
            if(cases[c].updating)
                updating = true;
            else if(!UpdatingExpr.isVacuous(cases[c].expr))
                ++ nonUpCount;
        }
        
        // OPTIM
        // detect all keys are constants of same type => map
        // fallback: evaluate keys in order and compare
        
        if(updating && (nonUpCount > 0))
            context.error("XUST0001", this, "mix of updating and non-updating "
                          + "expressions in typeswitch");
        return this;
    }
    
    public boolean isVacuous()
    {
        for (int c = 0, C = cases.length; c < C; c++) {
            if(!UpdatingExpr.isVacuous(cases[c].expr))
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
        XQItem value = switche.evalAsItem(focus, context);
        context.at(this);
        
        // default implementation
        int select = 0, bound = cases.length - 1;
        for(; select < bound; select++) {
            XQItem caseValue = cases[select].key.evalAsOptItem(focus, context);
            if (caseValue == null)
                continue;
            int cmp = value.compareTo(caseValue, context, XQItem.COMPAR_VALUE);
            if(cmp == 0)
                break;
        }
        
        Expression res = cases[select].expr;
        while(res == null && select < bound) {
            ++ select;
            res = cases[select].expr;
        }
        return cases[select].expr.eval(focus, context);
    }
}
