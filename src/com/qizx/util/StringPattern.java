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
 * Generic string pattern used for string matching.
 */
public abstract class StringPattern
{
    /**
     * Value returned by method {@link #match(char[])}: perfect match.
     */
    public static final int MATCH = 0;
    /**
     * Value returned by method {@link #match(char[])}: no match.
     */
    public static final int NOMATCH = 1;
    /**
     * Value returned by method {@link #match(char[])}: no match and instance 
     * is greater than pattern: used to quickly stop index iterator.
     */
    public static final int BEYOND = 2;
    
    
    protected char[] pattern;
    protected char[] fixedPrefix;
    protected int fixedPrefixStart; // can be > length(fixedPrefix) (b/c escapes)
    protected boolean caseSensitive;

    public StringPattern()
    {
    }

    public StringPattern(char[] pattern, int length)
    {
        setPattern(pattern, length);
    }

    /**
     * Redefinable matching method: returns match or not match.
     */
    public boolean matches(char[] string)
    {
        return match(string) == MATCH;
    }

    public boolean matches(String value)
    {
        return matches(value.toCharArray());
    }

    /**
     * Redefinable matching method: returns values MATCH, NOMATCH or BEYOND.
     */
    public abstract int match(char[] string);

    /**
     * Returns true if the pattern can only match one in a set of distinct values.
     * (In contrast with regexp patterns for example which can match several
     * different values).
     */
    public boolean singleMatch()
    {
        return false;
    }


    protected void setPattern(char[] pattern, int length)
    {
        this.pattern = pattern;
        if (pattern != null ) {
            this.pattern = Util.subArray(pattern, 0, length);
            fixedPrefix = this.pattern;
            fixedPrefixStart = fixedPrefix.length;
        }
    }

    /**
     * Returns the leading constant part of the pattern. Used to boost matching
     * and lookup in indexes.
     * Must not return null: return empty string instead.
     */
    public char[] fixedPrefix()
    {
        return fixedPrefix;
    }

    public char[] getPattern()
    {
        return pattern;
    }

    public String toString()
    {
        return Util.shortClassName(getClass()) +"(" + new String(pattern) +")";
    }
}
