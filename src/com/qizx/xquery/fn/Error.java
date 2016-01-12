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

import com.qizx.api.EvaluationException;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleQName;
import com.qizx.xquery.op.SequenceExpr;

/**
 *  Implementation of function fn:error.
 */
public class Error extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("error", XQType.NONE, Exec.class),
        Prototype.fn("error", XQType.NONE, Exec.class)
           .arg("error-name", XQType.QNAME.opt),
        Prototype.fn("error", XQType.NONE, Exec.class)
           .arg("error-name", XQType.QNAME.opt)
           .arg("description", XQType.STRING),
        Prototype.fn("error", XQType.NONE, Exec.class)
           .arg("error-name", XQType.QNAME.opt)
           .arg("description", XQType.STRING)
           .arg("arguments", XQType.ITEM.star)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            com.qizx.api.QName errName =
                IQName.get(NamespaceContext.ERR, "FOER0000");
            String description = "";
            XQValue value = null;
            
            int argc = args.length;
            if (argc > 0) {
                XQItem name = args[0].evalAsOptItem(focus, context);
                if (name != null) {
                    if(!context.sObs()
                          && name.getItemType().quickCode() == XQType.QT_STRING)
                        name = new SingleQName(IQName.get(NamespaceContext.ERR,
                                                          name.getString()));
                    if (name.getType() != XQType.QNAME)
                        context.error("XPTY0004", args[0], "QName expected");
                    errName = name.getQName();
                }
                if (argc >= 2) {
                    description = args[1].evalAsOptString(focus, context);
                }
                if (argc >= 3) {
                    value = args[2].eval(focus, context);
                }
            }
            
            EvaluationException err = new EvaluationException(errName, description);
            err.setValue(value);
            return context.error(this, err);    // throws
        }
        
        public boolean isVacuous() {
            return true;
        }
    }
}
