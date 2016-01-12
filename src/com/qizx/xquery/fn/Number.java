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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.dt.DurationType;
import com.qizx.xquery.dt.MomentType;

/**
 *  Implementation of function fn:number.
 */
public class Number extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("number", XQType.DOUBLE, Exec.class),
        Prototype.fn("number", XQType.DOUBLE, Exec.class)
        .arg("srcval", XQType.ITEM.opt)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.DoubleCall
    {
        public double evalAsDouble(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQItem item = null;
            if (args.length == 0) {
                checkFocus(focus, context);
                item = focus.currentItem();
            }
            else
                item = args[0].evalAsOptItem(focus, context);
            if (item == null)
                return Double.NaN;
            XQItemType type = item.getItemType();
            if (context.sObs() && // otherwise effective conversion
                (type instanceof MomentType || type instanceof DurationType
                 || type == XQType.ANYURI))
                return Double.NaN;
            try {
                return item.getDouble();
            }
            catch (EvaluationException e) {
                return Double.NaN;
            }
        }
    }

}
