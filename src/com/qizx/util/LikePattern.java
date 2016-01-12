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

import com.qizx.util.basic.Util;


/**
 * Pattern similar to fileNAME expansion (not full path, because ignores '/').
 * SQL-LIKE mapped to this by substituting meta characters.
 * <p>Case-insensitive by default.
 */
public class LikePattern extends StringPattern
{    
    public LikePattern(String pattern)
    {
        this(pattern.toCharArray(), pattern.length());
    }

    public LikePattern(char[] pattern, int length)
    {
        super(pattern, length);
        
        int pos = 0;
        if(pattern != null) {
            for (; pos < pattern.length; pos++)
                if (pattern[pos] == '*' || pattern[pos] == '?'
                    || pattern[pos] == '[')
                    break;
            fixedPrefix = new char[pos];
            System.arraycopy(pattern, 0, fixedPrefix, 0, pos);
            fixedPrefixStart = pos;
        }
    }

    public boolean matches(String string)
    {
        return matches(string.toCharArray());
    }

    public boolean matches(char[] string)
    {
        return glob(string, 0, 0);
    }

    public int match(char[] string)
    {
        
        // compare fixed prefix: used to determine if beyond
        int plen = fixedPrefix.length;
        int cmp = Util.prefixCompare(fixedPrefix, string, plen, caseSensitive, true);
        if(cmp != 0)
            return (cmp < 0) ? BEYOND : NOMATCH;
        // normal matching from end of fixed prefix:
        return glob(string, plen, fixedPrefixStart) ? MATCH : NOMATCH;
    }

    public boolean glob(char[] string, int ps, int pp)
    {
        int slen = string.length;
        int plen = pattern.length;

        for (;; ++pp, ++ps) {
            // See if we're at the end of both the pattern and the string.
            // If so, we succeeded. If we're at the end of the pattern
            // but not at the end of the string, we failed.
            if (pp == plen)
                return (ps == slen);

            if (ps == slen && pattern[pp] != '*')
                return false;

            // Check for a "*" as the next pattern character. It matches
            // any substring. We handle this by calling ourselves
            // recursively for each postfix of string, until either we
            // match or we reach the end of the string.
            if (pattern[pp] == '*') {
                ++pp;
                if (pp == plen)
                    return true; // no need to go on

                for (;; ps++) {
                    if (glob(string, ps, pp))
                        return true;
                    if (ps == slen)
                        return false;
                }
            }

            // Check for a "?" as the next pattern character. It matches
            // any single character.
            if (pattern[pp] == '?')
                continue;
            else if (pattern[pp] == '[') {
                ++pp;
                boolean negated = false;
                if (pp < plen && pattern[pp] == '^') {
                    negated = true;
                    ++pp;
                }
                for (;;) {
                    if (pattern[pp] == ']' || pp >= plen) {
                        // end of range reached: success if negated, else
                        // failure
                        if (!negated)
                            return false;
                        break;
                    }
                    else if (pattern[pp] == string[ps]) {
                        if (negated)
                            return false;
                        break;
                    }
                    if (pattern[pp + 1] == '-' && pp < plen - 2) {
                        if (pattern[pp] <= string[ps]
                            && string[ps] <= pattern[pp + 2]) {
                            if (negated)
                                return false;
                            break;
                        }
                        pp += 2;
                    }
                    ++pp;
                }
                // in case of match, skip remainder of [char set]
                while (pp < plen && pattern[pp] != ']') {
                    ++pp;
                }
                continue;
            }
            /*
             * If the next pattern character is '\', just strip it off to do
             * exact matching on the character that follows.
             */
            if (pattern[pp] == '\\')
                if (++pp == plen)
                    return false;

            // There's no special character. Just make sure that the next
            // characters of each string match.
            if(caseSensitive) {
                if (pattern[pp] != string[ps])
                    return false;
            }
            else {
                if (Character.toUpperCase(pattern[pp]) !=
                    Character.toUpperCase(string[ps]))
                    return false;
            }
        }
    }

    public String toString()
    {
        return new String(pattern);
    }

    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }
}
