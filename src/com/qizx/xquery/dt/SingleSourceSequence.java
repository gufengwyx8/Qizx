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
import com.qizx.xquery.XQValue;

public abstract class SingleSourceSequence extends GenericValue
{
    protected XQValue source;

    public SingleSourceSequence(XQValue source)
    {
        this.source = source;
    }

    public final XQValue getSource()
    {
        return source;
    }

    public double getFulltextScore(Item item) throws EvaluationException
    {
        if(item == null && isNode())
            item = getNode();
        return source.getFulltextScore(item);
    }
}
