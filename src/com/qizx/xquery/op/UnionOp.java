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
import com.qizx.api.QizxException;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.NodeFilter;
import com.qizx.xdm.UnionNodeFilter;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.DualNodeSequence;

public class UnionOp extends NodeExpression
{
    public UnionOp(Expression expr1, Expression expr2)
    {
        super(expr1, expr2);
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("expr1", expr1);
        d.child("expr2", expr2);
    }

    public Expression reduce()
    {
        if (expr1 instanceof UnionOp)
            expr1 = ((UnionOp) expr1).reduce();
        if (expr2 instanceof UnionOp)
            expr2 = ((UnionOp) expr2).reduce();
        // not only child, but any identical axes
        if (expr1.getClass() != expr2.getClass()
            || !(expr1 instanceof BasicStep))
            return this;

        NodeFilter t1 = ((BasicStep) expr1).nodeTest;
        NodeFilter t2 = ((BasicStep) expr2).nodeTest;
        
        BasicStep result;
        try {
            result = (BasicStep) expr1.getClass().newInstance();
            result.nodeTest = new UnionNodeFilter(t1, t2);
            return result.atSamePlaceAs(this);
        }
        catch (InstantiationException shouldNotHappen) {
            return this;
        }
        catch (IllegalAccessException shouldNotHappen) {
            return this;
        }
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        super.staticCheck(context, flags);
        return reduce();
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        return new Sequence(expr1.eval(focus, context),
                            expr2.eval(focus, context));
    }

    public class Sequence extends DualNodeSequence 
    {
        Sequence(XQValue s1, XQValue s2) throws EvaluationException
        {
            init(s1, s2);
        }

        // assumes that s1 and s2 are in doc order
        public boolean next()
            throws EvaluationException
        {

            // is one sequence finished ?
            if (n1 == null)
                if (n2 == null)
                    return false;
                else {
                    item = n2;
                    n2 = s2.next() ? s2.basicNode() : null;
                    return true;
                }
            if (n2 == null) {
                item = n1;
                n1 = s1.next() ? s1.basicNode() : null;
                return true;
            }
            // we have an item in both sequences:
            try {
                int cmp = n1.documentOrderCompareTo(n2);
                if (cmp <= 0) {
                    item = n1;
                    n1 = s1.next() ? s1.basicNode() : null;
                }
                if (cmp >= 0) {
                    item = n2;
                    n2 = s2.next() ? s2.basicNode() : null;
                }
                return true;
            }
            catch (DataModelException e) {
                throw BasicNode.wrapDMException(e);
            }
        }

        public XQValue bornAgain()
        {
            try {
                return new Sequence(s1.bornAgain(), s2.bornAgain());
            }
            catch (QizxException e) {
                return null; // cannot happen
            }
        }
    }
}
