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


/**
 * General container iterator.
 * <p>
 * Enumerates nodes of the 'container' iterator that contain at least one node
 * of the contained iterator.
 * <p>Replaces both the Ancestor and the Fulltext Container iterators.
 * <p>The 'predicate' iterator can typically be a boolean predicate 
 * represented by iterators All, Any, Occurs etc.
 * Clauses:<ul>
 * <li>Basic iterator: any kind
 * <li>Or, And, Then (ordered And), Not
 * <li>MildNot (not in) is FT specific
 * <li>Repeat: from M to N on an iterator
 * <li>Depth constraint: manage Child/Descendant
 * </ul>
 */
public class ContainerIterator extends PostingIteratorBase
{
    private PostingIterator containing;
    private PostingIterator contained;
    private PostingIterator ignored;
    
    public ContainerIterator(PostingIterator containing,
                             PostingIterator contained)
    {
        this.containing = containing;
        this.contained = contained;
    }
    
    public PostingIterator bornAgain()
    {
        ContainerIterator clone = new ContainerIterator(containing.bornAgain(),
                                                        contained.bornAgain());
        if(ignored != null)
            clone.setIgnored(ignored.bornAgain());
        return copyFilters(clone);
    }
    
    public void setIgnored(PostingIterator iterator)
    {
        ignored = iterator;
    }

    public int/*NId*/ getNodeSpan() {
        return containing.getNodeSpan();
    }
    
    public int/*DId*/  getDepth() {
        return containing.getDepth();
    }
    
    public boolean skipToDoc(int/*DId*/docId)
        throws EvaluationException
    {
        for (;;) {
            if (!containing.skipToDoc(docId))
                return noMoreDocs();
            int/*DId*/cdoc = containing.getDocId();
            if (!contained.skipToDoc(cdoc))
                return noMoreDocs();
            int/*DId*/idoc = contained.getDocId();
            if (cdoc == idoc) {
                if (cdoc != curDocId) {
                    changeDoc(cdoc);
                }
                return true;
            }
            docId = Math.max(cdoc, idoc);
        }
    }
    
    protected boolean basicSkipToNode(int/*NId*/ nodeId, int/*NId*/ limit)
        throws EvaluationException
    {
        for (;;) {
            if (!containing.skipToNode(nodeId, limit))
                return noMoreNodes(limit);
            int/*NId*/ cstart = containing.getNodeId();
            int/*NId*/ cend = containing.getNodeEnd();
 
            if(cstart < containing.getPrevNodeEnd()) {
                contained.resetToNode(cstart);
            }
            if(contained.inRange(cstart, cend)) {
                
                curNodeId = cstart;
                return true;
            }
            // failed: force container to move
            nodeId = cstart + 1;
            // TODO? how to progress more quickly ?
            // method contained.getNextStart() ?
        }
    }

    public void resetToNode(int/*NId*/  nodeId)
    {
        containing.resetToNode(nodeId);
        curNodeId = containing.getNodeId();        
    }

    // ----------------------------------------------------------------------
    
    // @see com.qizx.queries.iterators.PostingIteratorBase#initContainer(com.qizx.queries.iterators.ContainerIterator, com.qizx.api.fulltext.FullTextFactory)
    public void initContainer(ContainerIterator cont, FullTextFactory scoringFactory)
    {
        if(depthTest != -1) // otherwise useless
            this.container = cont;
        if(ftSelection != null) {   // implements FTContains
            // not same as PostingIteratorBase
            this.scoringInfo = new ScoringInfo(contained, scoringFactory);
        }

        containing.initContainer(cont, scoringFactory);
        // propagate down with 'this' as the container: attention!
        contained.initContainer(this, scoringFactory);
    }

    /**
     * Called for each word actually used in the query. Not called for words
     * excluded by full-text expressions 'ftnot' and 'not in'.
     * @param word
     * @param termFrequency
     * @param inverseDocFrequency
     * @param wordBoost
     */
    void includedWord(char[] word, double termFrequency,
                      double inverseDocFrequency, double wordBoost)
    {
    }


    public double getFulltextScore(Node node) throws EvaluationException
    {
        return contained.getFulltextScore(node);
    }
    
}
