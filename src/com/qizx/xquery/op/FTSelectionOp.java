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
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.queries.FullText.PosFilters;
import com.qizx.queries.FullText.Selection;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;

/**
 * Abstract Full-text selection (a list of terms or a boolean combination).
 * Bears the options of a FT selection.
 */
public abstract class FTSelectionOp extends Expression
{
    public Expression[]  children;
    public Expression    weight;      // optional
    public FTPosFilters  posFilters;  // optional
    public MatchOptions  matchOptions; // optional
    // occurs clause is only on words (why?)

    public FTSelectionOp() {
    }

    public FTSelectionOp(Expression exp)
    {
        children = new Expression[] { exp };
    }

    public void addChild(Expression child)
    {
        children = addExpr(children, child);
    }

    public Expression child(int rank)
    {
        if(children == null || rank >= children.length)
            return null;
        return children[rank];
    }
    
    public Expression staticCheck(ModuleContext context, int flags)
    {
        for (int i = 0; i < children.length; i++) {
            children[i] = context.staticCheck(children[i], flags);
        }
        if(posFilters != null) {
            posFilters.distance =
                (RangeExpr) context.staticCheck(posFilters.distance, 0);
             posFilters.windowExpr = 
                context.staticCheck(posFilters.windowExpr, 0);
        }
        if(weight != null)
            weight = context.staticCheck(weight, flags);
        return this;
    }

    public abstract Selection expand(Focus focus, EvalContext context,
                                     MatchOptions inheritedMatchOptions,
                                     float inheritedWeight)
        throws EvaluationException;

    /**
     * Evaluates both 'Match options' and 'Position filters'.
     * @param inherited 
     * @param heritedWeight 
     */
    protected void expandOptions(Selection s, Focus focus, EvalContext context,
                                 MatchOptions inherited, float heritedWeight)
        throws EvaluationException
    {
        s.setMatchOptions(expandMatchOptions(inherited));
        s.setPosFilters(expandPositionFilters(focus, context));
        s.setWeight(expandWeight(focus, context, heritedWeight));
    }

    protected PosFilters expandPositionFilters(Focus focus, EvalContext context)
        throws EvaluationException
    {
        if (posFilters == null)
            return null;
        PosFilters pof = new PosFilters(posFilters);
        // eval distances, window
        if(posFilters.distance != null) {
            pof.distanceRange = posFilters.distance.evaluate(focus, context);
        }
        if(posFilters.windowExpr != null) {
            if(posFilters.windowUnit != PosFilters.WORDS)
                context.error("FTST0003", this, "unsupported window unit");
            if(!XQType.NUMERIC.accepts(posFilters.windowExpr.getType()))
                context.error("XPTY0004", posFilters.windowExpr);
            pof.window =
                (int) posFilters.windowExpr.evalAsInteger(focus, context);
        }
        return pof;
    }

    protected MatchOptions expandMatchOptions(MatchOptions inherited)
    {
        MatchOptions mop = inherited; // if nothing defined locally
        if (matchOptions != null) {
            mop = new MatchOptions(matchOptions);
            // unspecified options are inherited:
            if(mop.language == null)
                mop.language = inherited.language;
            if(mop.caseSensitivity == MatchOptions.UNSPECIFIED)
                mop.caseSensitivity = inherited.caseSensitivity;
            if(mop.diacritics == MatchOptions.UNSPECIFIED)
                mop.diacritics = inherited.diacritics;
            if(mop.stemming == MatchOptions.UNSPECIFIED)
                mop.stemming = inherited.stemming;
            if(mop.wildcards == MatchOptions.UNSPECIFIED)
                mop.wildcards = inherited.wildcards;
            if(mop.thesauri == null)
                mop.thesauri = inherited.thesauri;
        }
        return mop;
    }

    protected float expandWeight(Focus focus, EvalContext context,
                                 float inheritedWeight)
        throws EvaluationException
    {
        if(weight != null)
            return weight.evalAsFloat(focus, context);
        return inheritedWeight;
    }
    
    public int getFlags()
    {
        return isConstant() ? Expression.CONSTANT : 0;
    }

    public boolean isConstant()
    {
        for (int i = 0; i < children.length; i++)
            if(!children[i].isConstant())
                return false;
        if(posFilters == null)
            return true;
        return (posFilters.distance == null || posFilters.distance.isConstant())
            && (posFilters.windowExpr == null || posFilters.windowExpr.isConstant());
    }
    
    // return true if the selection can generate "stringExcludes"
    public boolean hasExcludesOrOccurs()
    {
        for (int i = 0; i < children.length; i++)
            if(children[i] instanceof FTSelectionOp
               && ((FTSelectionOp) children[i]).hasExcludesOrOccurs())
                return true;
        return false;
    }
}

