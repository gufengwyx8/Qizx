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
import com.qizx.xquery.XQType;

/**
 *  Implementation of function fn:substring.
 */
public class Substring extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("substring", XQType.STRING.opt, Exec.class)
            .arg("sourceString", XQType.STRING.opt)
            .arg("startingLoc", XQType.DOUBLE),
        Prototype.fn("substring", XQType.STRING.opt, Exec.class)
            .arg("sourceString", XQType.STRING.opt)
            .arg("startingLoc", XQType.DOUBLE)
            .arg("length", XQType.DOUBLE)
    };
    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.OptStringCall
    {
        public String evalAsOptString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            String s = args[0].evalAsOptString(focus, context);
            if (s == null)
                return ""; // NOV2003 // return null
            double dstart = args[1].evalAsDouble(focus, context) - 1;
            if (Double.isNaN(dstart))
                return "";
            double length = s.length();
            if (args.length == 3) {
                length = args[2].evalAsDouble(focus, context);
                // The specs are pure rubbish!
                if (Double.isNaN(length) || Double.isNaN(dstart + length))
                    return "";
                if (Double.POSITIVE_INFINITY == length)
                    length = Integer.MAX_VALUE;
            }
            int start = (int) Math.round(dstart);
            int end = start + (int) Math.round(length);
            end = Math.max(0, Math.min(s.length(), end));
            start = Math.max(0, Math.min(end, start));
            return s.substring(start, end);
        }
    }
}
