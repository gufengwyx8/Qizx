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

public abstract class UpdatingExpr extends Expression
{
    public int mode;
    public Expression what;  // inserted nodes, new name, new value
    public Expression where; // 

    public UpdatingExpr()
    {
        super();
    }

    public Expression child(int rank)
    {
        return rank == 0? what : where;
    }

    public int getFlags()
    {
        return UPDATING;
    }

    public static boolean isUpdating(Expression e)
    {
        return e != null && e.isUpdating();
    }

    public static boolean isVacuous(Expression ex)
    {
        return ex != null && ex.isVacuous();
    }

    public XQType getType()
    {
        return super.getType(); // TODO
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("mode", mode);
        if(what != null)
            d.child(what);
        if(where != null)
            d.child(where);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        if(what != null) {
            what = context.staticCheck(what, flags);
        }
        if(where != null) {
            where = context.staticCheck(where, flags);
        }
        return this;
    }
}
