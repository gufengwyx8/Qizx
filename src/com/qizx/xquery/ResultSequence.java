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
import com.qizx.api.fulltext.Scorer;
import com.qizx.xquery.dt.ArraySequence;

import java.math.BigDecimal;

public class ResultSequence
    implements ItemSequence
{
    private XQValue values;
    long position;
    
    /**
     * Builds an in-memory sequence. This sequence supports adding items.
     */
    public ResultSequence()
    {
        this.values = new ArraySequence(4, null);
    }

    public ResultSequence(XQValue seq)
    {
        this.values = seq;
    }

    /**
     * Adds an item at the end of the sequence. This is supported only if the
     * sequence was built with the default constructor (in-memory sequence).
     * @param item a valid Item built by an ItemFactory
     */
    public void addItem(Item item)
    {
        // if invalid sequence you get a CastException...
        ((ArraySequence) values).addItem(item);            
    }
    
    public long countItems()
        throws EvaluationException
    {
        checkStatus();
        
        if(position == 0) {
            // Frequent case: to optimize sorted iterators
            // Principle: first count (hence sort etc) THEN clone and *reuse*
            long cnt = values.quickCount(null);
            position = cnt;
            moveTo(0);
            return cnt;
        }
        // fallback: clone a new iterator that is not reused
        return values.bornAgain().quickCount(null);
    }

    public Item getCurrentItem()
    {
        return (values == null)? null : values.getItem();
    }

    public long getPosition()
    {
        return position;
    }

    public void moveTo(long pos)
        throws EvaluationException
    {
        checkStatus();
        if(pos < position) {
            values = values.bornAgain();
            position = 0;
            // go on
        }
        
        if(pos > position) {
            values.doSkip((int) (pos - position));
            position = pos;
        }
    }

    public boolean moveToNextItem()
        throws EvaluationException
    {
        checkStatus();
        if(!values.next())
            return false;
        ++position;
        return true;
    }

    public int skip(int count)
        throws EvaluationException
    {
        checkStatus();
        int skipped = values.doSkip(count);
        if(skipped > 0)
            position += skipped;
        return skipped;
    }

    public void export(XMLPushStream output)
        throws QizxException
    {
        checkStatus();
        values.export(output);
    }

    public XMLPullStream exportNode()
        throws EvaluationException
    {
        checkStatus();
        return values.exportNode();
    }

    public boolean getBoolean()
        throws EvaluationException
    {
        checkStatus();
        return values.getBoolean();
    }

    public BigDecimal getDecimal()
        throws EvaluationException
    {
        checkStatus();
        return values.getDecimal();
    }

    public double getDouble()
        throws EvaluationException
    {
        checkStatus();
        return values.getDouble();
    }

    public float getFloat()
        throws EvaluationException
    {
        checkStatus();
        return values.getFloat();
    }

    public long getInteger()
        throws EvaluationException
    {
        checkStatus();
        return values.getInteger();
    }

    public Node getNode()
        throws EvaluationException
    {
        checkStatus();
        return values.getNode();
    }

    public Object getObject()
        throws QizxException
    {
        checkStatus();
        return values.getObject();
    }

    public QName getQName()
        throws EvaluationException
    {
        checkStatus();
        return values.getQName();
    }

    public String getString()
        throws EvaluationException
    {
        checkStatus();
        return values.getString();
    }

    public ItemType getType()
        throws EvaluationException
    {
        return values.getType();
    }

    public boolean isNode()
    {
        return values != null && values.isNode();
    }

    public double getFulltextScore() throws EvaluationException
    {
        double s = (values == null)? 0 : values.getFulltextScore(null);
        return (s < 0) ? Scorer.CORE_SCORE : s;
    }

    public XQValue getValues()
    {
        return values;
    }

    public void close()
    {
         if(values != null)
             values.close();
         values = null;
    }

    protected void finalize()   // fool proofing
        throws Throwable
    {
        close();
    }

    private void checkStatus() throws EvaluationException
    {
         if(values == null)
             throw new EvaluationException("closed sequence");
    }
}
