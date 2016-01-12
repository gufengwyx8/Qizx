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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.SequenceType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;


public class InsertExpr extends UpdatingExpr
{
    public static final int INTO = 1;
    public static final int FIRST = 2;
    public static final int LAST = 3;
    public static final int BEFORE = 4;
    public static final int AFTER = 5;
    
    static final SequenceType[] INTO_TYPES = { XQType.ELEMENT.one, XQType.DOCUMENT.one};
    static final SequenceType[] SIBLING_TYPES = { XQType.NODE.one };
    static final SequenceType[] INSERTABLE_TYPES = { XQType.NODE.star };
    static final SequenceType[] ATTR_TYPE = { XQType.ATTRIBUTE.one };
   
    public Expression staticCheck(ModuleContext context, int flags)
    {
        Expression checked = super.staticCheck(context, flags);
        
        if(mode == BEFORE || mode == AFTER) {
            context.staticTyping(where, SIBLING_TYPES, "XUTY0005", "target node");
            context.staticTypingExclude(where, ATTR_TYPE, "XUTY0006", "inserted node(s)");
            context.staticTyping(what, INSERTABLE_TYPES, "XUTY0005", "inserted node(s)");
        }
        else {
            context.staticTyping(where, INTO_TYPES, "XUTY0006", "target node");
            context.staticTyping(what, INSERTABLE_TYPES, "XUTY0005", "inserted node(s)");            
        }
        return checked;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        // all is done here:
        try {
            context.haveUpdateList().insertNodes(this, focus, context);
        }
        catch (DataModelException e) {
            context.error(e.getErrorCode(), this, e.getMessage());
        }        
        return XQValue.empty;
    }
}
