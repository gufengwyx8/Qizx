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
import com.qizx.xquery.*;


public class ReplaceExpr extends UpdatingExpr
{
    public static final int VALUE = 1;
    private static final SequenceType[] TARGET_TYPES = { XQType.NODE.one };
    private static final SequenceType[] REPL_TYPES = { XQType.NODE.star };
    private static final SequenceType[] VREPL_TYPES =
        { XQType.NODE.star, XQType.ITEM.star };
    
    public Expression staticCheck(ModuleContext context, int flags)
    {
        Expression checked = super.staticCheck(context, flags);
        
        context.staticTyping(where, TARGET_TYPES, "XUTY0008", "target node");
        if(mode == VALUE)
            context.staticTyping(what, VREPL_TYPES, "XUTY0010", "value");
        else {
            context.staticTyping(what, REPL_TYPES, "XUTY0010", "replacement node(s)");
            XQType tgType = where.getType();
            XQType repType = what.getType();
            if (XQType.ELEMENT.one.acceptsStatic(tgType) && 
                   XQType.ATTRIBUTE.one.acceptsStatic(repType))
                context.error("XUTY0010", this, "incompatible type " + repType
                              + " for replacing element");
            if (XQType.ATTRIBUTE.one.acceptsStatic(tgType) &&
                   XQType.ELEMENT.one.acceptsStatic(repType))
                 context.error("XUTY0011", this, "incompatible type " + repType
                               + " for replacing attribute");
        }
        
        return checked;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        // all is done here:
        try {
            context.haveUpdateList().replaceNodes(this, focus, context);
        }
        catch (DataModelException e) {
            context.error(e.getErrorCode(), this, e.getMessage());
        }        
        return XQValue.empty;
    }
}
