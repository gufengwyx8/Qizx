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
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.CoreDataModel;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xdm.CoreDataModel.CoreNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;

/**
 * Implementation of attribute constructors.
 */
public class AttributeConstructor extends NamedConstructor
{
    static final QName XMLNS = IQName.get("xmlns");

    // temporary for NS resolution:
    public String value, prefix;

    public AttributeConstructor(Expression name)
    {
        super(name);
        type = XQType.ATTRIBUTE;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("name", name);
        d.children(contents);
    }

    public BasicNode evalAsNode(Focus focus, EvalContext context)
        throws EvaluationException
    {
        QName qname = evalName(null, focus, context);
        if (qname == XMLNS)
            context.error("XQDY0044", this,
                          "invalid name 'xmlns' for attribute");
        CoreDataModel dm =
            new CoreDataModel(context.getStaticContext().getBaseURI());
        CoreNode node = dm.newAttribute(qname);
        evalContents(node, focus, context);
        return node;
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);

        QName qname = evalName(output, focus, context);
        if (qname == XMLNS)
            context.error("XQDY0044", this,
                          "invalid name 'xmlns' for attribute");
        try {
            output.putAttribute(qname, evalContents(focus, context), null);
        }
        catch (DataModelException e) {
            dmError(context, e);
        }
    }
}
