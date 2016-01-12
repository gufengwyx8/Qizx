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

/**
 * A simple hash table meant for efficiency. The key (subclass of Key) also
 * bears the value.
 */
public class HTable
{
    static final double LOAD_FACTOR = 1.0;

    protected Key[] hash;
    protected int count = 0;
    protected int maxCount;


    public HTable(int capacity)
    {
        hash = new Key[capacity];
        maxCount = (int) (LOAD_FACTOR * hash.length);
    }

    public HTable()
    {
        this(3);
    }

    public int getSize()
    {
        return count;
    }

    /**
     * Special purpose.
     */
    public Key[] getHash()
    {
        return hash;
    }

    /**
     * Must of course implement hashCode() and equals().
     */
    public abstract static class Key
    {
        public Key next;

        public abstract Key duplicate();
    }

    public int hash(Key key)
    {
        return hashFunction(key.hashCode()) % hash.length;
    }

    public static int hashFunction(int hashcode)
    {
        return (hashcode & 0x7FFFFFFF);
    }

    public Key get(Key probe)
    {
        int h = hash(probe);
        for (Key c = hash[h]; c != null; c = c.next)
            if (c.equals(probe))
                return c;
        return null;
    }

    public Key put(Key probe)
    {
        int h = hash(probe);
        for (Key c = hash[h]; c != null; c = c.next)
            if (c.equals(probe))
                return c;
        if (++count > maxCount) {
            resize();
            h = hash(probe); // may have changed
        }
        Key nc = probe.duplicate();
        nc.next = hash[h];
        hash[h] = nc;
        return nc;
    }

    /**
     * Like put, but returns true if the key was not found.
     */
    public boolean hasPut(Key probe)
    {
        int h = hash(probe);
        for (Key c = hash[h]; c != null; c = c.next)
            if (c.equals(probe))
                return false;
        if (++count > maxCount) {
            resize();
            h = hash(probe); // may have changed
        }
        Key nc = probe.duplicate();
        nc.next = hash[h];
        hash[h] = nc;
        return true;
    }

    /**
     * No check: allows duplicate keys.
     */
    public Key add(Key probe)
    {
        int h = hash(probe);
        Key nc = probe.duplicate();
        nc.next = hash[h];
        hash[h] = nc;

        if (++ count > maxCount) {
            resize();
        }
        return nc;
    }

    public void directPut(Key entry)
    {
        if (++count > maxCount)
            resize();
        int h = hash(entry);
        entry.next = hash[h];
        hash[h] = entry;
    }

    public Key[] getKeys(Key[] keys)
    {
        int s = 0;
        for (int h = hash.length; --h >= 0;)
            for (Key c = hash[h]; c != null; c = c.next)
                keys[s++] = c;
        return keys;
    }

    public void clear()
    {
        for (int h = hash.length; --h >= 0;)
            hash[h] = null;
        count = 0;
    }

    void resize()
    {
        Key[] old = hash;

        int emptyC = 0;
        for (int i = 0; i < hash.length; i++)
            if (hash[i] == null)
                emptyC++;
        hash = new Key[old.length * 2 + 1];
        for (int ic = old.length; --ic >= 0;) {
            for (Key c = old[ic]; c != null;) {
                Key ac = c;
                c = c.next;
                int h = hash(ac);
                ac.next = hash[h];
                hash[h] = ac;
            }
        }
        maxCount = (int) (LOAD_FACTOR * hash.length);
    }
} 
