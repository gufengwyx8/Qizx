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
package com.qizx.xquery.ext;

import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.api.QizxException;
import com.qizx.xdm.IQName;
import com.qizx.xquery.DynamicContext;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExpressionImpl;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;

/**
 *  Parses and evaluates a XQuery expression.
 *  The expression is parsed in the global context of the current query
 *  or module, without access to local variables.
 */
public class XfnEval extends ExtensionFunction
{
    static QName qfname = IQName.get(EXTENSION_NS, "eval");
    static Prototype[] protos = { 
        new Prototype(qfname, XQType.NODE.star, Exec.class)
                      .arg("expression", XQType.STRING)
    };
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {    
        public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
        {    
            context.at(this);
            String src = args[0].evalAsString(focus, context);
            src = src.replace('\r', ' ');
            ExpressionImpl expr = null;
            try {
                DynamicContext dynamicContext = context.dynamicContext();
                expr = dynamicContext.compileExpression(src);
                if(focus != null)
                    expr.setCurrentItem(focus.currentItem());
                
                // initialize globals of the expression from context globals:
                dynamicContext.passVariablesTo(expr);
                
                return expr.rawEval();
            }
            catch (QizxException e) {
                return context.error(e.getErrorCode(), this, e.getMessage());
            }
            finally {
                // retrieve possible compilation messages
                //context.messages(expr.getMessages());
            }
        }
    }
} 
