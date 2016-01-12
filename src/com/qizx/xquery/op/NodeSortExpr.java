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
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.ArraySequence;

import java.util.Arrays;

/**
 * Sorts a Node Sequence in document order without duplicates.
 */
public class NodeSortExpr extends Expression
{
    public Expression expr;

    public NodeSortExpr(Expression expr)
    {
        this.expr = expr;
        type = expr.getType();
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? expr : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("expr", expr);
    }

    public int getFlags()
    {
        return DOCUMENT_ORDER; // that's the purpose
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue v = expr.eval(focus, context);
        return new Sequence(v);
    }

    public static class Sequence extends ArraySequence
    {
        public boolean needsSort = true;
        public boolean isDistinct;

        public Sequence(XQValue source) throws EvaluationException
        {
            super(new XQItem[16], 0);
            for (; source.next();) {
                addItem((BasicNode) source.getNode());
            }
            setOrigin(source);
        }

        public Sequence(BasicNode[] nodes, int count)
        { // for reusing
            super(nodes, count);
        }

        public void addNodes(BasicNode[] nodes, int count)
        {
            super.addItems(nodes, count);
        }

        public void sort()
        {
            pack(); // if necessary
            Arrays.sort(items, 0, size, new java.util.Comparator() {
                public int compare(Object o1, Object o2)
                {
                    BasicNode n1 = (BasicNode) o1, n2 = (BasicNode) o2;
                    try {
                        return n1.documentOrderCompareTo(n2);
                    }
                    catch (DataModelException e) {
                        return 0; // OOPS
                    }
                }
            });
            
            if(!isDistinct && size > 0) {
                // remove duplicates:
                Object previous = items[0];
                int nsize = Math.min(size, 1);
                for (int i = 1; i < size; i++) {
                    Object node = items[i];
                    if (!node.equals(previous)) {
                        previous = node;
                        items[nsize++] = node;
                    }
                }
                size = nsize;
                isDistinct = true;
            }
            needsSort = false;
            // -System.err.println("SORT "+size);
            // -for(int i = 0; i<size; i++) System.err.println(i+" =
            // "+items[i]);
        }

        public boolean next()
            throws EvaluationException
        {
            // automatic packing:
            if (needsSort || overflow != null) {
                sort();
            }
            return super.next();
        }

        public long quickCount(EvalContext context)
            throws EvaluationException
        {
            // no need to pack/sort if no duplication
            if (!isDistinct)
                sort();
            return super.quickCount(context);
        }

        //public XQValue bornAgain() USE superclass with copy of origin
    }
}
