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

public class LetClause extends VarClause
{
    public boolean score;   // true if 'let score'
    
    public LetClause(QName variable)
    {
        super(variable);
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property(score? "score" : "variable", 
                   "$" + variable + " : " + declaredType
                   + (d.isPretty() ? "" : " addr "
                       + (varDecl != null ? ("" + varDecl.address) : "?")));
        d.child(expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        if (checked)
            return this; // because of the insertion of a let before it
        checked = true;
        // TRICK: declare variable without name because not yet visible
        // but we want it to take the right place in local var tree (BUG FIX)
        varDecl = context.defineLocalVariable(null, declaredType, this);
        varDecl.offset = this.offset;
        if(expr != null)    // used for decl var in blocks
            expr = context.staticCheck(expr, 0);
        
        // now we can name it:
        varDecl.name = variable;
        if(score)
            type = XQType.DOUBLE;
        else {
            type = declaredType; // used for dynamic check (let)
            if (declaredType == null)
                type = (expr != null)? expr.getType() : XQType.ANY;
            // seq type check is now at run time
        }
        
        varDecl.storageType(type, context);
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return new Sequence(focus, context);
    }

    public class Sequence extends VarClause.SingleDummy
    {
        Sequence(Focus focus, EvalContext context)
        {
            super(focus, context);
        }

        public boolean next()
            throws EvaluationException
        {
            if (!source.next())
                return false;
            doNext();
            return true;
        }

        public boolean nextCollection()
            throws EvaluationException
        {
            if (!source.nextCollection())
                return false;
            doNext();
            return true;
        }

        private void doNext()
            throws EvaluationException
        {
            try {
                if(score) { // use same var address
                    XQValue value = expr.eval(focus, context);
                    context.storeScore(varDecl.address,
                                       value.next()? value.getFulltextScore(null) : 0);
                }
                else
                    context.storeLocal(varDecl, expr, declaredType, false,
                                       focus, context);
            }
            catch (XQTypeException err) {
                context.error(ERRC_BADTYPE, LetClause.this,
                              "dynamic type mismatch on variable $" + variable
                                  + " : " + err.getMessage());
            }
        }
        
        public double getFulltextScore(Item item) throws EvaluationException
        {
            if(item == null)
                item = getNode();
            if (score)
                return context.loadLocalDouble(varDecl.address);
            return source.getFulltextScore(item);
        }        

        public XQValue bornAgain()
        {
            Sequence s = new Sequence(focus, context);
            s.setSource(source.bornAgain());
            return s;
        }
    }
}
