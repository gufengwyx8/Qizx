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

import com.qizx.api.QName;
import com.qizx.util.LikePattern;
import com.qizx.util.StringPattern;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.fn.Prototype;


/**
 *  Implementation of function x:ulike (similar to x:like but
 *  with Unix/glob style patterns).
 */
public class XfnUlike extends XfnLike
{
    static QName qfname = IQName.get(EXTENSION_NS, "ulike");
    static Prototype[] protos = { 
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("pattern", XQType.STRING)
            .arg("context", XQType.NODE.star),
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("pattern", XQType.STRING)
    };

    public Prototype[] getProtos() { return protos; }

    public static class Exec extends XfnLike.Exec
    {
        protected StringPattern preparePattern( String pattern, EvalContext context)
        {
            if(context == previousContext && pattern.equals(previous))
                return previousPattern;
            previousPattern = new LikePattern(pattern);
            previous = pattern;
            previousContext = context;
            return previousPattern;
        }
    }
}
