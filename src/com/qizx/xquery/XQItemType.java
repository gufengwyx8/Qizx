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
import com.qizx.api.QizxException;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.xdm.BaseNodeFilter;
import com.qizx.xdm.Conversion;
import com.qizx.xquery.dt.NodeType;
import com.qizx.xquery.dt.ObjectArraySequence;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * class ItemType: encompasses AtomicType (anyAtomicType) and NodeType.
 */
public class XQItemType extends XQType
    //implements ItemType
{
    public SequenceType star;
    public SequenceType plus;
    public SequenceType opt;
    public SequenceType one;

    protected XQItemType parent;


    public XQItemType()
    {
        star = new SequenceType(this, OCC_ZERO_OR_MORE);
        plus = new SequenceType(this, OCC_ONE_OR_MORE);
        opt = new SequenceType(this, OCC_ZERO_OR_ONE);
        one = new SequenceType(this, OCC_EXACTLY_ONE);
    }

    public ItemType getItemType()
    {
        return this;
    }

    public XQItemType itemType()
    {
        return this;
    }

    public int getOccurrence()
    {
        return OCC_EXACTLY_ONE;
    }

    public String getShortName()
    {
        return "anySimpleType";
    }

    public ItemType getSuperType()
    {
        return parent;
    }

    // Code used for static checking and evaluation
    public int quickCode()
    { // TODO abstract
        return QT_ANY;
    }

    public static boolean isNumeric(int quickCode)
    {
        return quickCode >= QT_INT && quickCode <= QT_UNTYPED;
    }

    public static boolean isNumeric(XQItemType type)
    {
        return isNumeric(type.quickCode());
    }

    /**
     * SuperType with slight differences wrt XSD: introduces NumericType,
     * MomentType
     */
    public boolean isSuperType(XQItemType subtype)
    {
        return subtype.isSubTypeOf(this);
    }

    /**
     * Derived in the sense of XSD hierarchy.
     */
    public boolean isSubTypeOf(ItemType root)
    {
        XQItemType ty = this;
        for (; ty != null; ty = (XQItemType) ty.getSuperType())
            // not ty == root, because some types have different instances
            if (ty.getClass() == root.getClass())
                return true;
        return false;
    }

    public boolean isOptional()
    {
        return false;
    }

    public boolean isRepeatable()
    {
        return false;
    }

    public XQType getSequenceType(boolean optional, boolean repeatable)
    {
        if(repeatable)
            return optional? this.star : this.plus;
        else
            return optional? this.opt : this.one;
    }

    public int getNodeKind()
    {
        if (!(this instanceof NodeType))
            return ItemType.ATOMIC_TYPE;
        NodeType nt = (NodeType) this;
        int nodetype = nt.nodeFilter == null ? -1 : nt.nodeFilter.getNodeKind();
        return nodetype < 0 ? ItemType.NODE_TYPE : nodetype;
    }

    public QName getNodeName()
    {
        if(!(this instanceof NodeType))
            return null;
        NodeType nt = (NodeType) this;
        return !(nt.nodeFilter instanceof BaseNodeFilter) ?  null
                : ((BaseNodeFilter) nt.nodeFilter).qname;
    }

    public boolean accepts(XQType valueType)
    {
        XQItemType type = valueType.itemType();
        // Unknown type is always accepted:
        if (type == XQType.ITEM)
            return true;
        return type.isSubTypeOf(this);
    }

    public boolean acceptsItem(XQItem item)
    {
        try {
            return accepts(item.getItemType());
        }
        catch (EvaluationException e) {
            return false; // hmm
        }
    }

    /**
     * Returns true if itemType can be promoted to this type.
     */
    public boolean encompasses(XQItemType itemType)
    {
        // default: simply accept sub-types:
        return isSuperType(itemType);
    }

    /**
     * Attempts to cast the current item of the value to this type.
     * 
     * @return if successful, the converted value as a single item sequence.
     * @param context Atttention may be null (automatic cast)
     * @throws XQTypeException if the item's type or value is incompatible.
     */
    public XQValue cast(XQItem item, EvalContext context)
        throws EvaluationException
    {
        throw new XQTypeException("invalid target type for cast: xs:"
                                  + getShortName());
    }

    protected void castException(Exception e)
        throws XQTypeException
    {
        QName errCode = Conversion.ERR_CAST;
        if(e instanceof QizxException) {
            QName err = ((QizxException) e).getErrorCode();
            if(err != null)
                errCode = err;
        }
        String msg = e.getMessage();
        if(e instanceof DateTimeException) {
            msg += " '" + ((DateTimeException) e).parsedInput + "'";
        }
        throw new XQTypeException(errCode, "cannot cast to xs:" + getShortName()
                                           + ": " + msg);
    }

    protected XQValue invalidCast(XQItemType type)
        throws XQTypeException
    {
        throw new XQTypeException("cannot cast to xs:" + getShortName()
                                  + ": invalid type " + type);
    }

    protected XQValue invalidCast(Object object)
        throws XQTypeException
    {
        throw new XQTypeException("cannot cast to xs:" + getShortName()
                                  + ": invalid type " +
                      (object == null? "void" : object.getClass().toString()));
    }

    /**
     * Conversion of external Java array of related type to internal value.
     * @throws XQTypeException 
     */
    public XQValue convertFromArray(Object object) throws XQTypeException
    {
        if(object == null)
            return XQValue.empty;
        if(object instanceof  Object[]) {
            Object[] array = (Object[]) object;
            return new ObjectArraySequence(array, array.length, null);
        }
        else if(object instanceof Iterator) {
            return new ObjectArraySequence((Iterator) object, null);
        }
        else if(object instanceof Enumeration) {
            return new ObjectArraySequence((Enumeration) object, null);
        }
        else if(object instanceof Collection) {
            return new ObjectArraySequence((Collection) object, null);
        }
        else
            return convertFromObject(object);
    }

    /**
     * Conversion of internal value to external Java array of proper type. For
     * example: xs:short produces short[].
     */
    public Object convertToArray(XQValue value)
        throws EvaluationException
    {
        throw new EvaluationException("XQItemType.convertToArray!");
    }

    /**
     * Returns true if item type can be promoted to this type
     */
    public boolean promotable(XQItemType type)
    {
        return false;
    }
}
