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
import com.qizx.xquery.dt.SingleItem;

/**
 * class VarReference: replaced by VarReference.Global or VarReference.Local
 */
public class VarReference extends LeafExpression
{
    public QName name;

    public VarReference(QName name)
    {
        this.name = name;
    }

    public void dump(ExprDisplay d)
    {
        d.header("var");
        d.property("name", name);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        // is it a true local, or is it in a closure ? (XQ 1.1)
        Local cv = context.lookforClosureVariable(name);
        if(cv != null)
            return cv.atSamePlaceAs(this);
        // is it a global ?
        GlobalVariable global = context.lookforGlobalVariable(name);
        if (global != null)
            return new Global(global).atSamePlaceAs(this);
        
        context.error("XPST0008", this,
                      "variable " + context.prefixedName(name) + " not declared");
        return this;
    }

    static class Global extends VarReference
    {
        GlobalVariable address;

        Global(GlobalVariable address)
        {
            super(address.name);
            this.address = address;
            this.type = address.getType();
        }

        public boolean equals(Object obj)
        {
            if(obj instanceof Global) {
                return address.name.equals(((Global) obj).address.name);
            }
            return false;
        }

        public int hashCode()
        {
            return address.name.hashCode();
        }
        
        public void dump(ExprDisplay d)
        {
            d.header("global");
            d.property("name", address.name);
            d.property("declaredType", address.declaredType);
            d.headerInfo(this);
        }

        public int getFlags()
        {
            return address.getFlags();
            // // if single Node: necessarily at same_depth!
            // return Type.isRepeatable(type.getOccurrence()) ?
            // 0 : (DOCUMENT_ORDER + SAME_DEPTH);
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return context.loadGlobal(address);
        }
    }

    public static class Local extends VarReference
    {
        public LocalVariable decl;  // declaration
        // Normally 0 (variables of immediate contexte), 
        // can be > 0 for a reference to lexical closure for lambda functions
        public int upLevel;         // 

        public Local(LocalVariable decl)
        {
            super(decl.name);
            this.decl = decl;
            type = decl.type;
            decl.use();
        }
        
        Local(QName name) {
            super(name);
        }

        public boolean equals(Object obj)
        {
            if(obj instanceof Local) {
                return decl.equals(((Local) obj).decl);
            }
            return false;
        }

        public int hashCode()
        {
            return decl.hashCode();
        }

        public void dump(ExprDisplay d)
        {
            d.header("local");
            d.property("name", name);
            //d.property("uses", decl.uses);
            if(upLevel > 0)
                d.property("up", upLevel);
            if(decl != null)
                d.property("address", decl.address);
            d.headerInfo(this);
        }

        public int getFlags()
        {
            // if single Node: necessarily at same_depth!
            return XQType.isRepeatable(type.getOccurrence())
                ? 0 : (DOCUMENT_ORDER + SAME_DEPTH);
        }
       
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            // special meaning: a flower var replaced by . in predicates
            if (decl == null) {
                checkFocus(focus, context);
                return new SingleItem(focus.currentItem());
            }
            return context.loadLocal(decl.address);
        }

        public XQItem evalAsItem(Focus focus, EvalContext context)
            throws EvaluationException
        {
            // special meaning: a flower var replaced by . in predicates
            if (decl == null)
                return focus.currentItem();
            EvalContext ctx = (upLevel == 0)? context : context.closure(upLevel);
            return ctx.loadLocalItem(decl.address);
        }

        public XQItem evalAsOptItem(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if (decl == null)
                return focus.currentItem();
            EvalContext ctx = (upLevel == 0)? context : context.closure(upLevel);
            return ctx.loadLocalItem(decl.address);
        }

        public long evalAsInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if (decl == null)
                return focus.currentItemAsInteger();
            EvalContext ctx = (upLevel == 0)? context : context.closure(upLevel);
            return ctx.loadLocalInteger(decl.address);
        }

        public long evalAsOptInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if (decl == null)
                return focus.currentItemAsInteger();
            EvalContext ctx = (upLevel == 0)? context : context.closure(upLevel);
            return ctx.loadLocalInteger(decl.address);
        }

        public double evalAsDouble(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if (decl == null)
                return focus.currentItemAsDouble();
            EvalContext ctx = (upLevel == 0)? context : context.closure(upLevel);
            return ctx.loadLocalDouble(decl.address);
        }

        public double evalAsOptDouble(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if (decl == null)
                return focus.currentItemAsDouble();
            EvalContext ctx = (upLevel == 0)? context : context.closure(upLevel);
            return ctx.loadLocalDouble(decl.address);
        }

        public String evalAsString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if (decl == null)
                return focus.currentItemAsString();
            EvalContext ctx = (upLevel == 0)? context : context.closure(upLevel);
            return ctx.loadLocalString(decl.address);
        }

        public String evalAsOptString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if (decl == null)
                return focus.currentItemAsString();
            EvalContext ctx = (upLevel == 0)? context : context.closure(upLevel);
            return ctx.loadLocalString(decl.address);
        }
    }
}
