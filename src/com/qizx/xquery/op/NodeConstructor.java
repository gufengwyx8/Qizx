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
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleNode;

/**
 * class NodeConstructor:
 */
public abstract class NodeConstructor extends Expression
{
    public final static int DIRECT = 0x200;
    
    public Expression[] contents;
    protected int flags;
    
    public void addItem(Expression item)
    {
        contents = addExpr(contents, item);
    }

    public Expression child(int rank)
    {
        return rank < contents.length ? contents[rank] : null;
    }

    public Expression addChild(Expression child, QName name)
    {
        addItem(child);
        return child;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
    }

    public boolean resolvesToConstructor()
    {
        return true;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        for (int e = 0, E = contents.length; e < E; e++) {
            contents[e] = context.staticCheck(contents[e], 0);
            if(UpdatingExpr.isUpdating(contents[e]))
                this.flags |= UPDATING;
        }
        return this;
    }

    public boolean isConstant()
    {
        for (int e = 0, E = contents.length; e < E; e++)
            if(!contents[e].isConstant())
                return false;
        return true;
    }

    public void setDirect() {
        flags |= DIRECT;
    }
    
    public int getFlags()
    {
        return flags;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        BasicNode node = evalAsNode(focus, context);
        return (node == null) ? XQValue.empty : new SingleNode(node);
    }
}
