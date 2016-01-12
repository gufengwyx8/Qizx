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
 * Pluggable factory of Full-text resources.
 * <p>
 * Allows redefining the way resources like full-text Tokenizer,
 * Stemmer and Thesaurus are created and accessed.
 * <p>The default Fulltext Provider provides a language-independent tokenizer,
 * no stemmer, no stop-word list, no thesaurus.
 * <p>Note: stopword lists are not supported.
 */
public interface FullTextFactory
{
    /**
     * Returns an instance of TextTokenizer suitable for the specified
     * language.
     * @param languageCode an ISO language code (e.g 'en', 'fr-CH'), or null if
     *        no language is specified, in which case a generic tokenizer
     *         ({@link com.qizx.api.util.fulltext.DefaultTextTokenizer})
     *        is provided.
     * @return a new instance of a WordTokenizer.
     */
    TextTokenizer getTokenizer(String languageCode);

    /**
     * Returns an appropriate Stemmer for the language, if any.
     * @param languageCode
     * @return an instance of a Stemmer, or null if no applicable stemmer can
     *         be found,
     */
    Stemmer getStemmer(String languageCode);

    /**
     * Returns a Thesaurus lookup driver. This driver is searched using the
     * URI and the language code. It is then assumed to filter lookup by the
     * specified relationship and levels.
     * @param uri identifier of the Thesaurus. It is the string "default" if
     * the default thesaurus is invoked.
     * @param languageCode a normalized language code like "en" or "fr-CH".
     * A null language code is in principle not meaningful.
     * @param relationship relationship from root word(s) to
     *        thesaurus-equivalent words. It can be null if the relationship is
     *        unspecified.
     * @param levelMin minimum value of the level of a synonym. A simple synonym
     * has a level equal to 1.
     * @param levelMax maximum value of the level of a synonym. A negative value
     * means no maximum.
     * @return an instance of a Thesaurus, or null if none can be found,
     */
    Thesaurus getThesaurus(String uri, String languageCode,
                           String relationship, int levelMin, int levelMax);
    

    /**
     * Returns a scoring algorithm.
     * @return an implementation of Scorer
     */
    Scorer createScorer();
}
