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
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.Scorer;

/**
 * Returns one dummy hit if 'counted' iterator verifies the count constraints.
 * <p>
 * Predicate iterator, can only be used by ContainerIter. NOT meant for
 * actually iterating.
 */
public class CountIterator extends PostingIteratorBase
{
    protected PostingIterator counted;
    protected int min, max;
    protected boolean docHasHits; // true if requested doc has hits
    
    /**
     * @param max if < 0 : no max
     */
    public CountIterator(int min, int max, PostingIterator counted)
    {
        this.counted = counted;
        counted.setOrdered(true);   // yeah we need it
        this.min = min;
        this.max = max;
    }

    public PostingIterator bornAgain()
    {
        return copyFilters(new CountIterator(min, max, counted.bornAgain()));
    }

    public boolean skipToDoc(int/*DId*/ docId) throws EvaluationException
    {
        boolean ok = counted.skipToDoc(docId);
        curDocId = counted.getDocId();
        // it is not always a failure if no more hits
        docHasHits = ok && (curDocId == docId);
        // This a bit of a hack: even though no hits in this doc, we use it 
        // anyway because this iterator can be used to implement ftnot...
        if(!docHasHits) {
            changeDoc(docId);
        }
        return true;
    }

    public void resetToNode(int/*NId*/ nodeId)
    {
        counted.resetToNode(nodeId);
    }

    // useful?
    protected boolean basicSkipToNode(int/*NId*/ nodeId, int/*NId*/ limit) 
        throws EvaluationException
    {
        return inRange(nodeId, MAX_NODEID);
    }

    public boolean inRange(int rangeStart, int rangeEnd)
        throws EvaluationException
    {
        counted.resetToNode(rangeStart);
        int count = counted.nextBefore(rangeEnd) &&     // needs be ordered
                    curDocId == counted.getDocId()
                  ? 1 : 0;
        
        // test count for 'ftnot' or 'not empty'
        if(max >= 0 && count > max) 
            return false;
        // take first occurrence of 'counted' as position
        if(count > 0)
            curNodeId = counted.getNodeId();
        
        // decision can be made with this number of occurrences:
        int neededCount = (max < 0)? min : (max + 1);
        if(curDocId == counted.getDocId())
            for( ; count < neededCount && counted.nextBefore(rangeEnd); ) {
                if(count == 0)
                    curNodeId = counted.getNodeId();
                ++count;
            }

        if((min < 0 || count >= min) && (max < 0 || count <= max)) {
            return true;
        }
        return false;
    }

    public void initContainer(ContainerIterator container, FullTextFactory scoringFactory)
    {
        setContainer(container, scoringFactory);
        counted.initContainer(container, scoringFactory);
    }

//    public boolean checkDepths(PostingIterator container)
//    {
//        return counted.checkDepths(container);
//    }

    public boolean checkWordDistance(int/*NId*/ posting1, int/*NId*/ posting2,
                                     int offset, int min, int max)
        throws EvaluationException
    {
        return counted.checkWordDistance(posting1, posting2, offset, min, max);
    }    
    
    public boolean checkBoundary(int posting, int boundary, boolean start)
        throws EvaluationException
    {
        return counted.checkBoundary(posting, boundary, start);
    }

    public int/*NId*/ closestTextNode(int/*DId*/ doc, int/*NId*/ posting,
                                      boolean before)
    {
        return counted.closestTextNode(doc, posting, before);
    }

    public float computeWeighting(Scorer scorer)
    {
        return counted.computeWeighting(scorer);
    }

    public float computeScore(Scorer scorer)
    {
        return counted.computeScore(scorer);
    }

}
