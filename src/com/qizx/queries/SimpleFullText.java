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
import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.api.util.fulltext.DefaultTextTokenizer;
import com.qizx.queries.FullText;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.util.basic.Unicode;

import java.util.ArrayList;
import java.util.List;


public class SimpleFullText
{
    protected String query;
    protected TextTokenizer tokenizer;
    private MatchOptions matchOptions;
    
    private Token[] tokens;
    private int tokenCount;
    private int from;
    
    private FullText.Any optTerms;
    private FullText.All reqTerms;
    private FullText.All negTerms;
    
    public SimpleFullText(TextTokenizer tokenizer)
    {
        if(tokenizer == null)
            tokenizer = new DefaultTextTokenizer();
        this.tokenizer = tokenizer;
    }
    
    public FullText.Selection parseQuery(String query, MatchOptions options)
        throws DataModelException
    {
        tokens = tokenize(query);
        tokenCount = tokens.length;
        matchOptions = new MatchOptions(options);
        optTerms = new FullText.Any();
        reqTerms = new FullText.All();
        negTerms = new FullText.All();
        
        from = 0;
        while (from < tokenCount) {
            FullText.Selection term = parseTerm();
            if(term != null)
                optTerms.addChild(term);
        }
       
        FullText.All result = new FullText.All();
        result.matchOptions = options;
        optTerms.matchOptions = options;
        
        if(optTerms.getChildCount() == 1)
            result.addChild(optTerms.getChild(0));
        else if(optTerms.getChildCount() > 0)
            result.addChild(optTerms);
        for(int c = 0; c < reqTerms.getChildCount(); c++)
            result.addChild(reqTerms.getChild(c));
        for(int c = 0; c < negTerms.getChildCount(); c++)
            result.addChild(negTerms.getChild(c));
        
        if(result.getChildCount() == 0)
            throw new DataModelException("void full-text query");
        if(result.getChildCount() == 1) // And with only 1 child
            return result.getChild(0);
        return result;
    }

    // -----------------------------------------------------------------------
    // Primary Tokenization
    // -----------------------------------------------------------------------

    private static final int SPACE = ' ';
    private static final int WORD = -1;
    private static final int STRING = -2;

    private static final class Token
    {
        public final int code;
        public final String lexeme;

        public Token(int code, String lexeme)
        {
            this.code = code;
            this.lexeme = lexeme;
        }

        public String toString()
        {
            switch (code) {
            case WORD:
                return "WORD: " + lexeme;
            case STRING:
                return "STRING:\"" + lexeme + '"';
            default:
                return "\'" + (char) code + '\'';
            }
        }
    }

    private static Token[] tokenize(String query)
    {
        int length = query.length();
        ArrayList/*Token*/ list = new ArrayList();

        boolean wasDelimiter = true;
        byte space = -1;
        char quote = 0;
        StringBuffer lexeme = null;

        for (int i = 0; i < length; ++i) {
            char c = query.charAt(i);

            boolean delimiter = false;
            switch (c) {
            case '+':
            case '-':
                if (wasDelimiter) {
                    // '+' and '-' are delimiters only at the beginning of a
                    // term.
                    delimiter = true;
                }
                space = -1;
                break;
            case '~':
                delimiter = true;
                space = -1;
                break;
            default:
                if (Character.isWhitespace(c)) {
                    delimiter = true;
                    if (space < 0) {
                        // Collapse consecutive whitespace characters.
                        space = 0;
                    }
                }
                else {
                    space = -1;
                }
                break;
            }
            wasDelimiter = delimiter;

            if (delimiter) {
                if (quote != 0) {
                    lexeme.append(c);
                }
                else {
                    if (lexeme != null) {
                        list.add(new Token(WORD, lexeme.toString()));
                        lexeme = null;
                    }

                    if (space >= 0) {
                        if (space == 0) {
                            space = 1;
                            list.add(new Token(SPACE, " "));
                        }
                    }
                    else {
                        list.add(new Token(c, Character.toString(c)));
                    }
                }
            }
            else {
                if (lexeme == null) {
                    lexeme = new StringBuffer();

                    switch (c) {
                    case '\"':
                    case '\'':
                        quote = c;
                        break;
                    default:
                        lexeme.append(c);
                    }
                }
                else {
                    if (c == quote) {
                        int last = lexeme.length() - 1;

                        if (last > 0 && lexeme.charAt(last) == '\\') {
                            lexeme.setCharAt(last, quote);
                        }
                        else {
                            list.add(new Token(STRING, lexeme.toString()));
                            lexeme = null;
                            quote = 0;
                        }
                    }
                    else {
                        lexeme.append(c);
                    }
                }
            }
        }

        if (lexeme != null) {
            list.add(new Token((quote != 0) ? STRING : WORD, lexeme.toString()));
        }

        int count = list.size();

        // Trim leading and trailing SPACE ---
        if (count > 0) {
            Token token = (Token) list.get(0);
            if (token.code == SPACE) {
                list.remove(0);
                --count;
            }
        }
        if (count > 0) {
            Token token = (Token) list.get(count - 1);
            if (token.code == SPACE) {
                list.remove(count - 1);
                --count;
            }
        }

        Token[] tokens = new Token[count];
        return (Token[]) list.toArray(tokens);
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    // parse a term starting at 'from'
    private FullText.Selection parseTerm()
    {
        from = skipSpace(tokens, from, tokenCount);
        if (from >= tokenCount)
            return null;

        int code = tokens[from].code;
        String lexeme = tokens[from].lexeme;

        switch (code) {
        case WORD:
        case STRING: {
            int distance = -1;
            if (code == STRING && from + 2 < tokenCount
                    && tokens[from + 1].code == '~'
                    && tokens[from + 2].code == WORD) {
                try {
                    distance = Integer.parseInt(tokens[from + 2].lexeme);
                }
                catch (NumberFormatException ignored) {
                }
                from += 3;
            }
            else {
                ++from;
            }

            startTokenizerForWildcards(lexeme);
            FullText.Phrase phrase = new FullText.Phrase();
            phrase.matchOptions = matchOptions;
            int wordCnt = 0;
            for (int tk; (tk = tokenizer.nextToken()) != TextTokenizer.END; ) {
                if(tk == TextTokenizer.WORD) {
                    char[] token = tokenizer.getTokenChars();
                    FullText.Selection sel = 
                        tokenizer.gotWildcard() ? new FullText.Wildcard(token)
                          : (FullText.Selection) new FullText.SimpleWord(token);
                    sel.matchOptions = matchOptions;
                    phrase.addChild(sel);
                    ++ wordCnt;
                }
            }
            if (distance > 0 && wordCnt > 1) {
                FullText.PosFilters posFilters = new FullText.PosFilters(true);
                posFilters.window = distance + wordCnt; // not same meaning as window
                phrase.setPosFilters(posFilters);
            }
            skipToSpace(tokens, from, tokenCount);
            if(wordCnt == 0)
                return null;
            return wordCnt == 1 ? phrase.getChild(0) : phrase;
        }

        case '+':
        case '-': {
            if (from + 1 < tokenCount
                && (tokens[from + 1].code == WORD || tokens[from + 1].code == STRING))
            {
                ++ from;
                FullText.Selection term = parseTerm();
                if(term != null)
                    if(code == '+') {
                        // add required term:
                        reqTerms.addChild(term);
                    }
                    else {
                        FullText.Not not = new FullText.Not();
                        not.setChild(term);
                        not.matchOptions = matchOptions;
                        negTerms.addChild(not);
                    }
                skipToSpace(tokens, from, tokenCount);
            }
            else {
                // Just ignore '+' or '-'.
                ++ from;
            }
            return null;
        }
        default:
            // Just ignore unknown or unexpected token.
            ++ from;
            return null;
        }
    }


    private static int skipSpace(Token[] tokens, int from, int count)
    {
        while (from < count && tokens[from].code == SPACE) {
            ++from;
        }
        return from;
    }

    private static int skipToSpace(Token[] tokens, int from, int count)
    {
        while (from < count && tokens[from].code != SPACE) {
            ++from;
        }
        return from;
    }

    private void startTokenizerForWildcards(String s)
    {
        tokenizer.setAcceptingWildcards(false);
        boolean wildcard1 = (s.indexOf('?') >= 0);
        boolean wildcard2 = (s.indexOf('*') >= 0);
        if (wildcard1 || wildcard2) {
            if (s.indexOf('.') >= 0) {
                s = s.replaceAll("\\.", "\\.");
            }
            if (wildcard1) {
                s = s.replaceAll("\\?", ".{1,1}");
            }
            if (wildcard2) {
                s = s.replaceAll("\\*", ".*");
            }

            tokenizer.setAcceptingWildcards(true);
        }
        tokenizer.start(s);
    }

    /**
     * Replaces simple wildcard chars by standard FT syntax:
     * - question mark replaced by .{1,1}
     * - simple star replaced by .*
     * - dot is escaped
     */
    private List/*<String>*/ tokenizeWithWildcards(String s)
    {
        tokenizer.setAcceptingWildcards(false);
        boolean wildcard1 = (s.indexOf('?') >= 0);
        boolean wildcard2 = (s.indexOf('*') >= 0);
        if (wildcard1 || wildcard2) {
            if (s.indexOf('.') >= 0) {
                s = s.replaceAll("\\.", "\\.");
            }
            if (wildcard1) {
                s = s.replaceAll("\\?", ".{1,1}");
            }
            if (wildcard2) {
                s = s.replaceAll("\\*", ".*");
            }

            tokenizer.setAcceptingWildcards(true);
        }
        
        ArrayList/*<String>*/ tokens = new ArrayList();
        tokenizer.start(s);
        loop: for (;;) {
            switch (tokenizer.nextToken()) {
            case TextTokenizer.WORD: {
                int begin = tokenizer.getTokenOffset();
                int end = begin + tokenizer.getTokenLength();
                String token = s.substring(begin, end);

                // Our XQuery Full-Text is always:
                // * case insensitive
                // * diacritics insensitive
                // 
                // and the tokenizer cannot normalize a token
                // so we have to do that ourselves.

                token = Unicode.collapseDiacritic(token);

                tokens.add(token);
            }
                break;
            case TextTokenizer.END:
                break loop;
            }
        }
        return tokens;
    }
}
