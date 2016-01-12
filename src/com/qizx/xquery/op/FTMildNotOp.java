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
import com.qizx.queries.FullText.MildNot;
import com.qizx.queries.FullText.Selection;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;

public class FTMildNotOp extends FTSelectionOp
{
    public FTMildNotOp(FTSelectionOp exp1, FTSelectionOp exp2)
    {
        children = new Expression[] { exp1, exp2 };
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        FTMildNotOp res = (FTMildNotOp) super.staticCheck(context, flags);
        checkChild(context, children[0], "left");
        checkChild(context, children[1], "right");
        return res;
    }

    private void checkChild(ModuleContext context, Expression child, String side)
    {
        FTSelectionOp kid = (FTSelectionOp) child;
        if(kid.hasExcludesOrOccurs())
            context.error("FTDY0017", child, "invalid expression at " + side +
            		    "-hand side of 'not in'");
    }

    public Selection expand(Focus focus, EvalContext context,
                            MatchOptions inherited, float heritedW)
        throws EvaluationException
    {
        MildNot result = new MildNot();
        expandOptions(result, focus, context, inherited, heritedW);
        MatchOptions mops = result.getMatchOptions();
        Selection k1 = ((FTSelectionOp) children[0]).expand(focus, context, mops,
                                                            result.getWeight());
        Selection k2 = ((FTSelectionOp) children[1]).expand(focus, context, mops,
                                                            result.getWeight());
        result.setChildren(k1, k2);
        return result;
    }
}
