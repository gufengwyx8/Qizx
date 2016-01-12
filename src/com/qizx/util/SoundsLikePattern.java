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
package com.qizx.util;

/**
 * Matches strings that "sound like" a pattern. The algorithm is inspired by
 * Soundex, though a little more accurate and generic.
 */
public class SoundsLikePattern extends StringPattern
{
    char[] codedPattern = new char[10];
    int cpatLen;
    char[] input;
    int ipos, ilen;
    int distance;

    public SoundsLikePattern(char[] pattern, int length, int distance)
    {
        super(pattern, length);
        parsePattern(pattern, length);
        this.distance = distance;
        fixedPrefix = new char[0];
    }

    public void parsePattern(char[] input, int length)
    {
        cpatLen = 0;
        setInput(input, length);
        if (ilen > codedPattern.length)
            codedPattern = new char[ilen];
        char prevCode = '?';
        for (;;) {
            char c = nextCode();
            if (c == C_END)
                break;
            if (c == prevCode && !(c <= '9' && c >= '0'))
                continue;
            prevCode = c;
            codedPattern[cpatLen++] = c;
            
        }
        
    }

    public boolean matches(char[] string)
    {
        setInput(string, string.length);
        if (Math.abs(ilen - cpatLen) > 2 * distance) // heuristics
            return false;
        char prevCode = '?';
        int pp = 0;
        for (;;) {
            char c = nextCode();
            if (c == C_END)
                break;
            if (c == prevCode && !(c <= '9' && c >= '0'))
                continue;
            prevCode = c;
            // System.out.print(" "+c);
            if (pp >= cpatLen || c != codedPattern[pp]) {
                // System.out.println(" ** "+cpatLen+" "+codedPattern[pp]);
                return false;
            }
            ++pp;
        }
        return (pp == cpatLen);
    }

    public int match(char[] string)
    {
        return matches(string) ? MATCH : NOMATCH; // OPTIM
    }

    public String toString()
    {
        return "Fuzzy " + distance + " " + new String(pattern);
    }

    void setInput(char[] input, int length)
    {
        this.input = input;
        ilen = length;
        ipos = 0;
    }

    final static char C_END = 0;

    // a very generic phonetic algorithm, probably not very good
    char nextCode()
    {
        for (;;) {
            if (ipos >= ilen)
                return C_END;
            char c;
            switch (c = input[ipos++]) {
            case 'a':
                if (ipos < ilen && (input[ipos] == 'y' || input[ipos] == 'i')) {
                    ++ipos;
                    return 'y';
                }
                return 'o';

            case 'o':
            case 'u':
                return 'o';

            case 'e':
            case 'i':
            case 'y':
                return 'y';

            case 'c':
                if (ipos < ilen && input[ipos] == 'h') {
                    ++ipos;
                    return 's';
                }
                return 'k';

            case 'd':
            case 't':
                return 'd';

            case 'h': // ignore
                break;

            case 'p':
                if (ipos < ilen && input[ipos] == 'h') {
                    ++ipos;
                    return 'f';
                }
                return 'p';

            case 'q':
                if (ipos < ilen && input[ipos] == 'u') {
                    ++ipos;
                    return 'k';
                }
                return c;

            case 'g':
                return 'k';

                // case 'l': case 'r': case 'w':

            case 'n':
            case 'm':
                return 'm';

            default:
                return c;

            }
        }
    }
}
