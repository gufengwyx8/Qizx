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

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xquery.*;


/**
 * The transform expression of XQuery Update.
 */
public class TransformExpr extends Expression
{
    public LetClause[] copies;
    public Expression modify;
    public Expression result;

    static final SequenceType[] ROOT_TYPES = { XQType.NODE.one };

    public Expression staticCheck(ModuleContext context, int flags)
    {
        LocalVariable mark = context.latestLocalVariable();
        
        // for and let clauses, declare local variables
        for (int c = 0; c < copies.length; c++) {
            ((VarClause) copies[c]).owner = this;
            context.staticCheck(copies[c], 0);
            context.staticTyping(copies[c].expr, ROOT_TYPES, 
                                 "XUTY0013", "copied node");            
        }

        // modify clause: updating
        modify = context.simpleStaticCheck(modify, 0);
        if(!modify.isUpdating() && !UpdatingExpr.isVacuous(modify))
            context.error("XUST0002", this,
                          "non updating expression in 'modify'");
        // returned expression:
        result = context.staticCheck(result, 0);
        type = result.getType();
        
        context.popLocalVariables(mark);
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        // create a local update list (zapped after execution)
        Updates pul = context.dynamicContext().pushUpdates();
        pul.setTransformContext(context);
        
        try {
            // evaluate the copy clauses: declare roots in update list
            // and bind values with variables (no actual copy)
            for (int c = 0; c < copies.length; c++) {
                Node value = copies[c].expr.evalAsNode(focus, context);
                
                // ignoble hack for XQUTS: if strict compliance, copy
                if(context.sObs()) {
                    try {
                        CorePushBuilder builder = new CorePushBuilder(null);
                        builder.putNodeCopy(value, context.getStaticContext().getCopyNSMode());
                        value = builder.harvest();
                    }
                    catch (DataModelException e) {
                        ; // should not happen
                    }
                }
                LocalVariable var = copies[c].varDecl;
                context.storeLocalItem(var.address, (XQItem) value);
                pul.addRoot(value);
            }

            // evaluate the modify clause: MUST expand, values are irrelevant 
            XQValue modified = modify.eval(focus, context);
            while (modified.next()) {
                ;
            }

            // apply updates and rebind 'copy' variables with modified values:
            for (int c = 0; c < copies.length; c++) {
                Node value = pul.applyTransformUpdates(c);
                LocalVariable var = copies[c].varDecl;
                context.storeLocalItem(var.address, (XQItem) value);
            }
            // finally evaluate the return clause:        
            return result.eval(focus, context);
        }
        finally {
            context.dynamicContext().popUpdates();
        }
    }

    public Expression child(int rank)
    { 
        int p = rank - copies.length;
        if(p < 0)
            return copies[rank];
        return (p == 0)? modify : (p == 1) ? result : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.children("copies", copies);
        d.child("modify", modify);
        d.child("return", result);
    }
}
