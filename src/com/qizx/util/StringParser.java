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
 * Simple String parsing utility.
 * 
 */
public class StringParser
{
    protected String s;
    protected int ptr;
    protected int end;
    
    protected int latestInt; // parsed by parseInt;
    
    protected void init(String input)
    {
        s = input;
        end = s.length();
        ptr = 0;
    }

    protected boolean parseDec(int minDigitCount)
    {
        char c = currentChar();
        int cnt = 0;
        latestInt = 0;
        for(; c >= '0' && c <= '9'; ++cnt) {
            latestInt = 10 * latestInt + c - '0';
            c = nextChar();
        }
        return (cnt >= minDigitCount);
    }

    protected boolean pick(char c)
    {
        if(currentChar() != c) 
            return false;
        ++ ptr;
        return true;
    }

    protected char nextChar()
    {
        return (ptr >= end - 1)? 0 : s.charAt(++ ptr);
    }
    
    protected char currentChar()
    {
        return (ptr >= end)? 0 : s.charAt(ptr);
    }
}
