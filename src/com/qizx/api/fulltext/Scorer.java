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
package com.qizx.api.fulltext;


/**
 * Customizable part of the full-text scoring algorithm.
 * <p>
 * This interface defines:<ul>
 * <li>Weighting formulas for different types of FT selections (basic term, all,
 * any, mild-not...)
 * <li>Scoring formulas for different types of FT selections
 * <li>Normalization of the final value, so that it is between 0 and 1
 * </ul>
 */
public interface Scorer
{
    // ---------- weighting: called when initializing a query
    
    /**
     * Computes the weight associated with a simple word.
     * @param inverseDocFrequency inverse of the fraction of documents that
     *        contain this term
     * @return the computed weighting of simple term. The default
     *         implementation is: 1 + log(inverseDocFrequency)
     */
    float normWord(float inverseDocFrequency);
    
    /**
     * Computes the weight norm associated with a conjunction ('ftand' or
     * 'all') of full-text selections. The default weight norm is the sum of
     * squared sub-weights power -1/2.
     * @param subWeights weight values computed for sub-selections.
     * @return the computed weighting.
     */
    float normAll(float[] subWeights);

    /**
     * Computes the weight norm associated with a disjunction ('ftor' or 'any') of
     * full-text selections.
     * @param subWeights weight values computed for sub-selections.
     * @return the normed weighting. The default implementation is a weighted
     *         average of the sum of sub-weights and their maximum value.
     */
    float normOr(float[] subWeights);
    
    // ---------- scoring: called at run-time
    
    /**
     * Computes the score of a single word.
     * @param norm normalized weight of word (as computed by {@link #normWord(float)}).
     * @param termFrequency relative term frequency in the current document
     * (number of occurrences divided by average number of occurrences in all
     * documents).
     * @return the computed score. The default implementation is termFrequency
     *  multiplied by 'norm'.
     */
    float scoreWord(float norm, float termFrequency);
    
    /**
     * Computes the score of a conjunction ('ftand' or 'all') of
     * full-text selections.
     * @param subScores weight values computed for sub-selections.
     * @return the computed weighting. The default implementation is the sum of
     *         scores.
     */
    float scoreAll(float[] subScores);

    /**
     * Computes the score of a disjunction ('ftor' or 'any') of full-text
     * selections.
     * @param subScores weight values computed for sub-selections.
     * @param scoreCount number of values in scores
     * @return the computed score. The default implementation is a weighted
     *         average of the sum of sub-scores and their maximum value.
     */
    float scoreOr(float[] subScores, int scoreCount);
    /**
     * Normalizes a score so that its value is between 0 and 1
     * @param rawScore a positive value possibly greater than 1
     */
    float normalizeScore(float rawScore);    /**

     * Score returned when the corresponding document does not belong to 
     * an XML Library.
     */
    float CORE_SCORE = 0.5f;
}
