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
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;


/**
 *  Implementation of function x:in-range:
 *    low-efficiency fallback implementation used in absence of indexing.
 */
public class XfnInRange extends ExtensionFunction
{
    static QName qfname = IQName.get(EXTENSION_NS, "in-range");
    static Prototype[] protos = { 
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("value", XQType.ANY)
            .arg("lower-bound", XQType.ITEM)
            .arg("upper-bound", XQType.ITEM),
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("value", XQType.ANY)
            .arg("lower-bound", XQType.ITEM)
            .arg("upper-bound", XQType.ITEM)
            .arg("lower-included", XQType.BOOLEAN)
            .arg("upper-included", XQType.BOOLEAN)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.BoolCall
    {
        public boolean evalAsBoolean(Focus focus, EvalContext context)
        throws EvaluationException
        {
            XQItem lo = args[1].evalAsItem(focus, context);
            XQItem hi = args[2].evalAsItem(focus, context);
            boolean loInc = true, hiInc = true;
            if(args.length == 5) {
                loInc = args[3].evalAsBoolean(focus, context);
                hiInc = args[4].evalAsBoolean(focus, context);
            }

            XQValue value = args[0].eval(focus, context);
            for(; value.next(); )
            {
                
                int loCmp = lo.compareTo(value, context, XQItem.COMPAR_ORDER);
                if(loCmp > 0 || (loCmp == 0 && !loInc))
                    continue;
                int hiCmp = hi.compareTo(value, context, XQItem.COMPAR_ORDER);
                if(hiCmp > 0 || (hiCmp == 0 && hiInc))
                    return true;
            }
            return false;
        }
    }
}
