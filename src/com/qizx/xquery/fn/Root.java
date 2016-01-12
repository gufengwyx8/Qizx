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
import com.qizx.api.Node;
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleNode;
import com.qizx.xquery.op.Expression;

/**
 *  Implementation of function fn:root.
 */
public class Root extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("root", XQType.NODE, Exec.class),
        Prototype.fn("root", XQType.NODE, Exec.class)
            .arg("arg", XQType.NODE)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            Node node = null;
            if (args.length == 0) {
                checkFocus(focus, context);
                XQItem currentItem = focus.currentItem();
                if(!currentItem.isNode())
                    context.error(Expression.ERRC_BADTYPE, this, "expecting Node");
                node = currentItem.getNode();
            }
            else {
                node = args[0].evalAsOptNode(focus, context);
                context.at(this);
            }
            if (node == null)
                return XQValue.empty;
            try {
                return new SingleNode((BasicNode) node.getDocumentNode());
            }
            catch (DataModelException e) {
                return dmError(context, e);
            }
        }
    }
}
