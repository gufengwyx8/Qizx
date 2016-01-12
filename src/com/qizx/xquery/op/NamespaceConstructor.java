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
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xdm.CoreDataModel.CoreNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;

/**
 * class NamespaceConstructor: used to remember NS on element constructors.
 */
public class NamespaceConstructor extends AttributeConstructor
{
    public NamespaceConstructor(String prefix, String uri)
    {
        super(null);
        this.prefix = prefix;
        this.value = uri;
    }

    public NamespaceConstructor(Expression prefix, Expression uri)
    {
        super(prefix);
        if (uri != null)
            addItem(uri);
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("name", prefix);
        d.property("uri", value);
    }

    public BasicNode evalAsNode(Focus focus, EvalContext context)
        throws EvaluationException
    {
        CoreDataModel dm =
            new CoreDataModel(context.getStaticContext().getBaseURI());
        if (prefix != null) { // direct constructor
            CoreNode node = dm.newNSNode(prefix);
            node.addText(value);
            return node;
        }
        QName qname = evalName(null, focus, context);
        CoreNode node = dm.newNSNode(qname.getLocalPart());
        evalContents(node, focus, context);
        return node;
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        try {
            if (prefix != null) // direct constructor
                output.putNamespace(prefix, value);
            else {
                QName qname = evalName(null, focus, context);
                output.putNamespace(qname.getLocalPart(), 
                                    evalContents(focus, context));
            }
        }
        catch (DataModelException e) {
            dmError(context, e);
        }
    }
}
