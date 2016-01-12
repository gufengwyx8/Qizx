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
import com.qizx.queries.FullText.Selection;
import com.qizx.xdm.FONIDocument;

/**
 * Iterates on postings (nodes or full-text tokens) matching a condition.
 * <ul>
 * <li>Real node positions enumerate Node Ids of a FONI document
 * <li>Word positions for full-text are of two kinds<ul>
 * </ul>
 * <p>
 * There is a inter-document interface for applicative purpose, and an
 * intra-document interface for implementations.
 * <p>
 * NB: "Node" is often used instead of posting for historical reasons.
 */
public interface PostingIterator extends NodeIterator
{
    /**
     * Forces nextBefore() to enumerate ordered postings.
     * Meaningful for some iterators only.
     */
    void setOrdered(boolean ordered);

    // ---- inter-document interface ---------------------------------------

    /**
     *	Moves to the next posting (inter-document).
     *<p> Returns false if no more posting available.
     */
    boolean nextNode() throws EvaluationException;

    /**
     * Moves before first document.
     */
    void resetDoc();
    
    /**
     *	Moves to next document. Equivalent to skipToDoc(getDocId() + 1);
     */
    boolean goNextDoc() throws EvaluationException;

    /**
     *	Moves to first document whose id is greater or equal to this docId.
     */
    boolean skipToDoc(int/*DId*/docId)
        throws EvaluationException;

    // ---- Intra-document interface: return false at the end of the current doc.
    
    /**
     *	Moves to next node (intra-document).
     *  In general equivalent to skipToNode(getNodeId() + 1);
     */
    boolean goNextNode(int/*NId*/ limit)
        throws EvaluationException;

    /**
     *	Moves to first node found from (ie after or equal) this nodeId (intra-document).
     *  Does not move if already beyond nodeId.
     * @param limit position not to be tresspassed. Often set to MAX_NODEID (no limit).
     */
    boolean skipToNode(int/*NId*/ nodeId, int/*NId*/ limit)
        throws EvaluationException;

    /**
     * Moves back or forth to node if necessary so that calling goNextNode
     * afterwards ensures we get the first node at or after nodeId.
     * <p>
     * Used in Container iterator. Defines the beginning of the containing
     * node. <p>
     * NOTE: implementations should be optimized to avoid unnecessary moves.
     */
    void resetToNode(int/*NId*/ nodeId);
   
    /**
     * Used when the iterator represents a predicate within a Container.
     * The method returns true if a hit can be found in the range.
     * Also called once (1st call) by CountIterator.
     * <p>
     * Allows some iterators (e.g Phrase) to optimize.
     * <p>Contract: rangeStart increases from one call to the next, or else
     * resetToNode is used. So the default implementation (skipTo + test end)
     * is OK, because this iterator is before or just at rangeStart, not beyond.
     * @param rangeStart a node position which serves as a first bound
     * It is assumed that this iterator is before or just at this position
     * @param rangeEnd a node position which serves as a last bound
     */
    boolean inRange(int/*NId*/ rangeStart, int/*NId*/ rangeEnd)
        throws EvaluationException;

    /**
     * Actual enumeration within a container, RARELY USED (either called by
     * CountIterator, or by inRange if constraints failed).
     * <p>
     * Always follows a inRange check, therefore sub-iterators are assumed to be
     * placed on an ACTUAL HIT.
     * <p>
     * Normally equivalent to: goNextNode + test end, except in AllIterator
     * where it has to enumerate the Cartesian product.
     */
    boolean nextBefore(int/*NId*/ rangeEnd)
        throws EvaluationException;
    
    /**
     * First node following this node, or MAXNODEID
     */
    int/*NId*/ firstFrom(int/*NId*/ posting)
        throws EvaluationException;
    
    /**
     *	Looks for next node which encloses this nodeId (intra-document).
     *  Stops if already beyond nodeId.
     *  <p>Default implem: while(nodeEnd <= nodeId) do goNext 
     *  TODO OPTIM on SingleKeyIter, Container
     *  @return true if not at end. Inclusion must be retested.
     */
    boolean skipToEnclosing(int/*NId*/ nodeId)
        throws EvaluationException;
   
    
    // ---- Return information about the current node or document:

    /**
     *	Returns the id of current document.
     */
    int/*DId*/ getDocId();
    
    /**
     *  Returns the contents of current document.
     */
    FONIDocument currentDataModel();
    
    /**
     * Returns the current match position. A node identifier or a word posting.
     */
    int/*NId*/ getNodeId();

    /**
     *	Gets the span of the current node (or match position). 
     *  Can be 0 for text indexes.
     */
    int/*NId*/ getNodeSpan();

    /**
     *	Gets the end position of the current hit.
     *  If the hit is a node, then this position is equivalent to start + span.
     *	For a FullText iterator he position can also represent the end of a 
     * phrase (PhraseIterator).
     */
    int/*NId*/ getNodeEnd()
        throws EvaluationException;


    /**
     * Returns the end position of previous node hit.
     * Used to detect embedded elements which require a resetToNode in Container
     */
    int/*NId*/ getPrevNodeEnd();

    /**
     * Returns true if no more nodes.
     */
    boolean reachedEnd();

    /**
     *	Gets the tree depth of the current node. If not available, returns -1.
     */
    int/*DId*/  getDepth();

    
    boolean segmentOverlap(PostingIterator iter);
    
    /**
     * Checks the number of words from position1 to position2, assumed to be 
     * properly ordered. Attention: "distance" == abs(p2 - p1) - offset
     * 
     * Calls to this method are delegated to primary iterators,
     * which know how to implement it.
    */
    boolean checkWordDistance(int/*NId*/ posting1, int/*NId*/ posting2,
                              int offset, int min, int max)
        throws EvaluationException;
 
    /**
     * Checks the Full-Text hit is contiguous.
     */
    boolean checkContiguity()
        throws EvaluationException;

    int computeWordDistance(int/*NId*/ posting1, int/*NId*/ posting2)
        throws EvaluationException;

    /**
     * Checks if a posting matches a boundary (at start / at end)
     * @param posting word position to check
     * @param boundary nodeId of boundary
     * @param start true for checking start, false for end
     */
    boolean checkBoundary(int/*NId*/ posting, int/*NId*/ boundary, boolean start)
        throws EvaluationException;

    /**
     * Returns the nodeId of the enclosing text node (if posting points a word,
     * and 'before' is true) or the closest previous/next text node otherwise.
     * @param i 
     */
    int/*NId*/ closestTextNode(int/*DId*/ doc, int/*NId*/ posting, boolean before);

    /**
     * Called when instantiated from a Query.FullText
     */
    void setFulltextSelection(Selection ftSelection);

    /**
     * Retrieve the first encompassed FT Container and ask it to compute a score.
     */
    double getFulltextScore(Node node)
        throws EvaluationException;

    /**
     * For iterators descendant of a FT Container iterator: compute the FT
     * score on the current document (in the current implem, only document-wise
     * scores are supported).
     * @param scorer an instance of a Scorer supporting specific weight and
     *        score computations (default is DefaultScorer).
     * @return the score: this is the weight defined by setWeight (default is
     *         1), multiplied by weight norm multiplied by a formula specific
     *         to the iterator (term, all, or, mildnot)
     */
    float computeScore(Scorer scorer);

    /**
     * Simply sets weight. Normally only one implementation.
     */
    void setWeight(float weight);

    /**
     * Upon creation, computes the normed FT weighting associated with the iterator.
     * @return the normalized weight, ie the weight multiplied
     */
    float computeWeighting(Scorer scorer);
   
    /**
     *	Additional posting Filter (implements non-indexable predicates).
     */
    interface Filter
    {
        // check the current node:
        boolean check( PostingIterator focus ) throws EvaluationException;
    }

    Filter[] getFilters();
    
    void addFilter(Filter filter);

    void setFilters(Filter[] filters);
    
    /**
     * A kind of filter: check the relative node depth from container
     * @param depthTest
     */
    void setDepthTest(int depthTest);
    
    /**
     * Depth filtering: sets the container.
     * @param scoringFactory TODO
     */
    void initContainer(ContainerIterator container, FullTextFactory scoringFactory);

    /**
     * Depth filtering: check the node depth relatively to container.
     */
//    boolean checkDepths(PostingIterator container);

    /**
     * Call filters if any and check that they accept the current node.
     */
    boolean checkFilters() throws EvaluationException;
    
    /**
     *	Returns a clone in initial state.
     */
    PostingIterator bornAgain();

    // dumps 'enough data to debug a query': reimplemented by concrete classes
    void debug() throws EvaluationException;
    
    /**
     * Largest Document identifier. Indicates end of iteration.
     */
    int/*DId*/ MAX_DOCID = Integer.MAX_VALUE - 1;

    /**
     * Largest Node identifier. Indicates end of iteration.
     */
    int/*NId*/ MAX_NODEID = Integer.MAX_VALUE - 1;
}


