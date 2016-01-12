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
import com.qizx.xquery.XQValue;

/**
 *  Implementation of function fn:idref.
 */
public class Idref extends Function {

    static Prototype[] protos = { 
        Prototype.fn("idref", XQType.NODE.star, Exec.class)
            .arg("arg", XQType.STRING.star),
        Prototype.fn("idref", XQType.ELEMENT.star, Exec.class)
            .arg("arg", XQType.STRING.star)
            .arg("node", XQType.NODE)
    };

    public Prototype[] getProtos()
    {
        return protos;
    }


    public static class Exec extends Id.Exec
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return eval(focus, context, true); // in fn:id()
        }
    }
}
