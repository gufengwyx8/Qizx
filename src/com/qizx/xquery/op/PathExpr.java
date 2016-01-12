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
import com.qizx.api.ItemType;
import com.qizx.api.Node;
import com.qizx.api.QizxException;
import com.qizx.xdm.BaseNodeFilter;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.NodeFilter;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.SingleSourceSequence;

import java.math.BigDecimal;

/**
 * Path Expression: composes several steps.
 * <p>
 * Alas there is this new dirty useless feature that the last step can be an
 * expression returning any atomic type. In that case we use a general and
 * possibly inefficient operator that check the homogeinity of the returned
 * sequence and performs sorting/duplicate elimination if it gets nodes.
 */
public class PathExpr extends Expression
{
    public Expression[] steps = new Expression[0];

    boolean needsSort = false;
    boolean lastStepNotNode = false;
    
    // optimization in evalAsEvents: when a path ends with /text() or //text(),
    // this suffix is removed and upper node is generated directly by
    int textMode;
    
    public static final int TEXTMODE_CHILD = 1;
    public static final int TEXTMODE_DESC = 2;
    
    public PathExpr()
    {
    }

    public PathExpr(Expression firstStep)
    {
        steps = new Expression[] { firstStep };
    }

    public int getStepCount()
    {
        return steps.length;
    }

    public Expression getStep(int rank)
    {
        return rank < 0 || rank >= steps.length ? null : steps[rank];
    }

    public void addStep(Expression step)
    {
        // step is union: optimise the most frequent case: /(a|b)/
        // replaced by a child:: with composite node test
        if (step instanceof UnionOp)
            step = ((UnionOp) step).reduce();

        // replace //child::TEST by descendant::TEST
        if (step instanceof ChildStep && steps.length > 0
            && steps[steps.length - 1] instanceof DescendantOrSelfStep
            && ((BasicStep) steps[steps.length - 1]).nodeTest == null) {
            // 
            steps[steps.length - 1] =
                new DescendantStep(((BasicStep) step).nodeTest);
            // prev.nodeTest = ((BasicStep) step).nodeTest;
            return;
        }
        steps = addExpr(steps, step);
    }

    public Expression child(int rank)
    {
        return rank < steps.length ? steps[rank] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        if (needsSort)
            d.property("needsSort", "" + needsSort);
        d.children(steps);
    }

    public int getFlags()
    {
        return DOCUMENT_ORDER; // that's our goal
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        type = context.getDotType() == null ? (XQType) XQType.NODE.star
                                            : context.getDotType();
        // TODO single node at root for $d in ... return $d//xx
        boolean previousSameDepth = true;
        int lastPos = steps.length - 1;
        for (int e = 0; e <= lastPos; e++) {
            context.pushDotType(type);
            Expression step = steps[e] = context.staticCheck(steps[e], 0);
            context.popDotType();

            type = steps[e].getType();
            XQItemType itemType =
                (type == null) ? XQType.ITEM : type.itemType();
            boolean isNodeStep = XQType.NODE.accepts(itemType);
            // error if static type of source surely does not derive from Node:
            if (itemType != XQType.ITEM
                && !(isNodeStep || e == lastPos ||
                     (e == 0 && XQType.WRAPPED_OBJECT.isSuperType(itemType))))
                context.error("XPTY0019", step, "invalid type for path step: "
                                       + step.getType().toString(context) + "'");
            if (e == lastPos && !isNodeStep)
                lastStepNotNode = true;
            
            // static type checking: detect certain failures
            // This is hardly useful except for XQTS scores !
            if(e > 0) {
                staticTypeCheck(steps[e - 1], step, context);
            }
            
            // Set the type of the path: we always consider it is itemType*
            // otherwise problem with FLWR optimisations... no need to be
            // smarter
            type = itemType.star;

            // Decide whether nodes generated by the path must be sorted:
            // the only case where the composition of 2 steps does not need
            // a sort or a merge is when the first step stays at same depth and
            // the second step stays within the subtree of its origin.
            int stepFlags = steps[e].getFlags();
            // TODO: check the context to see if it can stay unordered
            if (isNodeStep) {
                if ((stepFlags & DOCUMENT_ORDER) == 0
                    || e > 0
                    && !((stepFlags & WITHIN_SUBTREE) != 0
                         && previousSameDepth || (stepFlags & WITHIN_NODE) != 0))
                    needsSort = true;
            }
            previousSameDepth = (stepFlags & SAME_DEPTH) != 0;
            // Extension TODO?: use $obj/memberX as equivalent to
            // getMemberX(obj)
            // if(type instanceof WrappedObjectType) {
            // } else
            // else static type of the step required to be derived from node:
        }

        // The sort is inserted
        // return needsSort? (new NodeSortExpr(this)).atSamePlaceAs(this)
        // : (Expression)this;
        return this;
    }

    private void staticTypeCheck(Expression prevStep, Expression step, ModuleContext context)
    {
        if(!(prevStep instanceof BasicStep && step instanceof BasicStep))
            return;
        
        NodeFilter prevTest = ((BasicStep) prevStep).nodeTest;
        NodeFilter test = ((BasicStep) step).nodeTest;
        int kind = (test == null)? -1 : test.getNodeKind();
        int prevKind = (prevTest == null)? -1 : prevTest.getNodeKind();

        if(step instanceof SelfStep) {
            // same test required:
            if(kind != prevKind && prevKind >= 0 && kind >= 0)
                context.error("XPST0005", step,
                              "self:: with incompatible node kind");
            else if(test instanceof BaseNodeFilter &&
                    prevTest instanceof BaseNodeFilter) {
                BaseNodeFilter test1 = (BaseNodeFilter) test;
                BaseNodeFilter prevTest1 = (BaseNodeFilter) prevTest;
                if(test1.name != null && prevTest1.name != null
                   && test1.name != prevTest1.name)
                    context.error("XPST0005", step,
                                  "self:: with incompatible node name");
            }
        }
        
        switch(prevKind) {
        case Node.ATTRIBUTE:
        case Node.TEXT:
        case Node.COMMENT:
        case Node.PROCESSING_INSTRUCTION:
            if(kind == Node.ELEMENT)
                context.error("XPST0005", step, "node kind incompatible with previous step");
            if(step instanceof ChildStep || 
               step instanceof DescendantStep && !(step instanceof DescendantOrSelfStep))
                   context.error("XPST0005", step, "meaningless path step after attribute");
            break;
        case Node.DOCUMENT:
            if(step instanceof SelfStep) {
            }
            else if(!(step instanceof ChildStep || step instanceof DescendantStep))
                context.error("XPST0005", step, "meaningless path after root");
            break;
        }
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return context.evalPath(this, focus);
    }

    /**
     * In the context of a constructor, trim a trailing /text() or //text()
     * Replaced by flag textMode used in evalAsEvents.
     * A bit 'XMark special' but still useful in general.
     */
    public void tryToTrim()
    {
        if(steps.length < 2)
            return;
        Expression lastStep = getStep(steps.length - 1);
        if(lastStep instanceof ChildStep) {
            ChildStep st = (ChildStep) lastStep;
            if(st.nodeTest != null && st.nodeTest.getNodeKind() == Node.TEXT) {
                // remove last step
                textMode = TEXTMODE_CHILD;
                int newlen = steps.length - 1;
                lastStep = getStep(steps.length - 2);
                if(lastStep instanceof DescendantOrSelfStep)
                    newlen = steps.length - 2;
                // trim:
                Expression[] nsteps = new Expression[newlen];
                System.arraycopy(steps, 0, nsteps, 0, newlen);
                steps = nsteps;
            }
        }                
    }
    
    /**
     * Redefined to take into account the optimization mode for text()
     */
    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        XQValue seq = eval(focus, context);
        try {
            for (; seq.next();)
                if (seq.isNode()) {
                    BasicNode srcNode = seq.basicNode();
                    // traversal copies source node to output
                    // namespace copy according to context flags:
                    if(textMode == TEXTMODE_CHILD)
                        output.putNodeText(srcNode, 2);
                    else if(textMode == TEXTMODE_CHILD)
                        output.putNodeText(srcNode, Integer.MAX_VALUE);
                    else
                        output.putNodeCopy(srcNode,
                                    context.getStaticContext().getCopyNSMode());
                }
                else {
                    output.putAtomText(seq.getString());
                }
        }
        catch (QizxException e) {
            context.error(e.getErrorCode(), this, "error in constructor: "
                                                  + e.getMessage());
        }
    }

    // eval remaining steps from a source
    public XQValue evalNextSteps(XQValue src, int startStep,
                                 EvalContext context)
        throws EvaluationException
    {
        for (int s = startStep, L = steps.length - 1; s <= L; s++) {
            Expression step = getStep(s);
            if (lastStepNotNode && s == L) {
                // insert a sort BEFORE last step if needed
                if (needsSort)
                    src = new NodeSortExpr.Sequence(src);
            }
            src = new Composition(src, step, context);
        }
        if (needsSort && !lastStepNotNode)
            src = new NodeSortExpr.Sequence(src);
        return src;
    }

    /**
     * Composes a source with a step: from each node of the source, generate
     * new nodes This is for dumb querying, not for collections.
     */
    public class Composition extends SingleSourceSequence
        implements Focus
    {
        EvalContext context;
        Expression step;    // expression to evaluate on each source node
        XQValue    stepSeq; // iterator given by evaluating step on each source node
        BasicStep  basicStep;   // optimize by recycling stepSeq

        long position = 0, last = -1;

        // BasicNode current;

        public Composition(XQValue source, Expression step, EvalContext context)
        {
            super(source);
            this.step = step;
            this.context = context;
            this.stepSeq = XQValue.empty;
            if(step instanceof BasicStep)
                basicStep = (BasicStep) step;
            position = 0;
        }

        public XQValue bornAgain()
        {
            return new Composition(source.bornAgain(), step, context);
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                if (stepSeq.next()) {
                    item = stepSeq.getItem();
                    return true;
                }
                item = null;
                if (!source.next())
                    return false;
                ++position;
                // change the focus:
                context.at(step);
                if(basicStep != null) // reuse step iterator?
                    stepSeq = basicStep.eval(this, context, stepSeq);
                else
                    stepSeq = step.eval(this, context);
            }
        }

        public ItemType getType() throws EvaluationException
        {
            return stepSeq.getType();
        }

        public BasicNode getBasicNode() throws EvaluationException
        {
            return item.basicNode();
        }

        public boolean getBoolean() throws EvaluationException
        {
            return stepSeq.getBoolean();
        }

        public BigDecimal getDecimal()
            throws EvaluationException
        {
            return stepSeq.getDecimal();
        }

        public double getDouble()
            throws EvaluationException
        {
            return stepSeq.getDouble();
        }

        public float getFloat()
            throws EvaluationException
        {
            return stepSeq.getFloat();
        }

        public long getInteger()
            throws EvaluationException
        {
            return stepSeq.getInteger();
        }

        public String getString()
            throws EvaluationException
        {
            return stepSeq.getString();
        }

        // implementation of Focus

        public XQItem currentItem()
            throws EvaluationException
        {
            return source.getItem();
        }

        public Node currentItemAsNode()
            throws EvaluationException
        {
            return source.getNode();
        }

        // should never work, just for completion
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
        {
            if (last < 0) {
                try {
                    XQValue sba = source.bornAgain();
                    for (last = 0; sba.next();)
                        ++last;
                }
                catch (Exception e) {
                } // if it happens here, it will happen later
            }
            return last;
        }
        
        public double getFulltextScore(Item item) throws EvaluationException
        {
            if(item == null)
                item = getNode();
            return stepSeq.getFulltextScore(item);
        }
    }
//
//    public class LastStep extends GenericValue
//    {
//        Expression step;
//        EvalContext context;
//        Composition source; // path minus lastStep
//        XQValue current; // evaluated 'return' expression
//
//        LastStep(Composition source, Expression step, EvalContext context)
//        {
//            this.step = step;
//            this.source = source;
//            this.context = context;
//            current = XQValue.empty;
//        }
//
//        public XQValue bornAgain()
//        {
//            return new LastStep((Composition) source.bornAgain(),
//                                step, context);
//        }
//
//        public boolean next()
//            throws EvaluationException
//        {
//            for (;;) {
//                // context.at(expr); // for timeout
//                if (current.next()) { // inline 'return' expression {
//                    if (!lazy)
//                        item = current.getItem();
//                    return true;
//                }
//                if (!source.next())
//                    return false;
//                current = step.eval(source, context);
//            }
//        }
//    }
}
