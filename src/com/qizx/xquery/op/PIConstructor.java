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
import com.qizx.api.QName;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.CoreDataModel;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xdm.CoreDataModel.CoreNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;

/**
 * Computed or direct PI constructor.
 */
public class PIConstructor extends NamedConstructor
{
    private static final String URI_NOT_EMPTY =
        "URI part of PI target is not empty";

    public PIConstructor(String value) 
    {
        super(null);
        int ws = 0, L = value.length();
        for (; ws < L; ws++)
            if (Character.isWhitespace(value.charAt(ws)))
                break;
        String target = value.substring(0, ws);
        this.name = new StringLiteral(target);
        // Skip WS between target and body
        for (; ws < L; ws++)
            if (!Character.isWhitespace(value.charAt(ws)))
                break;
        this.contents = new Expression[] { 
            new StringLiteral(ws >= L ? "" : value.substring(ws))
        };
        type = XQType.PI;
        setDirect();
    }

    public PIConstructor(Expression target, Expression contents)
    {
        super(target);
        if (contents != null)
            addItem(contents);
    }

    public BasicNode evalAsNode(Focus focus, EvalContext context)
        throws EvaluationException
    {
        QName target = evalName(focus, context);
        if(target == null)
            System.err.println("PIConstructor.evalAsNode : OOPS"); //rub
        CoreNode node =
            new CoreDataModel(null).newPINode(target.getLocalPart());
        String contents = XMLUtil.normalizePI(evalContents(focus, context));
        if (contents == null)
            context.error("XQDY0026", this, "invalid PI contents");
        else
            node.addText(contents);
        return node;
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        QName target = evalName(focus, context);

        String contents = XMLUtil.normalizePI(evalContents(focus, context));
        if (contents == null)
            context.error("XQDY0026", this, "invalid PI contents");
        try {
            output.putProcessingInstruction(target.getLocalPart(), contents);
        }
        catch (DataModelException e) {
            dmError(context, e);
        }
    }

    private QName evalName(Focus focus, EvalContext context)
        throws EvaluationException
    {
        QName target = null;
        try {
            target = evalName(null, focus, context);
            if (!target.hasNoNamespace())
                context.error("XQDY0041", this, URI_NOT_EMPTY);
            if ("xml".equalsIgnoreCase(target.getLocalPart()))
                throw new XQTypeException("XQDY0064",
                                          "illegal PI name: " + target);
        }
        catch (EvaluationException e) {
            e.substituteCode("XQDY0074", "XQDY0041");
            throw e;
        }
        return target;
    }
}
