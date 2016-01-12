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
import com.qizx.util.basic.Comparison;
import com.qizx.util.basic.HTable;
import com.qizx.util.basic.HTable.Key;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQValue;

import java.text.Collator;

/**
 * Removes duplicate items from a source value.
 */
public class DistinctValueSequence extends SingleSourceSequence
{
    private Collator collator;
    private HTable seenSet = new HTable();
    private HashedItem probe = new HashedItem(null);
    private Atomizer atoms;

    public DistinctValueSequence(XQValue source, Collator collator)
    {
        super(source);
        atoms = new Atomizer(source);
        this.collator = collator;
    }

    public boolean next()
        throws EvaluationException
    {
        for (; atoms.next();) {
            item = atoms.getItem();
            probe.item = item;
            if (seenSet.hasPut(probe)) {
                return true;
            }
        }
        return false;
    }

    public XQValue bornAgain()
    {
        return new DistinctValueSequence(source.bornAgain(), collator);
    }

    static class HashedItem extends HTable.Key
    {
        XQItem item;

        public HashedItem(XQItem item)
        {
            this.item = item;
        }

        public int hashCode()
        {
            // TODO gross problem with collators: need special hash function
            // for strings
            int h = item.hashCode();
            return h;
        }

        public boolean equals(Object obj)
        {
            if ((!(obj instanceof HashedItem)))
                throw new RuntimeException("oops! should be an hitem: " + obj);
            XQItem other = ((HashedItem) obj).item;
            int cmp;
            try {
                cmp = item.compareTo(other, null, 0);
            }
            catch (EvaluationException e) {
                cmp = Comparison.ERROR;
            }
            if (cmp == Comparison.EQ)
                return true;
            try {
                if (cmp == Comparison.FAIL) {
                    double d1 = item.getDouble(), d2 = other.getDouble();
                    return d1 != d1 && d2 != d2;
                }
            }
            catch (EvaluationException ignore) {
                ;
            }
            return false;
        }

        public Key duplicate()
        {
            return new HashedItem(item);
        }
    }
}
