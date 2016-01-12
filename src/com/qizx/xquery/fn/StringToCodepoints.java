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
package com.qizx.xquery.fn;

import com.qizx.api.EvaluationException;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.IntegerValue;

/**
 *  Implementation of function fn:string-to-codepoints.
 */
public class StringToCodepoints extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("string-to-codepoints", XQType.INTEGER.star, Exec.class)
        .arg("srcval", XQType.STRING)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            String string = args[0].evalAsOptString(focus, context);
            if (string == null)
                return XQValue.empty;
            return new Sequence(string);
        }
    }

    static class Sequence extends IntegerValue
    {
        String source;
        int position = 0;
        int currentCode;

        Sequence(String source)
        {
            this.source = source;
        }

        public boolean next()
            throws EvaluationException
        {
            ++position;
            if (position - 1 >= source.length())
                return false;
            char c = source.charAt(position - 1);
            if (!XMLUtil.isSurrogateChar(c)) {
                currentCode = c;
            }
            else {
                if (position - 1 >= source.length()) // no low surrogate
                    return false; // or error?
                char losu = source.charAt(position - 1);
                ++position;
                // test validity of codes?
                currentCode = XMLUtil.supplementalChar(c, losu);
            }
            return true;
        }

        public long getInteger()
        {
            return currentCode;
        }

        public long getValue()
        {
            return currentCode;
        }

        public XQValue bornAgain()
        {
            return new Sequence(source);
        }
    }
}
