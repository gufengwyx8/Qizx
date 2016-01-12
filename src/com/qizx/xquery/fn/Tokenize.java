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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.StringValue;
import com.qizx.xquery.op.Expression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Implementation of function fn:tokenize.
 */
public class Tokenize extends Matches {

    static Prototype[] protos = { 
        Prototype.fn("tokenize", XQType.STRING.star, Exec.class)
        .arg("input", XQType.STRING.opt)
        .arg("pattern", XQType.STRING),
        Prototype.fn("tokenize", XQType.STRING.star, Exec.class)
        .arg("input", XQType.STRING.opt)
        .arg("pattern", XQType.STRING)
        .arg("flags", XQType.STRING)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.Call
    {

        Pattern precompiled;

        // precompile regexp if constant
        public void compilationHook()
        {
            Expression xflags = args.length < 3 ? null : args[2];
            precompiled = precompileRegexp(args[1], xflags, false);
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            String input = args[0].evalAsOptString(focus, context);
            if (input == null || input.length() == 0)
                return XQValue.empty;
            context.at(this);

            Expression xflags = args.length < 3 ? null : args[2];
            Pattern pat =
                precompiled != null ? precompiled : compileRegexp(args[1],
                                                                  xflags,
                                                                  false,
                                                                  focus,
                                                                  context);

            return new Sequence(input, pat);
        }
    }


    static class Sequence extends StringValue
    {

        String source, current;
        Pattern regexp;
        Matcher matcher;
        int lastStop = 0; // where the last match ends

        Sequence(String source, Pattern regexp)
        {
            this.source = source;
            this.regexp = regexp;
            matcher = regexp.matcher(source);
        }

        public boolean next()
            throws EvaluationException
        {
            if (lastStop < 0) // there was no match at end
                return false;
            if (!matcher.find()) {
                current = source.substring(lastStop);
                lastStop = -1; // means no match at end
            }
            else {
                current = source.substring(lastStop, matcher.start());
                lastStop = matcher.end(); // can be source.length() -> empty token
            }
            return true;
        }

        public String getString()
            throws XQTypeException
        {
            return current;
        }

        public XQValue bornAgain()
        {
            return new Sequence(source, regexp);
        }

        protected String getValue()
        {
            return current;
        }
    }

}
