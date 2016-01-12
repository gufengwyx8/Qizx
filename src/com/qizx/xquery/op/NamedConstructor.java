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
import com.qizx.api.QName;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xdm.XQName;
import com.qizx.xdm.CoreDataModel.CoreNode;
import com.qizx.xquery.*;

/**
 * Superclass of Element, Attribute, PI constructors.
 */
public abstract class NamedConstructor extends NodeConstructor
{
    public Expression name;

    public NamedConstructor(Expression name)
    {
        contents = new Expression[0];
        this.name = name;
    }

    public Expression child(int rank)
    {
        return rank == 0 ? name :
               rank <= contents.length? contents[rank - 1] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child("name", name);
        d.children(contents);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        super.staticCheck(context, flags);
        if (name != null) {
            name = context.staticCheck(name, 0);
        }
        return this;
    }

    public boolean isConstant()
    {
        return name.isConstant() && super.isConstant();
    }

    public Expression addTextItem(String chunk)
    {
        Expression last = null;
        TextLiteral result = null;
        if (contents.length > 0
            && (last = contents[contents.length - 1]) instanceof TextLiteral) {
            result = (TextLiteral) last;
            result.value += chunk;
        }
        else
            addItem(result = new TextLiteral(chunk));
        return result;
    }

    protected QName evalName(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        XQItem nameItem = name.evalAsItem(focus, context);
        return computeName(nameItem, output, context);
    }

    // Returns XQName to support W3C crap in XQUpdate
    private QName computeName(XQItem nameItem, XMLPushStreamBase output,
                               EvalContext context)
        throws EvaluationException
    {
        BasicNode node = null;
        int qtype = nameItem.getItemType().quickCode();
        if (nameItem.isNode()) {
            node = nameItem.basicNode();
            qtype = XQType.QT_UNTYPED;
        }
        context.at(this);
        switch (qtype) {
        case XQType.QT_UNTYPED:
        case XQType.QT_STRING:
            try {
                String pname = nameItem.getString();
                String prefix = IQName.extractPrefix(pname);
                String ncname = IQName.extractLocalName(pname);
                if (prefix.length() == 0)
                    return XQName.get("", ncname, "");
                // The draft is unclear: only static in-scope NS or
                // NS in-scope for the current node? TODO
                QName qname = null;
                String uri = null;
                if (node != null)
                    uri = node.getNamespaceUri(prefix);
                if (uri == null && output != null)
                    uri = output.getNSURI(prefix);
                if (uri != null)
                    qname = XQName.get(uri, ncname, prefix);
                else
                    qname = context.getStaticContext().getInScopeNS().expandName(pname);
                if (qname == null)
                    context.error("XQDY0074", this,
                                  "no namespace found for prefix " + prefix);
                return qname;
            }
            catch (Exception e) {
                context.error("XQDY0074", this,
                              "error converting string to QName: "
                                  + e.getMessage());
                return null;
            }
        case XQType.QT_QNAME:
            return nameItem.getQName();
        default:
            context.badTypeForArg(nameItem.getItemType(), name, 0, 
                                  "QName or string");
            return null;
        }
    }

    
    String evalContents(Focus focus, EvalContext context)
        throws EvaluationException
    {
        StringBuffer buf = new StringBuffer();
        for (int c = 0; c < contents.length; c++) {
            XQValue part = contents[c].eval(focus, context);
            if (part.next())
                buf.append(part.getString());
            for (; part.next();) {
                buf.append(' ');
                buf.append(part.getString());
            }
        }
        return buf.toString();
    }

    protected void evalContents(CoreNode node, Focus focus,
                                EvalContext context)
        throws EvaluationException
    {
        for (int c = 0; c < contents.length; c++) {
            XQValue part = contents[c].eval(focus, context);
            for (boolean first = true; part.next(); first = false) {
                if (!first)
                    node.addText(" ");
                node.addText(part.getString());
            }
        }
    }
}
