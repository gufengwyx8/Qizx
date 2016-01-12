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
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.NodeSortExpr;

import java.util.ArrayList;

/**
 *  Implementation of function fn:id.
 */
public class Id extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("id", XQType.ELEMENT.star, Exec.class)
            .arg("arg", XQType.STRING.star),
        Prototype.fn("id", XQType.ELEMENT.star, Exec.class)
            .arg("arg", XQType.STRING.star)
            .arg("node", XQType.NODE)
    };
    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return eval(focus, context, false);
        }

        public XQValue eval(Focus focus, EvalContext context, boolean isIdref)
            throws EvaluationException
        {
            ArrayList ids = expandIds(args[0], focus, context);
            BasicNode node = null;
            if (args.length == 2)
                node = args[1].evalAsNode(focus, context);
            else {
                checkFocus(focus, context);
                node = focus.currentItem().basicNode();
            }
            context.at(this);
            NodeSortExpr.Sequence result = null;
            try {
                // node cannot be null
                node = (BasicNode) node.getDocumentNode();

                if (node == null || node.getNodeNature() != Node.DOCUMENT)
                    context.error("FODC0001", this,
                    "node is not within a document");

                for (int i = 0; i < ids.size(); i++) {
                    String id = (String) ids.get(i);
                    BasicNode[] nodes = node.getIdMatchingNodes(id, isIdref);
                    if (nodes != null)
                        if (result == null)
                            result =
                                new NodeSortExpr.Sequence(nodes, nodes.length);
                        else
                            result.addNodes(nodes, nodes.length);
                }
            }
            catch (DataModelException e) {
                dmError(context, e);
            }
            if (result == null)
                return XQValue.empty;
            result.sort(); // and remove duplicates
            return result;
        }
    }

    static ArrayList expandIds(Expression expression, Focus focus,
                               EvalContext context)
        throws EvaluationException
    {
        XQValue seq = expression.eval(focus, context);
        ArrayList res = new ArrayList();
        for (; seq.next();) {
            String s = seq.getString();
            String[] tokens = s.split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                String name = tokens[i];
                if (XMLUtil.isName(name))
                    res.add(name);
            }
        }
        return res;
    }
}
