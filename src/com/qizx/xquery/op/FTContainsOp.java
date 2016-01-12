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
import com.qizx.api.Node;
import com.qizx.queries.FullText;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.queries.FullText.Selection;
import com.qizx.util.basic.Check;
import com.qizx.xquery.DynamicContext;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQValue;

import java.util.HashSet;

public class FTContainsOp extends BooleanExpression
{
    public Expression container;
    public FTSelectionOp selection;
    public Expression ignore;
    private FullText.Selection cachedQuery;
    
    public FTContainsOp(Expression container, FTSelectionOp selection)
    {
        this.container = container;
        this.selection = selection;
    }

    public Expression child(int rank)
    {
        switch(rank) {
        case 0:
            return container;
        case 1:
            return selection;
        case 2:
            return ignore;
        default:
            return null;
        }
    }
    
    public Expression staticCheck(ModuleContext context, int flags)
    {
        container = context.staticCheck(container, flags);
        selection = (FTSelectionOp) context.staticCheck(selection, flags);
        if(ignore != null)
            ignore = context.staticCheck(ignore, flags);
        // detect and precompute constant FT selection:
        if(selection.isConstant()) {
            EvalContext ctx = context.getConstantEvalContext();
            try {
                cachedQuery = selection.expand(null, ctx, 
                                               context.getDefaultFTOptions(), 1);
                cachedQuery = cachedQuery.applyThesauri();
            }
            catch (EvaluationException giveUp) { ; 
            }
        }
        return this;
    }
    
    /**
     * Converts the selection of a ftcontains expression into a
     * StdFullText.Selection. Evaluates sub-expressions.
     * @param wordSieve
     */
    public FullText.Selection expandSelection(Focus focus,
                                              EvalContext context,
                                              MatchOptions inheritedFTOptions,
                                              float inheritedWeight)
        throws EvaluationException
    {
        Check.nonNull(inheritedFTOptions, "inherited match options");
        return selection.expand(focus, context, inheritedFTOptions, inheritedWeight);
    }

    // brute-force evaluation without indexes
    public boolean evalAsBoolean(Focus focus, EvalContext context)
        throws EvaluationException
    {
        DynamicContext sctx = context.dynamicContext();
        
        Selection query = cachedQuery;
        if(query == null) {
            query = selection.expand(focus, context, sctx.getDefaultFTOptions(), 1);
            query = query.applyThesauri();
        }
        //new Displayer().display(query); 
        
        // source
        XQValue ctx = container.eval(focus, context);
        
        // optional 'without content':
        HashSet withoutNodes = null;
        if(ignore != null) {
            XQValue without = ignore.eval(focus, context);
            withoutNodes = new HashSet();
            for(; without.next(); ) {
                Node node = without.getNode();
                withoutNodes.add(node);
            }
        }
        // stop on first match of a context node:
        try {
            return query.matches(ctx, withoutNodes, sctx.getFulltextFactory());
        }
        catch (DataModelException e) {
            context.error(e.getErrorCode(), this,
                          "data model error" + e.getMessage());
        }
        return false;
    }

}
