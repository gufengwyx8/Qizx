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
package com.qizx.xquery.ext;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.util.SqlLikePattern;
import com.qizx.util.StringPattern;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;
import com.qizx.xquery.op.Expression;

/**
 *  Implementation of function x:like.
 */
public class XfnLike extends ExtensionFunction
{
    private static QName qfname =
        IQName.get(EXTENSION_NS, "like");
    private static Prototype[] protos = { 
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("pattern", XQType.STRING).arg("context", XQType.NODE.star),
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
	        .arg("pattern", XQType.STRING)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.BoolCall
    {
        public Expression subDomain;    // supplem arg
        StringPattern previousPattern;
        String previous;
        EvalContext previousContext;

        protected StringPattern preparePattern(String pattern,
                                               EvalContext context)
        {
            if (context == previousContext && pattern.equals(previous))
                return previousPattern;
            previousPattern = new SqlLikePattern(pattern);
            previous = pattern;
            previousContext = context;
            return previousPattern;
        }

        public StringPattern preparedPattern(EvalContext context)
            throws EvaluationException
        {
            if (args.length > 1)
                subDomain = args[1];
            return preparePattern(args[0].evalAsString(null, context), context);
        }

        public boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            StringPattern pattern = preparedPattern(context);

            try {
                if (args.length > 1) {
                    XQValue seq = args[1].eval(focus, context);
                    // stop on first matching node:
                    for (; seq.next();) {
                        Node node = seq.getNode();
                        if (pattern.matches(node.getStringValue()))
                            return true;
                    }
                    return false;
                }
                else {
                    checkFocus(focus, context);
                    return pattern.matches(focus.currentItem().getNode().getStringValue());
                }
            }
            catch (DataModelException e) {
                dmError(context, e); 
                return false; // dummy
            }
        }
    }
}
