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

import com.qizx.api.EvaluationException;
import com.qizx.api.ItemType;
import com.qizx.api.QName;
import com.qizx.xquery.op.Expression;

/**
 * Implementation of a Sequence Type with its occurrence indicator.
 */
public class SequenceType extends XQType
    implements com.qizx.api.SequenceType
{
    private XQItemType itemType;

    private int occ = OCC_ZERO_OR_MORE;

    public SequenceType(XQItemType itemType)
    {
        this.itemType = itemType;
    }

    public SequenceType(XQItemType itemType, int occ)
    {
        this.itemType = itemType;
        this.occ = occ;
    }

    public ItemType getItemType()
    {
        return itemType;
    }

    public XQItemType itemType()
    {
        return itemType;
    }

    public int getNodeKind()
    {
        return getItemType().getNodeKind();
    }

    public QName getNodeName()
    {
        return getItemType().getNodeName();
    }

    public int getOccurrence()
    {
        return occ;
    }

    public boolean isOptional()
    {
        return isOptional(occ);
    }

    public boolean isRepeatable()
    {
        return isRepeatable(occ);
    }

    public String toString(BasicStaticContext ctx)
    {
        String s = itemType.toString(ctx);
        switch (occ) {
        case OCC_ZERO_OR_ONE:
            return s + "?";
        case OCC_ONE_OR_MORE:
            return s + "+";
        case OCC_ZERO_OR_MORE:
            return s + "*";
        default:
            return s;
        }
    }

    public String toString()
    {
        String s = itemType.toString();
        switch (occ) {
        case OCC_ZERO_OR_ONE:
            return s + "?";
        case OCC_ONE_OR_MORE:
            return s + "+";
        case OCC_ZERO_OR_MORE:
            return s + "*";
        default:
            return s;
        }
    }


    public boolean acceptsItem(XQItem item)
    {
        return itemType.acceptsItem(item); // occ one is always ok
    }

    public boolean accepts(XQType valueType)
    {
        XQItemType vitemType = valueType.itemType();
        if (vitemType == XQType.NONE)
            return isOptional(valueType.getOccurrence());

        // That's all: we cannot check the occurrence count in static analysis
        return itemType.accepts(vitemType);
    }

    /**
     * Static typing: check Occurrence strictly
     */
    public boolean acceptsStatic(XQType valueType)
    {
        if(!accepts(valueType))
            return false;
        // we cannot check the occurrence count in static analysis
        int vocc = valueType.getOccurrence();
        switch(occ) {
        case OCC_ZERO_OR_ONE:
            return vocc <= OCC_EXACTLY_ONE;
        case OCC_EXACTLY_ONE:
            return vocc == OCC_EXACTLY_ONE;
        case OCC_ZERO_OR_MORE:
            return true;
        case OCC_ONE_OR_MORE:
            return vocc == OCC_EXACTLY_ONE || vocc == OCC_ONE_OR_MORE;
        }
        return false;
    }

    public XQValue convertFromObject(Object object) throws XQTypeException
    {
        if (!isRepeatable(occ)) {
            return itemType.convertFromObject(object);
        }
        return itemType.convertFromArray(object);
    }

    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        if (!isRepeatable(occ)) {
            return itemType.convertToObject(expr, focus, context);
        }
        try {
            return itemType.convertToArray(expr.eval(focus, context));
        }
        catch (XQTypeException e) {
            context.error(expr, e);
            return null;
        }
    }

    public ItemType getSuperType()
    {
        return getItemType().getSuperType();    // whatever
    }

    public boolean isSubTypeOf(ItemType type)
    {
        return false;   // does not make sense
    }
}
