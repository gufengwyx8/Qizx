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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.SequenceType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;


public class DeleteExpr extends UpdatingExpr
{    
    private static final SequenceType[] DEL_TYPES = { XQType.NODE.one };

    public Expression staticCheck(ModuleContext context, int flags)
    {
        Expression checked = super.staticCheck(context, flags);
        context.staticTyping(where, DEL_TYPES, "XUTY0007", "target node");
        return checked;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        // all is done here:
        context.haveUpdateList().deleteNodes(this, focus, context);
        
        return XQValue.empty;
    }
}
