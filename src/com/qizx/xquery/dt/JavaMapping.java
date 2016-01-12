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

import com.qizx.api.Item;
import com.qizx.api.Node;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XQName;
import com.qizx.xquery.SequenceType;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class JavaMapping
{
    static HashMap classToType = new HashMap();
    static ArrayList abstractTypes = new ArrayList();
    // Notice that we map java classes to *Sequence* types (for the mapping of
    // Java method arguments)
    static {
        classToType(void.class, XQType.NONE.opt);
        classToType(String.class, XQType.STRING.opt);
        classToType(boolean.class, XQType.BOOLEAN.one);
        classToType(Boolean.class, XQType.BOOLEAN.one);
        classToType(double.class, XQType.DOUBLE.one);
        classToType(Double.class, XQType.DOUBLE.one);
        classToType(float.class, XQType.FLOAT.one);
        classToType(Float.class, XQType.FLOAT.one);
        classToType(BigDecimal.class, XQType.DECIMAL.opt);
        classToType(BigInteger.class, XQType.DECIMAL.opt);
        classToType(long.class, XQType.INTEGER.one);
        classToType(Long.class, XQType.INTEGER.one);
        classToType(Integer.class, XQType.INT.one);
        classToType(int.class, XQType.INT.one);
        classToType(short.class, XQType.SHORT.one);
        classToType(Short.class, XQType.SHORT.one);
        classToType(byte.class, XQType.BYTE.one);
        classToType(Byte.class, XQType.BYTE.one);
        classToType(char.class, XQType.CHAR.one);
        classToType(Character.class, XQType.CHAR.one);
        classToType(Date.class, XQType.DATE_TIME.opt);
        classToType(Calendar.class, XQType.DATE_TIME.opt);
        classToType(IQName.class, XQType.QNAME.one);
        classToType(XQName.class, XQType.QNAME.one);
        
        // generic types:
        mapAbstractClass(Node.class, XQType.NODE.opt);
        mapAbstractClass(XQValue.class, XQType.ANY);
        XQItemType domNode = new DomNodeType(null);
        mapAbstractClass(org.w3c.dom.Node.class,
                         new SequenceType(domNode, XQType.OCC_ZERO_OR_ONE));

        // arrays:
        classToType(String[].class, XQType.STRING.star);
        classToType(Item[].class, XQType.ITEM.star);
        classToType(boolean[].class, XQType.BOOLEAN.star);
        classToType(double[].class, XQType.DOUBLE.star);
        classToType(float[].class, XQType.FLOAT.star);
        classToType(long[].class, XQType.INTEGER.star);
        classToType(int[].class, XQType.INT.star);
        classToType(short[].class, XQType.SHORT.star);
        classToType(byte[].class, XQType.BYTE.star);
        classToType(char[].class, XQType.INTEGER.star);
        
        mapAbstractClass(Node[].class, XQType.NODE.star);
        mapAbstractClass(org.w3c.dom.Node[].class,
                         new SequenceType(domNode, XQType.OCC_ZERO_OR_MORE));
        
        // iterators and collections
        mapAbstractClass(Collection.class, XQType.ITEM.star);
        mapAbstractClass(Iterator.class, XQType.ITEM.star);
        mapAbstractClass(Enumeration.class, XQType.ITEM.star);
        mapAbstractClass(Vector.class, XQType.ITEM.star);
        mapAbstractClass(ArrayList.class, XQType.ITEM.star);

    }
    
    // As strange as it may seem, we use the full classname as a key in the
    // classToType map: this is to cope with classes loaded by a different
    // ClassLoader
    private static void classToType(Class classe, XQType type)
    {
        classToType.put(classe.getName(), type);
    }
    
    private static void mapAbstractClass(Class classe, XQType type)
    {
         classToType(classe, type);
         abstractTypes.add(classe); // sequential matching
    }

    /**
     * Gets the XQ type for a declared Java type (argument or return value).
     * Use a simple lookup.
     */
    public static XQItemType getItemType(Class classe)
    {
        String name = classe.getName();
        SequenceType stype = (SequenceType) classToType.get(name);
        if(stype == null)
            return new WrappedObjectType(classe);
        else
            return (XQItemType) stype.getItemType();
    }
    
    /**
     * Gets the XQ type for a concrete Java type (to convert).
     * Use a simple lookup, if it fails, try abstract class mappings.
     */
    public static XQItemType matchingItemType(Class classe)
    {
        SequenceType stype = matchingType(classe);
        if(stype != null)
            return (XQItemType) stype.getItemType();
        return XQType.WRAPPED_OBJECT;
    }

    public static SequenceType matchingType(Class classe)
    {
        String name = classe.getName(); // yep
        SequenceType stype = (SequenceType) classToType.get(name);
        if(stype == null)
            for (int i = 0, size = abstractTypes.size(); i < size; i++) {
                Class aclass = (Class) abstractTypes.get(i);
                if(aclass.isAssignableFrom(classe)) {
                    // cannot be null:
                    stype = (SequenceType) classToType.get(aclass.getName());
                    break;
                }
            }
        return stype;
    }
    
    // Type conversion with recognition of arrays and iterators/collections
    // By default, returns WRAPPED_OBJECT? for non-array types and 
    // item()* for array types.
    public static SequenceType getSequenceType(Class classe)
    {
        SequenceType stype = matchingType(classe);
        if(stype != null)
            return stype;
        if(classe.isArray()) {
            XQItemType itype = getItemType(classe.getComponentType());
            if(itype == null)
                itype = XQType.ITEM;
            return itype.star;
        }
        else if(Iterator.class.isAssignableFrom(classe) ||
                Collection.class.isAssignableFrom(classe) ||
                Enumeration.class.isAssignableFrom(classe)) {
            return XQType.ITEM.star;
        }
        // unknown non-array type:
        return XQType.WRAPPED_OBJECT.opt;
    }
    
    /**
     * Convert Java object to XQuery item.
     * If the type is not specified (null), it is inferred from the object;
     * arrays and collections are not recognized, therefore converted to
     * wrapped objects, not to sequences.
     * @throws XQTypeException 
     */
    public static XQItem convertToItem(Object object, XQItemType type)
        throws XQTypeException
    {
        if (object == null)
            return null;
        if(type == null || type.equals(XQType.ANY) || type.equals(XQType.ITEM)
             || type.equals(XQType.ANY_ATOMIC_TYPE)) {
            type = matchingItemType(object.getClass());
        }
        return type.convertFromObject(object);
    }

    /**
     * Convert Java object to XQuery item.
     * If the type is not specified (null), it is inferred from the object;
     * arrays and collections are recognized and converted to sequences.
     */
    public static XQValue convertToSequence(Object object, SequenceType type)
        throws XQTypeException
    {
        if (object == null)
            return XQValue.empty;
        if (type == null) {
            type = getSequenceType(object.getClass());
        }
        return type.itemType().convertFromArray(object);
    }

    public static boolean isSequence(Object object)
    {
        if(object == null)
            return false;
        SequenceType type = getSequenceType(object.getClass());
        return type != null && XQType.isRepeatable(type.getOccurrence());
    }

}
