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
package com.qizx.xquery.impl;

import com.qizx.xquery.ComparisonContext;

import java.text.Collator;

public class BasicCompContext
    implements ComparisonContext
{
    private Collator collator;
    private int implicitTZ;

    public BasicCompContext(Collator collator, int implicitTZ)
    {
        this.collator = collator;
        this.implicitTZ = implicitTZ;
    }

    public Collator getCollator()
    {
        return collator;
    }

    public int getImplicitTimezone()
    {
        return implicitTZ;
    }

    public boolean emptyGreatest()
    {
        return false; // TODO
    }
}
