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
 * Base for composite SegmentIterators (Intersection, Union, Inclusion).
 */
public abstract class BinaryIterator extends PostingIteratorBase
{
    protected PostingIterator it1, it2;
    protected int/*NId*/ currentResetPos;
    protected int/*NId*/ nodeSpan;
    protected int/*DId*/ nodeDepth;

    protected BinaryIterator(PostingIterator iterator1,
                             PostingIterator iterator2)
    {
        it1 = iterator1;
        it2 = iterator2;
    }

    public int/*NId*/ getNodeSpan()
    {
        return nodeSpan;
    }

    public int/*DId*/ getDepth()
    {
        return nodeDepth;
    }
    
    public boolean checkWordDistance(int/*NId*/posting1, int/*NId*/posting2,
                                     int offset, int min, int max)
        throws EvaluationException
    {
        return it1.checkWordDistance(posting1, posting2, offset, min, max);
    }

    public boolean checkBoundary(int posting, int boundary, boolean start)
        throws EvaluationException
    {
        return it1.checkBoundary(posting, boundary, start);
    }

    public int/*NId*/closestTextNode(int/*DId*/doc, int/*NId*/posting,
                                      boolean before)
    {
        return it1.closestTextNode(doc, posting, before);
    }

    public double getFulltextScore(Node node)
        throws EvaluationException
    {
        double s = it1.getFulltextScore(node);
        return s >= 0 ? s : it2.getFulltextScore(node);
    }

    public boolean skipToDoc(int/*DId*/ docId) throws EvaluationException
    {
        // first, move both iterators to required docId:
        if (!it1.skipToDoc(docId) || !it2.skipToDoc(docId))
            return noMoreDocs();
        // find a match:
        while (it1.getDocId() != it2.getDocId()) {
            // System.err.println("not same "+it1.getDocId()+"
            // "+it2.getDocId());
            // TODO could be slightly more efficient (only one skipToDoc)
            if (!it1.skipToDoc(it2.getDocId()))
                return noMoreDocs();
            if (!it2.skipToDoc(it1.getDocId()))
                return noMoreDocs();
        }
        // System.err.println("CskipToDoc "+docId+" -> "+it1.getDocId()+"
        // "+it2.getDocId());
        if (it1.getDocId() > curDocId) {
            changeDoc(it1.getDocId()); // or it2.getDocId()
        }
        return true;
    }

    public void resetToNode(int/*NId*/nodeId)
    {
        currentResetPos = nodeId;
        it1.resetToNode(nodeId);
        it2.resetToNode(nodeId);
        curNodeId = -1;
    }

    public void initContainer(ContainerIterator container, FullTextFactory scoringFactory)
    {
        setContainer(container, scoringFactory);
        it1.initContainer(container, scoringFactory);
        it2.initContainer(container, scoringFactory);
    }

    protected void getValues(PostingIterator it)
    {
        curNodeId = it.getNodeId();
        nodeSpan = it.getNodeSpan();
        nodeDepth = it.getDepth();
    }

    public void debug() throws EvaluationException
    {
        System.err.println(this + " -- iter1 ->");
        it1.debug();
        System.err.println(this + " -- iter2 ->");
        it2.debug();
        PostingIterator me = bornAgain();
        for (; me.nextNode();) {
            System.err.println("= " + me.getNodeId());
        }
    }

}
