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

import java.util.regex.Pattern;

/**
 *	A String Pattern based on regular expressions.
 * Trick: define this as CharSequence so to reuse the Matcher
 */
public class RegexFindPattern extends RegexMatchPattern
{    
    public RegexFindPattern(Pattern pattern) {
        super(pattern);
        // cannot use prefix here:
        fixedPrefix = new char[0];
    }
    
    public boolean matches(char[] string) {
        toMatch = string;
        matcher.reset();        // mandatory
        return matcher.find();
    }
}
