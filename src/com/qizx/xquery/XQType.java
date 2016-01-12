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
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.SequenceType;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.BaseNodeFilter;
import com.qizx.xdm.IQName;
import com.qizx.xquery.dt.*;
import com.qizx.xquery.op.Expression;

import java.util.HashMap;

/**
 * Superclass of all XQuery Type representations. Defines basic types.
 */
public abstract class XQType
    implements ItemType
{
    public static QName anyType;
    public static QName anySimpleType;
    public static QName untypedAtomic;
    public static QName untyped;

    public static XQType ANY;

    public static XQItemType NONE;

    public static XQItemType ITEM;
    public static XQItemType FUNCTION;

    public static NodeType NODE;
    public static NodeType ELEMENT;
    public static NodeType DOCUMENT;
    public static NodeType ATTRIBUTE;
    public static NodeType TEXT;
    public static NodeType PI;
    public static NodeType COMMENT;
    public static NodeType NAMESPACE;

    public static AtomicType ATOM; // alias for ANY_ATOMIC_TYPE
    public static AtomicType ANY_ATOMIC_TYPE;

    public static AtomicType MOMENT;
    public static AtomicType TIME;
    public static AtomicType DATE;
    public static AtomicType DATE_TIME;
    public static AtomicType G_DAY;
    public static AtomicType G_MONTH;
    public static AtomicType G_YEAR;
    public static AtomicType G_YEAR_MONTH;
    public static AtomicType G_MONTH_DAY;

    public static AtomicType DURATION;
    public static AtomicType YEAR_MONTH_DURATION;
    public static AtomicType DAY_TIME_DURATION;

    public static AtomicType UNTYPED_ATOMIC;

    public static AtomicType BOOLEAN;

    public static AtomicType BINARY;
    public static AtomicType HEX_BINARY;
    public static AtomicType BASE64_BINARY;

    public static AtomicType NUMERIC;
    public static AtomicType FLOAT;
    public static AtomicType DOUBLE;
    public static AtomicType DECIMAL;
    public static AtomicType INTEGER;
    public static AtomicType NON_POSITIVE_INTEGER;
    public static AtomicType NEGATIVE_INTEGER;
    public static AtomicType LONG;
    public static AtomicType INT;
    public static AtomicType SHORT;
    public static AtomicType BYTE;
    public static AtomicType NON_NEGATIVE_INTEGER;
    public static AtomicType UNSIGNED_LONG;
    public static AtomicType UNSIGNED_INT;
    public static AtomicType UNSIGNED_SHORT;
    public static AtomicType UNSIGNED_BYTE;
    public static AtomicType POSITIVE_INTEGER;
    
    public static AtomicType STRING;
    public static AtomicType NORMALIZED_STRING;
    public static AtomicType TOKEN;
    public static AtomicType LANGUAGE;

    public static AtomicType NAME;
    public static AtomicType NCNAME;
    public static AtomicType ID;
    public static AtomicType IDREF;

    public static AtomicType ENTITY;
    public static AtomicType NMTOKEN;
    public static AtomicType NOTATION;

    public static AtomicType QNAME;
    public static AtomicType ANYURI;

    // xdt: types:
    public static AtomicType OBJECT;
    public static AtomicType WRAPPED_OBJECT; // alias for OBJECT


    public static AtomicType CHAR;

    public static final String ERR_EMPTY_UNEXPECTED =
        "empty sequence not allowed";

    public static final String ERR_TOO_MANY =
        "more than one item is not allowed";

    public static final String ERR_TYPE_MISMATCH =
        "improper item type";

    // Quick Types for evaluation and static checking:

    public static final int QT_ANY = 0;
    public static final int QT_INT = 1;
    public static final int QT_DEC = 2;
    public static final int QT_FLOAT = 3;
    public static final int QT_DOUBLE = 4;
    public static final int QT_UNTYPED = 5;
    public static final int QT_STRING = 6;
    public static final int QT_TIME = 7;
    public static final int QT_DATETIME = 8;
    public static final int QT_DATE = 9;
    public static final int QT_YMDUR = 10;
    public static final int QT_DTDUR = 11;
    public static final int QT_DUR = 12;
    public static final int QT_BOOL = 13;
    public static final int QT_ANYURI = 14;
    public static final int QT_QNAME = 15;

    // Combined quick types:

    public static final int INT_INT = (QT_INT << 4) + QT_INT;
    public static final int INT_DEC = (QT_INT << 4) + QT_DEC;
    public static final int DEC_INT = (QT_DEC << 4) + QT_INT;
    public static final int DEC_DEC = (QT_DEC << 4) + QT_DEC;
    public static final int INT_FLOAT = (QT_INT << 4) + QT_FLOAT;
    public static final int FLOAT_INT = (QT_FLOAT << 4) + QT_INT;
    public static final int DEC_FLOAT = (QT_DEC << 4) + QT_FLOAT;
    public static final int FLOAT_DEC = (QT_FLOAT << 4) + QT_DEC;
    public static final int FLOAT_FLOAT = (QT_FLOAT << 4) + QT_FLOAT;
    public static final int INT_DOUBLE = (QT_INT << 4) + QT_DOUBLE;
    public static final int DOUBLE_INT = (QT_DOUBLE << 4) + QT_INT;
    public static final int DOUBLE_DEC = (QT_DOUBLE << 4) + QT_DEC;
    public static final int DEC_DOUBLE = (QT_DEC << 4) + QT_DOUBLE;
    public static final int FLOAT_DOUBLE = (QT_FLOAT << 4) + QT_DOUBLE;
    public static final int DOUBLE_FLOAT = (QT_DOUBLE << 4) + QT_FLOAT;
    public static final int DOUBLE_DOUBLE = (QT_DOUBLE << 4) + QT_DOUBLE;

    public static final int DATETIME_DATETIME = (QT_DATETIME << 4) + QT_DATETIME;
    public static final int DATETIME_YMDUR = (QT_DATETIME << 4) + QT_YMDUR;
    public static final int DATETIME_DTDUR = (QT_DATETIME << 4) + QT_DTDUR;
    public static final int DATETIME_DOUBLE = (QT_DATETIME << 4) + QT_DOUBLE;
    public static final int DATETIME_INT = (QT_DATETIME << 4) + QT_INT;

    public static final int DATE_DATE = (QT_DATE << 4) + QT_DATE;
    public static final int DATE_YMDUR = (QT_DATE << 4) + QT_YMDUR;
    public static final int DATE_DTDUR = (QT_DATE << 4) + QT_DTDUR;
    public static final int DATE_INT = (QT_DATE << 4) + QT_INT;

    public static final int TIME_TIME = (QT_TIME << 4) + QT_TIME;
    public static final int TIME_DTDUR = (QT_TIME << 4) + QT_DTDUR;

    public static final int DTDUR_DATETIME = (QT_DTDUR << 4) + QT_DATETIME;
    public static final int DTDUR_DATE = (QT_DTDUR << 4) + QT_DATE;
    public static final int DTDUR_TIME = (QT_DTDUR << 4) + QT_TIME;
    public static final int DTDUR_DTDUR = (QT_DTDUR << 4) + QT_DTDUR;
    public static final int DTDUR_DOUBLE = (QT_DTDUR << 4) + QT_DOUBLE;
    public static final int DTDUR_FLOAT = (QT_DTDUR << 4) + QT_FLOAT;
    public static final int DTDUR_DEC = (QT_DTDUR << 4) + QT_DEC;
    public static final int DTDUR_INT = (QT_DTDUR << 4) + QT_INT;
    public static final int DOUBLE_DTDUR = (QT_DOUBLE << 4) + QT_DTDUR;
    public static final int FLOAT_DTDUR = (QT_FLOAT << 4) + QT_DTDUR;
    public static final int DEC_DTDUR = (QT_DEC << 4) + QT_DTDUR;
    public static final int INT_DTDUR = (QT_INT << 4) + QT_DTDUR;

    public static final int YMDUR_DATETIME = (QT_YMDUR << 4) + QT_DATETIME;
    public static final int YMDUR_DATE = (QT_YMDUR << 4) + QT_DATE;
    public static final int YMDUR_YMDUR = (QT_YMDUR << 4) + QT_YMDUR;
    public static final int YMDUR_DOUBLE = (QT_YMDUR << 4) + QT_DOUBLE;
    public static final int YMDUR_FLOAT = (QT_YMDUR << 4) + QT_FLOAT;
    public static final int YMDUR_DEC = (QT_YMDUR << 4) + QT_DEC;
    public static final int YMDUR_INT = (QT_YMDUR << 4) + QT_INT;
    public static final int DOUBLE_YMDUR = (QT_DOUBLE << 4) + QT_YMDUR;
    public static final int FLOAT_YMDUR = (QT_FLOAT << 4) + QT_YMDUR;
    public static final int DEC_YMDUR = (QT_DEC << 4) + QT_YMDUR;
    public static final int INT_YMDUR = (QT_INT << 4) + QT_YMDUR;

    
    
    public static boolean isOptional(int occ)
    {
        return occ == OCC_ZERO_OR_MORE || occ == OCC_ZERO_OR_ONE;
    }

    public static boolean isRepeatable(int occ)
    {
        return occ == OCC_ZERO_OR_MORE || occ == OCC_ONE_OR_MORE;
    }

    public abstract boolean isOptional();
    public abstract boolean isRepeatable();

    /**
     * Dynamic matching of a single item. For optimization of typeswitch.
     */
    public boolean acceptsItem(XQItem item)
    {
        return true;
    }

    /**
     * Static type checking. Tells whether a static expression type can be
     * accepted by this formal type.
     */
    public boolean accepts(XQType expressionType)
    {
        // default for xs:any :
        return true;
    }

    /**
     * Returns a type encompassing types this and that. TODO? optimize
     * @param choice for occurrence computation: true if we consider a choice
     * between the two types rather than a sequence.
     */
    public XQType unionWith(XQType that, boolean choice)
    {
        XQItemType union = itemType(), thatType = that.itemType();
        // NONE or empty() is neutral:
        if (union == XQType.NONE)
            union = thatType;
        else if (thatType != XQType.NONE) {
            // encompasses is different from accepts: does not
            // consider untyped atomic
            if (thatType.encompasses(union))
                union = thatType;
            else if (!union.encompasses(thatType)) {
                // smallest super-type of the two types:
                union = (XQItemType) union.getSuperType();
                for (; union != null && !thatType.isSubTypeOf(union);)
                    union = (XQItemType) union.getSuperType();
            }
        }
        // else : union is this type

        if (union == null) { // / should not happen
            System.err.println("cannot find union of types " + getItemType()
                               + " & " + thatType);
            return null;
        }
        
        // wrong:
        boolean repeatable = this.isRepeatable() || that.isRepeatable();
        if(choice)
            return union.getSequenceType(this.isOptional() || that.isOptional(),
                                         repeatable);
        else
            return union.getSequenceType(this.isOptional() && that.isOptional(),
                                         true);
    }

    /**
     * If this is a sequence type, return the item type, otherwise return the
     * type itself.
     */
    public XQItemType itemType()
    {
        return XQType.ITEM;
    }

    public SequenceType getSequenceType(int occurrence)
    {
        XQItemType type = itemType();
        switch(occurrence) {
        case SequenceType.OCC_ZERO_OR_ONE:
            return type.opt;
        case SequenceType.OCC_EXACTLY_ONE:
            return type;
        case SequenceType.OCC_ZERO_OR_MORE:
            return type.star;
        case SequenceType.OCC_ONE_OR_MORE:
            return type.plus;
        }
        return null; 
    }

    /**
     * If this is a sequence type, return the occurrence indicator, otherwise
     * return OCC_EXACTLY_ONE, except for anyType which is '*'.
     */
    public int getOccurrence()
    {
        return this == XQType.ANY ? OCC_ZERO_OR_MORE : OCC_EXACTLY_ONE;
    }

    public QName getName()
    { // redefined for untypedAtomic
        return IQName.get(NamespaceContext.XSD, getShortName());
    }

    public String getShortName()
    {
        return "anyType";
    }

    public String toString(BasicStaticContext ctx)
    {
        return toString();
    }

    public String toString()
    {
        return "xs:" + getShortName();
    }

    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;
        if(obj instanceof XQType) {
            XQType otype = (XQType) obj;
            if(getItemType() == otype.getItemType() &&
               getOccurrence() == otype.getOccurrence())
                return true;
        }
        return false;
    }

    public int hashCode()
    {
        return super.hashCode() ^ (getOccurrence() << 5);
    }
    
    public abstract ItemType getSuperType();

    public abstract boolean isSubTypeOf(ItemType type);

    public abstract ItemType getItemType();

    /**
     * Conversion of external Java objects to internal values.
     */
    public XQValue convertFromObject(Object object)
        throws XQTypeException
    {
        throw new XQTypeException("unimplemented conversion from " + object);
    }

    /**
     * Conversion of internal values to external Java objects.
     */
    public Object convertToObject(Expression expr, Focus focus,
                                  EvalContext context)
        throws EvaluationException
    {
        return expr.eval(focus, context);
        // return context.error(expr,this+" cannot convert to Java object:
        // "+expr);
    }

    // ----------- predefined types -------------------------------------------

    static HashMap itemTypeMap = new HashMap();

    /**
     * Searches a predefined Item Type by name. Returns null if none found.
     */
    public static XQItemType findItemType(QName typeName)
    {
        return (XQItemType) itemTypeMap.get(typeName);
    }
    
    public static XQItemType findItemType(String simpleName)
    {
        if(simpleName.startsWith("xs:"))
            simpleName = simpleName.substring(3);
        else if(simpleName.startsWith("xdt:object"))
            return WRAPPED_OBJECT;
        return findItemType(IQName.get(NamespaceContext.XSD, simpleName));
    }
    
    private static void defItemType(XQItemType type, XQItemType parent)
    {
        type.parent = parent;
        itemTypeMap.put(type.getName(), type);
    }

    static {
        anyType = IQName.get(NamespaceContext.XSD, "anyType");
        anySimpleType = IQName.get(NamespaceContext.XSD, "anySimpleType");
        // anySimpleType is ITEM
        // anyAtomicType is ATOM
        untypedAtomic = IQName.get(NamespaceContext.XSD, "untypedAtomic");
        untyped = IQName.get(NamespaceContext.XSD, "untyped");

        defItemType(ITEM = new XQItemType(), null);
        ANY = new com.qizx.xquery.SequenceType(ITEM);
        
        defItemType(FUNCTION = new FunctionType(null), ITEM);
        
        defItemType(NODE = new NodeType(null), ITEM);

        ELEMENT = new NodeType(new BaseNodeFilter(Node.ELEMENT, null, null));
        defItemType(ELEMENT, NODE);

        DOCUMENT = new NodeType(new BaseNodeFilter(Node.DOCUMENT, null, null));
        defItemType(DOCUMENT, NODE);

        ATTRIBUTE =
            new NodeType(new BaseNodeFilter(Node.ATTRIBUTE, null, null));
        defItemType(ATTRIBUTE, NODE);

        TEXT = new NodeType(new BaseNodeFilter(Node.TEXT, null, null));
        defItemType(TEXT, NODE);

        PI = new NodeType(new BaseNodeFilter(Node.PROCESSING_INSTRUCTION,
                                             null, null));
        defItemType(PI, NODE);

        COMMENT = new NodeType(new BaseNodeFilter(Node.COMMENT, null, null));
        defItemType(COMMENT, NODE);

        NAMESPACE = new NodeType(new BaseNodeFilter(Node.NAMESPACE, null, null));
        defItemType(NAMESPACE, NODE);

        defItemType(ATOM = ANY_ATOMIC_TYPE = new AtomicType(), ITEM);
        itemTypeMap.put(IQName.get(NamespaceContext.XDT, "anyAtomicType"), ATOM);

        defItemType(NONE = new EmptyType(), ITEM); // special for empty
        OBJECT = WRAPPED_OBJECT = new WrappedObjectType(Object.class);
        defItemType(OBJECT, ATOM);
        defItemType(CHAR = new CharType(), UNSIGNED_INT);

        defItemType(MOMENT = new MomentType(), ATOM); // internal
        defItemType(TIME = new TimeType(), MOMENT);
        defItemType(DATE = new DateType(), MOMENT);
        defItemType(DATE_TIME = new DateTimeType(), MOMENT);
        defItemType(G_DAY = new GDayType(), MOMENT);
        defItemType(G_MONTH = new GMonthType(), MOMENT);
        defItemType(G_YEAR = new GYearType(), MOMENT);
        defItemType(G_YEAR_MONTH = new GYearMonthType(), MOMENT);
        defItemType(G_MONTH_DAY = new GMonthDayType(), MOMENT);
        defItemType(DURATION = new DurationType(), ATOM);
        defItemType(YEAR_MONTH_DURATION = new YearMonthDurationType(),
                    DURATION);
        defItemType(DAY_TIME_DURATION = new DayTimeDurationType(), DURATION);

        defItemType(UNTYPED_ATOMIC = new UntypedAtomicType(), ATOM);

        defItemType(BOOLEAN = new BooleanType(), ATOM);
        defItemType(BINARY = new BinaryType(), ATOM);
        defItemType(HEX_BINARY = new HexBinaryType(), ATOM);
        defItemType(BASE64_BINARY = new Base64BinaryType(), ATOM);

        defItemType(NUMERIC = new NumericType(), ATOM); // internal
        defItemType(FLOAT = new FloatType(), NUMERIC);
        defItemType(DOUBLE = new DoubleType(), NUMERIC);
        defItemType(DECIMAL = new DecimalType(), NUMERIC);
        defItemType(INTEGER = new IntegerType(), DECIMAL);
        defItemType(NON_POSITIVE_INTEGER = new NonPositiveIntegerType(),
                    INTEGER);
        defItemType(NEGATIVE_INTEGER = new NegativeIntegerType(),
                    NON_POSITIVE_INTEGER);
        defItemType(LONG = new LongType(), INTEGER);
        defItemType(INT = new IntType(), LONG);
        defItemType(SHORT = new ShortType(), INT);
        defItemType(BYTE = new ByteType(), SHORT);
        defItemType(NON_NEGATIVE_INTEGER = new NonNegativeIntegerType(),
                    INTEGER);
        defItemType(UNSIGNED_LONG = new UnsignedLongType(),
                    NON_NEGATIVE_INTEGER);
        defItemType(UNSIGNED_INT = new UnsignedIntType(), UNSIGNED_LONG);
        defItemType(UNSIGNED_SHORT = new UnsignedShortType(), UNSIGNED_INT);
        defItemType(UNSIGNED_BYTE = new UnsignedByteType(), UNSIGNED_SHORT);
        defItemType(POSITIVE_INTEGER = new PositiveIntegerType(),
                    NON_NEGATIVE_INTEGER);

        defItemType(STRING = new StringType(), ATOM);
        defItemType(NORMALIZED_STRING = new NormalizedStringType(), STRING);
        defItemType(TOKEN = new TokenType(), NORMALIZED_STRING);
        defItemType(LANGUAGE = new LanguageType(), TOKEN);
        defItemType(NAME = new NameType(), TOKEN);
        defItemType(NCNAME = new NCNameType(), NAME);
        defItemType(ID = new IDType(), NCNAME);
        defItemType(IDREF = new IDREFType(), NCNAME);
        defItemType(ENTITY = new ENTITYType(), NCNAME);
        defItemType(NMTOKEN = new NMTOKENType(), TOKEN);
        defItemType(NOTATION = new NotationType(), ATOM);
        defItemType(QNAME = new QNameType(), ATOM);
        defItemType(ANYURI = new AnyURIType(), ATOM);
    }
}
