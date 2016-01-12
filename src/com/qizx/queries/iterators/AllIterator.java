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
import com.qizx.api.Node;
import com.qizx.api.fulltext.Scorer;

/**
 * Represents the 'all' clauses of FT. 
 * Sub-iterators can be any other FT iterator like Term, Or, Phrase.
 * <p>
 * DIFFERENT than IntersectionIterator, because enumerates the Cartesian product
 * of sub-iterators. 
 * <p>
 * <b>Always</b> used as a predicate iterator within a ContainerIterator.
 */
public class AllIterator extends PolyIterator
{
    protected PostingIterator fastest;
    // If ordered and using constraints like "window < N" or "distance == 0",
    // (typically: PhraseIter) then it is possible to iterate forward only
    protected boolean fastPhrase;
    protected boolean fastInit;
    
    public AllIterator(PostingIterator[] iterators)
    {
        super(iterators);
    }

    public PostingIterator bornAgain()
    {
        return copyConstraints(new AllIterator(copy(iterators)));
    }

    private void init()
    {
        if(!fastInit) {
            fastInit = true;
            // useful only for this:
            fastPhrase = ordered && constraints != null && 
                       (constraints.window > 0 || constraints.maxDistance == 0);

            // iterate fastest on the last subiter: (except if 'fastPhrase')
            if(iterators.length > 0)
                fastest = fastPhrase? iterators[0] : iterators[iterators.length - 1];
        }
    }

    protected boolean basicSkipToNode(int/*NId*/ nodeId, int/*NId*/ limit)
        throws EvaluationException
    {
        return inRange(nodeId, limit);
    }

    public void resetToNode(int/*NId*/ nodeId)
    {
        init();
        super.resetToNode(nodeId);
        // remember range start
        currentResetPos = nodeId;
    }
    
    public boolean inRange(int/*NId*/ rangeStart, int/*NId*/ rangeEnd)
        throws EvaluationException
    {
        init();
        currentResetPos = rangeStart;
        if(iterators.length == 0)
            return false;

        // Dispatch to specialized methods:
        if(fastPhrase)
            return phraseInRange(rangeStart, rangeEnd);
        
        // quick unordered test:
        int/*NId*/ start = rangeStart;
        for (int i = 0, size = iterators.length; i < size; i++) {
            if(!iterators[i].inRange(start, rangeEnd))
                return false;
            if(ordered)
                start = iterators[i].getNodeId();
        }
        computeBounds();
        if ((constraints == null || checkFTConstraints(rangeStart, rangeEnd))
                && checkFilters())
            return true;
        // failure of constraints: use heavier means
        return nextBefore(rangeEnd);
    }
    
    // Typically used for a phrase 
    //    (more generally: ordered + max window constraint).
    //
    private boolean phraseInRange(int/*NId*/ rangeStart, int/*NId*/ rangeEnd)
        throws EvaluationException
    {
        int/*NId*/ start = rangeStart;
    
        for (;;) {
            for (int i = 0, size = iterators.length; i < size; i++) {
                if(!iterators[i].skipToNode(start, rangeEnd))
                    return false;
                start = iterators[i].getNodeId() + 1;
                if(start > rangeEnd) { // strict > because of + 1
                    return false;
                }
            }
            computeBounds();
            if ((constraints == null || checkFTConstraints(rangeStart, rangeEnd))
                    && checkFilters())
                   return true;

            // move first iterator
            start = iterators[0].getNodeId() + 1;
        }
    }

    // assume that sub-iters are already on a hit
    public boolean nextBefore(int/*NId*/ rangeEnd) throws EvaluationException
    {
        init();
        if(fastPhrase)
            return phraseNextBefore(rangeEnd);

        for(;;) {
            if(!fastest.nextBefore(rangeEnd)) {
                // end of fastest subiter: try move previous sub-iter and so on
                if(!moveOtherIters(iterators.length - 2, rangeEnd))
                    return false;
                // OK retry on fastest
                if(!fastest.nextBefore(rangeEnd))
                    return false;   // can happen if ordered
            }
            // OK, compute first and last position of this match:
            computeBounds();
            // window/distance constraints
            if(constraints != null && !checkFTConstraints(currentResetPos, rangeEnd)
               || !checkFilters())
                continue;
            return true;
        }
    }

    /**
     * Recursively moves the previous iterators.
     */
    private boolean moveOtherIters(int rank, int/*NId*/rangeEnd)
        throws EvaluationException
    {
        if (rank < 0)
            return false;
        if (!iterators[rank].nextBefore(rangeEnd)) {
            if (!moveOtherIters(rank - 1, rangeEnd))
                return false;
            // iterator at this level has been reset:
            if (!iterators[rank].goNextNode(rangeEnd)) {
                return false;
            }
        }
        // reset iterator at next level:
        if(ordered) {
            // start from *current* token not from next token
            iterators[rank + 1].resetToNode(iterators[rank].getNodeId());
        }
        else
            iterators[rank + 1].resetToNode(currentResetPos);
        return true;
    }

    /**
     * Faster version of moveOtherIters typically used for a phrase 
     *    (more generally if ordered + max window constraint).
     * Iterates faster on first iterator, which "drags" others
     */
    private boolean phraseNextBefore(int/*NId*/ rangeEnd)
        throws EvaluationException
    {
        for (;;) {
            if (fastest.getNodeId() >= rangeEnd
                || !fastest.nextBefore(rangeEnd))
                return false;
            int/*NId*/start = iterators[0].getNodeId();
            for (int i = 1, size = iterators.length; i < size; i++) {
                if(!iterators[i].skipToNode(start, rangeEnd))
                    return false;
                start = iterators[i].getNodeId();
                if(start >= rangeEnd)
                    return false;
            }
            computeBounds();
            if(constraints != null && !checkFTConstraints(currentResetPos, rangeEnd)
               || !checkFilters())
                continue;
            return true;
        }
    }

    protected void computeBounds() throws EvaluationException
    {
        if(ordered) {
            curNodeId = iterators[0].getNodeId();
            // BUG : max
            matchEnd = iterators[iterators.length - 1].getNodeEnd();
        }
        else {
            curNodeId = MAX_NODEID;
            matchEnd = -1;
            for (int p = 0, cnt = iterators.length; p < cnt; p++) {
                int/*NId*/ s = iterators[p].getNodeId();
                int/*NId*/ e = iterators[p].getNodeEnd();
                if(s < curNodeId)
                    curNodeId = s;
                if(e > matchEnd)
                    matchEnd = e;
            }
        }
    }

    @Override
    public boolean checkContiguity() throws EvaluationException
    {
        PostingIterator[] kids = getChildrenInOrder();
        int nkids = kids.length;
        if(nkids == 0)
            return false; // should not happen
        PostingIterator first = kids[0];
        
        int/*NId*/ reach = first.getNodeEnd();
        for(int k = 1; k < nkids; ++k) {
            if(kids[k].getNodeId() > reach + 1)
                return false;
            int/*NId*/ end = kids[k].getNodeEnd();
            if(end > reach)
                reach = end;
            if(!kids[k].checkContiguity())
                return false;
        }
        return true;
    }

    public PostingIterator[] getChildrenInOrder()
    {
        if(ordered)
            return iterators;
        return super.getChildrenInOrder();
    }

    public double getFulltextScore(Node node) throws EvaluationException
    {
        if(scoringInfo != null) 
            return doScoring(node);

     // used when a Container has a 'and' clause /TODO? not hardcoded, use scorer
        double sqSum = 0;
        int count = 0;
        for (int i = 0; i < iterators.length; i++) {
            double score = iterators[i].getFulltextScore(node);
            if(score >= 0) {
                sqSum += score * score;
                count ++;
            }
        }
        return count == 0? -1 : Math.sqrt(sqSum / count);
    }

    public float computeWeighting(Scorer scorer)
    {
        float[] subWeights = new float[iterators.length];
        for (int i = 0; i < iterators.length; i++) {
            subWeights[i] = iterators[i].computeWeighting(scorer);
        }
        weightNorm = scorer.normAll(subWeights);
        return weight / weightNorm; // propagate to upper level
    }

    public float computeScore(Scorer scorer)
    {
        if(subScores == null)
            subScores = new float[iterators.length];
        for (int i = 0; i < iterators.length; i++) {
            subScores[i] = iterators[i].computeScore(scorer);
        }
        return weight * weightNorm * scorer.scoreAll(subScores);
    }
}
