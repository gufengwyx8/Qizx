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
package com.qizx.xquery.dt;

import com.qizx.api.EvaluationException;
import com.qizx.api.Item;
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.XQValue;

public abstract class DualNodeSequence extends GenericValue
{
    protected XQValue s1, s2;
    protected BasicNode n1, n2; // current nodes

    protected void init(XQValue s1, XQValue s2)
        throws EvaluationException
    {
        this.s1 = s1;
        this.s2 = s2;
        n1 = s1.next() ? s1.basicNode() : null;
        n2 = s2.next() ? s2.basicNode() : null;
    }

    public double getFulltextScore(Item item)
        throws EvaluationException
    {
        if(item == null)
            item = getNode();
        double d = s1.getFulltextScore(item);
        if(d >= 0)
            return d;
        return s2.getFulltextScore(item);
    }
}
