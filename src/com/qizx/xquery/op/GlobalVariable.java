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
package com.qizx.xquery.op;

import com.qizx.api.QName;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.SequenceType;

/**
 * Declared global variable in a module.
 */
public class GlobalVariable extends Expression
{
    public QName name;

    public Expression init;

    public SequenceType declaredType;

    public GlobalVariable(QName name, SequenceType declaredType, Expression init)
    {
        this.name = name;
        this.declaredType = declaredType;
        if(declaredType != null)
            this.type = declaredType;
        this.init = init;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("name", name);
        d.property("declaredType", declaredType);
        d.child("init", init);
    }

    public Expression child(int rank)
    {
        return (rank == 0) ? init : null;
    }

    public int getFlags()
    {
        return init == null ? 0 : init.getFlags();
    }

    public String toString()
    {
        return "[Global " + name + "]";
    }
}
