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

import com.qizx.api.DataModelException;
import com.qizx.api.QName;
import com.qizx.xquery.op.Expression;

/**
 * Abstract expression displayer. Each expression node is
 */
public abstract class ExprDisplay
{
    protected boolean pretty;

    public boolean isPretty()
    {
        return pretty;
    }

    public abstract void header(QName name);

    public abstract void header(String name);

    public abstract void header(Expression expr);
    
    public abstract void headerInfo(Expression expr);

    // ------- properties of an expression --------------------------------
    
    public abstract void property(String prop, String value);

    public void property(String prop, double value)
    {
        property(prop, (value == (long) value)? Long.toString((long) value)
                       : Double.toString(value));
    }

    /**
     * Puts a property whose value is a QName.
     */
    public void property(String prop, QName name)
    {
        property(prop, name.toString());
    }

    /**
     * Puts a property whose value is a Type.
     */
    public abstract void property(String prop, XQType targetType);

    // Child expression
    public abstract void child(String role, Expression expr);

    public void child(Expression expr)
    {
        if(expr != null) {
            expr.dump(this);
            end();
        }
    }

    public abstract void child(String role, String value);
    
    public void children(Expression[] args)
    {
        for (int i = 0; i < args.length; i++) {
            if(args[i] != null) {
                args[i].dump(this);
                end();
            }
        }
    }

    public abstract void children(String role, Expression[] args);

    /**
     * Shortcut for binary expressions.
     */
    public void binary(Expression op, Expression arg1, Expression arg2)
    {
        header(op);
        child(arg1);
        child(arg2);
    }

    /**
     * Shortcut for N-ary expressions.
     */
    public void multi(Expression op, Expression[] operands)
    {
        header(op);
        children(operands);
    }

    /**
     * End of a node (called automatically)
     */
    public abstract void end();

    /**
     * can be used to terminate
     */
    public abstract void flush()
        throws DataModelException;
}
