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
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.IQName;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.BooleanValue;
import com.qizx.xquery.dt.SingleSourceSequence;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Last;
import com.qizx.xquery.fn.Name;
import com.qizx.xquery.fn.Number;
import com.qizx.xquery.fn.Position;
import com.qizx.xquery.fn.Function.Call;

/**
 * class FilterExpr: represents a path step followed by predicates.
 */
public class FilterExpr extends Expression
{
    private static QName POSITION = IQName.get(NamespaceContext.FN, "position");
    
    public Expression source;
    public Expression[] predicates;


    public FilterExpr(Expression source)
    {
        this.source = source;
        predicates = new Expression[0];
    }

    public void addPredicate(Expression predic)
    {
        predicates = addExpr(predicates, predic);
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("source", source);
        d.children(predicates);
    }

    public Expression child(int rank)
    {
        return rank == 0 ? source : rank <= predicates.length
            ? predicates[rank - 1] : null;
    }

    public int getFlags()
    {
        return source.getFlags();
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        source = context.staticCheck(source, 0);
        context.pushDotType(source.getType());
        for (int p = 0; p < predicates.length; p++) {
            Expression pred =
                predicates[p] = context.staticCheck(predicates[p], 0);
            // CAUTION: use 'isSuperType', not 'accepts' which is too lax
            XQItemType predType = pred.getType().itemType();

            // System.err.println("pred type "+predType+"
            // "+Type.NUMERIC.isSuperType(predType));

            // numeric predicates : wrap into special construct
            if (XQType.NUMERIC.isSuperType(predType)
                && !(pred instanceof Last.Exec))
            {
                // if "simple", then use optimized PosTest (Index sequence)
                if (pred.findSubExpression(Position.Exec.class) == null
                    && pred.findSubExpression(Last.Exec.class) == null
                    && pred.findSubExpression(SelfStep.class) == null
                    && !containsCallTo(Number.Exec.class, 0, pred) // without args
                    && !containsCallTo(Name.Exec.class, 0, pred))
                {
                    predicates[p] = new PosTest(pred);
                }
                else {
                    // transform into: position() eq <expr>
                    // which is not the same as PosTest because <expr>
                    // is reevaluated on each item
                    ValueEqOp test = new ValueEqOp(new DoubleLiteral(0), // dummy
                                                   new FunctionCall(POSITION));
                    predicates[p] = context.staticCheck(test, 0);
                    Function.Call op = (Function.Call) predicates[p];
                    op.args[0] = pred;  // avoid double check (FIX)
                }
            }
            // TODO: recognize "slices":
            // position() >= M and position() <= N | position = M to N
        }
        // manage naturalStepOrder:
        if (source instanceof BasicStep)
            ((BasicStep) source).naturalStepOrder = true;
        context.popDotType();
        type = source.getType();
        // filtering can make a sequence empty:
        // transform '+' into '*' and one-occ into '?' :
        if (XQType.isRepeatable(type.getOccurrence()))
            type = type.itemType().star;
        else
            type = type.itemType().opt;
        // EBV of predicates is used : no need of type check
        return this;
    }

    private boolean containsCallTo(Class function, int argc, Expression pred)
    {
        Function.Call call = (Call) pred.findSubExpression(function);
        if (call == null)
            return false;
        return (argc < 0 || call.args.length == argc);
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        XQValue src = source.eval(focus, context);
        for (int p = 0; p < predicates.length; p++) {
            Expression pred = predicates[p];
            if (pred instanceof PosTest) {
                Expression indexExpr = ((PosTest) pred).index;
                double index = indexExpr.evalAsDouble(focus, context);
                long lindex = (long) index;
                if (index != lindex)
                    lindex = -1; // fail
                src = new Index(src, lindex);
            }
            else if (pred instanceof Last.Exec)
                src = new LastFilter(src);
            else { // general predicate
                src = new Sequence(src, pred, context);
                // TODO optimize if predic does not depend on position,
                // eval once only (probably numeric).
            }
        }
        return src;
    }

    /**
     * General filtering sequence, where the predicate is evaluated for each
     * input item.
     */
    public static class Sequence extends SingleSourceSequence
        implements Focus
    {
        protected Expression predicate;
        protected EvalContext evalContext; // for evaluation of the predicate
        long position = 0;
        long last = -1; // caching: if >= 0 , it is already evaluated

        Sequence(XQValue source, Expression predicate, EvalContext evalCtext)
        {
            super(source);
            this.evalContext = evalCtext;
            this.predicate = predicate;
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                evalContext.at(predicate);
                if (!source.next())
                    return false;
                item = source.getItem();
                ++position;
                // change the focus:
                XQItem boo = predicate.evalForPredicate(this, evalContext);
                if (boo == null)
                    continue;
                if (boo instanceof BooleanValue) {
                    if (boo.getBoolean())
                        return true;
                }
                else { // numeric predicate
                    double pos = boo.getDouble();
                    if (pos == position)
                        return true;
                }
            }
        }

        public XQValue bornAgain()
        {
            return new Sequence(source.bornAgain(), predicate, evalContext);
        }

        // implementation of Focus

        public XQItem currentItem()
        {
            return item;
        }

        public Node currentItemAsNode()
            throws EvaluationException
        {
            if(!source.isNode())
                throw new EvaluationException("current item is not a node");
            return source.getNode();
        }

        public long currentItemAsInteger()
            throws EvaluationException
        {
            return source.getInteger();
        }

        public double currentItemAsDouble()
            throws EvaluationException
        {
            return source.getDouble();
        }

        public String currentItemAsString()
            throws EvaluationException
        {
            return source.getString();
        }

        public long getPosition()
        {
            return position;
        }

        public long getLast()
            throws EvaluationException
        {
            if (last < 0) {
                XQValue sba = source.bornAgain();
                for (last = 0; sba.next();)
                    ++last;
            }
            return last;
        }
    }


    static class PosTest extends Expression
    {
        Expression index;

        PosTest(Expression index)
        {
            this.index = index;
        }

        public Expression child(int rank)
        {
            return (rank == 0) ? index : null;
        }

        public boolean evalEffectiveBooleanValue(Focus focus,
                                                 EvalContext context)
            throws EvaluationException
        {
            return focus.getPosition() == index.evalAsDouble(focus, context);
        }
    }

    public static class LastFilter extends Sequence
    {
        private boolean started = false;

        public LastFilter(XQValue source)
        {
            super(source, null, null);
        }

        public XQValue bornAgain()
        {
            return new LastFilter(getSource().bornAgain());
        }

        public boolean next()
            throws EvaluationException
        {
            if (started)
                return false;
            started = true;
            for (; source.next();)
                item = source.getItem();
            return item != null;
        }
    }

    public static class Index extends Sequence
    {
        long index;

        public Index(XQValue source, long index)
        {
            super(source, null, null);
            this.index = index;
        }

        public XQValue bornAgain()
        {
            return new Index(source.bornAgain(), index);
        }

        public boolean next()
            throws EvaluationException
        {
            // optimized version:
            if (source.hasQuickIndex()) {
                if (position > 0)
                    return false;
                item = source.quickIndex(index);
                if (item == null)
                    return false; // bad index
                position = 1;
                return true;
            }

            for (;;) {
                ++position;
                if (!source.next() || position > index)
                    return false;
                if (position == index) {
                    item = source.getItem();
                    return true;
                }
            }
        }
    }

    public static class Slice extends Index
    {
        long indexHi;

        public Slice(XQValue source, long index, long indexHi)
        {
            super(source, index);
            this.indexHi = indexHi;
        }

        public XQValue bornAgain()
        {
            return new Slice(source.bornAgain(), index, indexHi);
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                ++position;
                if (!source.next() || position > indexHi)
                    return false;
                if (position >= index) {
                    item = source.getItem();
                    return true;
                }
            }
        }
    }
}
