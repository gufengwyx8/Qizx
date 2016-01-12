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
package com.qizx.xquery.dt;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.util.DOMToPushStream;
import com.qizx.api.util.PushStreamToDOM;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.NodeFilter;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.op.Expression;

/**
 * Used in JavaBinding for mapping Node to DOM 
 */
public class DomNodeType extends NodeType
{
    public DomNodeType(NodeFilter nodeFilter)
    {
        super(nodeFilter);
        parent = XQType.NODE; // unless set by class Type
    }

    public XQValue convertFromObject(Object object) throws XQTypeException
    {
        if (object instanceof BasicNode)
            return new SingleNode((BasicNode) object);
        else if (object == null)
            return XQValue.empty;
        else if(object instanceof org.w3c.dom.Node) {
             try {
                Node node = DOMToPushStream.convertNode((org.w3c.dom.Node) object);
                return new SingleNode((BasicNode) node);
            }
            catch (DataModelException e) {
                throw new XQTypeException(e.getErrorCode(), e.getMessage());
            }
        }
        return invalidCast(this);
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        Node node = expr.evalAsNode(focus, context);
        try {
            return new PushStreamToDOM().exportNode(node);
        }
        catch (DataModelException e) {
            throw BasicNode.wrapDMException(e);
        }
    }

    // TODO: convert arrays
}
