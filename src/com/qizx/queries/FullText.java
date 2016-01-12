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
package com.qizx.queries;


import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.Thesaurus;
import com.qizx.api.fulltext.Thesaurus.LookupResult;
import com.qizx.api.fulltext.Thesaurus.TokenSequence;
import com.qizx.queries.iterators.*;
import com.qizx.util.RegexMatchPattern;
import com.qizx.util.basic.Unicode;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQValue;

import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Full-Text Query wrapper, implementing standard XQuery Full-Text specifications.
 * <p>
 * 
 */
public interface FullText
{
    public static final int ANY = 0;
    public static final int ANY_WORD = 1;
    public static final int ALL = 2;
    public static final int ALL_WORDS = 3;
    public static final int PHRASE = 4;
    static QName WEIGHT_ERR = ModuleContext.xqueryErrorCode("FTDY0016");
    
    public static Selection NULL_QUERY = new All();
        
    /**
     * Super-class of all 'Full-text Selections' at the right hand-side of the
     * ftcontains operator.
     */
    public static abstract class Selection extends Query
    {
        protected PosFilters posFilters;     // optional
        protected MatchOptions matchOptions; // optional
        protected float  weight = 1;   // scoring
        // 'occurs times' option is allowed only on simple terms or phrases but 
        // for simplicity and generality we put it here:
        public int[] occRange; 

        public PosFilters getPosFilters()
        {
            return posFilters;
        }
        
        public void setPosFilters(PosFilters posFilters)
        {
            this.posFilters = posFilters;
        }
        
        public MatchOptions getMatchOptions()
        {
            return matchOptions;
        }
        public void setMatchOptions(MatchOptions matchOptions)
        {
            this.matchOptions = matchOptions;
        }
        
        public float getWeight()
        {
            return weight;
        }
        
        public void setWeight(float weight) throws EvaluationException
        {
            if(Math.abs(weight) > 1000)
                throw new EvaluationException(WEIGHT_ERR, "weight greater than 1000");
            this.weight = weight;
        }
        
        public abstract PostingIterator realize(Instantiator factory);

        public abstract void display(Query.Displayer display);
        
        public void displayFilters(Query.Displayer display)
        {
            if(posFilters != null) {
                if(posFilters.ordered)
                    display.print(" ordered");
                if(posFilters.distanceRange != null) {
                    display.print(" dist " + posFilters.distanceRange[0]
                                  + " " + posFilters.distanceRange[1]);
                }
                if(posFilters.window > 0) {
                    display.print(" window " + posFilters.window);
                }
            }
            if(occRange != null) {
                display.print(" occ[" + occRange[0] + " " + occRange[1] + "]");
            }
            if(weight != 1)
                display.print(" w=" + weight);
        }

        protected PostingIterator filtered(PostingIterator iterator)
        {
            if(posFilters != null && iterator instanceof PolyIterator)
            {
                PolyIterator polite = (PolyIterator) iterator;
                if(posFilters.ordered)
                    iterator.setOrdered(true);
                if(posFilters.windowUnit == PosFilters.WORDS) {
                    polite.setWindowConstraint(posFilters.window);
                }
                if(posFilters.distanceRange != null) {
                    polite.setDistanceConstraint(posFilters.distanceRange[0],
                                                   posFilters.distanceRange[1]);
                }
                if(posFilters.content != PosFilters.UNSPECIFIED) {
                    polite.setContentConstraint(posFilters.content);
                }
            }
            iterator.setWeight(weight);
            if(occRange != null) {
                return new CountIterator(occRange[0], occRange[1], iterator);
            }
            return iterator;
        }
        
        public abstract Selection applyThesauri();
        
        protected Selection expand(TokenSequence sequence,
                                   MatchOptions matchOptions, float weight)
        {
            if(sequence.size() == 1) {
                SimpleWord w = new SimpleWord(sequence.getTokenAt(0));
                return w.decorate(matchOptions, weight);
            }
            Phrase ph = new Phrase();
            for(int t = 0; t < sequence.size(); t++) {
                SimpleWord w = new SimpleWord(sequence.getTokenAt(t));
                ph.addChild(w.decorate(matchOptions, weight));
            }
            return ph.decorate(matchOptions, weight);
        }
        
        protected Selection decorate(MatchOptions matchOptions, float weight)
        {
            this.weight= weight;
            this.matchOptions = matchOptions;
            return this;
        }
        
        protected LookupResult matchThesauri(Thesaurus[] thesauri,
                                             TokenSequence tokens)
        {
            LookupResult best = null;
            for (int i = 0; i < thesauri.length; i++) {
                LookupResult res = thesauri[i].lookup(tokens);
                if(res == null)
                    continue;
                int ct = res.consumedTokens();
                if(best == null || ct > best.consumedTokens())
                    best = res;
                else if(ct == best.consumedTokens()) {
                    for(int t = 0; t < ct; t++)
                        best.addSynonym(res.getSequence(t), null, 1);
                }
            }
            return best;
        }

        public String asString()
        {
            return toString();
        }

        public boolean matches(XQValue ctx, HashSet maskedNodes,
                               FullTextFactory ftFactory)
            throws EvaluationException, DataModelException
        {
            for (; ctx.next();) {
                TokenStream tokens =
                    new TokenStream(ftFactory, matchOptions.language);
                if (ctx.isNode()) {
                    Node node = ctx.getNode();
                    tokens.parseWords(node, maskedNodes);
                }
                else {
                    String s = ctx.getString();
                    tokens.parseText(s.toCharArray(), null);
                }

                PostingIterator matches = realize(tokens);
                // initContainer useful even here:
                matches.initContainer(null, null);
                matches.skipToDoc(0); // for init

                // it's that simple:
                if (matches.inRange(0, tokens.getTokenCount())) {
                    return true;
                }
            }
            return false;
        }
    }

    // super class for Or And MildNot
    public static abstract class SelectionList extends Selection
    {
        protected Selection[] subs;

        public SelectionList(Selection s)
        {
            if (s != null)
                this.subs = new Selection[] { s };
        }

        public SelectionList(Selection s1, Selection s2)
        {
            this.subs = new Selection[] { s1, s2 };
        }

        public int getChildCount()
        {
            return (subs == null)? 0 : subs.length;
        }

        public Selection getChild(int index)
        {
            return index < 0 || index >= subs.length? null : subs[index];
        }

        public void setChild(int index, Selection s)
        {
            subs[index] = s;
        }
        
        public void setChildren(Selection s1, Selection s2)
        {
            this.subs = new Selection[] { s1, s2 };
        }
        
        public void addChild(Selection s)
        {
            if(s == NULL_QUERY)
                return;
            Selection[] old = subs;
            int olen = (old == null)? 0 : old.length;
            subs = new Selection[olen + 1];
            if(old != null)
                System.arraycopy(old, 0, subs, 0, olen);
            subs[olen] = s;
        }
        
        protected void displayList(Displayer display, String title)
        {
            display.print(title);
            display.print("(");
            if(subs != null)
                for (int i = 0; i < subs.length; i++) {
                    if(i > 0)
                        display.print(", ");
                    subs[i].display(display);
                }
            display.print(")");
            displayFilters(display);
        }
    }

    /**
     * Alternative between selections
     */
    public static class Any extends SelectionList
    {
        public Any(Selection s1, Selection s2) {
            super(s1, s2);
        }

        public Any() {
            super(null);
        }

        public PostingIterator realize(Instantiator factory)
        {
            int nsubs = (subs == null)? 0 : subs.length;
            if(nsubs == 1) {
                PostingIterator iter = subs[0].realize(factory);
                if(iter instanceof PolyIterator)
                    return filtered((PolyIterator) iter);
                return iter;    
            }
            PostingIterator[] its = new PostingIterator[nsubs];
            for (int i = 0; i < nsubs; ++i) {
                its[i] = subs[i].realize(factory);                
            }
            return filtered(new OrIterator(its));
        }

        public void display(Displayer display)
        {
            displayList(display, "Or");
        }

        public Selection applyThesauri()
        {
            // inline inner Or's
            if(subs == null)
                return this;
            for (int i = 0, len = subs.length; i < len; ++i) {
                subs[i] = subs[i].applyThesauri();
                if(subs[i] instanceof Any
                   && ((Any) subs[i]).getChildCount() > 0
                   && subs[i].matchOptions == matchOptions
                   && subs[i].posFilters == null)
                {
                    Any tmp = (Any) subs[i];
                    subs[i] = tmp.getChild(0);
                    for(int c = 1; c < tmp.getChildCount(); c++)
                        addChild(tmp.getChild(c));
                }
            }
            return this;
        }
    }

    /**
     * Conjunction of selections
     */
    public static class All extends SelectionList
    {
        public All(Selection s1, Selection s2) {
            super(s1, s2);
        }

        public All() {
            super(null);
        }

        public PostingIterator realize(Instantiator factory)
        {
            int nsubs = (subs == null)? 0 : subs.length;
            
            boolean order = posFilters != null && posFilters.ordered;
            if(nsubs == 1 && posFilters == null) {
                return subs[0].realize(factory);    
            }
            PostingIterator[] its = new PostingIterator[nsubs];
            for (int i = 0; i < nsubs; ++i) {
                its[i] = subs[i].realize(factory);
                if(order)
                    its[i].setOrdered(true);
            }
            return filtered(new AllIterator(its));
        }

        public void display(Displayer display)
        {
            if(getChildCount() == 0)
                display.print("Null");
            else
                displayList(display, "And");
        }

        public Selection applyThesauri()
        {
            // simply replace each term
            if(subs != null)
                for (int i = 0; i < subs.length; ++i) {
                    subs[i] = subs[i].applyThesauri();
                }
            return this;
        }
    }

    /**
     * OPerator 'not in' or mild-not.
     */
    public static class MildNot extends Selection
    {
        public Selection what, notin;
        
        public void setChildren(Selection what, Selection where) {
            this.what = what;
            this.notin = where;
        }

        public PostingIterator realize(Instantiator factory)
        {
            return new MildNotIterator(what.realize(factory),
                                       notin.realize(factory));
        }
        
        public void display(Displayer display)
        {
            display.print("Notin(");
            what.display(display);
            display.print(", ");
            notin.display(display);
            display.print(")");
            displayFilters(display);
        }

        public Selection applyThesauri()
        {
            what = what.applyThesauri();
            notin = notin.applyThesauri();
            return this;
        }
    }

    public static class Not extends Selection
    {
        protected Selection child;

        public Selection getChild()
        {
            return child;
        }

        public void setChild(Selection s)
        {
            this.child = s;
        }

        public PostingIterator realize(Instantiator factory)
        {
            // 'not' means exactly 0 occurrences
            return new CountIterator(0, 0, child.realize(factory));
        }
        
        public void display(Displayer display)
        {
            display.print("Not(");
            child.display(display);
            display.print(")");
            displayFilters(display);
        }

        public Selection applyThesauri()
        {
            child = child.applyThesauri();
            return this;
        }
    }

    public static class Phrase extends SelectionList
    {
        public Phrase() {
            super(null);
        }
        
        public Phrase(Selection t1, Selection t2)
        {
            super(t1, t2);
        }
        
        // overridden to add window if unspecified
        public void setPosFilters(PosFilters posFilters)
        {
            this.posFilters = posFilters;
            if(posFilters != null && posFilters.window <= 0)
                posFilters.window = subs.length;
        }

        public PostingIterator realize(Instantiator factory)
        {
            if(subs == null) 
                return new OrIterator(new PostingIterator[0]); // always fails
            if(subs.length == 1 && posFilters == null) {
                return subs[0].realize(factory);    
            }
            boolean variable = false;
            PostingIterator[] its = new PostingIterator[subs.length];
            for (int i = subs.length; --i >= 0;) {
                its[i] = subs[i].realize(factory);
                // useful if sub iter is a Or (wildcard, stemming):
                its[i].setOrdered(true);
                if(its[i] instanceof OrIterator)
                    variable = true;
            }
            return filtered(new PhraseIterator(its, variable)); 
        }
        
        public void display(Displayer display)
        {
            displayList(display, "Phrase");
        }

        public Selection applyThesauri()
        {
            Thesaurus[] thesauri = matchOptions.thesauri;
            if(thesauri == null)
                return this;    // no need to descend
            // expand as a list of simple tokens
            TokenSequence ph = new TokenSequence();
            for (int i = 0; i < subs.length; ++i) {
                if(subs[i] instanceof SimpleWord) {
                    SimpleWord sw = (SimpleWord) subs[i];
                    ph.addToken(sw.word);
                }
                else // cant do
                    return this;
            }
            // repeated multi-token lookup
            Phrase repl = new Phrase();
            for(int spos = 0; ph.size() > 0; ) {
                LookupResult match = matchThesauri(thesauri, ph);
                if(match == null) {
                    repl.addChild(subs[spos]);
                    ph.removeTokens(0, 1);
                    ++spos;
                }
                else {
                    spos += match.consumedTokens();
                    ph.removeTokens(0, match.consumedTokens());
                    Any or = new Any();
                    for (int i = match.size(); --i >= 0;) {
                        or.addChild(expand(match.getSequence(i),
                                           matchOptions, weight));
                    }
                    repl.addChild(or);
                }
            }
            Selection result = repl;
            if(repl.getChildCount() == 1)   // simplify
                result = repl.getChild(0);
            result.posFilters = posFilters;
            return result.decorate(matchOptions, weight);
        }
    }

    /**
     * Simple word selection.
     */
    public static class SimpleWord extends Selection
    {
        private char[] word;
        
        public SimpleWord(char[] token) {
            this.word = token;
        }

        public PostingIterator realize(Instantiator factory)
        {
            PostingIterator iter = factory.enumWord(word, matchOptions);
            return filtered(iter);
        }
        
        public void display(Displayer display)
        {
            display.print("Term '" + new String(word) + "'");
            displayFilters(display);
        }

        public String asString() {
            return new String(word);
        }

        public Selection applyThesauri()
        {
            if(matchOptions != null && matchOptions.thesauri != null) {
                Thesaurus[] thes = matchOptions.thesauri;
                TokenSequence seq1 = new TokenSequence();
                seq1.addToken(word);
                LookupResult match = matchThesauri(thes, seq1);
                if(match != null) {
                    Any replacement = new Any();
                    for (int i = match.size(); --i >= 0;) {
                        replacement.addChild(expand(match.getSequence(i),
                                                    matchOptions, weight));
                    }
                    return replacement.decorate(matchOptions, weight);
                }
            }
            return this;
        }
    }

    public static class Wildcard extends Selection
    {
        private char[] pattern;
        
        public Wildcard(char[] tokenChars)
        {
            pattern = tokenChars;
        }
        
        public PostingIterator realize(Instantiator factory)
        {
            return filtered(factory.enumWildcard(pattern, matchOptions));
        }    
        
        public void display(Displayer display)
        {
            display.print("Wildcard '" + new String(pattern) + "'");
            displayFilters(display);
        }

        public String asString() {
            return new String(pattern);
        }

        public Selection applyThesauri()
        {
            return this;
        }
    }
    
    /**
     * Match options defined in static context or on primary FT Selections.
     */
    public static class MatchOptions
    {
        public static final byte UNSPECIFIED = 0;
        public static final byte SENSITIVE = 1;
        public static final byte INSENSITIVE = 2;
        public static final byte LOWERCASE = 3;
        public static final byte UPPERCASE = 4;
        public static final byte WITH = 1;
        public static final byte WITHOUT = 2;
        
        public String language;
        public byte wildcards;
        public byte stemming;
        public byte diacritics;
        public byte caseSensitivity; // INSENSITIVE, SENSITIVE, LOWERCASE, UPPERCASE
        // list of resolved Thesaurus drivers:
        public Thesaurus[] thesauri;
                
        public MatchOptions()
        {
            language = null;
            stemming = WITHOUT;
            wildcards = WITHOUT;
            diacritics = INSENSITIVE;
            caseSensitivity = INSENSITIVE;
        }
        
        public MatchOptions(MatchOptions options)
        {
            language = options.language;
            wildcards = options.wildcards;
            stemming = options.stemming;
            diacritics = options.diacritics;
            caseSensitivity = options.caseSensitivity;
            thesauri = options.thesauri;
//            if(options.addedStopWords != null)
//                addedStopWords = (ArrayList) options.addedStopWords.clone();  // deep copy?
//            if(options.exceptedStopWords != null)
//                exceptedStopWords = (ArrayList) options.exceptedStopWords.clone(); 
        }
        
        public boolean likeDefault()
        {
            return language == null
                    && stemming == WITHOUT
                    && wildcards == WITHOUT
                    && diacritics == INSENSITIVE
                    && caseSensitivity == INSENSITIVE
                    && thesauri == null;
        }

//        public void addStopWords(Object swords)
//        {
//            if(addedStopWords == null)
//                addedStopWords = new ArrayList();
//            addedStopWords.add(swords);
//        }
//        
//        public void exceptStopWords(Object swords)
//        {
//            if(exceptedStopWords == null)
//                exceptedStopWords = new ArrayList();
//            exceptedStopWords.add(swords);
//        }

        public void addThesaurus(Thesaurus thesaurus)
        {
            if (thesauri == null)
                thesauri = new Thesaurus[] { thesaurus };
            else {
                Thesaurus[] old = thesauri;
                thesauri = new Thesaurus[old.length + 1];
                System.arraycopy(old, 0, thesauri, 0, old.length);
                thesauri[old.length] = thesaurus;
            }
        }
        
        public static int caseMode(MatchOptions matchOptions)
        {
            byte cs = (matchOptions != null)?
                                matchOptions.caseSensitivity : INSENSITIVE;
            return cs == UNSPECIFIED? INSENSITIVE : cs;
        }

        public static boolean diacMode(MatchOptions matchOptions)
        {
            return (matchOptions != null)?
                     (matchOptions.diacritics == SENSITIVE) : false;
        }

        public static RegexMatchPattern compilePattern(char[] pattern,
                                                       int caseSensitive,
                                                       boolean diacriticsSense)
        {
            pattern = mapPattern(pattern, caseSensitive, diacriticsSense);
            int flags = (caseSensitive != INSENSITIVE)? 0
                    : (Pattern.CASE_INSENSITIVE + Pattern.UNICODE_CASE);
            RegexMatchPattern pat =
                  new RegexMatchPattern(Pattern.compile(new String(pattern), flags));
            pat.setDiacriticsSensitive(diacriticsSense);
            return pat;
        }

        public static char[] mapPattern(char[] token, 
                                        int caseMode, boolean diacriticsSense)
        {
            char[] clone = null;
            if(caseMode == LOWERCASE) {
                clone = (char[]) token.clone();
                token = Unicode.toLowerCase(clone);
            }
            else if(caseMode == UPPERCASE) {
                clone = (char[]) token.clone();
                token = Unicode.toUpperCase(clone);
            }
            if(!diacriticsSense) {
                if(clone == null)
                    clone = (char[]) token.clone();
                token = Unicode.collapseDiacritic(token);
            }
            return token;
        }        
    }
    
    /**
     * Position filters optionally defined on a FT Selection.
     * <p>
     * Position filtering options are: <ul>
     * <li>Ordered or non-ordered (applicable only to a ftand)
     * <li>Window and window-unit: the maximum span of a match expressed in
     * words, sentences or paragraphs.
     * <li>Distance and unit (applicable only to a ftand): the distance
     * between matches (at least, at most, from..to).
     * <li>Scope: same sentence, same paragraph, different sentence, different
     * paragraph (<b>Not supported</b>).
     * <li>Content: at start, at end, entire content
     * </ul>
     */
    public static class PosFilters
    {
        public static final int WORDS = 1;
        public static final int SENTENCES = 2;
        public static final int PARAGRAPHS = 3;
        
        public static final int UNSPECIFIED = 0;
        public static final int SAME_SENTENCE = 1;
        public static final int SAME_PARAGRAPH = 2;
        public static final int DIFF_SENTENCE = 3;
        public static final int DIFF_PARAGRAPH = 4;
        
        public static final int AT_START = 1;
        public static final int AT_END = 2;
        public static final int ENTIRE_CONTENT = 3;
        
        public boolean ordered;
        public int window;
        public int windowUnit;    // WORDS, SENTENCES, PARAGRAPHS
        public int[] distanceRange;
        public int distanceUnit;    // WORDS, SENTENCES, PARAGRAPHS
        public int scope;   // SAME_SENTENCE, DIFF_SENTENCE, SAME_PARAG, DIFF_PARAG
        public int content; // UNDEF, AT_START, AT_END, ENTIRE_CONTENT   
        
        public PosFilters(boolean ordered)
        {
            distanceUnit = windowUnit = WORDS;
            this.ordered = ordered;
        }
        
        public PosFilters(PosFilters filters)
        {
            ordered = filters.ordered;
            window = filters.window;
            windowUnit = filters.windowUnit;
            distanceRange = filters.distanceRange;
            distanceUnit = filters.distanceUnit;
            scope = filters.scope;
            content = filters.content;
        }
    }

}
