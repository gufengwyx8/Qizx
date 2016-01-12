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
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

/**
 * A sequence that stores String items in an array.
 */
public class StringArraySequence extends GenericValue
{
    protected String[] items;
    protected int size;
    protected int index = -1;

    public StringArraySequence(String[] items, int size)
    {
        this.items = items;
        this.size = size;
    }

    public boolean next()
        throws EvaluationException
    {
        if (++index >= size)
            return false;
        item = new SingleString(items[index]); // Hmmm
        return true;
    }

    public XQItem quickIndex(long index)
    {
        return index <= 0 || index > size
                  ? null : new SingleString(items[(int) index - 1]);
    }

    public XQValue bornAgain()
    {
        return new StringArraySequence(items, size);
    }

    /**
     * Converts a Value representing a sequence of strings into an array.
     * @param enforceTypeCheck 
     */
    public static String[] expand(XQValue value, boolean enforceTypeCheck)
        throws EvaluationException
    {
        String[] items = new String[8];
        int ptr = 0;
        for (; value.next();) {
            if (ptr >= items.length) {
                String[] old = items;
                items = new String[old.length * 2];
                System.arraycopy(old, 0, items, 0, old.length);
            }
            if(enforceTypeCheck) {                
                int itemType = value.getItemType().quickCode();
                if(XQType.QT_STRING != itemType && itemType != XQType.QT_UNTYPED)
                    throw new XQTypeException("expecting string");
            }
            items[ptr++] = value.getString();
        }
        // return a full array: TODO OPTIM
        String[] ritems = new String[ptr];
        System.arraycopy(items, 0, ritems, 0, ptr);
        return ritems;
    }
}
