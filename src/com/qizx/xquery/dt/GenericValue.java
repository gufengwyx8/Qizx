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
import com.qizx.api.ItemType;
import com.qizx.api.Node;
import com.qizx.api.QizxException;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.Duration;
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;

import java.math.BigDecimal;

/**
 * Generic Value implementation.
 * <p>
 * Abstract class for variant values, like those returned by Sequence or Flower
 * expressions. Holds a current item and delegates all methods to this current
 * item.
 */
public abstract class GenericValue extends BaseValue
{
    protected XQItem item;
    
    public ItemType getType()
        throws EvaluationException
    {
        return item != null ? item.getType() : (ItemType) XQType.ITEM;
    }

    public XQItemType getItemType() throws EvaluationException
    {
        return item != null ? item.getItemType() : XQType.ITEM;
    }

    public XQItem getItem()
    {
//        if (item == null) {
//            System.err.println("null item in " + this);
//            Thread.dumpStack();
//        }
        return item;
    }

    public XQItem asAtom()
    {
        if (item.isNode())
            try {
                item = new SingleString(item.getNode().getStringValue(),
                                        XQType.UNTYPED_ATOMIC);
            }
            catch (QizxException shouldNotHappen) {
                return item; //OOPS sure?
            }
        return item;
    }

    public boolean getBoolean()
        throws EvaluationException
    {
        return item.getBoolean();
    }

    public long getInteger()
        throws EvaluationException
    {
        return item.getInteger();
    }

    public BigDecimal getDecimal()
        throws EvaluationException
    {
        return item.getDecimal();
    }

    public double getDouble()
        throws EvaluationException
    {
        return item.getDouble();
    }

    public float getFloat()
        throws EvaluationException
    {
        return item.getFloat();
    }

    public String getString()
        throws EvaluationException
    {
        if (item == null) {
            System.err.println("null item in " + this);
            Thread.dumpStack();
        }
        return item.getString();
    }

    public Duration getDuration()
        throws EvaluationException
    {
        return item.getDuration();
    }

    public DateTimeBase getMoment()
        throws EvaluationException
    {
        return item.getMoment();
    }

    public Node getNode()
        throws EvaluationException
    {
        if (item == null) {
            throw new EvaluationException("null item!");
        }
        return item.getNode();
    }

    public BasicNode basicNode() throws EvaluationException
    {
        if (item == null)
            throw new RuntimeException("null item!");
        return item.basicNode();
    }

    public boolean isNode()
    {
        // -if(item == null) System.err.println(" OOOPS "+ this);
        return item != null && item.isNode();
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException
    {
        return item.compareTo(that, context, 0);
    }

    public boolean equals(Object that)
    {
        return item.equals(that);
    }

    public int hashCode()
    {
        return item == null ? 0 : item.hashCode();
    }
}

