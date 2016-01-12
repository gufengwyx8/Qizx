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
 * Matches strings representing a path (with '/' separator) against a pattern
 * which is a parent path.
 * Can match direct children only, or any descendants.
 */
public class PathPrefixPattern extends PrefixPattern
{
    boolean directChild;
    
    /**
     * @param path a normalized path using slashes
     * @param directChild if true, matches only child paths
     */
    public PathPrefixPattern(String path, boolean directChild)
    {
        super(null, 0); // required
        this.directChild = directChild;
        if(!path.endsWith("/"))
            path = path + "/";
        setPattern(path.toCharArray(), path.length());
    }

    public boolean matches(char[] string)
    {
        // check length:
        int plen = pattern.length;
        if(string.length <= plen)   // must be strictly longer
            return false;  
        
        // start from the end: intentional, should be faster in general
        for(; --plen >= 0; )
            if(string[plen] != pattern[plen])
                return false;
        
        // if direct child, check there is no slash after:
        if(directChild) {
            plen = pattern.length;
            for(int i = string.length; --i >= plen; )
                if(string[i] == '/')
                    return false;
        }
        return true;
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
        if(string.length <= plen)
            return NOMATCH;
        // prefix OK: check slash if child
        if(directChild) {
            plen = pattern.length;
            for(int i = string.length; --i >= plen; )
                if(string[i] == '/')
                    return NOMATCH;
        }
        return MATCH;
    }
}
