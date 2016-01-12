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
package com.qizx.xquery;

import com.qizx.api.EvaluationException;
import com.qizx.api.Item;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.impl.ErrorValue;

/**
 * Internal representation of the result of the evaluation of a query, which is
 * a sequence of Items (IItem). This interface generally cannot be used for
 * remote access.
 * <p>
 * The next() method must be repeatedly invoked to check whether an item is
 * available.
 * <p>
 * When next() returns true, the current item value can be retrieved by
 * specialized methods getX() (defined in super-interface XQItem) according to
 * its type.
 * <p>
 * Value has often the dual aspect of a sequence and an Item. It is not
 * immutable, because of the next() method. It cannot be rewinded, but it can
 * be regenerated by bornAgain().
 */
public interface XQValue extends XQItem
{
    /**
     * Attempts to get the next atomic item. If true is returned, the item
     * value is available through one of the specialized accessors.
     */
    boolean next()
        throws EvaluationException;

    /**
     * Attempts to get the next atomic item, without expanding collections.
     */
    boolean nextCollection()
        throws EvaluationException;

    /**
     * Gets the current item (undefined result if end of sequence reached).
     */
    XQItem getItem();

    /**
     * Returns the full-text score of the specified item (or the current item
     * if null is passed).
     * If the sequence does not involve a full-text query,
     * then return a negative value, otherwise a value between 0 and 1.
     * @param item if null, use the current item of this sequence
     * @throws EvaluationException 
     */
    double getFulltextScore(Item item) throws EvaluationException;

    /**
     * Returns a new version of the sequence <i>in its initial state</i>.
     * Value objects are in principle immutable, but due to the iterative
     * implementation style (see the {@link #next()} method), this is not
     * actually true. Therefore when a value is used several times (in
     * particular the value of a variable), there is a need for "regeneration".
     * <p>
     * NOTE: this needs not be a deep copy, because only the state of the
     * iterator is concerned, not the underlying data.
     */
    XQValue bornAgain();

    void close();

    /**
     * Expansion of a sequence with type-checking. Returns an ArraySequence.
     * Both the item types and the length of the sequence are checked. May be
     * redefined by a few implementations (IntRange) to avoid actually building
     * a sequence.
     * 
     * @param type sequence type: may be null.
     * @param conversion if true, conversion of nodes to untypedAtomic and cast
     *        (or promotion) are allowed: used for function parameters and
     *        result.
     * @param build if false, dont build a result sequence (simple test)
     * @return if OK, the expanded sequence, with possible conversions if
     *         'conversion' is true, or null if build is false. If not OK,
     *         return special values TOO_SHORT, TOO_LONG, or match error (Spe
     * @throws EvaluationException
     */
    XQValue checkTypeExpand(XQType type, EvalContext context,
                            boolean conversion, boolean build)
        throws EvaluationException;

    /**
     * Returns false if it is more efficient to keep the value as an iterator,
     * and not to expand it into an array sequence. This method is used when a
     * value is stored into variable. It should return false only when the
     * value is cheap to compute AND doesnt involve Node constructors (because
     * of a problem with Node identity).
     */
    boolean worthExpanding();

    /**
     * Boosts iterations that dont need the item value (count(), skipping)
     */
    void setLazy(boolean lazy);

    /**
     * Optimized evaluation of count() or last(). Assumes the iterator to be in
     * initial state.
     * 
     * @param context evaluation context: used for monitoring.
     */
    long quickCount(EvalContext context)
        throws EvaluationException;

    /**
     * Repetition of next().
     */
    int doSkip(int count)
        throws EvaluationException;

    /**
     * Optimized evaluation of operator [] with integer $index.
     */
    XQItem quickIndex(long index)
        throws EvaluationException;

    /**
     * Check whether quick indexing is supported.
     */
    boolean hasQuickIndex();

    // ---------------- special values -------------------------------------

    XQValue empty = new ArraySequence(null, 0) {
        public XQValue bornAgain()
        {
            return this;
        }
    };

    XQValue TOO_SHORT = new ErrorValue(XQType.ERR_EMPTY_UNEXPECTED);

    XQValue TOO_LONG = new ErrorValue(XQType.ERR_TOO_MANY);
}
