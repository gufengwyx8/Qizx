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
package com.qizx.api.util.fulltext;

import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.util.RegexMatchPattern;
import com.qizx.util.basic.IntSet;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Generic Text Tokenizer, suitable for most Western languages.
 * <p>Words are 1) a sequence of letters or digits, beginning with a letter; 
 * 2) a number, without exponent. Words never contain a dash or an apostrophe.
 */
public class DefaultTextTokenizer
    implements TextTokenizer
{
    private char[] txt;
    private int txtLength;
    private int ptr;
    private int wordStart;

    private boolean parseSpecialChars; 
    private boolean acceptWildCards; 
    private int maxDigit = 3;   // word rejected if contains more than N digits
    
    private boolean gotWildCard;
    private IntSet specialChars; 
    
    RegexMatchPattern rangePattern;

    public void start(char[] text, int length)
    {
        txt = text;
        txtLength = length;
        ptr = 0;
    }

    public void start(CharSequence text)
    {
        int len = text.length();
        if(text instanceof String) {
            this.txt = ((String) text).toCharArray();
        }
        else { // copy!
            if(this.txt == null || this.txt.length < len)
                this.txt = new char[len];
            for (int i = 0; i < len; i++) {
                this.txt[i] = text.charAt(i);
            }
        }
        this.txtLength = len;
        ptr = 0;
    }

    public void copyTokenTo(char[] array, int start)
    {
        int tlen = ptr - wordStart;
        for (int iw = tlen; --iw >= 0;)
            array[start + iw] = txt[wordStart + iw];
    }

    public char[] getTokenChars()
    {
        if(ptr > txtLength)
            ptr = txtLength;    // OOPS?
        int tlen = ptr - wordStart;
        if (tlen <= 0)
            return null;
        // dont remove words of length 1
        char[] word = new char[tlen];
        for (int iw = tlen; --iw >= 0;)
            word[iw] = txt[wordStart + iw];
        return word;
    }
    
    public int getTokenOffset()
    {
        return wordStart;
    }

    public int getTokenLength()
    {
        return ptr - wordStart;
    }

    public boolean isAcceptingWildcards()
    {
        return acceptWildCards;
    }

    public void setAcceptingWildcards(boolean acceptingWildcards)
    {
        acceptWildCards = acceptingWildcards;
    }

    public boolean isParsingSpecialChars()
    {
        return parseSpecialChars;
    }

    public void setParsingSpecialChars(boolean parsingSpecialChars)
    {
        parseSpecialChars = parsingSpecialChars;
    }

    public void defineSpecialChar(char ch)
    {
        if(specialChars == null)
            specialChars = new IntSet(ch);
        else
            specialChars.add(ch);
    }

    public boolean gotWildcard()
    {
        return gotWildCard;
    }

    public int nextToken()
    {
        gotWildCard = false;
        wordStart = ptr;
        for (; ptr < txtLength; ptr++)
        {
            wordStart = ptr;
            char ch = txt[ptr];
            
            if(Character.isLetter(ch) || testWildcard(ch)) {
                // begin word on a letter:
                // simple: break on dash and quote/apostrophe
                // accept dot always if acceptWildCards (query parsing)
                int digitCnt = 0;
                ++ptr;
                for(; ptr < txtLength; ) {
                    ch = txt[ptr];
                    if(ch == '\\' && acceptWildCards && ptr < txtLength) {
                        ptr += 2; //swallow both
                    }
                    else if(Character.isLetter(ch))
                        ++ ptr;
                    else if(Character.isDigit(ch)) {
                        ++ ptr;
                        ++ digitCnt;
                    }
                    else if(testWildcard(ch))
                        ++ ptr;
                    else
                        break;
                }
                if(digitCnt > maxDigit)
                    continue;
                return WORD;
            }
            else if(Character.isDigit(ch)) {
                // accept dot and comma inside number (but not at end)
                ++ptr;
                for(; ptr < txtLength; ptr++) {
                    ch = txt[ptr];
                    if(!Character.isDigit(ch) && !testWildcard(ch)
                         //&& !((ch == '.' || ch == ',') && Character.isDigit(charAhead(1)))
                        )
                        break;
                }
                return WORD;
            }
            else if(parseSpecialChars && 
                    specialChars != null && specialChars.test(ch)) {
                ++ ptr;
                return ch;
            }
            else if(ch == '\\' && acceptWildCards && ptr < txtLength) {
                ++ ptr;
            }
            else ; // throw away
        }
        wordStart = ptr;
        return END;
    }

    /**
     * Test whether a wildchar sequence lies ahead. On return the current 
     * position is on the last character of the sequence.
     * @param c current char (at current position ptr)
     * @return true if a wildchar sequence has been recognized.
     */
    protected boolean testWildcard(char c)
    {
        if(!acceptWildCards)
            return false;
        if(c != '.')
            return false;
        char ch2 = charAhead(1);
        if(ch2 == '?' || ch2 == '+' || ch2 == '*')
            ptr += 2; // ptr is on occ indicator
        else if(ch2 == '{') {
            int cp = ptr + 1;
            for(; cp < txtLength && txt[cp] != '}'; )
                ++ cp;
//            if(cp < txtLength)
//                ++ cp;
            if(cp >= txtLength || txt[cp] != '}') {
                ptr = cp;
                throw new PatternSyntaxException("missing closing '}'",
                                                 new String(txt), txtLength);
            }
            if(rangePattern == null)
                // 
                rangePattern = new RegexMatchPattern(Pattern.compile("\\d+,\\d+?"));
            String rex = new String(txt, ptr + 2, cp - ptr - 2);
            if (!rangePattern.matches(rex))
                throw new PatternSyntaxException("invalid wildcard repeat range",
                                                 rex, 0);
            ptr = cp;
        }
        else ++ptr;
        gotWildCard = true;
        return true;
    }
    
    protected char charAhead(int offset)
    {
        int pos = ptr + offset;
        return (pos < 0 || pos >= txtLength)? (char)0 : txt[pos];
    }

    public int getDigitMax()
    {
        return maxDigit;
    }

    public void setDigitMax(int max)
    {
        maxDigit = max;
    }
}
