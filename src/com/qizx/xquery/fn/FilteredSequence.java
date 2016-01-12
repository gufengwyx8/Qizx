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
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleSourceSequence;

public abstract class FilteredSequence extends SingleSourceSequence
{
    protected long position; // current position
    // caching last(): if last >= 0 , it is already evaluated
    protected long last = -1;

    public FilteredSequence(XQValue source)
    {
        super(source);
    }

    public abstract boolean next()
        throws EvaluationException;

    public long getPosition()
    {
        return position;
    }

    public long getLast()
        throws EvaluationException
    {
        if (last < 0) {
            XQValue sba = source.bornAgain();
            for (last = 0; sba.next();)
                ++last;
        }
        return last;
    }
}
