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
 * Matches the strings that start with a given prefix.
 */
public class PrefixPattern extends StringPattern
{
    public PrefixPattern(char[] pattern, int length)
    {
        super(pattern, length);
    }

    public PrefixPattern(String pattern)
    {
        this(pattern.toCharArray(), pattern.length());
    }

    public int match(char[] string)
    {
        int plen = pattern.length;
        for (int i = 0, len = Math.min(plen, string.length); i < len; i++) {
            int diff = string[i] - pattern[i];
            if(diff > 0)
                return BEYOND;
            if(diff != 0)
                return NOMATCH;
        }
        return (string.length < plen)? NOMATCH : MATCH;
    }

    public boolean matches(char[] string)
    {
        // boost by checking length:
        if(string.length < pattern.length)
            return false;        
        return match(string) == MATCH;
    }
}
