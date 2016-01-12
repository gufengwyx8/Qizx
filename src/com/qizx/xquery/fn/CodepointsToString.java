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
import com.qizx.util.basic.XMLUtil;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

/**
 * Implementation of function fn:codepoints-to-string.
 */
public class CodepointsToString extends Function
{

    static Prototype[] protos = {
            Prototype.fn("codepoints-to-string", XQType.STRING, Exec.class)
                .arg("srcval", XQType.INTEGER.star)
        };

    public Prototype[] getProtos()
    {
        return protos;
    }

    public static class Exec extends Function.StringCall
    {
        public String evalAsString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            XQValue cpts = args[0].eval(focus, context);
            StringBuffer buf = new StringBuffer();
            for (int rank = 1; cpts.next(); ++rank) {
                if(cpts.getItemType().quickCode() != XQType.QT_INT)
                    context.badTypeForArg(cpts.getItemType(), this, rank,
                                          "xs:integer");
                int code = (int) cpts.getInteger();
                if (XMLUtil.isXMLChar(code))
                    buf.append((char) code);
                else if (XMLUtil.isSupplementalChar(code)) {
                    buf.append(XMLUtil.highSurrogate(code));
                    buf.append(XMLUtil.lowSurrogate(code));
                }
                else
                    context.error("FOCH0001", args[0]);

            }
            return buf.toString();
        }
    }
}
