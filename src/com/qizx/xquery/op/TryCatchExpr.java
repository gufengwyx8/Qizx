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
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.QizxException;
import com.qizx.xdm.BaseNodeFilter;
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.dt.SingleNode;
import com.qizx.xquery.dt.SingleQName;
import com.qizx.xquery.dt.SingleString;

/**
 * XQuery 1.1:
 *   try { expr } [ catch($err) { expr } ]+
 * + compatibility with old Qizx extension: try { expr } catch($err) { expr }
 */
public class TryCatchExpr extends Expression
{
    protected Expression caught;
    protected Catch[] catches;
    protected boolean compatibility; // with Qizx versions until 4.0

    public TryCatchExpr(Expression caught, Catch handler)
    {
        this.caught = caught;
        this.catches = new Catch[] { handler };
    }
    
    // compatibility
    public TryCatchExpr(Expression caught, QName varName, Expression handler)
    {
        compatibility = true;
        this.caught = caught;
        Catch cat = new Catch(null);
        this.catches = new Catch[] { cat };
        cat.handler = handler;
        cat.valueVarName = varName; // different semantics
    }
    
    public void addCatch(Catch handler)
    {
        Catch[] old = catches;
        catches = new Catch[old.length + 1];
        System.arraycopy(old, 0, catches, 0, old.length);
        catches[old.length] = handler;
    }

    public Expression child(int rank)
    {
        rank--;
        return rank < 0 ? caught : rank < catches.length ? catches[rank] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("caught", caught);
        d.children("catches", catches);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        caught = context.staticCheck(caught, 0);
        type = caught.getType();

        for (int i = 0; i < catches.length; i++) {
            Catch cat = catches[i];
            LocalVariable mark = context.latestLocalVariable();
            if(cat.codeVarName != null) {
                cat.codeVar = context.defineLocalVariable(cat.codeVarName,
                                                          XQType.QNAME, this);
                cat.codeVar.storageType(XQType.QNAME, context);
            }
            if(cat.descVarName != null) {
                cat.descVar = context.defineLocalVariable(cat.descVarName,
                                                          XQType.STRING, this);
                cat.descVar.storageType(XQType.STRING, context);
            }
            if(cat.valueVarName != null) {
                cat.valueVar = context.defineLocalVariable(cat.valueVarName,
                                                           XQType.ANY, this);
                cat.valueVar.storageType(XQType.ANY, context);
            }

            cat.handler = context.staticCheck(cat.handler, 0);
            context.popLocalVariables(mark);
            type = type.unionWith(cat.handler.getType(), true);            
        }
        
        
        return this;
    }
    
    public static class Catch extends Expression
    {
        public BaseNodeFilter[] names;
        
        // names and bindings of catch variables
        public QName codeVarName;
        public LocalVariable codeVar;
        public QName descVarName;
        public LocalVariable descVar;
        public QName valueVarName;
        public LocalVariable valueVar;
        
        public Expression handler;

        public Catch(BaseNodeFilter test)
        {
            if(test != null)
                names = new BaseNodeFilter[] { test };
        }
        
        public void addTest(BaseNodeFilter test)
        {
            BaseNodeFilter[] old = names;
            names = new BaseNodeFilter[old.length + 1];
            System.arraycopy(old, 0, names, 0, old.length);
            names[old.length] = test;
        }

        public Expression child(int rank) {
            return rank == 0? handler : null;
        }

        public boolean catches(QName errorCode)
        {
            if(names == null)
                return true;
            for (int i = names.length; --i >= 0; ) {
                if(names[i].accepts(Node.ELEMENT, errorCode))
                    return true;                
            }
            return false;
        }
    }
    

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        try {
            XQValue res = caught.eval(focus, context);
            // it is crucial to expand the sequence here, so that
            // eval errors are actually caught
            ArraySequence expanded = new ArraySequence(4, res);
            for (; res.next();)
                expanded.addItem(res.getItem());
            return expanded;
        }
        catch (EvaluationException e) {
            Catch catcher = null;
            for (int i = 0; i < catches.length; i++) {
                if(catches[i].catches(e.getErrorCode())) {
                    catcher = catches[i];
                    break;
                }                
            }
            
            if(catcher == null)
                throw e;
            if(compatibility) {
                // feature: show error as a node whose name is the error code
                // and whose content the message
                CorePushBuilder builder = new CorePushBuilder("");
                QName errName = IQName.get(e.getErrorCode());
                try {
                    builder.putElementStart(errName);   // cant be attr value
                    builder.putText(e.getMessage());
                    builder.putElementEnd(errName);
                }
                catch (QizxException e1) {
                    ;
                }
                context.storeLocal(catcher.valueVar.address,
                                   new SingleNode(builder.harvest()),
                                   false/* current */, null);
            }
            else { // 1.1
                if(catcher.codeVar != null) {
                    context.storeLocal(catcher.codeVar.address,
                                       new SingleQName(e.getErrorCode()),
                                       false/* current */, null);
                }
                if(catcher.descVar != null) {
                    context.storeLocal(catcher.descVar.address,
                                       new SingleString(e.getMessage()),
                                       false/* current */, null);
                }
                if(catcher.valueVar != null) {
                    XQValue v = e.getValue();
                    if(v == null)
                        v = XQValue.empty;
                    context.storeLocal(catcher.valueVar.address, v,
                                       false/* current */, null);
                }
            }
            // execute the handler with bound variables (if any)
            return catcher.handler.eval(focus, context);
        }
    }
}
