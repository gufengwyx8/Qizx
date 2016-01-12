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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Implementation of function fn:replace.
 */
public class Replace extends Matches {

    static Prototype[] protos = { 
        Prototype.fn("replace", XQType.STRING.opt, Exec.class)
            .arg("input", XQType.STRING.opt)
            .arg("pattern", XQType.STRING)
            .arg("replacement", XQType.STRING),
        Prototype.fn("replace", XQType.STRING.opt, Exec.class)
            .arg("input", XQType.STRING.opt)
            .arg("pattern", XQType.STRING)
            .arg("replacement", XQType.STRING)
            .arg("flags", XQType.STRING)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.StringCall
    {
        Pattern precompiled;

        // precompile regexp if constant
        public void compilationHook()
        {
            Expression xflags = args.length < 4 ? null : args[3];
            precompiled = precompileRegexp(args[1], xflags, false);
        }

        public String evalAsString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            String input = args[0].evalAsOptString(focus, context);
            String replacement = args[2].evalAsString(focus, context);
            if (input == null)
                input = "";
            context.at(this);

            Expression xflags = args.length < 4 ? null : args[3];
            Pattern pat = precompiled != null
                            ? precompiled
                            : compileRegexp(args[1], xflags, false,
                                            focus, context);
            
            int bs = replacement.indexOf('\\'), rlen = replacement.length();
            if(bs >= 0) {
                char next = (bs == rlen - 1)? '\n' : replacement.charAt(bs + 1);
                if(next != '\\' && next != '$')
                    context.error("FORX0004", this,
                                  "invalid replacement group at position " + bs);
            }
            
            Matcher matcher = pat.matcher(input);

            try {
                return matcher.replaceAll(replacement);
            }
            catch (RuntimeException e) { // protect against wrong replacements
                context.error("FORX0004", this, e.getMessage());
                return null;
            }
        }
    }
}
