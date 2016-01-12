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
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * A sequence that stores Object items in an array.
 * Converts items on the fly to required type.
 */
public class ObjectArraySequence extends GenericValue
{
    protected Object[] items;
    protected int size;
    protected int index = -1;

    public ObjectArraySequence(Object[] items, int size, XQItemType itemType)
    {
        this.items = items;
        this.size = size;
        this.itemType = itemType;
    }

    public ObjectArraySequence(Collection collec, XQItemType itemType)
    {
        this(collec.iterator(), itemType);
    }

    public ObjectArraySequence(Iterator iterator, XQItemType itemType)
    {
        Object[] items = new Object[4];
        int ptr = 0;
        for (; iterator.hasNext();) {
            if (ptr >= items.length) {
                Object[] old = items;
                items = new Object[old.length + 1 + old.length / 2];
                System.arraycopy(old, 0, items, 0, old.length);
            }
            items[ptr++] = iterator.next();
        }
        this.items = items;
        this.size = ptr;
        this.itemType = itemType;
    }

    public ObjectArraySequence(Enumeration enu, XQItemType itemType)
    {
        Object[] items = new Object[4];
        int ptr = 0;
        for (; enu.hasMoreElements();) {
            if (ptr >= items.length) {
                Object[] old = items;
                items = new Object[old.length * 2];
                System.arraycopy(old, 0, items, 0, old.length);
            }
            items[ptr++] = enu.nextElement();
        }
        this.items = items;
        this.size = ptr;
        this.itemType = itemType;
    }

    public boolean next()
        throws EvaluationException
    {
        do {
            if (++index >= size)
                return false;
           // onthefly conversion:
            item = JavaMapping.convertToItem(items[index], itemType);
        }
        while(item == null);
        return true;
    }

    public XQValue bornAgain()
    {
        return new ObjectArraySequence(items, size, itemType);
    }

    private static Object[] newArray(Class itemClass, int size)
    {
        return itemClass == null ? new Object[size]
                : (Object[]) Array.newInstance(itemClass, size);
    }

    /**
     * Converts a Value representing a sequence of objects into an array.
     */
    public static Object[] expand(XQValue value, Class itemClass)
        throws EvaluationException
    {
        Object[] items = newArray(itemClass, 8);
        int ptr = 0;
        for (; value.next();) {
            if (ptr >= items.length) {
                Object[] old = items;
                items = newArray(itemClass, old.length * 2);
                System.arraycopy(old, 0, items, 0, old.length);
            }
            XQItemType typ = value.getItemType();
            Object item = null;
            if (typ == XQType.STRING)
                item = value.getString();
            else if (typ instanceof WrappedObjectType)
                item = ((WrappedObjectValue) value.getItem()).getObject();
            else
                item = value.getItem();
            try {
                items[ptr++] = item;
            }
            catch (ArrayStoreException e) { // type checking too loose
                throw new XQTypeException("bad item type: " + typ
                                          + ", in Java array[" + itemClass
                                          + "]");
            }
        }
        // return a full array: TODO OPTIM
        if (ptr == items.length)
            return items;
        Object[] ritems = newArray(itemClass, ptr);
        System.arraycopy(items, 0, ritems, 0, ptr);
        return ritems;
    }
}
