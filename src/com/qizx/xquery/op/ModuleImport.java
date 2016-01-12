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

import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.ModuleContext;

/**
 * Declared module import in a module.
 */
public class ModuleImport extends Expression
{
    public ModuleContext imported;

    public ModuleImport(ModuleContext imported)
    {
        this.imported = imported;
    }

    public void dump(ExprDisplay d)
    {
        d.header(this);
        d.property("name", "?");
    }

    public Expression child(int rank)
    {
        return null;
    }

    public String toString()
    {
        return "[Module import]";
    }
}
