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

import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQValue;

/**
 * Implementation of a single item of foreign type.
 */
public class SingleWrappedObject extends WrappedObjectValue
{
    protected Object value;

    private boolean started = false;

    public SingleWrappedObject(Object wrapped)
    {
        this.value = wrapped;
    }

    public SingleWrappedObject(Object wrapped, XQItemType itemType)
    {
        this.value = wrapped;
        this.itemType = itemType;
    }

    public boolean next()
    {
        return started ? false : (started = true);
    }

    public XQValue bornAgain()
    {
        return new SingleWrappedObject(value, itemType);
    }

    public Object getObject()
    {
        return value;
    }
}
