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
import com.qizx.util.Collations;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;

import java.text.Collator;

/**
 *  Implementation of function fn:starts-with.
 */
public class StartsWith extends Function {

    static Prototype[] protos = { 
        Prototype.fn("starts-with", XQType.BOOLEAN.opt, Exec.class)
            .arg("op1", XQType.STRING.opt)
            .arg("op2", XQType.STRING.opt),
        Prototype.fn("starts-with", XQType.BOOLEAN.opt, Exec.class)
            .arg("op1", XQType.STRING.opt)
            .arg("op2", XQType.STRING.opt)
            .arg("collationLiteral", XQType.STRING)
    };

    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.BoolCall
    {
        public boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            String s1 = args[0].evalAsOptString(focus, context);
            String s2 = args[1].evalAsOptString(focus, context);
            if (s2 == null || s2.length() == 0)
                return true;
            if (s1 == null || s1.length() == 0)
                return false;
            Collator collator =
                getCollator(args.length < 3 ? null : args[2], focus, context);
            context.at(this);
            return Collations.indexOf(s1, s2, collator) == 0;
        }
    }
}
