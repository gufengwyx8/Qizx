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
package com.qizx.xquery.fn;

import com.qizx.api.EvaluationException;
import com.qizx.api.QName;
import com.qizx.api.XQueryContext;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.IQName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.EmptyType;
import com.qizx.xquery.dt.WrappedObjectType;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.FunctionItemCall;

/**
 * Description of the prototype of an operator or a function.
 */
public class Prototype 
    extends Expression // (only for locating Signature)
{
    public QName qname;

    public XQType returnType;
    public XQType declaredReturnType;

    // Argument list: number, name and declared type:
    public int argCnt = 0;
    public QName[]  argNames = new QName[3];
    public XQType[] argTypes = new XQType[3];

    public boolean vararg = false; // allows more than argCnt arguments
    public boolean hidden = false;
    
    public static int NO_MATCH = 1000;
    
    // actual builtin functions, or UserFunction.Call or JavaFunction.Call
    public Class implem;


    public Prototype(QName name, XQType returnType, Class implem, boolean vararg)
    {
        this.qname = name;
        this.returnType = returnType;
        this.implem = implem;
        this.vararg = vararg;
    }

    public Prototype(QName qname, XQType returnType, Class classe)
    {
        this(qname, returnType, classe, false);
    }

    public static Prototype op(String name, XQType returnType, Class classe)
    {
        return new Prototype(IQName.get(NamespaceContext.OP, name),
                             returnType, classe);
    }

    public static Prototype fn(String name, XQType returnType, Class classe)
    {
        return new Prototype(IQName.get(NamespaceContext.FN, name),
                             returnType, classe);
    }

    public static Prototype varfn(String name, XQType returnType, Class classe)
    {
        return new Prototype(IQName.get(NamespaceContext.FN, name),
                             returnType, classe, true);
    }

    public static Prototype xs(String name, XQType returnType, Class classe)
    {
        return new Prototype(IQName.get(NamespaceContext.XSD, name),
                             returnType, classe);
    }

    public Prototype hidden()
    {
        this.hidden = true;
        return this;
    }

    public Prototype arg(String name, XQType type)
    {
        return arg(IQName.get(name), type);
    }

    public Prototype arg(QName name, XQType type)
    {
        if (argCnt >= argNames.length) {
            QName[] oldn = argNames;
            argNames = new QName[argNames.length + 3];
            System.arraycopy(oldn, 0, argNames, 0, oldn.length);
            XQType[] oldt = argTypes;
            argTypes = new XQType[argTypes.length + 3];
            System.arraycopy(oldt, 0, argTypes, 0, oldt.length);
        }
        argNames[argCnt] = name;
        argTypes[argCnt] = type;
        ++argCnt;
        return this;
    }

    // redefined in JavaFunction and UserFunction
    public Function.Call instanciate(Expression[] actualArguments)
    {
        try {
            Function.Call fc = (Function.Call) implem.newInstance();
            fc.prototype = this;
            fc.args = actualArguments;
            return fc;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // dynamic invocation: redefinable
    public XQValue invoke(Expression[] args, Expression call,
                          EvalContext lexicalContext, 
                          Focus focus, EvalContext context)
        throws EvaluationException
    {
        if(!accepts(args.length))
            context.error("XPTY0004", call, "invalid number of arguments");
        // needs to create a call, no way around
        Function.Call fc = instanciate(args);
        fc.module = call.module;
        fc.offset = call.offset;
        return fc.eval(null, context);
    }

    public String getArgName(int arg, ModuleContext ctx)
    {
        return ctx.prefixedName(argNames[arg]);
    }

    public static String displayName(QName name, NamespaceContext ctx)
    {
        if(ctx != null)
            return ctx.prefixedName(name);
        else
            return name.getLocalPart();
    }

    public String displayName(NamespaceContext ctx)
    {
        if (NamespaceContext.OP == qname.getNamespaceURI())
            return "operator " + qname.getLocalPart();
        else 
            return displayName(qname, ctx);
    }

    public String toString()
    {
        return toString(null);
    }
    
    public String toString(NamespaceContext ctx)
    {
        StringBuffer buf = new StringBuffer();
        if(qname != null)
            buf.append(displayName(ctx));
        else
            buf.append(" function");
            
        buf.append(" ( ");
        for (int a = 0; a < argCnt; a++) {
            if (a > 0)
                buf.append(", ");
            buf.append('$');
            buf.append(displayName(argNames[a], ctx));
            buf.append(" as ").append(argTypes[a].toString());
        }
        if (vararg)
            buf.append(" ...");
        return buf.append(" ) as ").append(returnType.toString()).toString();
    }

    public boolean accepts(int argCount)
    {
        return argCnt == argCount || (vararg && argCount >= argCnt);
    }
    
    /**
     * Tests an argument list for the corresponding formal argument types.
     * Raises no error. 
     */
    public boolean checkArgCount(Expression[] actualArguments)
    {
        if (!accepts(actualArguments.length))
            return false;

        // DISABLED to comply with XQ standard
        // for(int a = 0; a < argCnt; a++)
        // if(!checkArg( a, actualArguments[a] ))
        // return false;
        return true;
    }
    
    /**
     * Computes a "distance" cumulating type distances of arguments
     */
    public int matchingDistance(Expression[] actualArguments)
    {
        // need to have the same 
        if (actualArguments.length != argCnt
            && !(vararg && actualArguments.length >= argCnt))
                return NO_MATCH;
        // sum matching distances of arguments
        int dist = 0;
        for (int i = 0; i < argCnt; i++) {
            XQType actualType = actualArguments[i].getType();
            dist += typeDistance(actualType, argTypes[i]);
        }
        
        return dist;
    }

    private int typeDistance(XQType actualType, XQType declaredType)
    {
        if(actualType == null || declaredType == null) {
            // abnormal
            System.err.println("OOPS Type matching: " + actualType
                               + " vs " + declaredType);
            return NO_MATCH;
        }
        XQItemType acType = actualType.itemType();
        XQItemType decType = declaredType.itemType();
        if(acType == decType)
            return 0;   // perfect match, dont care about occurrence factor
        if(acType.isSubTypeOf(decType))
            return 1;   // nearly perfect match
        
        switch(acType.quickCode()) {
        case XQType.QT_INT:
        case XQType.QT_DEC:
            if(decType.isSubTypeOf(XQItemType.DECIMAL) ||
               decType == XQItemType.FLOAT || decType == XQItemType.DOUBLE)
                return 2;   // good match
            return NO_MATCH;
            
        case XQType.QT_DOUBLE:
        case XQType.QT_FLOAT:
            if(decType == XQItemType.FLOAT || decType == XQItemType.DOUBLE)
                return 1;   // good match
            if(decType.isSubTypeOf(XQItemType.DECIMAL))
                return 2;
            return stringMatch(decType);

        case XQType.QT_DTDUR:
        case XQType.QT_YMDUR:
        case XQType.QT_DUR:
            if(decType.isSubTypeOf(XQItemType.DURATION))
                return 1;
            return stringMatch(decType);

        case XQType.QT_DATE:
        case XQType.QT_DATETIME:
        case XQType.QT_TIME:
            if(decType.isSubTypeOf(XQItemType.DATE_TIME))
                return 1;
            return stringMatch(decType);

        case XQType.QT_STRING:
        case XQType.QT_ANYURI:
            return stringMatch(decType);
            
        case XQType.QT_UNTYPED:
            return decType.isSubTypeOf(XQItemType.STRING) ? 1 : NO_MATCH;
            
        default:
            if(decType == XQItemType.ITEM || 
               acType == XQItemType.ITEM || 
               acType instanceof EmptyType)
                return 1;   // leave it to runtime
            if(acType instanceof WrappedObjectType && 
               decType instanceof WrappedObjectType)
            {
                // consider wrapped Java types:
                Class aClass = ((WrappedObjectType) acType).getWrappedClass();
                Class dClass = ((WrappedObjectType) decType).getWrappedClass();
                if(aClass == dClass)
                    return 0;
                if(dClass != null && dClass.isAssignableFrom(aClass))
                    return 1;
                // else fail
            }
            return NO_MATCH;
        }
    }

    private int stringMatch(XQItemType decType)
    {
        return decType.isSubTypeOf(XQItemType.STRING) ? 2 : NO_MATCH;
    }

    public Expression child(int rank)
    {
        return null; 
    }
}
