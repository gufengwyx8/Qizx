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
 * SQL-LIKE pattern matching. Similar to Glob pattern but slightly different
 * syntax.
 */
public class SqlLikePattern extends LikePattern
{
    public SqlLikePattern(String pattern)
    {
        this(pattern.toCharArray(), pattern.length());
    }

    public SqlLikePattern(char[] pattern, int length)
    {
        super(null, 0);
        StringBuffer buf = new StringBuffer(length);
        int fix = -1;
        for (int p = 0; p < length; ++p)
            switch (pattern[p]) {
            case '%':
                if(fix < 0) {
                    fix = p;
                    fixedPrefixStart = buf.length();
                }
                buf.append('*');
                break;
            case '_':
                if(fix < 0) {
                    fix = p;
                    fixedPrefixStart = buf.length();
                }
                buf.append('?');
                break;
            case '*':
                buf.append("\\*");
                break;
            case '?':
                buf.append("\\?");
                break;
            default:
                buf.append(pattern[p]);
                break;
            }
        
        this.pattern = new char[buf.length()];
        buf.getChars(0, this.pattern.length, this.pattern, 0);
        
        fixedPrefix = Util.subArray(pattern, 0, fix < 0? pattern.length : fix);
    }
}
