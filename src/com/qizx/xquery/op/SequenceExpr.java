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
import com.qizx.api.Item;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.GenericValue;

/**
 * class SequenceExpr:
 */
public class SequenceExpr extends Expression
{
    Expression[] exprs = new Expression[0];
    boolean updating;
    
    public SequenceExpr()
    {
    }

    public void addExpr(Expression expr)
    {
        exprs = addExpr(exprs, expr);
    }

    Expression getExpr(int rank)
    {
        return rank < 0 || rank >= exprs.length ? null : exprs[rank];
    }

    public Expression child(int rank)
    {
        return (rank < exprs.length) ? exprs[rank] : null;
    }

    public int getFlags()
    {
        return updating ? UPDATING : 0;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.children("exprs", exprs);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        int nupCount = 0;
        type = exprs.length == 0 ? XQType.NONE.opt : null;
        for (int e = 0, E = exprs.length; e < E; e++) {
            Expression subx = exprs[e] = context.simpleStaticCheck(exprs[e], 0);
            if (type == null) {
                type = subx.getType();
            }
            else {
                // System.err.println("union "+type+" avec
                // "+exprs[e].getType());
                type = type.unionWith(subx.getType(), false);
                // System.err.println("donne "+type);
            }
            if (type == null)
                context.error("(BUG)", subx, "no union type found");
            
            if(UpdatingExpr.isUpdating(subx))
                updating = true;
            else if(!UpdatingExpr.isVacuous(subx))
                ++ nupCount;
        }

        // if updating, only updating expressions, or empty or error()
        if(updating && nupCount > 0)
            context.error("XUST0001", this, "mix of updating and non-updating "
                          + "expressions in Comma expression");
        return this;
    }

    public boolean isVacuous()
    {
        if(exprs == null || exprs.length == 0)
            return true;
        for (int c = 0, C = exprs.length; c < C; c++) {
            if(!UpdatingExpr.isVacuous(exprs[c]))
                return false;
        }
        return true;
    }
    
    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return new Sequence(focus, context);
    }

    public class Sequence extends GenericValue
    {
        Focus focus;
        EvalContext context;
        XQValue curComp;
        int index;

        Sequence(Focus focus, EvalContext context)
        {
            this.focus = focus;
            this.context = context;
            curComp = XQValue.empty;
            index = -1;
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                if (curComp.next()) {
                    item = curComp.getItem();
                    return true;
                }
                if (++index >= exprs.length)
                    return false;
                curComp = getExpr(index).eval(focus, context);
            }
        }

        public boolean nextCollection()
            throws EvaluationException
        {
            for (;;) {
                if (curComp.nextCollection()) {
                    item = curComp.getItem();
                    return true;
                }
                if (++index >= exprs.length)
                    return false;
                curComp = getExpr(index).eval(focus, context);
            }
        }

        public XQValue bornAgain()
        {
            return new Sequence(focus, context);
        }

        public double getFulltextScore(Item item) throws EvaluationException
        {
            if(item == null)
                item = getNode();
            return curComp.getFulltextScore(item);
        }
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        for (int e = 0, E = exprs.length; e < E; e++) {
            // output.noSpace(); // too brutal
            exprs[e].evalAsEvents(output, focus, context);
        }
    }
}
