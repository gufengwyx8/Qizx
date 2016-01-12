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
import com.qizx.api.QName;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.dt.SingleInteger;
import com.qizx.xquery.dt.SingleItem;
import com.qizx.xquery.fn.True;

import java.util.ArrayList;

public class WindowClause extends VarClause
{
    public Condition startCond;
    public Condition endCond;
    public boolean sliding;
    public boolean onlyEnd;
    
    public static class Condition
    {
        public Expression cond;
        // bound variables: all optional
        public QName itemVarName;
        public LocalVariable itemVar;
        public QName atVarName;
        public LocalVariable atVar;
        public QName previousVarName;
        public LocalVariable previousVar;
        public QName nextVarName;
        public LocalVariable nextVar;
        

        public void staticCheck(ModuleContext context, WindowClause decl)
        {
            if(itemVarName != null)
                itemVar = context.defineLocalVariable(itemVarName, XQType.ITEM.opt, decl);
            if(atVarName != null)
                atVar = context.defineLocalVariable(atVarName, XQType.INTEGER, decl);
            if(previousVarName != null)
                previousVar = context.defineLocalVariable(previousVarName, XQType.ITEM.opt, decl);
            if(nextVarName != null)
                nextVar = context.defineLocalVariable(nextVarName, XQType.ITEM.opt, decl);
            cond = cond.staticCheck(context, 0);
            if(cond instanceof True.Exec)
                cond = null;    // optimization
        }

        boolean bindEval(Sequence seq, Focus focus, EvalContext ctx)
            throws EvaluationException
        {
            // always bind, even if no evaluation (vars of start used by 'end')
            bindVar(itemVar, seq.current, ctx);
            bindVar(previousVar, seq.previous, ctx);
            bindVar(nextVar, seq.next, ctx);
            
            if(atVar != null) {
                
                ctx.storeLocal(atVar.address, new SingleInteger(seq.position),
                               false, null);
            }
            if(cond == null)    // optimization for true()
                return true;
            return cond.evalEffectiveBooleanValue(focus, ctx);
        }

        private void bindVar(LocalVariable var, XQItem value, EvalContext ctx)
            throws EvaluationException
        {
            if(var != null) {
                
                ctx.storeLocal(var.address,
                              value == null? XQValue.empty : new SingleItem(value),
                              false, null);
        }
        }
        
    
        public void dump(ExprDisplay d, boolean start)
        {
            d.header(start? "start" : "end");
            if(itemVar != null)
                d.child("current", "" + itemVar);
            if(atVar != null)
                d.child("at", "" + atVar);
            if(previousVar != null)
                d.child("previous", "" + previousVar);
            if(nextVar != null)
                d.child("next", "" + nextVar);
            d.child(cond);
            d.end();
        }
    }
    
    public WindowClause(QName variable, boolean sliding)
    {
        super(variable);
        this.sliding = sliding;
        startCond = new Condition();
        endCond = new Condition();
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("variable", variable);
        d.property("varAddress", varDecl.address);
        d.property("declType", declaredType);
        startCond.dump(d, true);
        if(endCond != null)
            endCond.dump(d, false);
        d.child("expr", expr);
    }
    
    public Expression staticCheck(ModuleContext context, int flags)
    {
        if (checked)
            return this; // because of the insertion of a let before it
        checked = true;
        
        expr = context.staticCheck(expr, 0);
        //LocalVariable mark = context.latestLocalVariable();
        type = declaredType;
        if (type == null)
            type = expr.getType();
      
        // check conditions. Start vars can be used in end condition
        startCond.staticCheck(context, this);
        if(endCond != null)
            endCond.staticCheck(context, this);
        
        // undeclare vars of conditions: NOPE
        //context.popLocalVariables(mark);
        
        varDecl = context.defineLocalVariable(variable, declaredType, this);
        varDecl.offset = this.offset;
        // define storage type: registers disabled if dyn type check
        if (declaredType != null)
            varDecl.address = -1;
        else
            varDecl.storageType(type, context);
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return new Sequence(focus, context);
    }

    /**
     * Does the chunking. Binds the window *sequence* to the main variable
     */
    public class Sequence extends VarClause.SingleDummy
    {
        // latest window
        private ArrayList window;
        // buffering on top of input
        private XQValue input;  // in <expr>
        protected int position;
        protected XQItem previous, current, next;
        private ArrayList backInput;
        
        Sequence(Focus focus, EvalContext context)
        {
            super(focus, context);
        }

        public XQValue bornAgain()
        {
            Sequence s = new Sequence(focus, context);
            s.setSource(source.bornAgain());
            return s;
        }
        
        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                if(nextWindow(false))
                    return true;
                // like for any 'for' clause, need to restart
                if (!source.next())
                    return false;
                resetInput();
            }
        }

        public boolean nextCollection()
            throws EvaluationException
        {
            for (;;) {
                if(nextWindow(true))
                    return true;
                if (!source.next())
                    return false;
                input = expr.eval(focus, context);
                resetInput();
            }
        }  
        
        /**
         * Computes the next window by finding the start and end positions
         * and buffering the items in between.
         */
        private boolean nextWindow(boolean collection)
            throws EvaluationException
        {
            if(input == null) { // first call: must return false to force eval
                return false;
            }
            window = new ArrayList();

            // First, find the start position in the input:
            for(; ; ) {
                if(!moveToNextItem())
                    return false;
                if(startCond.bindEval(this, focus, context))
                    break;
            }
            window.add(current);
            
            // special case: no end clause (tumbling); need to go forward
            boolean skipOne = (endCond == null);
                
            // Find the end: from start
            
            for(; ; ) {
                Condition c = (endCond == null)? startCond : endCond;
                if(!skipOne && c.bindEval(this, focus, context))
                    break;
                skipOne = false;
                if(!moveToNextItem()) {
                    if(onlyEnd)
                        return false;
                    break;
                }
                window.add(current);
            }
            
            XQItem stop = null;
            if(endCond == null && current != null) {
                stop = current;
                window.remove(window.size() - 1);
            }
            
            // OK, store the window into the main variable:
            try {
                ArraySequence windowSeq =
                    new ArraySequence(window.size(), window.toArray(), this);
                context.storeLocal(varDecl.address, windowSeq, false,
                                   declaredType);
            }
            catch (XQTypeException err) {
                context.error(ERRC_BADTYPE, WindowClause.this,
                              "dynamic type mismatch on window variable $"
                                  + varDecl.name + ": expecting "
                                  + declaredType);
            }
            
            // prepare for next window:
            if(sliding) {
                // put back all because we will do a moveToNextItem
                int w = window.size() - 1;
                current = (XQItem) window.get(w);   // hacky
                for(; --w >= 0; )
                    putBack((XQItem) window.get(w));
                //++ position; // hacky
            }
            else if(endCond == null)
                putBack(stop);

            return true;
        }

        // ----------- input primitives -----------------
        
        private void resetInput()
            throws EvaluationException
        {
            input = expr.eval(focus, context);
            position = 0;
            previous = current = null;
            next = input.next()? input.getItem() : null;
        }

        private boolean moveToNextItem()
            throws EvaluationException
        {
            previous = current;
            current = next;
            if(backInput != null) {
                next = (XQItem) backInput.remove(backInput.size() - 1);
                if(backInput.size() == 0)
                    backInput = null;
            }
            else {
                if(next == null)
                    return false;
                if(!input.next())
                    next = null;
                else
                    next = input.getItem();
            }
            ++ position;
            return true;
        }
        
        // reinject item into input stream
        private void putBack(XQItem item)
        {
            if(next != null) {
                if(backInput == null)
                    backInput = new ArrayList();
                backInput.add(next);
            }
            --position;
            next = current;
            current = item;
        }
    }
}
