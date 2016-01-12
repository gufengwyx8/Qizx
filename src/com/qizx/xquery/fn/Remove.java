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
 *  Implementation of function fn:remove.
 */
public class Remove extends Function {

    static Prototype[] protos = { 
        Prototype.fn("remove", XQType.ITEM.star, Exec.class)
            .arg("target", XQType.ITEM.star)
            .arg("position", XQType.INTEGER)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQValue src = args[0].eval(focus, context);
            long pos = args[1].evalAsInteger(focus, context);
            context.at(this);
            return new Sequence(src, pos);
        }
    }

    public static class Sequence extends FilteredSequence
    {
        protected long removed;

        public Sequence(XQValue source, long removed)
        {
            super(source);
            this.removed = removed;
        }

        public XQValue bornAgain()
        {
            return new Sequence(source.bornAgain(), removed);
        }

        public boolean next()
            throws EvaluationException
        {
            for (;;) {
                ++position;
                if (!source.next())
                    return false;
                if (position == removed)
                    continue;
                item = source.getItem();
                return true;
            }
        }
    }
}
