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
import com.qizx.queries.FullText;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.queries.FullText.Selection;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;

public class FTNotOp extends FTSelectionOp
{

    public FTNotOp(FTSelectionOp exp1)
    {
        super(exp1);
    }

    public Selection expand(Focus focus, EvalContext context,
                            MatchOptions inherited, float inheritedWeight)
        throws EvaluationException
    {
        FullText.Not not = new FullText.Not();
        expandOptions(not, focus, context, inherited, inheritedWeight);
        FTSelectionOp sel = (FTSelectionOp) children[0];
        Selection child =
            sel.expand(focus, context, not.getMatchOptions(), not.getWeight());
        if(child == FullText.NULL_QUERY)
            return child;
        not.setChild(child);
        return not;
    }
    
    public boolean hasExcludesOrOccurs()
    {
        return true;
    }
}
