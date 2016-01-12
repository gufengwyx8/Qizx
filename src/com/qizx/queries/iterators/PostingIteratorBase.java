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
import com.qizx.api.fulltext.Scorer;
import com.qizx.queries.FullText;
import com.qizx.queries.FullText.Selection;
import com.qizx.util.basic.Util;
import com.qizx.xdm.FONIDocument;
import com.qizx.xdm.FONIDataModel.FONINode;

/**
 *	Base class for all implementations of PostingIterator
 */
public abstract class PostingIteratorBase
    implements PostingIterator
{
    protected Filter[] filters;
    protected int depthTest = -1;
    protected ContainerIterator container;

    protected boolean ordered;  // used in PolyIter, WildCard, Or
   
    protected int/*DId*/ curDocId = -1;
    protected int/*NId*/ curNodeId = -1;
    protected int/*NId*/ prevNodeEnd = -1;

    // FT scoring:
    protected Selection ftSelection;
    protected ScoringInfo scoringInfo;  // only if realizes child of ftcontains
    protected float weight = 1;  // optional, default 1 
    protected float weightNorm;        // weight * normalization
    
    // cached document data model
    protected FONIDocument dm;
 

    public int/*DId*/ getDocId()
    {
        return curDocId;
    }
    
    public FONIDocument currentDataModel()
    {
        return dm;
    }

    public void resetDoc()
    {
        curDocId = -1;
        curNodeId = -1;
    }
  
    public boolean goNextDoc() throws EvaluationException
    {
        dm = null;
        return skipToDoc(curDocId + 1);
    }

    public int/*NId*/ getNodeId()
    {
        return curNodeId;
    }

    public int/*NId*/ getNodeEnd() throws EvaluationException
    {
        if (curNodeId < 0) {
            skipToNode(0, MAX_NODEID);
        }
        return curNodeId + getNodeSpan();
    }

    public int/*NId*/ getPrevNodeEnd()
    {
        return prevNodeEnd;
    }

    public int/*DId*/ getDepth()
    {
        return 0; // default
    }

    
    public boolean reachedEnd()
    {
        return curNodeId >= MAX_NODEID;
    }

    public int/*NId*/ getNodeSpan()
    {
        return 0; // default
    }

    // called when this iter is the rhs of a ftcontains (the ContainerIter is
    // often absent)
    public void setFulltextSelection(FullText.Selection selection)
    {
        this.ftSelection = selection;
    }

    // default implementation: ContainerIter really implements it 
    public double getFulltextScore(Node node) throws EvaluationException
    {
        if(scoringInfo != null) 
            return doScoring(node);
        return -1;
    }
    
    // assumes scoringInfo != null, when rhs of a ftcontains
    protected double doScoring(Node node) throws EvaluationException
    {
        if (scoringInfo.scorer == null) {
            scoringInfo.init();
        }
        return -1;
    }
    
    private boolean changeScoredDocument(int docId, FONINode node)
    {
        if(docId <= 0 || docId == scoringInfo.currentScoredDocId)
            return false;
        
        scoringInfo.currentScoredDocId = docId;
        scoringInfo.currentScore = 0;
        return true;
    }
    
//    private boolean changeScoredDocument(Document doc, FONINode node)
//    {
//        if(doc == null || doc.equals(scoringInfo.currentScoredDoc))
//            return false;
//        
//        scoringInfo.currentScoredDoc = doc;
//        scoringInfo.currentScore = 0;
//        return true;
//    }
    
    public void setWeight(float weight)
    {
        this.weight = weight;
    }
    
    
    protected static class ScoringInfo
    {
        public FullTextFactory scoringFactory;
        private PostingIterator ftIterator;
        public int currentScoredDocId;
        private double currentScore;
        // When used for full-text: info for scoring
        private Scorer scorer;
        
        public ScoringInfo(PostingIterator ft, FullTextFactory scoringFactory)
        {
            ftIterator = ft;
            this.scoringFactory = scoringFactory;
            // brutal normalization: weight of top FT expression forced to 1
            ft.setWeight(1);
        }

        public void init()
        {
            scorer = scoringFactory.createScorer();
            ftIterator.computeWeighting(scorer);
        }

    }

    public float computeWeighting(Scorer scorer)
    {
        System.err.println("OOPS computeWeighting !!! "+this);
        return 1;   // should not happen
    }
   
    public float computeScore(Scorer scorer)
    {
        throw Util.shouldNotHappen(); 
    }

    public int computeWordDistance(int/*NId*/ posting1, int/*NId*/ posting2)
        throws EvaluationException
    {
        throw Util.shouldNotHappen(getClass()); 
    }

    public boolean checkContiguity() throws EvaluationException
    {
        return true;
    }

    public boolean checkWordDistance(int/*NId*/ posting1, int/*NId*/ posting2,
                                     int offset, int min, int max)
        throws EvaluationException
    {
        throw Util.shouldNotHappen(getClass()); 
    }

    public boolean checkBoundary(int/*NId*/ posting, int/*NId*/ boundary,
                                 boolean start)
        throws EvaluationException
    {
        throw Util.shouldNotHappen(getClass()); 
    }

    public int/*NId*/ closestTextNode(int/*DId*/ doc, int/*NId*/ posting,
                                      boolean before)
    {
        throw Util.shouldNotHappen(getClass()); 
    }

    protected void changeDoc(int/*DId*/ docId)
    {
        if(curDocId != docId)
            curNodeId = -1;
        curDocId = docId;
    }        

    // VERY IMPORTANT : set curDocId to max when end reached
    protected boolean noMoreDocs()
    {
        curDocId = MAX_DOCID;
        return false;
    }

    // VERY IMPORTANT : set curNodeId to max when end reached
    protected boolean noMoreNodes()
    {
        curNodeId = MAX_NODEID;
        return false;
    }
    
    protected boolean noMoreNodes(int/*NId*/ limit)
    {
        if(limit == MAX_NODEID)
            curNodeId = limit;
        return false;
    }

    public boolean nextNode() throws EvaluationException
    {
        for (;;) {
            for (; goNextNode(MAX_NODEID);) {
                if (checkFilters())
                    return true;
            }
            dm = null;
            if (!skipToDoc(curDocId + 1))
                return false;
        }
    }

    public boolean goNextNode(int/*NId*/ limit) // intra-doc
        throws EvaluationException
    {
        if (curDocId < 0) {
            if (!skipToDoc(0))
                return false;
        }
        else if(curDocId >= MAX_DOCID)
            return false;
        return skipToNode(curNodeId + 1, limit);
    }

    public boolean skipToNode(int/*NId*/ nodeId, int/*NId*/ limit)
        throws EvaluationException 
    {
        if(curNodeId >= limit)
            return false;
        // if we are beyond curNodeId, dont move
        // (curNodeId must be ON A REAL HIT or be a dummy value MAX_NODEID]
        // NOTE: causes problems with Phrase => redefined
        if (curNodeId >= nodeId && checkFilters()) {
            return true; // need to recheck due to depth
        }
        prevNodeEnd = curNodeId + getNodeSpan(); // OPTIM do it only if some flag set
        while (basicSkipToNode(nodeId, limit)) {
            if (checkFilters() && curNodeId < limit) {
                return true;
            }
            if(curNodeId >= limit)
                return false;
            nodeId = curNodeId + 1;
        }
        return false;
    }

    abstract protected boolean basicSkipToNode(int/*NId*/ nodeId, int/*NId*/ limit)
        throws EvaluationException;


    public boolean skipToEnclosing(int/*NId*/ nodeId) throws EvaluationException
    {
        if(curNodeId < 0)
            if(!skipToNode(0, MAX_NODEID))
                return false;
        int/*NId*/ end = getNodeEnd(), cur = curNodeId;
        int cnt = 0;
        while (end < nodeId) {
            // -System.err.println("container.nextNode
            // "+container.getNodeId());
            if (!basicSkipToNode(cur + 1, MAX_NODEID)) { // avoid filter
                return false;
            }
            cur = getNodeId();
            end = getNodeEnd();
            ++cnt;
        }
        // -System.err.println(nodeId+" skipped "+cnt);
        return true;
    }

    // generic implementation: skipTo(start) and before end
    // Assumes that rangeStart will never decrease
    public boolean inRange(int/*NId*/ rangeStart, int/*NId*/ rangeEnd)
        throws EvaluationException
    {
        return skipToNode(rangeStart, rangeEnd) && getNodeId() < rangeEnd;
    }

    // generic implementation: if not already beyond, try to go to next
    public boolean nextBefore(int/*NId*/ rangeEnd) throws EvaluationException
    {
        for (;;) {
            // always move, unless already too far:
            if (getNodeId() >= rangeEnd || !goNextNode(rangeEnd))
                return false;
            // test inclusion
            if (getNodeId() < rangeEnd)
                return true;
        }
    }

    public int/*NId*/ firstFrom(int/*NId*/ posting) throws EvaluationException
    {
        resetToNode(posting);
        return goNextNode(MAX_NODEID)? getNodeId() : MAX_NODEID;
    }

    public void initContainer(ContainerIterator container, 
                              FullTextFactory scoringFactory)
    {
        setContainer(container, scoringFactory);
    }

    protected void setContainer(ContainerIterator container,
                                FullTextFactory scoringFactory)
    {
        if(depthTest != -1) // otherwise useless
            this.container = container;
        if(ftSelection != null) {   // implements FTContains
            this.scoringInfo = new ScoringInfo(this, scoringFactory);
        }
    }

    public boolean checkFilters() throws EvaluationException
    {
        if(container != null && !basicCheckDepth(container))
            return false;

        if (filters != null)
            for (int f = filters.length; --f >= 0;)
                if (!filters[f].check(this))
                    return false;
        return true;
    }

    protected boolean basicCheckDepth(ContainerIterator container)
    {
        int refDepth = (container == null)? 0 : container.getDepth();
        int ddiff = getDepth() - refDepth;
         
        if(ddiff < 0) {
            return false;
        }
        // depthTest < 0 means depth-diff must be at least  -1 - depthTest
        // depthTest >= 0 means exactly equal
        return (depthTest < 0) ? (ddiff >= -1 - depthTest) : (ddiff == depthTest);
    }

//  public boolean checkDepths(ContainerIterator container)
//  {
//      return basicCheckDepth(container);
//  }

    public Filter[] getFilters()
    {
        return filters;
    }
    public void setFilters(Filter[] filters)
    {
        this.filters = filters;
    }

    public void addFilter(Filter filter)
    {
        if (filters == null)
            filters = new Filter[] { filter };
        else {
            Filter[] old = filters;
            filters = new Filter[old.length + 1];
            System.arraycopy(old, 0, getFilters(), 0, old.length);
            filters[old.length] = filter;
        }
    }

    protected PostingIterator copyFilters(PostingIteratorBase clone)
    {
        // problem: need to clone the filters or not?
        clone.setFilters(filters);
        clone.setDepthTest(depthTest);
        
        // not really filters...
        clone.ftSelection = ftSelection;
        if(scoringInfo != null)
            clone.scoringInfo = new ScoringInfo(clone, scoringInfo.scoringFactory);
        return clone;
    }
    
    public void setDepthTest(int depthTest)
    {
        this.depthTest = depthTest;
    }
    
    static PostingIterator[] bornAgain(PostingIterator[] iterators)
    {
        if(iterators == null)
            return null;
        return bornAgain(iterators, iterators.length);
    }
    
    protected static PostingIterator[] bornAgain(PostingIterator[] iterators,
                                                 int iterCount)
    {
        if(iterators == null)
            return null;
        PostingIterator[] nit = new PostingIterator[iterCount];
        for (int i = iterators.length; --i >= 0;) {
            PostingIterator it = iterators[i];
            if(it != null)
                nit[i] = it.bornAgain();
        }
        return nit;
    }

    public void debug() throws EvaluationException
    {
        for (; nextNode();) {
            System.err.println("node " + getNodeId() + "-" + getNodeEnd()
                               + " d=" + getDepth());
        }
    }
    
    public boolean isOrdered()
    {
        return ordered;
    }
    
    public void setOrdered(boolean ordered)
    {
        this.ordered = ordered;
    }
    
    public boolean segmentOverlap(PostingIterator iter)
    {
        return false;
    }
}
