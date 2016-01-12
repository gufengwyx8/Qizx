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

/**
 *  Implementation of function fn:sequence-node-identical.
 */
public class SequenceNodeIdentical extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("sequence-node-identical", XQType.BOOLEAN.opt, Exec.class)
            .arg("parameter1", XQType.NODE.star)
            .arg("parameter2", XQType.NODE.star)
    };
    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.BoolCall
    {
        public boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQValue s1 = args[0].eval(focus, context);
            XQValue s2 = args[1].eval(focus, context);
            // NOTE: this is not conformant but the spec is absurd
            for (;;) {
                if (!s1.next())
                    return !s2.next();
                if (!s2.next())
                    return false;
                if (!s1.getNode().equals(s2.getNode()))
                    return false;
            }
        }
    }
}
