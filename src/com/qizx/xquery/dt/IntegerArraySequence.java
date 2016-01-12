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
import com.qizx.xquery.XQValue;

/**
 * A sequence that stores Integer items in an array.
 */
public class IntegerArraySequence extends GenericValue
{
    protected long[] items;

    protected int size;

    protected int index = -1;

    public IntegerArraySequence(long[] items, int size, XQItemType itemType)
    {
        init(items, size);
        this.itemType = itemType;
    }

    public IntegerArraySequence(long[] items, int size)
    {
        this(items, size, XQType.INTEGER);
    }

    public IntegerArraySequence(int[] items, int size)
    {
        long[] ritems = new long[size];
        for (int i = size; --i >= 0;)
            ritems[i] = items[i];
        init(ritems, size);
        itemType = XQType.INT;
    }

    public IntegerArraySequence(short[] items, int size)
    {
        long[] ritems = new long[size];
        for (int i = size; --i >= 0;)
            ritems[i] = items[i];
        init(ritems, size);
        itemType = XQType.SHORT;
    }

    public IntegerArraySequence(byte[] items, int size)
    {
        long[] ritems = new long[size];
        for (int i = size; --i >= 0;)
            ritems[i] = items[i];
        init(ritems, size);
        itemType = XQType.BYTE;
    }

    public IntegerArraySequence(char[] items, int size)
    {
        long[] ritems = new long[size];
        for (int i = size; --i >= 0;)
            ritems[i] = items[i];
        init(ritems, size);
    }

    private void init(long[] items, int size)
    {
        this.items = items;
        this.size = size;
        itemType = XQType.INTEGER;
    }

    public boolean next()
        throws EvaluationException
    {
        if (++index >= size)
            return false;
        item = new SingleInteger(items[index], itemType); // Hmmm
        return true;
    }

    public XQItem quickIndex(long index)
    {
        return index <= 0 || index > size
            ? null : new SingleInteger(items[(int) index - 1], itemType);
    }

    public XQValue bornAgain()
    {
        return new IntegerArraySequence(items, size, itemType);
    }

    static long[] unroll(XQValue value)
        throws EvaluationException
    {
        long[] items = new long[8];
        int ptr = 1; // items[0] stores the length
        for (; value.next();) {
            if (ptr >= items.length) {
                long[] old = items;
                items = new long[old.length * 2];
                System.arraycopy(old, 0, items, 0, old.length);
            }
            items[ptr++] = value.getInteger();
        }
        items[0] = ptr - 1;
        return items;
    }

    /**
     * Converts a Value representing a sequence of integers into an integer
     * array.
     */
    public static long[] expandIntegers(XQValue value)
        throws EvaluationException
    {
        long[] items = unroll(value);
        int len = (int) items[0];
        long[] ritems = new long[len];
        System.arraycopy(items, 1, ritems, 0, len);
        return ritems;
    }

    /**
     * Converts a Value representing a sequence of ints into an int array.
     */
    public static int[] expandInts(XQValue value)
        throws EvaluationException
    {
        long[] items = unroll(value);
        int len = (int) items[0];
        int[] ritems = new int[len];
        for (int i = 0; i < len; i++)
            ritems[i] = (int) items[i + 1];
        return ritems;
    }

    /**
     * Converts a Value representing a sequence of shorts into an short array.
     */
    public static short[] expandShorts(XQValue value)
        throws EvaluationException
    {
        long[] items = unroll(value);
        int len = (int) items[0];
        short[] ritems = new short[len];
        for (int i = 0; i < len; i++)
            ritems[i] = (short) items[i + 1];
        return ritems;
    }

    /**
     * Converts a Value representing a sequence of bytes into an byte array.
     */
    public static byte[] expandBytes(XQValue value)
        throws EvaluationException
    {
        long[] items = unroll(value);
        int len = (int) items[0];
        byte[] ritems = new byte[len];
        for (int i = 0; i < len; i++)
            ritems[i] = (byte) items[i + 1];
        return ritems;
    }

    /**
     * Converts a Value representing a sequence of chars into an char array.
     */
    public static char[] expandChars(XQValue value)
        throws EvaluationException
    {
        long[] items = unroll(value);
        int len = (int) items[0];
        char[] ritems = new char[len];
        for (int i = 0; i < len; i++)
            ritems[i] = (char) items[i + 1];
        return ritems;
    }
}
