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
package com.qizx.api;

/**
 * Sequence of Items returned by the evaluation of a XQuery Expression.
 * <p>
 * It can be used as input if bound to a variable of an expression.
 * <p>
 * Attention: the expansion of a result sequence through moveToNextItem or
 * moveTo can cause runtime errors (EvaluationException) due to lazy - or late -
 * evaluation mechanisms used in Qizx.
 */
public interface ItemSequence
    extends Item
{
    /**
     * Moves to the next item. An iteration loop typically looks like this:
     * <pre>
     *  ItemSequence seq = ...;
     *  while(seq.moveToNextItem()) {
     *      Item item = seq.getCurrentItem();
     *      // process item...
     *  }
     * </pre>
     * <p><b>Attention:</b> if access control is enabled, a result item belonging
     * to a blocked document is silently discarded.
     * @return true if another item has been found. The item is available
     *         through {@link #getCurrentItem()}).
     * @throws EvaluationException
     */
    boolean moveToNextItem()
        throws EvaluationException;

    /**
     * Returns the current item of the sequence. If the sequence is in an
     * invalid state, the result is indetermined.
     * @return a non-null Item.
     */
    Item getCurrentItem();

    /**
     * Returns the total number of items, without moving the current position.
     * <p>
     * Performs lazy evaluation if possible.
     * <p>
     * This method does not take into account items rejected by Access Control
     * (change from v2.1).
     * @return the number of items as a long integer
     * @throws EvaluationException if the expansion of the sequence caused a
     *         runtime error
     */
    long countItems()
        throws EvaluationException;

    /**
     * Moves the current position to the specified value.
     * <p>
     * Attention: if the sequence is returned by an XQuery expression
     * evaluation, moving backwards involves reevaluating the expression and
     * therefore can be inefficient and potentially have side-effects if
     * extension functions are used in the expression.
     * 
     * @param position number of items before desired position: moveTo(0) is
     *        equivalent to rewind.
     * @throws EvaluationException if the expansion of the sequence caused a
     * runtime error
     */
    void moveTo(long position)
        throws EvaluationException;

    /**
     * Returns the current position. Position 0 corresponds to "before first",
     * position N means just after the N-th item (which is available through
     * {@link #getCurrentItem()}).
     * @return the current item position, 0 initially
     */
    long getPosition();

    /**
     * Returns the full-text score, if applicable.
     * <p>
     * If this sequence is produced by evaluating an XQuery expression that
     * contains a <code>ftcontains</code> operator, then the value returned
     * is the score of the current <em>document</em> evaluated against the
     * top-level <i>full-text selection</i>. Otherwise the value returned is 0.
     * <p>
     * This is the value returned by the score clause of
     * <code>for</code> and <code>let</code>.
     * @return a value between 0 and 1 which represents the score of the
     *         current <em>document</em> evaluated against the
     *         <code>ftcontains</code> expression.
     * @throws EvaluationException 
     */
    double getFulltextScore()
        throws EvaluationException;
    
    /**
     * Skips items in forward direction.
     * <p>
     * Performs lazy evaluation if possible.
     * @param count number of items to skip. Must be >= 0 otherwise ignored.
     * @return the actual number of skipped items: smaller than
     *         <code>count</code> if the end of the sequence is reached.
     * @throws EvaluationException if the expansion of the sequence caused a
     *         runtime error
     */
    int skip(int count)
        throws EvaluationException;
    
    /**
     * Closes the sequence after use, and releases resources immediately,
     *  instead of waiting for this to happen automatically.
     * <p>It is recommended to call this method when a sequence is no
     * more needed. Failing to do so could result into errors due to an excess
     * of used up resources.
     * <p>The sequence can no more be used after calling this method. Calling
     * this method twice has no effect.
     */
    void close();        
}
