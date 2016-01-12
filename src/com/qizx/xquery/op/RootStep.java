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
package com.qizx.xquery.op;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.NodeFilter;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleNode;

public class RootStep extends BasicStep
{
    public RootStep() {
        super(null);
    }

    public RootStep(NodeFilter nodeTest)
    {
        super(nodeTest);
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("nodeTest", nodeTest == null ? null : nodeTest.toString());
    }

    public int getFlags()
    {
        return DOCUMENT_ORDER + WITHIN_SUBTREE + SAME_DEPTH;
    }

    public XQValue eval(Focus focus, EvalContext context, XQValue recycled)
        throws EvaluationException
    {
        context.at(this);
        // if there is a current item: normal evaluation
        XQItem current = null;
        if (focus != null && (current = focus.currentItem()) != null)
            try {
                return new SingleNode((BasicNode) current.getNode().getDocumentNode());
            }
            catch (DataModelException e) {
                throw BasicNode.wrapDMException(e);
            }
        // if we have a default collection, use it as root:
        XQValue dc = context.dynamicContext().getDefaultCollection();
        if(dc != null)
            return dc;
        // otherwise failure
        checkFocus(focus, context);
        return null; // dummy
    }
}
