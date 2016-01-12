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

import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.xdm.NodeFilter;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

/**
 * Superclass of path steps.
 */
public abstract class BasicStep extends Expression
{
    public NodeFilter nodeTest;

    // for reverse axes: whether the step must be generated in natural step
    // order
    // (ie reverse) or in document order.
    boolean naturalStepOrder = false;

    public BasicStep(NodeFilter nodeTest)
    {
        this.nodeTest = nodeTest;
    }

    public Expression child(int rank)
    {
        return null;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        type = XQType.NODE.star;
        if (nodeTest != null)
            switch (nodeTest.getNodeKind()) {
            case Node.ELEMENT:
                type = XQType.ELEMENT.star;
                break;
            case Node.DOCUMENT:
                type = XQType.DOCUMENT.star;
                break;
            case Node.TEXT:
                type = XQType.TEXT.star;
                break;
            case Node.COMMENT:
                type = XQType.COMMENT.star;
                break;
            case Node.PROCESSING_INSTRUCTION:
                type = XQType.PI.star;
                break;
            case Node.ATTRIBUTE:
                type = XQType.ATTRIBUTE.star;
                break;
            }
        return this;
    }
    
    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        return eval(focus, context, null);
    }

    protected abstract XQValue eval(Focus focus, EvalContext context,
                                    XQValue recycled)
        throws EvaluationException;
}
