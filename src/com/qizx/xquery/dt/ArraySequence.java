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
import com.qizx.api.Node;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQValue;

import java.util.ArrayList;

/**
 * A sequence that stores Items in an array. 
 * Used for local variables holding a true Item sequence.
 * TODO pb with scores: accumulate in array?
 */
public class ArraySequence extends GenericValue
{
    final static int MAX_BLOCK_SIZE = 65536;

    protected Object[] items; // not Item[] to avoid realloc after FLWR sort
    protected int size;

    protected int index = -1;
    protected ArrayList<Object[]> overflow; // when growing too much, use overflow blocks
    protected XQValue origin;     // expanded sequence

    protected double[] scores;


    public ArraySequence(Object[] items, int size)
    {
        this.items = (items == null) ? null : (Object[]) items.clone();
        this.size = size;
    }

    // constructor without copy: for bornAgain()
    public ArraySequence(int size, Object[] items, XQValue origin)
    {
        this.items = items;
        this.size = size;
        this.origin = origin;
    }

    public ArraySequence(int initialSize, XQValue origin)
    {
        items = new Object[initialSize];
        size = 0;
        this.origin = origin;
    }

    public int append(XQValue sequence)
        throws EvaluationException
    {
        int count = 0;
        for(; sequence.next(); ++count )
            addItem(sequence.getItem());
        return count;
    }
    
    public static ArraySequence copy(XQValue sequence)
        throws EvaluationException
    {
        ArraySequence seq = new ArraySequence(8, sequence);
        seq.append(sequence);
        return seq;
    }    
    

    public XQValue bornAgain()
    {   // TRICK: constructor w/o copy
        ArraySequence seq = new ArraySequence(size, items, origin);
        seq.scores = scores;
        return seq;
    }

    public XQValue getOrigin()
    {
        return origin;
    }

    public void setOrigin(XQValue origin)
    {
        this.origin = origin;
    }

    public int getSize()
    {
        return size;
    }

    public double getFulltextScore(Item item) throws EvaluationException
    {
        if(item == null)
            item = getNode();
        if(scores != null)
            return scores[index];
        if(origin != null)
            return origin.getFulltextScore(item);
        return super.getFulltextScore(item);
    }
    
//    public void getFulltextSelections(java.util.Collection collect)
//    {
//        if(origin != null)
//            origin.getFulltextSelections(collect);
//    }

    public void addItem(Item item)
    {
        if(item == null)
            return; // no crap please
        if (size >= items.length) {
            if (items.length < MAX_BLOCK_SIZE) {
                Object[] old = items;
                items = new Object[old.length * 2];
                System.arraycopy(old, 0, items, 0, old.length);
            }
            else {
                // avoid big blocks: accumulate blocks into a vector,
                // then use pack() to cleanup
                if (overflow == null)
                    overflow = new ArrayList<Object[]>();
                overflow.add(items);
                items = new Object[items.length];
                size = 0;
            }
        }
        items[size++] = item;
    }

    public void addItems(Item[] addedItems, int count)
    {
        // -System.err.println("addItems "+count);
        if (size + count >= items.length) { // OPTIM todo
            for (int i = 0; i < count; i++)
                addItem(addedItems[i]);
        }
        else { // most frequent case: no OVF
            System.arraycopy(addedItems, 0, items, size, count);
            size += count;
        }
    }

    /**
     * if addItem has been used, must be called at the end of the construction.
     */
    public void pack()
    {
        if (overflow == null)
            return;
        int nsize = overflow.size() * items.length + size;
        Object[] nitems = new Object[nsize];
        int ptr = 0;
        for (int b = 0; b < overflow.size(); b++) {
            System.arraycopy(overflow.get(b), 0, nitems, ptr, items.length);
            ptr += items.length;
        }
        System.arraycopy(items, 0, nitems, ptr, size);
        items = nitems;
        size = nsize;
        overflow = null;
        reset();
    }

    public boolean next()
        throws EvaluationException
    {
        // automatic packing:
        if (overflow != null)
            pack();
        // -System.err.println(this+" ArraySEQ.next index "+index+" "+size);
        do {
            if (index >= size - 1)
                return false;
            // here we *know* that we have Items (unlike ObjectArraySequence)
            item = (XQItem) items[++index];
        }
        while(item == null);
        return true;
    }

    public long quickCount(EvalContext context)
        throws EvaluationException
    {
        if (items == null)
            return 0;
        // no need to pack (assumes no duplication)
        int ovfsize = (overflow != null) ? overflow.size() : 0;
        return ovfsize * items.length + size;
    }

    public boolean hasQuickIndex()
    {
        return true;
    }

    public XQItem quickIndex(long index)
    {
        return index <= 0 || index > size
            ? null : (XQItem) items[(int) index - 1];
    }

    public void reset()
    {
        index = -1;
    }

    public boolean worthExpanding()
    {
        return false; // because it is already expanded!
    }

    /**
     * Converts a Value representing a sequence of Nodes into an array.
     */
    public static Node[] expandNodes(XQValue value)
        throws EvaluationException
    {
        Node[] nodes = new Node[8];
        int ptr = 0;
        for (; value.next();) {
            if (ptr >= nodes.length) {
                Node[] old = nodes;
                nodes = new Node[old.length * 2];
                System.arraycopy(old, 0, nodes, 0, old.length);
            }
            nodes[ptr++] = value.getNode();
        }
        // return a full array:
        if(ptr == nodes.length)
            return nodes;
        Node[] rnodes = new Node[ptr];
        System.arraycopy(nodes, 0, rnodes, 0, ptr);
        return rnodes;
    }

    /**
     * Converts a Value representing a sequence of Nodes into an array.
     */
    public static Item[] expand(XQValue value)
        throws EvaluationException
    {
        Item[] items = new Item[8];
        int ptr = 0;
        for (; value.next();) {
            if (ptr >= items.length) {
                Item[] old = items;
                items = new Item[old.length * 2];
                System.arraycopy(old, 0, items, 0, old.length);
            }
            items[ptr++] = value.getItem();
        }
        // return a full array:
        if(ptr == items.length)
            return items;
        Item[] rnodes = new Item[ptr];
        System.arraycopy(items, 0, rnodes, 0, ptr);
        return rnodes;
    }

    public void setScores(double[] scores)
    {
        this.scores = scores;
    }
}
