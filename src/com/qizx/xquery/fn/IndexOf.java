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
import com.qizx.xquery.*;
import com.qizx.xquery.dt.IntegerValue;

import java.text.Collator;

/**
 *  Implementation of function fn:index-of.
 */
public class IndexOf extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("index-of", XQType.INTEGER.star, Exec.class)
        .arg("seqParam", XQType.ANY_ATOMIC_TYPE.star)
        .arg("srchParam", XQType.ANY_ATOMIC_TYPE),
        Prototype.fn("index-of", XQType.INTEGER.star, Exec.class)
        .arg("seqParam", XQType.ANY_ATOMIC_TYPE.star)
        .arg("srchParam", XQType.ANY_ATOMIC_TYPE)
        .arg("collationLiteral", XQType.STRING)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQValue src = args[0].eval(focus, context);
            XQItem item = args[1].evalAsItem(focus, context);
            Collator collator =
                getCollator(args.length <= 2 ? null : args[2], focus, context);
            context.at(this);
            return new Sequence(src, item, context, collator);
        }
    }

    public static class Sequence extends IntegerValue
        implements ComparisonContext
    {
        XQValue source;
        XQItem searched;
        int position = 0;
        EvalContext context;
        Collator collator;

        Sequence(XQValue source, XQItem searched, EvalContext context,
                 Collator collator)
        {
            this.source = source;
            this.searched = searched;
            this.context = context;
            this.collator = collator;
        }

        public boolean next()
            throws EvaluationException
        {
            for (; source.next();) {
                ++position;
                if (source.compareTo(searched, this, 0) == 0)
                    return true;
            }
            return false;
        }

        protected long getValue()
        {
            return position;
        }

        public long getInteger()
        {
            return position;
        }

        public XQValue bornAgain()
        {
            return new Sequence(source.bornAgain(), searched, context, collator);
        }

        // --- comp context:
        public boolean emptyGreatest()
        {
            return context.emptyGreatest();
        }

        public Collator getCollator()
        {
            return collator;
        }

        public int getImplicitTimezone()
        {
            return context.getImplicitTimezone();
        }
    }
}
