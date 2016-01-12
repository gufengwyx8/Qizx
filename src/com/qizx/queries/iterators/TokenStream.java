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

import com.qizx.api.DataModelException;
import com.qizx.api.Node;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.Stemmer;
import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.queries.FullText;
import com.qizx.queries.Query;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.util.StringPattern;
import com.qizx.util.basic.Util;
import com.qizx.xdm.IQName;
import com.qizx.xdm.FONIDataModel.FONINode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.PatternSyntaxException;


public class TokenStream
    implements Query.Instantiator
{
    // instance members:
    private FullTextFactory factory;
    private TextTokenizer tokenizer;
    
    // token data:
    private int tokenCount;
    private char[][] tokens; // extracted normalized form
    private Node[] nodes;  // containing text node
    private int[] offsets; // offset in chars inside its text node
    private int[] lengths; // source word length (generally = token.length)
    
    public TokenStream(FullTextFactory factory, String language)
    {
        this.factory = factory;
        this.tokenizer = factory.getTokenizer(language);
        tokenizer.setAcceptingWildcards(false);
        tokenizer.setParsingSpecialChars(false);
        
        int isize = 8;
        tokens = new char[isize][];
        nodes = new Node[isize];
        offsets = new int[isize];
        lengths = new int[isize];
    }
    
    public void reset()
    {
        tokenCount = 0;
    }
    
    /**
     * Simply count tokens in the text fragment.
     */
    public int countTokens(String text)
    {
        int count = 0;
        tokenizer.start(text);
        int token = tokenizer.nextToken();
        for (; token != TextTokenizer.END; ) {
            if(token == TextTokenizer.WORD)
                ++ count;
            token = tokenizer.nextToken();
        }
        return count;
    }

    public void parseText(char[] atext, Node node)
    {
        tokenizer.start(atext, atext.length);
        doTokenize(node);
    }

    public void parseText(String text, Node node)
    {
        tokenizer.start(text);
        doTokenize(node);
    }

    private void doTokenize(Node node)
    {
        int token = tokenizer.nextToken();
        for (; token != TextTokenizer.END; ) {
            if(token == TextTokenizer.WORD)
            {
                add(tokenizer.getTokenChars(), node,
                    tokenizer.getTokenOffset(), tokenizer.getTokenLength());
            }
            token = tokenizer.nextToken();
        }
    }

    private void add(char[] token, Node node, int offset, int wlength)
    {
        if (tokenCount >= tokens.length) {
            int nsize = tokenCount * 2;
            Node[] oldn = nodes;
            nodes = new Node[nsize];
            System.arraycopy(oldn, 0, nodes, 0, tokenCount);
            char[][] oldw = tokens;
            tokens = new char[nsize][];
            System.arraycopy(oldw, 0, tokens, 0, tokenCount);
            int[] old = offsets;
            offsets = new int[nsize];
            System.arraycopy(old, 0, offsets, 0, tokenCount);
            old = lengths;
            lengths = new int[nsize];
            System.arraycopy(old, 0, lengths, 0, tokenCount);
        }
        tokens[tokenCount] = token;
        nodes[tokenCount] = node;
        offsets[tokenCount] = offset;
        lengths[tokenCount] = wlength;
        ++tokenCount;
    }

    public void parseWords(Node node, HashSet withoutNodes) 
        throws DataModelException
    {
        if(withoutNodes != null && withoutNodes.contains(node)) {
            return;
        }
        switch (node.getNodeNature()) {
        case Node.DOCUMENT:
        case Node.ELEMENT:
            Node kid = node.getFirstChild();
            for (; kid != null; kid = kid.getNextSibling()) {
                parseWords(kid, withoutNodes);
            }
            break;
        case Node.TEXT:
            char[] text = node.getCharValue();
            if(text == null)
                System.err.println("TokenStream.parseWords : "+node); //rub
            else
                parseText(text, node);
            break;
        case Node.ATTRIBUTE:    // only if at top level (no scan in ELEMENT)
            char[] atext = node.getCharValue();
            parseText(atext, node);
            break;
         // ignore the rest: PI, comment
        }
    }

    public void complete()
    {
    //        
    //        Thesaurus the = null;
    //        if(options.thesauri != null) {
    //            ThesaurusRef thRef = options.thesauri[0];
    //            Thesaurus t = context.getThesaurus(thRef.uri);
    //        }
    }

    public int getTokenCount()
    {
        return tokenCount;
    }

    public int getTokenStart(int posting)
    {
        return offsets[posting];
    }

    public int getTokenLength(int posting)
    {
        return lengths[posting];
    }

    // returns null because no scoring is performed on brute-force FT
    public FullTextFactory getScoringFactory()
    {
        
        return null;
    }

    public TextTokenizer getTokenizer()
    {
        return tokenizer;
    }

    public PostingIterator enumWord(char[] word, MatchOptions matchOptions)
    {
        return new TokenIterator(word, matchOptions);
    }

    public PostingIterator enumWildcard(char[] pattern, MatchOptions matchOptions)
    {
        int cass = MatchOptions.caseMode(matchOptions);
        boolean diacSense = MatchOptions.diacMode(matchOptions);
        try {
            return new WildcardIterator(
                        MatchOptions.compilePattern(pattern, cass, diacSense));
        }
        catch (PatternSyntaxException e) {
            return new TokenIterator(new char[] { '?' }, matchOptions); // FIXME!
        }
    }
    
    // ------------ iterators ------------------------------------------------
    
    /**
     * Iterates on tokens matching a simple token value
     */
    protected class TokenIterator extends PostingIteratorBase
    {
        char[] word;
        // current position curNodeId is an index in token stream
        boolean caseSense;
        boolean diacriticSense;
        Stemmer stemmer;
        
        public TokenIterator(char[] word, MatchOptions matchOptions)
        {
            if(matchOptions.stemming == MatchOptions.WITH) {
                stemmer = factory.getStemmer(matchOptions.language);
                if(stemmer != null)
                    word = stemmer.stem(word);
            }

            int cs = MatchOptions.caseMode(matchOptions);
            caseSense = cs != MatchOptions.INSENSITIVE;
            this.diacriticSense = MatchOptions.diacMode(matchOptions);
            // need to map only for upper lower
            if(cs == MatchOptions.UPPERCASE || cs == MatchOptions.LOWERCASE)
                this.word = MatchOptions.mapPattern(word, cs, diacriticSense);
            else
                this.word = word;
            curNodeId = -1;            
        }

        public TokenIterator(char[] word,
                             boolean caseSense, boolean diacriticSense)
        {
            this.word = word;
            this.caseSense = caseSense;
            this.diacriticSense = diacriticSense;
            curNodeId = -1;
        }

        public PostingIterator bornAgain()
        {
            return copyFilters(new TokenIterator(word, caseSense, diacriticSense));
        }

        public boolean skipToDoc(int/*DId*/ docId)
        {
            changeDoc(docId);
            if(docId >= 1)   // one doc by definition
                return noMoreDocs();
            return true; 
        }

        protected boolean basicSkipToNode(int/*NId*/ pos, int/*NId*/ limit)
        {
            curNodeId = pos;
            for( ; curNodeId < tokenCount; ++curNodeId) {
                if(equalTokens(curNodeId)) {
                    
                    return true;
                }
            }
            return noMoreNodes();
        }
        
        protected boolean equalTokens(int/*NId*/ rank)
        {
            // TODO? maybe optimize with hash key for quick elimination
            // only interesting if several iters scanning all tokens
            char[] streamToken = tokens[(int) rank];
            if(stemmer != null) // fairly slow: need caching in stemmer
                streamToken = stemmer.stem(streamToken);
            
            
            if(streamToken.length != word.length )
                return false;
            if(caseSense && diacriticSense)
                return Arrays.equals(streamToken, word);
            return Util.prefixCompare(streamToken, word, word.length,
                                      caseSense, diacriticSense) == 0;
        }


        public void resetToNode(int/*NId*/ pos)
        {
            if(pos < 0)
                curNodeId = -1;
            else
                curNodeId = pos - 1;
        }
        public boolean checkWordDistance(int/*NId*/ posting1, int/*NId*/ posting2,
                                         int offset, int min, int max)
        {
            
            
            // fairly simple here:
            int/*NId*/ d = Math.abs(posting2 - posting1) + offset;
            return d >= min && (max < 0 || d <= max);
        }
        
        public boolean checkBoundary(int posting, int boundary, boolean start)
        {
            
            if(start)
                return posting <= 0;
            else
                return posting >= tokenCount - 1;
        }

        public int computeWordDistance(int/*NId*/ posting1, int/*NId*/ posting2)
        {
            return (int) Math.abs(posting2 - posting1);     // cast NId!
        }
    }
    
    
    protected class WildcardIterator extends TokenIterator
    {
        StringPattern pattern;
       
        public WildcardIterator(StringPattern pattern)
        {
            super(null, false, false);
            this.pattern = pattern;
        }

        public PostingIterator bornAgain()
        {
            return copyFilters(new WildcardIterator(pattern));
        }

        protected boolean basicSkipToNode(int/*NId*/ pos, int/*NId*/ limit)
        {
            curNodeId = pos;
            for( ; curNodeId < tokenCount; ++curNodeId) {
                char[] tok = tokens[(int) curNodeId];
                if(pattern.match(tok) == StringPattern.MATCH)
                    return true;
            }
            return noMoreNodes();
        }
    }

    
    // ------------- dummy methods for Query.Instantiator

    public PostingIterator enumDocElements() {
        return null; 
    }

    public PostingIterator enumDocNodes() {
        return null; 
    }


    public PostingIterator singleNodeIterator(FONINode node) {
        return null;
    }
}
