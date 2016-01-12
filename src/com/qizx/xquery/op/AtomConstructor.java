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
import com.qizx.api.Node;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.CoreDataModel;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xdm.CoreDataModel.CoreNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

/**
 * Implementation of constructors for TEXT, COMMENT.
 */
public class AtomConstructor extends NodeConstructor
{
    public int kind;

    public AtomConstructor(int kind, Expression contents)
    {
        this.kind = kind;
        this.contents =
            (contents == null)
                ? new Expression[0] : new Expression[] { contents };
        type = XQType.TEXT;
        if (kind == Node.COMMENT)
            type = XQType.COMMENT;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("kind", "" + kind);
        d.children(contents);
    }

    public BasicNode evalAsNode(Focus focus, EvalContext context)
        throws EvaluationException
    {
        CoreDataModel dm = new CoreDataModel(null);
        CoreNode node;
        if (kind == BasicNode.TEXT)
            node = dm.newTextNode();
        else if (kind == BasicNode.COMMENT)
            node = dm.newCommentNode("");
        else
            throw new RuntimeException("bad AtomConstructor");

        boolean empty = true;
        for (int c = 0; c < contents.length; c++) {
            XQValue cont = contents[c].eval(focus, context);
            for (; cont.next();) {
                if (!empty)
                    node.addText(" ");
                empty = false;
                node.addText(cont.getString());
            }
        }
        if (empty)
            return null;
        if (kind == BasicNode.COMMENT) {
            try {
                String contents = node.getStringValue();
                if (!XMLUtil.checkComment(contents))
                    context.error("XQDY0072", this, "invalid comment");
            }
            catch (DataModelException e) {
                dmError(context, e);
            }
        }
        return node;
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);

        try {
            if (kind == BasicNode.TEXT) {
                for (int c = 0; c < contents.length; c++) {
                    XQValue cont = contents[c].eval(focus, context);
                    for (; cont.next(); )
                        output.putAtomText(cont.getString());
                }
                return;
            }

            StringBuffer buf = new StringBuffer();
            for (int c = 0; c < contents.length; c++) {
                XQValue cont = contents[c].eval(focus, context);
                for (; cont.next();)
                    buf.append(cont.getString());
            }
            String chunk = buf.toString();
            if (kind == BasicNode.COMMENT)
                output.putComment(chunk);
            else
                throw new RuntimeException("bad AtomConstructor");
        }
        catch (DataModelException e) {
            dmError(context, e);
        }
    }
}
