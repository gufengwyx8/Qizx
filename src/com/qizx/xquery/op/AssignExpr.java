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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQValue;

public class AssignExpr extends VarClause
{
    private GlobalVariable global;

    public AssignExpr(QName variable, Expression expr)
    {
        super(variable);
        this.expr = expr;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("variable", "$" + variable + " : " + declaredType
                   + (d.isPretty() ? "" : " addr "
                       + (varDecl != null ? ("" + varDecl.address) : "?")));
        d.child(expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        // FIXME what if assigning a var in outer closure?
        varDecl = context.lookforLocalVariable(variable);
        if(varDecl != null) {
            varDecl.offset = this.offset;
            // TODO check variable declaration: assignable etc
        }
        else { // is it a global ?
            global = context.lookforGlobalVariable(variable);
            if(global == null)
                context.error("XPST0008", this,
                              "variable " + context.prefixedName(variable) + " not declared");
        }
        
        expr = context.staticCheck(expr, 0);

        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        if(varDecl != null) {
            context.storeLocal(varDecl, expr, null, false, focus, context);
        }
        else {
            XQValue value = expr.eval(focus, context);
            context.setGlobal(global, value);
        }
        return XQValue.empty;
    }
}
