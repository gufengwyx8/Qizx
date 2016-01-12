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
import com.qizx.util.basic.FileUtil;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleString;
import com.qizx.xquery.dt.StringValue;

import java.net.URI;

/**
 *  Implementation of function fn:resolve-uri.
 */
public class ResolveUri extends Function
{
    static Prototype[] protos = { 
        Prototype.fn("resolve-uri", XQType.ANYURI.opt, Exec.class)
        .arg("relative", XQType.STRING),
        Prototype.fn("resolve-uri", XQType.ANYURI.opt, Exec.class)
            .arg("relative", XQType.STRING)
            .arg("base", XQType.ANYURI)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            String relative = args[0].evalAsOptString(focus, context);
            if (relative == null)
                return XQValue.empty;
            URI relUri = FileUtil.uriConvert(relative);
            if (relUri == null)
                context.error("FORG0002", this, "invalid URI: " + relative);

            String base = null;
            if (args.length > 1) {
                XQItem baseUri = args[1].evalAsOptItem(focus, context);
                if (baseUri != null)
                    if (!(baseUri instanceof StringValue))
                        context.invalidArgType(args[1], 1, baseUri.getType(),
                                               "anyURI");
                    else
                        base = baseUri.getString();
                if (base != null && base.length() == 0) {
                    base = context.getStaticContext().getBaseURI();
                }
            }
            else
                base = context.getStaticContext().getBaseURI();
            if (base == null) {
                if (!relUri.isAbsolute())
                    context.error("FONS0005", this, "undefined base URI");
                else
                    return new SingleString(relative, XQType.ANYURI);
            }
            else if (FileUtil.uriConvert(base) == null)
                context.error("FORG0002", this, "invalid base URI: " + base);
            try {
                URI result = new URI(base).resolve(relative);
                return new SingleString(result.toString(), XQType.ANYURI);
            }
            catch (IllegalArgumentException also) {
                // for some strange reason this can also be thrown
                context.error("FORG0002", this, also.getMessage());
            }
            catch (java.net.URISyntaxException e) {
                context.error("FORG0002", this, e.getMessage());
            }
            return null; // dummy
        }
    }
}
