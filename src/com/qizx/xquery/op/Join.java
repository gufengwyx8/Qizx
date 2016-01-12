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
package com.qizx.xquery.op;

import com.qizx.api.EvaluationException;
import com.qizx.util.basic.HTable;
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleWrappedObject;
import com.qizx.xquery.dt.StringValue;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Wrapper for join computation and retrieval.
 */
public class Join
{
    /**
     * Builds a join hash table from a node sequence and an expression relative
     * to each node (i.e. computed with the node as focus). Value of a LET
     * expression inserted before the outer loop of the join.
     */
    protected static class Maker extends Expression
    {
        Expression source;

        Expression key;

        LocalVariable tmpVar;

        XQType keyType;

        Maker(Expression source, Expression key, LocalVariable tmpVar,
              XQType keyType)
        {
            this.source = source;
            this.key = key;
            this.tmpVar = tmpVar;
            this.keyType = keyType;
        }

        public Expression child(int rank)
        {
            return null; // TODO?
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQValue src = source.eval(focus, context);
            // -long t0 = System.currentTimeMillis();
            // -System.err.println("start join "+keyType);
            Table table = null;
            for (; src.next();) {
                BasicNode curNode = src.basicNode();
                // store to tmp variable:
                context.storeLocal(tmpVar.address, src, true, null);
                XQValue keys = key.eval(focus, context);
                // optimize string and numeric cases:
                if (keyType == XQType.NUMERIC) {
                    if (table == null)
                        table = new NTable();
                    NTable tbl = (NTable) table;
                    for (; keys.next();)
                        tbl.put(keys.getDouble(), curNode);
                }
                else if (keyType == XQType.STRING) {
                    if (table == null)
                        table = new STable(context.getCollator(null));
                    STable tbl = (STable) table;
                    for (; keys.next();)
                        tbl.put(keys.getString(), curNode);
                }
                else { // general case
                    if (table == null)
                        table = new ITable(context);
                    ITable tbl = (ITable) table;
                    for (; keys.next();)
                        tbl.put(keys.getItem(), curNode);
                }
            }
            // -System.err.println(table+" join built");
            // if(table == null) // protection (empty input sequence)
            // table = new ITable();
            return new SingleWrappedObject(table);
        }

        public void dump(ExprDisplay d)
        {
            d.header(this);
            d.property("key-type", keyType);
            d.child("key", key);
            d.child("source", source);
        }
    }

    /**
     * Gets a node sequence from a key expression. 
     * References the key expression to evaluate, and the local variable 
     * containing the join table built outside the loop.
     */
    protected static class Get extends Expression
    {
        Expression key;
        LocalVariable joinVar;
        Comparison.Test mode;   // e.g get(for key < value)

        Get(Expression key, LocalVariable joinVar, Comparison.Test mode)
        {
            this.key = key;
            this.joinVar = joinVar;
            this.mode = mode;
            this.type = XQType.NODE.star;
        }

        public Expression child(int rank)
        {
            return rank == 0 ? key : null;
        }

        public void dump(ExprDisplay d)
        {
            d.header(this);
            d.property("join", joinVar.address);
            d.property("comp", mode.getName());
            d.child("key", key);
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            // retrieve table from joinVar:
            XQItem tbl = context.loadLocalItem(joinVar.address);
            if (!(tbl instanceof SingleWrappedObject))
                context.error("OOPS", this, "not a join table in $"
                                            + joinVar.name + " " + tbl);
            Table table = (Table) ((SingleWrappedObject) tbl).getObject();
            NodeSortExpr.Sequence seq = null;
            // evaluate key:
            int keyCnt = 0;
            XQValue kval = key.eval(focus, context);
            
            for (; kval.next();)
            {
                Table.Entry entry = null;
                // -System.err.println("--- find entries for "+kval);
                if (table instanceof STable)
                    entry = ((STable) table).findEntries(kval.getString(), mode);
                else if (table instanceof NTable)
                    entry = ((NTable) table).findEntries(kval.getDouble(), mode);
                else if (table instanceof ITable)
                    entry = ((ITable) table).findEntries(kval.getItem(), mode);

                for (; entry != null; entry = table.nextEntry()) {
                    // really something: create an array sequence (maybe to
                    // sort) if needed
                    // -System.err.println(" entry "+entry);
                    ++keyCnt;
                    if (seq == null) {
                        seq = new NodeSortExpr.Sequence(entry.nodes,
                                                        entry.nodeCount);
                    }
                    else {
                        seq.addItems(entry.nodes, entry.nodeCount);
                    }
                }
            }
            
            if(seq == null) 
                return XQValue.empty;
            
            if (keyCnt == 1)
                seq.needsSort = false;
            seq.isDistinct = true;  // by construction
            return seq;
        }
    }

    // base class for String, Numeric, Generic implementations
    public static class Table extends HTable
    {
        Entry[] keys; // sorted keys

        Comparator comparator;

        // iteration:
        int index, lastIndex;

        protected static class Entry extends HTable.Key
        {
            BasicNode[] nodes;
            int nodeCount;

            Entry(BasicNode node)
            {
                nodes = new BasicNode[] { node, null };
                nodeCount = 1;
            }

            void add(BasicNode node)
            {
                if (nodeCount >= nodes.length) {
                    BasicNode[] old = nodes;
                    nodes = new BasicNode[old.length * 2];
                    System.arraycopy(old, 0, nodes, 0, old.length);
                }
                nodes[nodeCount++] = node;
            }

            public BasicNode[] getNodes()
            {
                if (nodeCount == nodes.length)
                    return nodes;
                // need to copy:
                BasicNode[] nodes = new BasicNode[nodeCount];
                System.arraycopy(this.nodes, 0, nodes, 0, nodeCount);
                return nodes;
            }

            public HTable.Key duplicate()
            { // not needed
                System.err.println("Key.duplicate not needed");
                return null;
            }
        }

        Entry nextEntry()
        {
            // -System.err.println("nextEntry "+index+" "+lastIndex);
            return (index >= lastIndex) ? null : keys[index++];
        }

        void sortedKeys()
        {
            keys = new Entry[count];
            for (int s = 0, h = hash.length; --h >= 0;)
                for (HTable.Key c = hash[h]; c != null; c = c.next)
                    keys[s++] = (Entry) c;
            Arrays.sort(keys, comparator); // TODO collation
            // -for(int k=0; k<keys.length;k++) System.err.println(" :
            // "+keys[k]);
        }

        Entry findEntries(Entry probe, Comparison.Test mode)
        {
            index = lastIndex = 0;
            if (mode == ValueEqOp.TEST)
                return (Entry) get(probe);
            int ix = locate(probe); // binary search

            if (mode == ValueGtOp.TEST) { // ie probe > keys 
                lastIndex = ix; // probe/key excluded in all cases
            }
            else if (mode == ValueGeOp.TEST) { // ie keys <= probe
                if (ix < keys.length
                    && comparator.compare(keys[ix], probe) == 0)
                    ++ix; // probe/key included if equality
                lastIndex = ix;
            }
            else if (mode == ValueLtOp.TEST) { // ie probe < keys 
                if (ix < keys.length
                    && comparator.compare(keys[ix], probe) == 0)
                    ++ix; // exclude if equal
                index = ix;
                lastIndex = keys.length;
            }
            else { // Le: // ie keys >= probe
                index = ix;
                lastIndex = keys.length;
            }

            Entry res =
                index < Math.min(lastIndex, keys.length)
                    ? keys[index++] : null;

            return res;
        }

        // returns the position of the smallest entry >= probe
        int locate(Entry probe)
        {
            if (keys == null)
                sortedKeys();
            int lo = 0, hi = keys.length - 1;
            while (lo <= hi) {
                int mid = (lo + hi) / 2;
                int cmp = comparator.compare(probe, keys[mid]);
                if (cmp < 0)
                    hi = mid - 1;
                else if (cmp > 0)
                    lo = mid + 1;
                else
                    return mid; // found
            }
            return lo;
        }
    }

    //
    // String implementation
    //
    public static class STable extends Table
    {
        Entry probe = new Entry("", null);

        Collator collator;

        STable(final Collator collator)
        {
            this.collator = collator;
            comparator = new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    Entry e1 = (Entry) o1, e2 = (Entry) o2;
                    return StringValue.compare(e1.key, e2.key, collator);
                }
            };
        }

        protected static class Entry extends Table.Entry
        {
            String key;

            Entry(String key, BasicNode node)
            {
                super(node);
                this.key = key;
            }

            public int hashCode()
            {
                return key.hashCode();
            }

            public boolean equals(Object that)
            {
                Entry other = (Entry) that;
                return other.key.equals(key);
            }

            public String toString()
            {
                return "STRING KEY " + key + " nodes: " + nodeCount;
            }
        }

        public void put(String key, BasicNode node)
        {
            probe.key = key;
            Entry entry = (Entry) get(probe);
            if (entry != null)
                entry.add(node);
            else
                directPut(new Entry(key, node));
            keys = null;
        }

        Table.Entry findEntries(String key, Comparison.Test mode)
        {
            probe.key = key;
            return findEntries(probe, mode);
        }
    } // end of class STable

    //
    // Numeric implementation
    //
    public static class NTable extends Table
    {
        Entry probe = new Entry(0, null);

        NTable()
        {
            comparator = new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    Entry e1 = (Entry) o1, e2 = (Entry) o2;
                    return com.qizx.util.basic.Comparison.of(e1.key, e2.key);// TODO empty
                    // greates
                }
            };
        }

        protected static class Entry extends Table.Entry
        {
            double key;

            Entry(double key, BasicNode node)
            {
                super(node);
                this.key = key;
            }

            public int hashCode()
            {
                long h = Double.doubleToRawLongBits(key);
                return (int) (h ^ (h >>> 32));
            }

            public boolean equals(Object that)
            {
                Entry other = (Entry) that;
                return other.key == key;
            }

            public String toString()
            {
                return "NUMERIC KEY " + key + " nodes: " + nodeCount;
            }
        }

        public void put(double key, BasicNode node)
        {
            probe.key = key;
            Entry entry = (Entry) get(probe);
            if (entry != null)
                entry.add(node);
            else
                directPut(new Entry(key, node));
        }

        Table.Entry findEntries(double key, Comparison.Test mode)
        {
            probe.key = key;
            return findEntries(probe, mode);
        }
    }

    //
    // General implementation
    //
    public static class ITable extends Table
    {
        Entry probe = new Entry(null, null);

        EvalContext context;

        ITable(final EvalContext context)
        {
            this.context = context; // useless?
            comparator = new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    try {
                        Entry e1 = (Entry) o1, e2 = (Entry) o2;
                        return e1.key.compareTo(e2.key, context, 0);
                    }
                    catch (Exception e) {
                        return 0; // whatever
                    }
                }
            };
        }

        protected static class Entry extends Table.Entry
        {
            XQItem key;

            Entry(XQItem key, BasicNode node)
            {
                super(node);
                this.key = key;
            }

            public int hashCode()
            {
                // System.err.println(key+" hashcode "+key.hashCode());
                return key.hashCode();
            }

            public boolean equals(Object that)
            {
                Entry other = (Entry) that;
                return other.key.equals(key);
            }

            public String toString()
            {
                return "ITEM KEY " + key + " nodes: " + nodeCount;
            }
        }

        public void put(XQItem key, BasicNode node)
        {
            probe.key = key;
            Entry entry = (Entry) get(probe);
            if (entry != null)
                entry.add(node);
            else
                directPut(new Entry(key, node));
        }

        Table.Entry findEntries(XQItem key, Comparison.Test mode)
        {
            probe.key = key;
            return findEntries(probe, mode);
        }
    }
}
