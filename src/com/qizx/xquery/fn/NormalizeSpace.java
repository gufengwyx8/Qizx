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
 *  Implementation of function fn:normalize-space.
 */
public class NormalizeSpace extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("normalize-space", XQType.STRING.opt, Exec.class),
        Prototype.fn("normalize-space", XQType.STRING.opt, Exec.class)
            .arg("srcval", XQType.STRING.opt)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    static String normalize(String src)
    {
        StringBuffer buf = new StringBuffer();
        boolean pendingSpace = false;

        for (int i = 0, L = src.length(); i < L; i++) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) {
                if (buf.length() > 0)
                    pendingSpace = true;
            }
            else {
                if (pendingSpace)
                    buf.append(' ');
                buf.append(c);
                pendingSpace = false;
            }
        }
        return buf.toString();
    }


    public static class Exec extends Function.OptStringCall
    {
        public String evalAsOptString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            String src = null;
            if (args.length == 0) {
                checkFocus(focus, context);
                src = focus.currentItem().getString();
            }
            else
                src = args[0].evalAsOptString(focus, context);
            context.at(this);
            return src == null ? "" : normalize(src);
        }
    }
}
