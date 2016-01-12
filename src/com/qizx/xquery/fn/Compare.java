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
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.impl.EmptyException;

import java.text.Collator;

/**
 *  Implementation of function fn:compare.
 */
public class Compare extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("compare", XQType.INTEGER.opt, Exec.class)
            .arg("comparand1", XQType.STRING.opt)
            .arg("comparand2", XQType.STRING.opt),
        Prototype.fn("compare", XQType.INTEGER.opt, Exec.class)
            .arg("comparand1", XQType.STRING.opt)
            .arg("comparand2", XQType.STRING.opt)
            .arg("collationLiteral", XQType.STRING)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.IntegerCall
    {
        public long evalAsInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            String s1 = args[0].evalAsOptString(focus, context);
            String s2 = args[1].evalAsOptString(focus, context);

            if (s1 == null || s2 == null)
                throw EmptyException.instance();

            Collator coll =
                getCollator(args.length <= 2 ? null : args[2], focus, context);
            context.at(this);
            // use Comparison.of to obtain EQ LT GT etc
            return Comparison.of(coll != null ? coll.compare(s1, s2)
                                              : s1.compareTo(s2), 0);
        }
    }
}
