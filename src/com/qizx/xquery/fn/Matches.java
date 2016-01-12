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
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.StringLiteral;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *  Implementation of function fn:matches.
 */
public class Matches extends Function
{    
    private static Prototype[] protos = { 
        Prototype.fn("matches", XQType.BOOLEAN.opt, Exec.class)
            .arg("input", XQType.STRING.opt)
            .arg("pattern", XQType.STRING),
        Prototype.fn("matches", XQType.BOOLEAN.opt, Exec.class)
            .arg("input", XQType.STRING.opt)
            .arg("pattern", XQType.STRING)
            .arg("flags", XQType.STRING)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static int convertFlags(String flags)
    {
        int cflags = Pattern.UNIX_LINES;
        for (int i = flags.length(); --i >= 0;)
            if (flags.charAt(i) == 'm')
                cflags |= Pattern.MULTILINE;
            else if (flags.charAt(i) == 'i')
                cflags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            else if (flags.charAt(i) == 's') // Nov 2003
                cflags |= Pattern.DOTALL;
            else if (flags.charAt(i) == 'x') // Nov 2003
                cflags |= Pattern.COMMENTS;
            else
                return -1;
        return cflags;
    }

    public static Pattern precompileRegexp(Expression patternx,
                                           Expression flagsx,
                                           boolean allowEmptyMatch)
    {
        if (patternx instanceof StringLiteral
            && (flagsx == null || flagsx instanceof StringLiteral)) {
            try {
                String pattern = ((StringLiteral) patternx).value, flags = "";
                if (flagsx != null)
                    flags = ((StringLiteral) flagsx).value;
                int cflags = convertFlags(flags);
                if (cflags < 0)
                    return null; // delay to runtime
                Pattern pat = Pattern.compile(pattern, cflags);
                if (!allowEmptyMatch && pat.matcher("").matches())
                    return null; // delay to runtime
                return pat;
            }
            catch (PatternSyntaxException e) {
            } // delay to runtime
        }
        return null;
    }

    // run-time:
    public static Pattern compileRegexp(Expression patternExp,
                                        Expression flagsExp,
                                        boolean allowEmptyMatch, Focus focus,
                                        EvalContext context)
        throws EvaluationException
    {
        String regexp = patternExp.evalAsString(focus, context);
        String sflags =
            flagsExp == null ? "" : flagsExp.evalAsString(focus, context);
        int cflags = convertFlags(sflags);
        if (cflags < 0)
            context.error("FORX0001", flagsExp,
                          "illegal regular expression flags");
        try {
            Pattern pattern = Pattern.compile(regexp, cflags);
            if (!allowEmptyMatch && pattern.matcher("").matches())
                context.error("FORX0003", patternExp,
                              "pattern matches empty string");
            return pattern;
        }
        catch (PatternSyntaxException e) {
            context.error("FORX0002", patternExp,
                          "invalid regular expression: " + e.getMessage());
        }
        return null;
    }


    public static class Exec extends Function.BoolCall
    {

        Pattern precompiled = null;

        // precompile regexp if constant
        public void compilationHook()
        {
            Expression xflags = args.length < 3 ? null : args[2];
            precompiled = precompileRegexp(args[1], xflags, true);
        }

        public Pattern preparedPattern(EvalContext context)
            throws EvaluationException
        {
            Expression xflags = args.length < 3 ? null : args[2];
            return precompiled != null 
                    ? precompiled
                    : compileRegexp(args[1], xflags, true, null, context);
        }

        public boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            String input = args[0].evalAsOptString(focus, context);
            if (input == null)
                input = "";
            context.at(this);

            Pattern pat = preparedPattern(context);
            Matcher mat = pat.matcher(input);
            return mat.find();
        }
    }
}
