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

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

/**
 *  Implementation of function fn:collection in Qizx/open.
 */
public class Collection extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("collection", XQType.NODE.star, Exec.class),
        Prototype.fn("collection", XQType.NODE.star, Exec.class)
            .arg("path", XQType.STRING),
        Prototype.fn("collection", XQType.NODE.star, Exec.class)
            .arg("path", XQType.STRING)
            .arg("predicate", XQType.ANY)
    };
    public Prototype[] getProtos() { return protos; }
    public static com.qizx.api.QName DM_ERR =
        ModuleContext.xqueryErrorCode("DM000001");
    
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if (args.length == 0) {
                XQValue in = context.dynamicContext().getDefaultCollection();
                if (in == null)
                    context.error("XPDY0002", this,
                                  "unspecified default collection");
                return in;
            }
            if (args.length == 2) { // help poor user to understand
                context.error("XLIB0000", this,
                              "not connected to a XML library");
            }
            String uri = args[0].evalAsOptString(focus, context);
            if (uri == null)
                return XQValue.empty;
            context.at(this);
            try {
                return context.dynamicContext().getCollectionSequence(uri);
            }
            catch (DataModelException e) {
                return context.error(DM_ERR, this, e.getMessage());
            }
        }

        public int getFlags()
        {
            return DOCUMENT_ORDER + SAME_DEPTH;
        }
    }
}
