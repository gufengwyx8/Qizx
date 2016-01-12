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
 * Pluggable text tokenizer compatible with standard full-text features.
 * Analyzes text chunks to extract and normalize words.
 * <p>
 * To parse words, the tokenizer is first initialized with method
 * {@link #start} on a text chunk. Then the {@link #nextToken()} method
 * is called repeatedly until the last token is parsed.
 */
public interface TextTokenizer
{
    /**
     * Starts the analysis of a new text chunk.
     * @param text characters to tokenize
     * @param length number of characters in the text array
     */
    void start(char[] text, int length);

    /**
     * Starts the analysis of a new text chunk.
     * @param text fragment to tokenize
     */
    void start(CharSequence text);

    /**
     * Returns the type of the next token, or END if no more token can be
     * found.
     * @return the type of the next token: END, WORD, SENTENCE, PARAGRAPH, or
     *         the code of a character if the option 'Special Characters' is
     *         set on this tokenizer using the method setParsingSpecialChars().
     */
    int nextToken();

    /**
     * Code returned by nextToken when the end of the text to tokenize is
     * reached.
     */
    int END = 0;
    /** Code returned by nextToken when a word is recognized. */
    int WORD = 1;
    /** Code returned by nextToken when a sentence boundary is recognized.
        <p><b>Not yet supported.</b> */
    int SENTENCE = 2;
    /** Code returned by nextToken when a paragraph boundary is recognized.
        <p><b>Not yet supported.</b> */
    int PARAGRAPH = 3;

    /**
     * Returns the offset (in source text chunk) of the last word returned by nextWord.
     * @return an index in the source text fragment
     */
    int getTokenOffset();

    /**
     * Returns the original length of the last word returned by nextWord. Most
     * often equal to the length of the array returned by nextWord, but can be
     * different if normalization or stemming is performed.
     * @return word length
     */
    int getTokenLength();

    /**
     * Returns the current token as a new character array.
     */
    char[] getTokenChars();

    /**
     * Copies the current token into a character array.
     * @param array destination array. Must fit the size of the token.
     * @param start offset in the destination array.
     */
    void copyTokenTo(char[] array, int start);

    /**
     * Returns true if special characters are recognized.
     * @see #defineSpecialChar
     */
    public boolean isParsingSpecialChars();

    /**
     * If set to true, special characters are recognized. Otherwise, they are
     * ignored like whitespace.
     * @see #defineSpecialChar
     */
    public void setParsingSpecialChars(boolean parsingSpecialChars);
    
    /**
     * Returns the maximum number of digits a word can contain.
     */
    public int getDigitMax();

    /**
     * Sets the maximum number of digits a word can contain. Beyond the 
     * specified value (default is 4), a token is not retained as a word.
     */
    public void setDigitMax(int max);

    /**
     * Define a character to recognize when parsing of special characters is
     * enabled.
     */
    public void defineSpecialChar(char ch);

    /**
     * Returns true if wildcard characters are recognized.
     * <p>Wildcard character sequences are ".", ".?", ".*", ".+", and ".{n,m}"
     */
    public boolean isAcceptingWildcards();

    /**
     * If set to true, wildcard characters are recognized. Otherwise, they are
     * ignored.
     */
    public void setAcceptingWildcards(boolean acceptingWildcards);

    /**
     * Returns true if wildcard characters have been recognized in the current
     * token. Requires the the option AcceptingWildcards to be set to true.
     */
    public boolean gotWildcard();
}
