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

/**
 * Enumerates the items of a single Flower 'for' clause and manages the
 * associated variables.
 */
public class ForClause extends VarClause
{
    public QName position;
    public LocalVariable posDecl;
    public QName score;
    public LocalVariable scoreDecl;

    public ForClause(QName variable)
    {
        super(variable);
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("variable", "$" + variable + " as " + declaredType
                              + (d.isPretty() ? "" : (" addr " + varDecl.address)));
        if (position != null) {
            d.property("position", position + " addr " + posDecl.address);
        }
        if (score != null) {
            d.property("score", score + " addr " + scoreDecl.address);
        }
        d.child("expr", expr);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        if (checked)
            return this; // because of the insertion of a let before it
        checked = true;

        // source:
        expr = context.staticCheck(expr, 0);
        if (declaredType != null
            && declaredType.getOccurrence() != XQType.OCC_EXACTLY_ONE)
            module.warning("XPTY0004", offset,
                           "improper type occurrence for variable $"
                               + variable + ": must be one");
        type = declaredType;
        if (type == null)
            type = expr.getType().itemType();
 
        varDecl = context.defineLocalVariable(variable, declaredType, this);
        varDecl.offset = this.offset;
        // define storage type: registers disabled if dyn type check
        if (declaredType != null)
            varDecl.address = -1;
        else
            varDecl.storageType(type, context);

        if (position != null) {
            if (position == variable)
                module.error("XQST0089", this, "duplicate local variable "
                                               + position);
            posDecl =
                context.defineLocalVariable(position, XQType.INTEGER, this);
            posDecl.storageType(XQType.INTEGER, context);
        }

        if (score != null) {
            if (score == position || score == variable)
                module.error("XQST0089", this, 
                             "duplicate local variable " + score);
            scoreDecl = context.defineLocalVariable(score, XQType.INTEGER, this);
            scoreDecl.storageType(XQType.DOUBLE, context);
        }
        return this;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return new Sequence(focus, context);
    }

    public class Sequence extends VarClause.SingleDummy
    {
        XQValue current; // evaluated expression
        int curPos;

        Sequence(Focus focus, EvalContext context)
        {
            super(focus, context);
            current = XQValue.empty;
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                if (current.next()) {
                    doNext();
                    return true;
                }
                if (!source.next())
                    return false;
                current = expr.eval(focus, context);
                curPos = 0;
            }
        }

        public boolean nextCollection()
            throws EvaluationException
        {
            for (;;) {
                if (current.nextCollection()) {
                    doNext();
                    return true;
                }
                if (!source.next())
                    return false;
                current = expr.eval(focus, context);
                curPos = 0;
            }
        }

        private void doNext()
            throws EvaluationException
        {
            try {
                context.storeLocal(varDecl.address, current, true,
                                   declaredType);
            }
            catch (XQTypeException err) {
                context.error(ERRC_BADTYPE, ForClause.this,
                              "dynamic type mismatch on 'for' variable $"
                              + varDecl.name + ": expecting "
                              + declaredType);
            }
            ++curPos;
            if (position != null)
                context.storeLocalInteger(posDecl.address, curPos);
            if (scoreDecl != null)
                context.storeScore(scoreDecl.address,
                                   current.getFulltextScore(null));
        }

        public XQValue bornAgain()
        {
            Sequence s = new Sequence(focus, context);
            s.setSource(source.bornAgain());
            return s;
        }
        
        public double getFulltextScore(Item item) throws EvaluationException
        {
            if(item == null && isNode())
                item = getNode();
            if (scoreDecl != null)
                return context.loadLocalDouble(scoreDecl.address);
            return current.getFulltextScore(item);
        }
//
//        public void getFulltextSelections(java.util.Collection collect)
//        {
//            current.getFulltextSelections(collect);
//        }
    }
}
