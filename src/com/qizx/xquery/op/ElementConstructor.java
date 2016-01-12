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
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.BasicStaticContext;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;

import java.util.ArrayList;

/**
 * class ElementConstructor:
 */
public class ElementConstructor extends NamedConstructor
{
    public ArrayList attributes = new ArrayList();

    public ElementConstructor(Expression name)
    {
        super(name);
    }

    public void addAttribute(AttributeConstructor attribute)
    {
        attributes.add(attribute);
    }

    public void putCharacters(char ch[], int start, int length)
    {
        addTextItem(new String(ch, start, length));
    }

    public AttributeConstructor getAttribute(int rank)
    {
        return rank < 0 || rank >= attributes.size()
            ? null : (AttributeConstructor) attributes.get(rank);
    }

    public Expression child(int rank)
    {
        if(rank == 0)
            return name;
        rank -= 1;
        if(rank < attributes.size())
            return getAttribute(rank);
        rank -= attributes.size();
        return (rank < contents.length) ? contents[rank] : null;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.child(name);
        for (int i = 0, size = attributes.size(); i < size; i++)
            d.child(getAttribute(i));
        d.children(contents);
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        super.staticCheck(context, flags);
        for (int e = 0, E = attributes.size(); e < E; e++) {
            Expression attr = context.staticCheck(getAttribute(e), 0);
            attributes.set(e, attr);
            if(attr instanceof PathExpr)
                ((PathExpr) attr).tryToTrim();
        }
        for (int c = 0; c < contents.length; c++) 
            if(contents[c] instanceof PathExpr)
                ((PathExpr) contents[c]).tryToTrim();

        type = XQType.ELEMENT;
        return this;
    }

    public int getFlags()
    { // single Node: necessarily at same_depth!
        return DOCUMENT_ORDER + SAME_DEPTH + flags;
    }
    
    public BasicNode evalAsNode(Focus focus, EvalContext context)
        throws EvaluationException
    {
        BasicStaticContext staticContext = context.getStaticContext();
        // we arrive here if building nodes is really wanted
        CorePushBuilder builder =
            new CorePushBuilder(staticContext.getBaseURI(), 
                                 staticContext.getInScopeNS());
        // for dynamic qname evaluation: make inscope ns visible
        context.setInScopeNS(builder.getNamespaceContext());
        evalAsEvents(builder, focus, context);
        return builder.harvest();
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        context.at(this);
        QName qname = evalName(output, focus, context);

        try {
            output.putElementStart(qname);

            for (int a = 0; a < attributes.size(); a++) {
                try {
                    getAttribute(a).evalAsEvents(output, focus, context);
                }
                catch(EvaluationException e) {
                    if((getFlags() & DIRECT) != 0)
                        e.substituteCode("XQDY0025", "XQST0040");
                    throw e;
                }
            }
            for (int c = 0; c < contents.length; c++) {
                contents[c].evalAsEvents(output, focus, context);
                output.noSpace();
            }
            output.putElementEnd(qname);
        }
        catch (DataModelException e) {
            dmError(context, e);
        }
    }
}
