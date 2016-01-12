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
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;

public class DocumentConstructor extends NodeConstructor
{
    public DocumentConstructor(Expression expr)
    {
        contents = new Expression[] { expr };
        type = XQType.DOCUMENT;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.children(contents);
    }

    public BasicNode evalAsNode(Focus focus, EvalContext context)
        throws EvaluationException
    {
        CorePushBuilder builder =
            new CorePushBuilder(context.getStaticContext().getBaseURI());
        evalAsEvents(builder, focus, context);
        return builder.harvest();
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        try {
            boolean ok = output.putDocumentStart();
            for (int c = 0; c < contents.length; c++) {
                contents[c].evalAsEvents(output, focus, context);
            }
            if (ok)
                output.putDocumentEnd();
            else
                output.noSpace(); // boundary
        }
        catch (DataModelException e) {
            dmError(context, e);
        }
    }
}
