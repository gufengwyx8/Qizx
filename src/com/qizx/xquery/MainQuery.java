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
package com.qizx.xquery;

import com.qizx.api.EvaluationException;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.impl.EmptyException;
import com.qizx.xquery.op.ExitException;
import com.qizx.xquery.op.Expression;

/**
 * A compiled main Query.
 * This is essentially a module plus a main expression.
 */
public class MainQuery extends ModuleContext
{
    public Expression body;

    public MainQuery(BasicStaticContext env)
    {
        super(env);
        moduleNS = NamespaceContext.LOCAL_NS;
    }

    public XQType getType()
    {
        return body.getType();
    }

    public void dump(ExprDisplay d)
    {
        d.header("MainQuery");
        for (int i = 0; i < declarations.size(); i++) {
            d.child((Expression) declarations.get(i));
        }
        d.child("body", body);
        d.end();
    }

    public void staticCheck(ModuleManager mman)
    {
        resetLocals(); // needed for joins
        super.staticCheck(mman); // module
        // save as locals can be used in globals inits:
        int saveMaxLocalSize = maxLocalSize;
        resetLocals(); // twice is better
        if (body != null) {
            body = simpleStaticCheck(body, 0);
            allocateLocalAddress(lastLocal);
            // dumpLocals2("locals after allocation");
        }
        if(saveMaxLocalSize > maxLocalSize)
            maxLocalSize = saveMaxLocalSize;
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        try {
            return body == null ? XQValue.empty : body.eval(focus, context);
        }
        catch (EmptyException e) {
            return XQValue.empty;
        }
        catch (ExitException e) {
            return e.getValue();
        }
    }

    public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                             EvalContext context)
        throws EvaluationException
    {
        try {
            body.evalAsEvents(output, focus, context);
        }
        catch (EmptyException e) {
        }
    }
}
