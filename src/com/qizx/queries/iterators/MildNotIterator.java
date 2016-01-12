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
package com.qizx.queries.iterators;

import com.qizx.api.EvaluationException;
import com.qizx.api.fulltext.Scorer;

/**
 * Implements "not in".
 */
public class MildNotIterator extends BinaryIterator
{
    public MildNotIterator(PostingIterator what,
                           PostingIterator enclosing)
    {
        super(what, enclosing);
    }

    public PostingIterator bornAgain()
    {
        return copyFilters(new MildNotIterator(it1.bornAgain(),
                                               it2.bornAgain()));
    }

    protected boolean basicSkipToNode(int/*NId*/ nodeId, int/*NId*/ limit)
        throws EvaluationException
    {
        for(;;) {
            if(!it1.skipToNode(nodeId, limit))
                return noMoreNodes();
            if(!findInclusion()) {
                getValues(it1);
                return true;
            }
            nodeId = it1.getNodeId() + 1;
        }
    }

    public boolean inRange(int/*NId*/ rangeStart, int/*NId*/ rangeEnd)
        throws EvaluationException
    {
        it1.resetToNode(rangeStart);
        it2.resetToNode(rangeStart);
        return nextBefore(rangeEnd);
    }

    public boolean nextBefore(int/*NId*/ rangeEnd) throws EvaluationException
    {
        for(;;) {
            if(it1.getNodeId() >= rangeEnd || !it1.nextBefore(rangeEnd))
                return noMoreNodes();
            if(!findInclusion()) {
                getValues(it1);
                return true;
            }
        }
    }

    // tries to find an inclusion of it1 in it2 
    private boolean findInclusion() throws EvaluationException
    {
        if(!it2.skipToEnclosing(it1.getNodeId())) {  // TODO huh? correct?
            
            it2.resetToNode(it1.getNodeId() + 1);
            return false;
        }
          
        return it1.getNodeEnd() <= it2.getNodeEnd() &&
               it1.getNodeId() >= it2.getNodeId();
    }

    public float computeWeighting(Scorer scorer)
    {
        weightNorm = it1.computeWeighting(scorer);
        return weight;
    }

    public float computeScore(Scorer scorer)
    {
        return weight * weightNorm * it1.computeScore(scorer); 
    }
}
