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
package com.qizx.util.basic;

import com.qizx.util.io.ByteInput;
import com.qizx.util.io.ByteOutput;

import java.io.IOException;

/**
 * A set of integer values.
 * Implemented as an array of bits.
 */
public class IntSet implements java.io.Serializable
{
    // use bytes as cell:
    private static final int SHIFT = 5;
    private static final int USIZE = 1 << SHIFT;  // unit size in bits
    private static final int MASK = USIZE - 1;    // on lower bits of element
    
    // loBound is the smallest value represented by bits
    private int/*LId*/   loBound; // multiple of USIZE (aligned)
    private int/*LId*/   hiBound; // multiple of USIZE (aligned)
    private int[] bits;
    
    //TODO manage several bit groups for very sparse sets

    /**
     * Builds an empty set.
     */
    public IntSet()
    {
    }

    /**
     * Builds a set containing a single element.
     */
    public IntSet(int/*LId*/ firstElement)
    {
        add(firstElement);
    }
    
    public IntSet(int/*LId*/ firstElement, int/*LId*/ lastELement)
    {
        add(firstElement, lastELement);
    }

    // number of set bits in a hex digit
    static final int[] HEXCOUNT = {
        0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4
    };

    public int size()
    {
        if (bits == null || hiBound == loBound)
            return 0;
        int sz = 0;
        for (int c = bits.length; --c >= 0;) {
            int b = bits[c];
            sz += HEXCOUNT[b & 0xf] + HEXCOUNT[(b >> 4) & 0xf];
            sz += HEXCOUNT[(b >> 8) & 0xf] + HEXCOUNT[(b >> 12) & 0xf];
            sz += HEXCOUNT[(b >> 16) & 0xf] + HEXCOUNT[(b >> 20) & 0xf];
            sz += HEXCOUNT[(b >> 24) & 0xf] + HEXCOUNT[(b >> 28) & 0xf];
        }
        return sz;
    }

    public boolean test(int/*LId*/ element)
    {
        if (element < loBound || element >= hiBound)
            return false;
        int/*LId*/ bit = element - loBound;
        return (bits[(int) (bit >> SHIFT)] & (1 << (bit & MASK))) != 0;
    }

    /**
     * Iteration mechanism: returns the first id in the set that is >= to
     * argument.
     */
    public int/*LId*/ getNext(int/*LId*/ id)
    {
        int/*LId*/ bit = id - loBound;
        if (bit < 0)
            bit = 0;
        int cell = (int) (bit >> SHIFT);
        bit &= MASK;
        int unitCount = (bits == null)? 0 : bits.length;
        for (; cell < unitCount; bit = 0, cell++) {
            int unit = bits[cell] >>> bit;
            if (unit == 0)
                continue;
            for (; bit < USIZE; )
                if ((unit & 1) != 0)
                    return loBound + cell * USIZE + bit;
                else if((unit & 0xf) == 0) {
                    bit += 4; 
                    unit >>>= 4;
                }
                else {
                    ++ bit;
                    unit >>>= 1;
                }
        }
        return -1;
    }

    public void add(int/*LId*/ element)
    {
        if(element < 0)
            throw new IllegalArgumentException("negative element " + element);
        int/*LId*/ unit = element & ~MASK; 
        extend(unit, unit + USIZE);
        // lowBound may have changed:
        int/*LId*/ bit = element - loBound;
        bits[(int) (bit >> SHIFT)] |= (1 << (bit & MASK));        
    }

    public void add(int/*LId*/ first, int/*LId*/ last)
    {
        if(first < 0)
            throw new IllegalArgumentException("negative element " + first);
        extend(first & ~MASK, (last + MASK) & ~MASK); // optim
        for (int/*LId*/ i = first; i <= last; i++)
            add(i);
    }

    public void add(IntSet set)
    {
        extend(set.loBound, set.hiBound);
        int pos = (int) ((set.loBound - loBound) / USIZE);
        int[] sbits = set.bits;
        if(sbits != null)
            for (int i = sbits.length; --i >= 0;)
                bits[i + pos] |= sbits[i];
    }
    
    public void expectedbounds(int lowBound, int highBound)
    {
        extend(lowBound & ~MASK, (hiBound + MASK) & ~MASK);
    }
    
    public void remove(int/*LId*/ element)
    {
        if (element < loBound || element >= hiBound)
            return;
        int bit = (int) (element - loBound);
        bits[bit >> SHIFT] &= ~(1 << (bit & MASK));
    }

    public void remove(int/*LId*/ first, int/*LId*/ last)
    {
        for (int/*LId*/ i = first; i <= last; i++)
            remove(i);
    }
    
    public void remove(IntSet set)
    {
        int/*LId*/ lo = Math.max(loBound, set.loBound);
        int/*LId*/ hi = Math.min(hiBound, set.hiBound);
        if (hi <= lo)
            return;
        
        int up = (int) ((hi - loBound) / USIZE);
        int diff = (int) ((loBound - set.loBound) / USIZE);
        for(int i = (int) ((lo - loBound) / USIZE); i < up; i++ )
            bits[i] &= ~ set.bits[i + diff];
    }

    public void invert(int/*LId*/ element)
    {
        if (element < loBound || element >= hiBound)
            return;
        int/*LId*/ bit = element - loBound;
        bits[(int) (bit >> SHIFT)] ^= ~(1 << (bit & MASK));
    }

    public void clear()
    {
         bits = null;
         hiBound = loBound = 0;
    }

    public IntSet copy()
    {
        IntSet clone = new IntSet();
        clone.loBound = loBound;
        clone.hiBound = hiBound;
        if (bits != null)
            clone.bits = (int[]) bits.clone();
        return clone;
    }

    /**
     * Creates a new set which is the union of the two sets.
     */
    public static IntSet unionOf(IntSet set1, IntSet set2)
    {
        if(set2 == null || set2.hiBound == set2.loBound)
            return set1.copy();
        if(set1 == null || set1.hiBound == set1.loBound)
            return set2.copy();
        
        int/*LId*/ lo = Math.min(set1.loBound, set2.loBound);
        int/*LId*/ hi = Math.max(set1.hiBound, set2.hiBound);

        IntSet r = new IntSet();
        r.init(lo, hi);

        int pos = (int) ((set1.loBound - r.loBound) / USIZE);
        for(int i = set1.bits.length; --i >= 0; )
            r.bits[i + pos] = set1.bits[i];

        pos = (int) ((set2.loBound - r.loBound) / USIZE);
        for(int i = set2.bits.length; --i >= 0; )
            r.bits[i + pos] |= set2.bits[i];
        return r;
    }

    /**
     * Creates a new set which is the intersection of the two sets.
     */
    public static IntSet intersectionOf(IntSet set1, IntSet set2)
    {
        IntSet res = new IntSet();

        if (set2 == null || set2.hiBound == set2.loBound ||
            set1 == null || set1.hiBound == set1.loBound)
            return res; // empty

        int/*LId*/ lo = Math.max(set1.loBound, set2.loBound);
        int/*LId*/ hi = Math.min(set1.hiBound, set2.hiBound);
        if (hi <= lo)
            return res; // empty
        
        res.init(lo, hi);
        
        int diff = (int) ((lo - set1.loBound) / USIZE);
        for(int i = res.bits.length; --i >= 0; )
            res.bits[i] = set1.bits[i + diff];
        
        diff = (int) ((lo - set2.loBound) / USIZE);
        for(int i = res.bits.length; --i >= 0; )
            res.bits[i] &= set2.bits[i + diff];
        
        return res;
    }

    /**
     * Tests whether range [lowBound, highBound] has a non-empty intersection
     * with this set.
     */
    public boolean intersectsRange(int lowBound, int highBound)
    {
        int/*LId*/ first = getNext(lowBound);
        return first >= lowBound && first <= highBound; 
    }

    public boolean equals(Object obj)
    {
        if(!(obj instanceof IntSet))
            return false;
        IntSet oset = (IntSet) obj;
        for(int/*LId*/ e1 = -1, e2 = -1; ; ) {
            e1 = getNext(e1 + 1);
            e2 = oset.getNext(e2 + 1);
            if(e1 < 0)
                return e2 < 0;
            if(e1 != e2)
                return false;
        }
    }

    /**
     * Creates a new set which is the difference of set1 and set2.
     */
    public static IntSet differenceOf(IntSet set1, IntSet set2)
    {
        IntSet res = set1.copy();
        if (set2 == null || set2.hiBound == set2.loBound)
            return res;
        
        int/*LId*/ lo = Math.max(set1.loBound, set2.loBound);
        int/*LId*/ hi = Math.min(set1.hiBound, set2.hiBound);
        if (hi <= lo)
            return res;
        
        int up = (int) ((hi - res.loBound) / USIZE);
        int diff = (int) ((set1.loBound - set2.loBound) / USIZE);
        for(int i = (int) ((lo - res.loBound) / USIZE); i < up; i++ )
            res.bits[i] &= ~ set2.bits[i + diff];
        return res;
    }

    public void save(ByteOutput output)
        throws IOException
    {
        output.putVint/*LId*/(loBound);
        output.putVint/*LId*/(hiBound - loBound);
        if (bits != null)
            for (int i = 0, asize = bits.length; i < asize; i++)
                output.putInt(bits[i]);
    }

    public void load(ByteInput input)
        throws IOException
    {
        loBound = input.getVint();
        int span = input.getVint();
        hiBound = loBound + span;
        bits = new int[span / USIZE];
        for (int i = 0, asize = bits.length; i < asize; i++)
            bits[i] = input.getInt();
    }

    public static void skip(ByteInput input)
        throws IOException
    {
        input.getVint();
        int ispan = input.getVint() / USIZE;
        for (int i = 0; i < ispan; i++)
            input.getInt();
    }

    public String toString()
    {
        return show(50);
    }

    public String show(int maxSize)
    {
        StringBuffer buf = new StringBuffer("[");
        int/*LId*/ id = -1;
        for (; buf.length() < maxSize;) {
            id = getNext(id + 1);
            if (id < 0)
                break;
            if(buf.length() > 1)
                buf.append(' ');
            buf.append(Long.toString(id));
        }
        buf.append(id < 0 ? "]" : "..]");
        return buf.toString();
    }

    private void init(int/*LId*/ low, int/*LId*/ hi)
    {
        this.loBound = low;
        this.hiBound = hi;
        int/*LId*/ span = hi - low;
        if(span > 0)
            bits = new int[(int) (span / USIZE)];
    }

    // Extends the storage to given bounds, keeping the elements
    // low and hi must be multiples of USIZE
    private void extend(int/*LId*/ low, int/*LId*/ hi)
    {
        if(low >= this.loBound && hi <= hiBound)
            return; // OK
        
        if(hiBound == loBound) { // empty
            init(low, hi);
            return;
        }
        
        int oldUnits[] = bits;
        int/*LId*/ oldLow = loBound;
        
        int/*LId*/ newLow = Math.min(loBound, low);
        int/*LId*/ newHi = Math.max(hiBound, hi);
        
        // optim strategy: add 1/8 to the necessary size
        int/*LId*/ more = ((newHi - newLow) / 16 / USIZE) * USIZE;
        newLow -= more;
        if(newLow < 0) {
            newLow = 0;
        }
        newHi += more;
        
        
        init(newLow, newHi);
        
        // copy old units to the right place:
        System.arraycopy(oldUnits, 0, bits, (int) ((oldLow - loBound) / USIZE),
                         oldUnits.length);
    }

}
