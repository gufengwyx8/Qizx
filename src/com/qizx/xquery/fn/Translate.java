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
import com.qizx.util.basic.XMLUtil;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;

/**
 *  Implementation of function fn:translate.
 */
public class Translate extends Function
{    
    static Prototype[] protos = { 
        Prototype.fn("translate", XQType.STRING.opt, Exec.class)
        .arg("srcval", XQType.STRING.opt)
        .arg("mapString", XQType.STRING.opt)
        .arg("transString", XQType.STRING.opt)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    private static String translate(String src, String map, String trans)
        throws DataModelException
    {
        // simple optim: detect min and max of mapped chars
        int min = 0xffff, max = 0;
        for (int i = map.length(); --i >= 0;) {
            char c = map.charAt(i);
            if (c < min)
                min = c;
            if (c > max)
                max = c;
            if (XMLUtil.isSurrogateChar(c))
                throw new DataModelException(
                                "translation of surrogate pairs not supported");
            // surrogate pairs in the source string dont harm: they are left
            // untouched
        }
        int transLen = trans.length();

        StringBuffer buf = new StringBuffer();
        for (int i = 0, L = src.length(), index; i < L; i++) {
            char c = src.charAt(i);
            if (c >= min && c <= max && (index = map.indexOf(c)) >= 0)
                if (index >= transLen)
                    continue;
                else
                    c = trans.charAt(index);
            buf.append(c);
        }
        return buf.toString();
    }


    public static class Exec extends Function.OptStringCall
    {
        public String evalAsOptString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            String src = args[0].evalAsOptString(focus, context);
            String map = args[1].evalAsString(focus, context);
            String trans = args[2].evalAsString(focus, context);
            if (src == null)
                return "";
            try {
                return translate(src, map, trans);
            }
            catch (DataModelException e) {
                context.error("FOCH0002", this, e.getMessage());
                return null;
            }
        }
    }
}
