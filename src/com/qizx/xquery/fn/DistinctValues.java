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
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.DistinctValueSequence;

import java.text.Collator;

public class DistinctValues extends Function
{

    static Prototype[] protos = { 
        Prototype.fn("distinct-values", XQType.ANY_ATOMIC_TYPE.star, Exec.class)
            .arg("srcval", XQType.ANY_ATOMIC_TYPE.star),
        Prototype.fn("distinct-values", XQType.ANY_ATOMIC_TYPE.star, Exec.class)
            .arg("srcval", XQType.ANY_ATOMIC_TYPE.star)
            .arg("collationLiteral", XQType.STRING)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            Collator collator =
                getCollator((args.length <= 1) ? null : args[1], focus, context);

            context.at(this);
            return new DistinctValueSequence(args[0].eval(focus, context),
                                             collator);
        }
    }
}
