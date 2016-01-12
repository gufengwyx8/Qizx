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
package com.qizx.xquery;

import com.qizx.api.*;
import com.qizx.api.util.time.Date;
import com.qizx.api.util.time.DateTime;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.api.util.time.Duration;
import com.qizx.util.basic.Comparison;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.Conversion;
import com.qizx.xdm.NodePullStream;
import com.qizx.xquery.dt.ArraySequence;
import com.qizx.xquery.dt.Atomizer;
import com.qizx.xquery.dt.SingleItem;
import com.qizx.xquery.dt.UntypedAtomicType;
import com.qizx.xquery.impl.EmptyException;
import com.qizx.xquery.impl.ErrorValue;

import java.math.BigDecimal;

/**
 * Default Value implementation.
 * <p>
 * The <code>as<i>Type</i>()</code> methods implemented by this class
 * always throw errors. Subclasses must redefine these methods according to the
 * values held.
 */
public abstract class BaseValue
    implements XQValue
{
    // CAUTION: asItem() should not return 'this' : else possible infinite loop
    protected XQItemType itemType;

    public boolean nextCollection()
        throws EvaluationException
    {
        return next();
    }

    public ItemType getType()
        throws EvaluationException
    {
        return itemType;
    }

    public XQItemType getItemType() throws EvaluationException
    {
        return itemType;
    }

    public XQItem asAtom() throws EvaluationException
    {
        return getItem();
    }

    private EvaluationException notConvertibleTo(String typeName)
        throws EvaluationException
    {
        try {
            ItemType type = getType();
            return new EvaluationException(Conversion.ERR_CAST,
                                           "required type xs:" + typeName
                                           + ", cannot accept actual type "
                                           + type);
        }
        catch (EvaluationException e) {
            return e; // / oops
        }
    }


    // CAUTION: asItem() should not return 'this' : else possible infinite loop
    
    public boolean getBoolean()
        throws EvaluationException
    {
        throw notConvertibleTo("boolean");
    }

    public long getInteger()
        throws EvaluationException
    {
        throw notConvertibleTo("integer");
    }

    public BigDecimal getDecimal()
        throws EvaluationException
    {
        throw notConvertibleTo("decimal");
    }

    public float getFloat()
        throws EvaluationException
    {
        throw notConvertibleTo("float");
    }

    public double getDouble()
        throws EvaluationException
    {
        throw notConvertibleTo("double");
    }

    public String getString()
        throws EvaluationException
    {
        throw notConvertibleTo("string");
    }

    public QName getQName()
        throws EvaluationException
    {
        throw notConvertibleTo("QName");
    }

    public Duration getDuration()
        throws EvaluationException
    {
        throw notConvertibleTo("duration");
    }

    public DateTimeBase getMoment()
        throws EvaluationException
    {
        throw notConvertibleTo("date or time");
    }

    public Date getDate()
        throws EvaluationException, DateTimeException
    {
        return Date.parseDate(getString().trim());
    }

    public DateTime getDateTime()
        throws EvaluationException, DateTimeException
    {
        return DateTime.parseDateTime(getString().trim());
    }

    public Object getObject()
        throws QizxException
    {
        return getString(); // fallback
    }

    public Node getNode()
        throws EvaluationException
    {
        throw notConvertibleTo("node");
    }

    public boolean isNode()
    {
        return false;
    }

    public BasicNode basicNode() throws EvaluationException
    {
        throw new XQTypeException("sequence item is not a node");
    }

    // Default implem for leaf iterators
    public double getFulltextScore(Item item) throws EvaluationException
    {
        
        return -1;
    }

    // Used in compareTo (not sorts)
    public static int compare(long diff)
    {
        return diff < 0 ? Comparison.LT
             : diff > 0 ? Comparison.GT
                        : Comparison.EQ;
    }

    // Used in compareTo (not sort)
    public static int compare(double d1, double d2)
    {
        if (d1 != d1 || d2 != d2) // NaN
            return Comparison.FAIL;
        return d1 < d2 ? Comparison.LT
             : d1 > d2 ? Comparison.GT
                       : Comparison.EQ;
    }

    // Used in sort
    public static int compare(double d1, double d2, boolean emptyGreatest)
    {
        if (d1 != d1) // NaN
            return emptyGreatest ? Comparison.GT : Comparison.LT;
        if (d2 != d2) // NaN
            return emptyGreatest ? Comparison.LT : Comparison.GT;
        return d1 < d2 ? Comparison.LT
             : d1 > d2 ? Comparison.GT
                       : Comparison.EQ;
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException
    {
        return UntypedAtomicType.comparison(this, that, context, flags);
    }

    public boolean deepEquals(XQItem item, ComparisonContext context)
    {
        try {
            return compareTo(item, context, 0) == 0;
        }
        catch (EvaluationException e) {
            return false;
        }
    }

    public XQValue checkTypeExpand(XQType type, EvalContext context,
                                   boolean conversion, boolean build)
        throws EvaluationException
    {
        XQItemType reqItemType = (type == null) ? null : type.itemType();

        if (!this.next()) {
            if (type == null || type.isOptional())
                return XQValue.empty; // NOT "return this" ! (FIX)
            // otherwise error:
            return XQValue.TOO_SHORT;
        }
        ArraySequence resSeq = null;
        XQItem resItem = null;
        int count = 0;
        do {
            XQItem item = this.getItem();
            XQItemType itemType = item.getItemType();
            if (reqItemType != null && !reqItemType.acceptsItem(item)) {
                // still a chance if 'conversion':
                boolean ok = false;
                if (conversion) {
                    // atomize to single item (if Node)
                    // Actually atomization should be applied to whole sequence
                    // IF itemType is atomic
                    item = Atomizer.toSingleAtom(item);
                    if (reqItemType.promotable(item.getItemType())) {
                        item = reqItemType.cast(item, context);
                        ok = true;
                    }
                }
                if (!ok)
                    return new ErrorValue(reqItemType
                                          + " cannot accept item of type "
                                          + itemType);
            }
            ++count;
            if (build)
                if (resItem == null)
                    resItem = item;
                else {
                    if (resSeq == null) {
                        resSeq = new ArraySequence(2, this);
                        resSeq.addItem(resItem);
                    }
                    resSeq.addItem(item);
                }
        }
        while (this.next());

        if (resSeq != null)
            resSeq.pack();
        XQValue result = build ? (resSeq != null ? 
                                    (XQValue) resSeq : new SingleItem(resItem))
                               : this.bornAgain();
        if (type == null)
            return result;
        switch (type.getOccurrence()) {
        case XQType.OCC_ZERO_OR_ONE:
            if (count <= 1)
                return result;
            return TOO_LONG;
        case XQType.OCC_ZERO_OR_MORE:
            return result;
        case XQType.OCC_ONE_OR_MORE:
            if (count >= 1)
                return result;
            return TOO_SHORT;
        default: // one
            if (count == 1)
                return result;
            return (count == 0) ? TOO_SHORT : TOO_LONG;
        }
    }

    public boolean worthExpanding()
    {
        return true; // default behaviour
    }

    public void setLazy(boolean value)
    {
        // ignored
    }

    // default: can be optimized by some iterators
    public long quickCount(EvalContext context)
        throws EvaluationException
    {
        try {
            // lazy is only for avoiding building DM
            setLazy(true);
            int cnt = 0;
            for (; this.next();) {
                if (context != null && (cnt & 255) == 0) {
                    context.at(null);
                }
                ++cnt;
            }
            return cnt;
        }
        catch (EmptyException e) {
            return 0;
        }
        catch (RuntimeException t) {
            t.printStackTrace();
            throw t;
        }
        finally {
            setLazy(false); // useful!
        }
    }

    public int doSkip(int count)
        throws EvaluationException
    {
        int skipped = 0;
        try {
            // lazy is only for avoiding accessing DM
            setLazy(true);

            for (; --count >= 0 && this.next();)
                ++skipped;
        }
        catch (EmptyException ignored) { ;
        }
        finally {
            setLazy(false); // useful!
        }
        return skipped;
    }

    public boolean hasQuickIndex()
    {
        return false;
    }

    public XQItem quickIndex(long index)
    {
        return null;
    }

    public void export(XMLPushStream writer)
        throws DataModelException, EvaluationException
    {
        if(isNode())
            writer.putNodeCopy(getNode(), 0);
        else
            writer.putText(getString());
    }

    public XMLPullStream exportNode() throws EvaluationException
    {
        if(!isNode())
            throw new EvaluationException("node item required");
        return new NodePullStream(getNode());
    }
    
    public void close()
    {
        // reserved for future use (SQL results)
    }
}
