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
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.queries.FullText.PosFilters;

import java.util.Arrays;
import java.util.Comparator;


/**
 * Parent class of iterators handling a list of sub-iterators, like AllIterator
 * or PhraseIterator.
 */
public abstract class PolyIterator extends PostingIteratorBase
{
    protected PostingIterator[] iterators;
    protected Constraints constraints;
    protected int/*NId*/ currentResetPos;
    
    protected int/*NId*/ matchEnd;
    
    protected float[] subScores; // recycled for score computation
    private PostingIterator[] orderedChildren; // recycled
    

    public PolyIterator(PostingIterator[] iters)
    {
        this.iterators = iters;
    }
    
    protected PostingIterator[] copy(PostingIterator[] preds)
    {
        PostingIterator[] nc = new PostingIterator[iterators.length];
        for (int i = 0; i < nc.length; i++) {
            nc[i] = iterators[i].bornAgain();
        }
        return nc;
    }

    protected PostingIterator copyConstraints(PolyIterator clone)
    {
        clone.constraints = constraints; // immutable
        clone.setOrdered(ordered);
        return copyFilters(clone);
    }

    public void resetToNode(int/*NId*/ nodeId)
    {
        for (int i = 0, length = iterators.length; i < length; i++) {
            iterators[i].resetToNode(nodeId);
        }
    }
    

    public void resetDoc()
    {
        curDocId = -1;
        curNodeId = -1;
        for (int i = 0, length = iterators.length; i < length; i++) {
            iterators[i].resetDoc();
        }
    }

    // this implementation requires ALL subiters to match the same doc
    // OrIterator is different and redefines it
    public boolean skipToDoc(int/*DId*/ docId) throws EvaluationException
    {
        if(iterators.length == 0) {
            changeDoc(docId);
            return true;
        }
        for (;;) {
            // find a conjunction between all iterators
            int/*DId*/ firstDoc = MAX_DOCID, lastDoc = 0;
            for (int pt = iterators.length; --pt >= 0;) {
                if (!iterators[pt].skipToDoc(docId))
                    return noMoreDocs();
                int/*DId*/ doc = iterators[pt].getDocId();
                if (doc > lastDoc)
                    lastDoc = doc;
                if (doc < firstDoc)
                    firstDoc = doc;
            }
            if (lastDoc == firstDoc) { // OK
                changeDoc(firstDoc);
                return true;
            }
            docId = lastDoc;
        }
    }

    public int/*NId*/ getNodeId()
    {
        return curNodeId;
    }

    public int/*NId*/ getNodeEnd()
    {
        return matchEnd;
    }
    
    // full-text constraints (aka position filters)
    protected static class Constraints
    {
        int minDistance = -1;
        int maxDistance = -1;
        int window = -1;
        int content;
    }
    
    public void setDistanceConstraint(int min, int max)
    {
        if(constraints == null)
            constraints = new Constraints();
        constraints.minDistance = min;
        constraints.maxDistance = max;
    }    
    
    public void setWindowConstraint(int window)
    {
        if(constraints == null)
            constraints = new Constraints();
        constraints.window = window;
    }
    
    public void setContentConstraint(int content)
    {
        constraints.content = content;
    }
    
    public int computeWordDistance(int/*NId*/ posting1, int/*NId*/ posting2)
        throws EvaluationException
    {
        return iterators[0].computeWordDistance(posting1, posting2);
    }

    public boolean checkWordDistance(int/*NId*/ posting1, int/*NId*/ posting2,
                                     int offset, int min, int max)
        throws EvaluationException
    {
        return iterators[0].checkWordDistance(posting1, posting2, offset, min, max);
    }

    public boolean checkBoundary(int/*NId*/ posting, int/*NId*/ boundary,
                                 boolean start)
        throws EvaluationException
    {
        PostingIterator[] kids = getChildrenInOrder();
        if(start)
            return kids[0].checkBoundary(posting, boundary, start);
        else
            return kids[kids.length - 1].checkBoundary(posting, boundary, start);
    }

    public int/*NId*/ closestTextNode(int/*DId*/ doc, int/*NId*/ posting, boolean before)
    {
        if(iterators.length == 0)
            return 0;
        return iterators[0].closestTextNode(doc, posting, before);
    }

    public void initContainer(ContainerIterator container, FullTextFactory scoringFactory)
    {
        setContainer(container, scoringFactory);
        for (int i = 0, length = iterators.length; i < length; i++)
            iterators[i].initContainer(container, scoringFactory);
    }

    
//    public void getFulltextSelections(java.util.Collection list)
//    {
//        if(ftSelection != null)
//            list.add(ftSelection);
//        else
//            for (int i = 0, length = iterators.length; i < length; i++)
//                iterators[i].getFulltextSelections(list);
//    }

    protected boolean checkFTConstraints(int/*NId*/ rangeStart, int/*NId*/ rangeEnd)
        throws EvaluationException
    {
        if(constraints == null)
            return true;

           
        PostingIterator[] kids = getChildrenInOrder();
        int nkids = kids.length;
        if(nkids == 0)
            return false; // should not happen
        PostingIterator first = kids[0];
        PostingIterator last = kids[nkids - 1];
        
        if(constraints.window > 0) {
            // only word distance supported:
            if(!first.checkWordDistance(first.getNodeId(), last.getNodeEnd(),
                                        // ATTENTION adapt to checkWordDistance
                                        +1, 0, constraints.window))
                return false;
        }

        if(constraints.minDistance >= 0 || constraints.maxDistance >= 0) {
            for(int k = 0; k < nkids - 1; ++k) {
                // attention: distance is the number of words between 2 postings,
                // not the difference of postings => offset -1
                
                int/*NId*/ kidEnd = kids[k].getNodeEnd();
                int/*NId*/ nkidStart = kids[k + 1].getNodeId();
                     
                if(!kids[k].checkWordDistance(kidEnd, nkidStart, -1,
                                              constraints.minDistance,
                                              constraints.maxDistance))
                    return false;
            }
        }

        if(constraints.content != PosFilters.UNSPECIFIED) {
            switch(constraints.content) {
            case PosFilters.AT_START:
                if(!first.checkBoundary(first.getNodeId(), rangeStart, true))
                    return false;
                break;
            case PosFilters.AT_END:
                if(!first.checkBoundary(last.getNodeEnd(), rangeEnd, false))
                    return false;
                break;
            case PosFilters.ENTIRE_CONTENT:
                if(!first.checkBoundary(first.getNodeId(), rangeStart, true) ||
                   !last.checkBoundary(last.getNodeEnd(), rangeEnd, false))
                        return false;
                // children must cover the range and be contiguous. Bummer!
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
                break;
            }
        }
        return true; 
    }

    public PostingIterator[] getChildrenInOrder()
    {
        if(orderedChildren == null) {
            orderedChildren = new PostingIterator[iterators.length];
            for (int i = iterators.length; --i >= 0;)
                orderedChildren[i] = iterators[i];
        }
        Arrays.sort(orderedChildren, iteratorCompar);
        return orderedChildren;
    }
    
    public static Comparator iteratorCompar = new Comparator() {
        public int compare(Object o1, Object o2)
        {
            PostingIterator it1 = (PostingIterator) o1;
            PostingIterator it2 = (PostingIterator) o2;
            
            return (int) (it1.getNodeId() - it2.getNodeId());
        }
    };
}
