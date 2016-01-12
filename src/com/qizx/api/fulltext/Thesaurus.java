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

import java.util.ArrayList;

/**
 * Access to a thesaurus.
 */
public interface Thesaurus
{    
    /**
     * Looks up a thesaurus for a sequence of tokens. Returns a list of
     * replacement words or word sequences, and the number of tokens consumed
     * (all this gathered in LookupResult type).
     * <p>
     * This method call can consume one or several input tokens, according to
     * the thesaurus entry it recognizes. Therefore, the returned result also
     * contains the number of consumed tokens. Replacement sequences may or may
     * not contain the consumed sequence, this is left to the thesaurus
     * implementation.
     * <p>
     * For example, if the thesaurus contains equivalences "wealthy",
     * "well-off" and "well-to-do", then looking up the sequence "well off man"
     * would return a LookupResult where consumedTokens = 2 (length of "well
     * off"), and containing the token sequences "wealthy", "well off" and
     * "well to do". (Note that the TextTokenizer is assumed here to cut on
     * hyphens).
     * @param tokens sequence of source tokens. Each token is an array of
     *        characters rather than a string.
     * @return a list of replacement sequences. If no replacement is found in
     *         the thesaurus, null must be returned.
     */
    LookupResult lookup(TokenSequence tokens);


    /**
     * Sequence of tokens. Used as an interface between thesauri and the FT
     * engine.
     */
    public static class TokenSequence
    {
        private ArrayList tokens;
        
        public TokenSequence()
        {
            tokens = new ArrayList();
        }
        
        /**
         * Returns the number of tokens in the sequence.
         */
        public int size()
        {
            return tokens.size();
        }
        
        public String toString()
        {
            StringBuffer buf = new StringBuffer(30);
            for(int i = 0; i < size(); i ++) {
                if(i > 0)
                    buf.append(' ');
                buf.append(getTokenAt(i));
            }
            return buf.toString();
        }

        /**
         * Returns the token at position 'index'.
         */
        public char[] getTokenAt(int index)
        {
            return (char[]) tokens.get(index);
        }
        
        /**
         * Adds a token in last position.
         */
        public void addToken(char[] token)
        {
            tokens.add(token);
        }

        /**
         * Removes tokens from positions index to index + count -1 inclusive. 
         */
        public void removeTokens(int index, int count)
        {
            for(int i = 0; i < count && tokens.size() > index; i++)
                tokens.remove(index);
        }
    }
    
    /**
     * Structure returned by Thesaurus lookup.
     * <p>
     * Contains:<ul>
     * <li>A list of Synonyms, equivalent to the looked up sequence, 
     * <li>the number of consumed input tokens (i.e the length of
     * the recognized thesaurus entry).
     * </ul>
     * For example, if the thesaurus contains synonyms "wealthy",
     * "well-off" and "well-to-do", then looking up the sequence "well off man"
     * would return a LookupResult where consumedTokens = 2 (length of "well off"),
     * and containing the token sequences "wealthy", "well off" and "well to do".
     */
    public static class LookupResult extends ArrayList
    {
        protected int consumedTokens;
        
        /**
         * Creates a new empty lookup result.
         * @param consumedTokens the number of consumed input tokens.
         */
        public LookupResult(int consumedTokens)
        {
            this.consumedTokens = consumedTokens;
        }

        /**
         * Returns the number of consumed input tokens.
         */
        public int consumedTokens()
        {
            return consumedTokens;
        }
        
        /**
         * Returns the number of equivalent word sequences.
         */
        public int size()
        {
            return super.size(); // just for the doc
        }

        public Synonym getSynonym(int index)
        {
            return ((Synonym) this.get(index));
        }

        /**
         * Returns the word sequence at rank 'index'.
         */
        public TokenSequence getSequence(int index)
        {
            return getSynonym(index).tokens;
        }

        /**
         * Appends a Synonym.
         */
        public void addSynonym(TokenSequence sequence, 
                               String relationship, int level)
        {
            this.add(new Synonym(sequence, relationship, level));
        }

        public void addSynonym(Synonym syn)
        {
             this.add(syn);
        }
    }
    
    /**
     * A synonym associated with an entry in a Thesaurus.
     * <p>An entry 
     */
    public static class Synonym
    {
        Synonym(TokenSequence tokens, String rel, int level)
        {
            this.tokens = tokens;
            this.relationship = rel;
            this.level = level;
        }
        
        public TokenSequence tokens;
        public String relationship;
        public int level;
    }
}
