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
package com.qizx.xquery.fn;
import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;

/**
 *  Implementation of function fn:lang.
 */
public class Lang extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("lang", XQType.BOOLEAN, Exec.class)
            .arg("testlang", XQType.STRING),
        Prototype.fn("lang", XQType.BOOLEAN, Exec.class)
            .arg("testlang", XQType.STRING)
            .arg("node", XQType.NODE)
    };
    public Prototype[] getProtos() { return protos; }

    private static final QName LANG = IQName.get(NamespaceContext.XML, "lang");
    
    public static class Exec extends Function.BoolCall
    {
        public boolean evalAsBoolean(Focus focus, EvalContext context)
        throws EvaluationException
        {
            context.at(this);
            String lang = args[0].evalAsOptString(focus, context);
            if(lang == null)
                lang = "";
            
            Node node = null;
            if (args.length < 2) {
                checkFocus(focus, context);
                XQItem dot = focus.currentItem();
                if (dot == null || !dot.isNode())
                    context.error("FOTY0011", this,
                                  "current item should be a Node");
                node = dot.getNode();
            }
            else {
                node = args[1].evalAsOptNode(focus, context);
            }

            try {
                for (; node != null; node = node.getParent()) {
                    Node attr = node.getAttribute(LANG);
                    if (attr == null)
                        continue;
                    String value = attr.getStringValue();
                    for (int i = 0; i < lang.length(); i++)
                        if (i >= value.length()
                            || Character.toLowerCase(lang.charAt(i)) != Character.toLowerCase(value.charAt(i)))
                            return false;
                    return true;
                }
            }
            catch (DataModelException e) {
                dmError(context, e);
            }
            return false;
        }
    }
}
