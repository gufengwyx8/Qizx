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
import com.qizx.xquery.XQValue;

/**
 * A sequence that stores Float/Double items in an array.
 */
public class FloatArraySequence extends GenericValue
{
    protected float[] items;

    protected int size;

    protected int index = -1;

    public FloatArraySequence(float[] items, int size)
    {
        this.items = items;
        this.size = size;
    }

    public boolean next()
        throws EvaluationException
    {
        if (++index >= size)
            return false;
        item = new SingleFloat(items[index]); 
        return true;
    }

    public XQItem quickIndex(long index)
    {
        return index <= 0 || index > size
                                ? null
                                : new SingleFloat(items[(int) index - 1]);
    }

    public XQValue bornAgain()
    {
        return new FloatArraySequence(items, size);
    }

    static float[] unroll(XQValue value)
        throws EvaluationException
    {
        float[] items = new float[8];
        int ptr = 1; // items[0] stores the length
        for (; value.next();) {
            if (ptr >= items.length) {
                float[] old = items;
                items = new float[old.length * 2];
                System.arraycopy(old, 0, items, 0, old.length);
            }
            items[ptr++] = value.getFloat();
        }
        items[0] = ptr - 1;
        return items;
    }

    /**
     * Converts a Value representing a sequence of float into an float array.
     */
    public static float[] expandFloats(XQValue value)
        throws EvaluationException
    {
        float[] items = unroll(value);
        int len = (int) items[0];
        float[] ritems = new float[len];
        for (int i = 0; i < len; i++)
            ritems[i] = (float) items[i + 1];
        return ritems;
    }
}
