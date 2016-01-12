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
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.dt.SingleString;

/**
 *  Implementation of function fn:get-in-scope-namespaces.
 */
public class InScopePrefixes extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("in-scope-prefixes", XQType.STRING.star, Exec.class)
            .arg("element", XQType.ELEMENT.opt)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            BasicNode node = args[0].evalAsOptNode(focus, context);
            if (node == null)
                return XQValue.empty;
            ArraySequence seq = new ArraySequence(2, null);
            NamespaceContext nspaces = new NamespaceContext();
            try {
                node.addInScopeNamespacesTo(nspaces);
            }
            catch (DataModelException e) {
                dmError(context, e); 
            }
            for (int n = 0, size = nspaces.getLocalSize(); n < size; n++) {
                String prefix = nspaces.getLocalPrefix(n);
                String uri = nspaces.getLocalNamespaceURI(n);
                // white URI means removed NS:
                if (prefix != null && uri != null && uri != "") // interned
                    seq.addItem(new SingleString(prefix));
            }
            // here only add XML, otherwise big pollution
            seq.addItem(new SingleString("xml"));
            return seq;
        }
    }
}
