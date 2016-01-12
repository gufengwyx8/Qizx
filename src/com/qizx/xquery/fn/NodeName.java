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
import com.qizx.api.QName;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleQName;

/**
 *  Implementation of function fn:node-name.
 */
public class NodeName extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("node-name", XQType.QNAME.opt, Exec.class),

        Prototype.fn("node-name", XQType.QNAME.opt, Exec.class)
        .arg("node", XQType.ANY)
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
                if (focus == null || focus.currentItem() == null
                    || !focus.currentItem().isNode())
                    context.error("XPTY0020", this,
                                  "context item is not a node");
                else
                    node = focus.currentItem().getNode();
            }
            else
                node = args[0].evalAsOptNode(focus, context);
            if (node == null)
                return XQValue.empty;

            try {
                QName qname = node.getNodeName();
                if (qname == null)
                    return XQValue.empty;
                String prefix = node.getNamespacePrefix(qname.getNamespaceURI());
                if(prefix == null)
                    return new SingleQName(IQName.get(qname));
                return new SingleQName(XQName.get(qname, prefix));
            }
            catch (DataModelException e) {
                dmError(context, e);
                return null; // dummy
            }
        }
    }
}
