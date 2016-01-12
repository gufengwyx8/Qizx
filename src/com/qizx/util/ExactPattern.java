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
 * Matching of a string with a pattern.
 * <p>
 * This superclass implements the identity comparison.
 */
public class ExactPattern extends StringPattern
{
    public ExactPattern(char[] pattern, int length)
    {
        setPattern(pattern, length);
    }
    
    public ExactPattern(String pattern)
    {
        this(pattern.toCharArray(), pattern.length());
    }


    public boolean singleMatch()
    {
        return true;
    }

    public String toString()
    {
        return "ExactPattern(" + new String(pattern) + ")";
    }

    public int match(char[] string)
    {
        int plen = pattern.length;
        int slen = string.length;
        for (int i = 0, len = Math.min(plen, slen); i < len; i++) {
            int diff = string[i] - pattern[i];
            if(diff > 0)
                return BEYOND;
            if(diff != 0)
                return NOMATCH;
        }
        return (plen < slen) ? BEYOND : (plen > slen) ? NOMATCH : MATCH;
    }
    
    public boolean matches(char[] string)
    {
        if (string.length != pattern.length)
            return false;
        for (int i = string.length; --i >= 0;)
            if (string[i] != pattern[i])
                return false;
        return true;
    }
}
