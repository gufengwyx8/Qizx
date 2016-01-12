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
 * Union of iterators.
 * In enumeration mode, simply find first matching iterator, without sort: 
 * not quite fast, but much more memory efficient.
 * In predicate mode, no need to check order, so test each iterator in turn.
 */
public class OrIterator extends PolyIterator
{
    protected int iterCount;
    protected PostingIterator current;
    protected int activeCnt;
    // iterators having a hit in current document:
    protected PostingIterator[] active;

    public OrIterator(PostingIterator[] iterators)
    {
        this(iterators, iterators.length);
    }

    public OrIterator(PostingIterator[] iterators, int count)
    {
        super(iterators);
        iterCount = count;
        active = new PostingIterator[count];
    }

    public PostingIterator bornAgain()
    {
        return copyConstraints(new OrIterator(bornAgain(iterators, iterCount)));
    }

    public int/*NId*/ getNodeSpan()
    {
        return matchEnd - curNodeId;
    }

    public int/*DId*/ getDepth()
    {
        return current == null? 0 : current.getDepth();
    }
    
    /**
     * Returns the rank of current child iterator among all children.
     */
    public int getRankOfCurrent()
    {
        for(int r = iterators.length; --r >= 0; )
            if(current == iterators[r])
                return r;
        return -1;
    }

    public boolean skipToDoc(int/*DId*/ docId) throws EvaluationException
    {
        curNodeId = -1;
        activeCnt = 0;
        // take the min doc of iterators: 
        // create list of active iterators which are on same current doc
        curDocId = MAX_DOCID;
        for (int i = iterCount; --i >= 0;) {
            PostingIterator it = iterators[i];
            if (!it.skipToDoc(docId))
                continue;
            int/*DId*/ doc = it.getDocId();
            if (doc < curDocId) {
                activeCnt = 1;
                active[0] = it;
                curDocId = doc;
                curNodeId = -1;
            }
            else if (doc == curDocId) {
                active[activeCnt++] = it;
            }
        }
        
        return activeCnt > 0;
    }

    protected boolean basicSkipToNode(int/*NId*/ nodeId, int/*NId*/ limit)
        throws EvaluationException
    {
        curNodeId = MAX_NODEID;
        current = null;
        matchEnd = nodeId;
        // consider only active iterators: take first (ordered)
        for (int i = activeCnt; --i >= 0;) {
            PostingIterator it = active[i];
            if (it != null) {
                if (!it.skipToNode(nodeId, limit)) {
                    if(it.reachedEnd())
                        active[i] = null;
                }
                else {
                    int/*NId*/ node = it.getNodeId();
                    if (node < curNodeId) {
                        curNodeId = node;
                        current = it;
                        matchEnd = it.getNodeEnd();
                    }
                }
            }
        }
        // -System.err.println("curNodeId "+curNodeId+" "+current);
        return current != null;
    }
    
    public boolean inRange(int/*NId*/ rangeStart, int/*NId*/ rangeEnd)
        throws EvaluationException
    {
        for (int i = 0; i < activeCnt; i++) {
            PostingIterator it = active[i];
            if(it != null && it.inRange(rangeStart, rangeEnd)) {
                curNodeId = it.getNodeId();
                matchEnd = it.getNodeEnd();
                // TODO: constraints should be applied on children 
                if((constraints == null || checkFTConstraints(rangeStart, rangeEnd))
                    && checkFilters())
                {
                    current = it;
                    return true;
                }
            }
        }
        return false;
    }
    
    public void resetToNode(int/*NId*/ nodeId)
    {
        // some iterators may have been desactivated on eod, so check again
        activeCnt = 0;
        for (int i = iterCount; --i >= 0;) {
            PostingIterator it = iterators[i];
            if (it.getDocId() == curDocId) {
                active[activeCnt++] = it;
                it.resetToNode(nodeId);
            }
        }
        currentResetPos = nodeId;
        curNodeId = nodeId - 1;
    }

    public double getFulltextScore(Node node) throws EvaluationException
    {
        if(scoringInfo != null) 
            return doScoring(node);

     // used in a Container with a 'or' clause TODO? not hardcoded, use scorer
        double max = -1;
        for (int i = 0; i < iterators.length; i++) {
            double score = iterators[i].getFulltextScore(node);
            if(score > max) {
                max = score;
            }
        }
        return max;
    }

    public float computeWeighting(Scorer scorer)
    {
        float[] subWeights = new float[iterCount];
        for (int i = 0; i < iterCount; i++) {
            subWeights[i] = iterators[i].computeWeighting(scorer);
        }
        weightNorm = scorer.normOr(subWeights);
        return weight / weightNorm;
    }

    public float computeScore(Scorer scorer)
    {
        if(subScores == null)
            subScores = new float[iterCount];
        for (int i = 0; i < iterCount; i++) {
            if(iterators[i].getDocId() == getDocId())
                subScores[i] = iterators[i].computeScore(scorer);
            else
                subScores[i] = 0;
        }
        return weight * weightNorm * scorer.scoreOr(subScores, iterCount); 
    }
}
