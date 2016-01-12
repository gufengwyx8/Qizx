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
package com.qizx.xdm;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.ItemType;
import com.qizx.api.Node;
import com.qizx.api.util.time.Date;
import com.qizx.api.util.time.DateTime;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.xquery.BaseValue;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleString;
import com.qizx.xquery.dt.UntypedAtomicType;

public abstract class NodeSequenceBase extends BaseValue
{
    public static final NodeSequenceBase noNodes = new NodeSequenceBase() {
        
        public boolean next() {
            return false;
        }

        public BasicNode basicNode() {
            return null;
        }

        public XQValue bornAgain() {
            return null;
        }
    };

    public ItemType getType()
        throws EvaluationException
    {
        return XQType.NODE;
    }

    public XQItemType getItemType()
    {
        return XQType.NODE;
    }

    public boolean isNode()
    {
        return true;
    }

    public Node getNode()
    {
        return basicNode();
    }

    public XQItem getItem()
    {
        return basicNode();
    }

    public abstract BasicNode basicNode();

    public XQItem asAtom() throws EvaluationException
    {
        // this is OK for Basic XQ: improve it later
        try {
            return new SingleString(getNode().getStringValue(),
                                    XQType.UNTYPED_ATOMIC);
        }
        catch (DataModelException e) {
            throw BasicNode.wrapDMException(e);
        }
    }

    public boolean getBoolean()
        throws EvaluationException
    {
        return true;
    }

    public String getString()
        throws EvaluationException
    {
        try {
            return getNode().getStringValue();
        }
        catch (DataModelException e) {
            throw BasicNode.wrapDMException(e);
        }
    }

    public long getInteger()
        throws EvaluationException
    {
        return Conversion.toInteger(getString());
    }

    public double getDouble()
        throws EvaluationException
    {
        try {
            return Conversion.toDouble(getString());
        }
        catch (EvaluationException e) {
            double v = ((BasicNode) getNode()).getDoubleByRules();
            if(v == v)  // successful
                return v;
            throw e;
        }
    }

    public float getFloat()
        throws EvaluationException
    {
        try {
            return Conversion.toFloat(getString());
        }
        catch (EvaluationException e) {
            double v = ((BasicNode) getNode()).getDoubleByRules();
            if(v == v)  // successful
                return (float) v;
            throw e;
        }
    }

    public Date getDate()
        throws EvaluationException, DateTimeException
    {
        return basicNode().getDate();
    }

    public DateTime getDateTime()
        throws EvaluationException, DateTimeException
    {
        return basicNode().getDateTime();
    }

    public int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException
    {
        return UntypedAtomicType.comparison(this, that, context, flags);
    }

}
