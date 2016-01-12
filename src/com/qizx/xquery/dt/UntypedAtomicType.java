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
import com.qizx.api.QName;
import com.qizx.util.basic.Comparison;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;

public class UntypedAtomicType extends StringType /* trick to share code */
{
    public QName getName()
    {
        return XQType.untypedAtomic;
    }

    public int quickCode()
    {
        return QT_UNTYPED;
    }

    public String getShortName()
    {
        return "untypedAtomic";
    }

    public String toString()
    {
        return "xs:untypedAtomic";
    }

    // compares an atomic value with another item
    // item1 is assumed to be a node or untypedAtomic
    public static int comparison(XQItem item1, XQItem item2,
                                 ComparisonContext context, int flags)
        throws EvaluationException
    {
        XQItemType type2 = item2.getItemType();
        // if second argument is _typed_ atomic, use its type:
        if (type2 != XQType.UNTYPED_ATOMIC && !type2.isSubTypeOf(XQType.NODE)) {
            int cmp = item2.compareTo(item1, context, flags);
            // reverse comparison:
            switch (cmp) {
            case Comparison.GT:
                return Comparison.LT;
            case Comparison.LT:
                return Comparison.GT;
            default:
                return cmp;
            }
        }
        // both items are UNTYPED_ATOMIC/Node: string compare
        return StringValue.compare(item1.getString(), item2.getString(),
                                   (context != null) ? context.getCollator()
                                                     : null);
    }
}
