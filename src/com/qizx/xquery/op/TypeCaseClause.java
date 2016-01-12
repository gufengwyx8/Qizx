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
 * Representation of a typeswitch case (or default) clause.
 */
public class TypeCaseClause extends VarClause
{
    public TypeCaseClause()
    {
        super(null);
        declaredType = XQType.ANY;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("variable", variable);
        d.property("varAddress", varDecl != null ? varDecl.address : -1);
        d.property("varType", declaredType);
        d.child("return", expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        LocalVariable mark = context.latestLocalVariable();
        if (variable != null) {
            varDecl =
                context.defineLocalVariable(variable, declaredType, this);
            varDecl.storageType(declaredType, context);
        }
        expr = context.simpleStaticCheck(expr, 0);
        
        updating = UpdatingExpr.isUpdating(expr);
        
        if (variable != null)
            context.popLocalVariables(mark);
        type = expr.getType();
        return this;
    }
}
