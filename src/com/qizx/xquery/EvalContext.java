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
import com.qizx.api.util.time.DateTime;
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.Conversion;
import com.qizx.xdm.IQName;
import com.qizx.xquery.dt.SingleDouble;
import com.qizx.xquery.dt.SingleInteger;
import com.qizx.xquery.dt.SingleItem;
import com.qizx.xquery.dt.SingleNode;
import com.qizx.xquery.dt.SingleString;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.UserFunction;
import com.qizx.xquery.fn.Function.Call;
import com.qizx.xquery.impl.EmptyException;
import com.qizx.xquery.impl.ErrorValue;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.GlobalVariable;
import com.qizx.xquery.op.LocalVariable;
import com.qizx.xquery.op.PathExpr;

import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.Properties;

/**
 * Evaluation context for main query or functions. Holds the local variables.
 * Caution: due to differed evaluation of some expressions (Flowers etc.), the
 * context can persist even after exit of the related function.
 */
public class EvalContext implements ComparisonContext
{
    /*
     * Maximum number of registers for each scalar type (integer, double,
     * string, item)
     */
    public static final int MAX_REGISTER = 4;

    public static final int INT_REGISTER = 0;
    public static final int DOUBLE_REGISTER = INT_REGISTER + MAX_REGISTER;
    public static final int STRING_REGISTER = DOUBLE_REGISTER + MAX_REGISTER;
    public static final int ITEM_REGISTER = STRING_REGISTER + MAX_REGISTER;
    // twice more item registers:
    public static final int LAST_REGISTER = ITEM_REGISTER + 2 * MAX_REGISTER;

    public static final int STOP = 1, TIMEOUT = 2, SUSPEND = 3, TRACING = 4;

    private static final int MAX_STACK_DEPTH = 500;

    private static Properties errorCodes;

    public static final QName DM_ERRCODE = IQName.get("XQDM0001");

    // ------ context members: ----------------
    
    protected EvalContext upContext;
    protected DynamicContext dynCtx;
    protected UserFunction.Signature called;
    protected int depth;

    protected EvalContext closure;

    protected Expression point; // currently evaluated expression
    // NS table visible at current evaluation point: for constructors
    private NamespaceContext constructorNS;

    // copied for quick test:
    private boolean traceExec;
    private Collator defaultCollator; // for efficiency

    protected XQValue[] locals;
    // fast local registers : dont use arrays
    // TODO Value registers
    public long   registerInt0, registerInt1, registerInt2, registerInt3;
    public double registerDouble0, registerDouble1, 
                  registerDouble2, registerDouble3;
    public String registerString0, registerString1, 
                  registerString2, registerString3;
    public XQItem registerItem0, registerItem1, registerItem2, registerItem3,
                  registerItem4, registerItem5, registerItem6, registerItem7;



    // top context for main query
    public EvalContext(DynamicContext dynCtx)
    {
        upContext = null;
        this.dynCtx = dynCtx;

        MainQuery mq = dynCtx.mainQuery;
        int localSize = (mq == null) ? 0 : mq.getLocalSize();
        locals = localSize > 0 ? new XQValue[localSize] : null;
        depth = 1;
        defaultCollator = (mq == null)? null : getCollator(null);
    }

    public EvalContext(int localSize)
    {
        if(localSize > 0)
            locals = new XQValue[localSize];
    }

    // Creates a stack frame for a function:
    public EvalContext subContext(UserFunction.Signature called)
        throws EvaluationException
    {
//        if (depth >= MAX_STACK_DEPTH)
//            throw new EvaluationException("XQuery stack overflow");
        EvalContext ctx = new EvalContext(called.maxStackSize);
        ctx.upContext = this;
        ctx.called = called;
        ctx.dynCtx = dynCtx;
        ctx.depth = depth + 1;
        ctx.traceExec = traceExec;
        return ctx;
    }

    public EvalContext getCallerContext()
    {
        return upContext;
    }

    public EvalContext getClosure()
    {
        return closure;
    }

    public void setClosure(EvalContext closure)
    {
        this.closure = closure;
    }

    public final ModuleContext getStaticContext()
    {
        return dynCtx.mainQuery;
    }

    // --------------------------------------------------------------------

    public XQValue error(String errCode, Expression expression)
        throws EvaluationException
    {
        return error(errCode, expression, getMessageText(errCode));
    }

    public XQValue error(QName errorCode, Expression expression,
                         String message)
        throws EvaluationException
    {
        return error(errorCode, expression, message, null);
    }

    public XQValue error(QName errorCode, Expression expression,
                         String message, Exception err)
        throws EvaluationException
    {
        EvaluationException e = new EvaluationException(errorCode, message, err);
        return error(expression, e);
    }

    public XQValue error(String errCode, Expression expression,
                         EvaluationException err)
        throws EvaluationException
    {
        err.setErrorCode(ModuleContext.xqueryErrorCode(errCode));
        return error(expression, err); 
    }

    public XQValue error(String errCode, Expression expression, String message)
        throws EvaluationException
    {
        EvaluationException e = new EvaluationException(message);
        return error(errCode, expression, e);
    }

    // 'error' must have the XQ error code set
    public XQValue error(Expression origin, EvaluationException error)
        throws EvaluationException
    {
        point = origin;
        dynCtx.lastContext = this;
        
        // Extract stack trace:
        EvaluationStackTrace[] stack = new EvaluationStackTrace[depth];
        EvalContext ctx = this, caller = null;
        for(int d = 0; ctx != null; ctx = caller)
        {
            caller = ctx.upContext;
            stack[d++] = newStackTrace(ctx.point, caller);
        }
        error.setStack(stack);

        throw error;
    }
    
    public static synchronized String getMessageText(String codeName)
    {
        if(errorCodes == null) {
            errorCodes = new Properties();
            InputStream res = ModuleContext.class
                                .getResourceAsStream("XQErrorCodes.properties");
            if(res != null) {
                try {
                    errorCodes.load(res);
                    res.close();
                }
                catch (IOException e) {
                    System.err.println("*** Cannot load XQuery error codes");
                }
            }
            else return null;
        }
        return errorCodes.getProperty(codeName);
    }


    public EvaluationStackTrace newStackTrace(Expression ctxPoint,
                                              EvalContext caller)
    {
        if(ctxPoint == null || ctxPoint.module == null) {
            return new EvaluationStackTrace(null, "<null>", 0, "?", 0, -1);
        }
        
        String module = ctxPoint.module.getPhysicalURI();
        int offset = ctxPoint.offset;
        String source = ctxPoint.module.getSource();
        int lnum = 1;
        int eol = source.indexOf('\n'), sol = 0;    // Mac?
        for(; eol >= 0 && eol < offset; ++ lnum) {
            sol = eol + 1;
            eol = source.indexOf('\n', eol + 1);
        }
        String srcLine = 
            source.substring(sol, eol < 0 ? source.length() : eol);
        String signature = null;
        if(caller != null && caller.point instanceof UserFunction.Call) {
            UserFunction.Call call = (UserFunction.Call) caller.point;
            if(call.prototype != null)
                signature = call.prototype.toString(getInScopeNS());
        }        
        return new EvaluationStackTrace(signature, module, lnum, srcLine,
                                        offset - sol, offset);
    }

    public void checkArgType(XQItem item, 
                             Expression origin, int rank, ItemType reqType)
        throws EvaluationException
    {
        XQItemType type = item.getItemType();
        if(type != reqType)
            badTypeForArg(type, origin, rank, reqType.toString());
    }

    // general arg error
    public void badTypeForArg(XQItemType type, Expression origin, int rank,
                              String reqType)
        throws EvaluationException
    {
        error(Expression.ERRC_BADTYPE, origin, 
              "invalid type " + type + " for argument"
              + (rank < 0 ? "" : (" " + (rank + 1)))
              + " expecting " + reqType);
    }
    
    // this one is used rarely: min, max, avg etc
    // fairly subtle difference with preceding method!!! (error code)
    public void invalidArgType(Expression origin, int rank, ItemType type,
                               String expected)
        throws EvaluationException
    {
        error(Function.ERR_ARGTYPE, origin,
              "invalid type " + type + " for argument"
              + (rank < 0 ? "" : (" " + (rank + 1)))
              + (expected == null? "" : (", expecting " + expected)));
    }

    public void incompatibleType(int rank, String type, Call origin)
        throws EvaluationException
    {
        // OOPS: FORG0006 conflicts with XPTY0004 ?
        error(Function.ERR_ARGTYPE, origin,
              "item type (rank " + rank + ") incompatible with " + type);
    }


    public String getSignature()
    {
        return called == null ? null
                         : called.toString(getStaticContext().getInScopeNS());
    }

    public final void at(Expression pt)
        throws EvaluationException
    {
        if (pt != null) {
            point = pt;
        }

        if (dynCtx.monitorMode != 0) {
            switch (dynCtx.monitorMode) {
            case STOP:
                throw new EvaluationException(EvaluationException.CANCELLED,
                                              "cancelled");
                
            case TIMEOUT:
                throw new EvaluationException(EvaluationException.TIME_LIMIT,
                                              "time limit reached");
                
            case SUSPEND:
                dynCtx.lastContext = this;
                // block execution:
                synchronized (dynCtx) {
                    try {
                        dynCtx.wait();
                    }
                    catch (InterruptedException e) {
                    }
                }
                break;
            }
        }
    }

    public Expression getCurrentLocation()
    {
        return point;
    }

    public XQValue loadGlobal(GlobalVariable var)
        throws EvaluationException
    {
        ModuleContext staticContext = getStaticContext();
        GlobalVariable curInitVar = null;
        if(staticContext == null)   // constant reduction
            error("XPDY0002", var, "variable in constant expression");
        else
            curInitVar = staticContext.getCurInitVar();
        
        XQValue v = (XQValue) dynCtx.globals.get(var);
        if (v == null) {
//            if(var == curInitVar)
//                error("XQST0054", var,
//                      "variable $" + staticContext.prefixedName(var.name)
//                      + " depends on itself");
//            else
                error("XPDY0002", var,
                      "variable $" + staticContext.prefixedName(var.name)
                          + " has no specified value");
            return null; // dummy
        }
        return v.bornAgain(); // OPTIM?
    }

    public void setGlobal(GlobalVariable var, XQValue value)
    {
        dynCtx.globals.put(var, value);
    }

    public EvalContext closure(int upLevel)
        throws EvaluationException
    {
        EvalContext ctx = closure;
        int up = upLevel;
        for(; ctx != null && up > 1; --up)
            ctx = ctx.closure;
        if(ctx == null)
            throw new EvaluationException("OOPS: cannot reached closure " + upLevel);
        return ctx;
    }

    public XQValue loadLocal(int address)
        throws EvaluationException
    {
        switch (address) {
        case INT_REGISTER + 0:
            return new SingleInteger(registerInt0);
        case INT_REGISTER + 1:
            return new SingleInteger(registerInt1);
        case INT_REGISTER + 2:
            return new SingleInteger(registerInt2);
        case INT_REGISTER + 3:
            return new SingleInteger(registerInt3);
        case DOUBLE_REGISTER + 0:
            return new SingleDouble(registerDouble0);
        case DOUBLE_REGISTER + 1:
            return new SingleDouble(registerDouble1);
        case DOUBLE_REGISTER + 2:
            return new SingleDouble(registerDouble2);
        case DOUBLE_REGISTER + 3:
            return new SingleDouble(registerDouble3);
        case STRING_REGISTER + 0:
            return new SingleString(registerString0);
        case STRING_REGISTER + 1:
            return new SingleString(registerString1);
        case STRING_REGISTER + 2:
            return new SingleString(registerString2);
        case STRING_REGISTER + 3:
            return new SingleString(registerString3);
        case ITEM_REGISTER + 0:
            return new SingleItem(registerItem0);
        case ITEM_REGISTER + 1:
            return new SingleItem(registerItem1);
        case ITEM_REGISTER + 2:
            return new SingleItem(registerItem2);
        case ITEM_REGISTER + 3:
            return new SingleItem(registerItem3);
        case ITEM_REGISTER + 4:
            return new SingleItem(registerItem4);
        case ITEM_REGISTER + 5:
            return new SingleItem(registerItem5);
        case ITEM_REGISTER + 6:
            return new SingleItem(registerItem6);
        case ITEM_REGISTER + 7:
            return new SingleItem(registerItem7);
        default:
            // OPTIM: if a var is used only once, dont clone it by bornAgain()
            // -System.err.println("load "+address+" "+locals[ address -
            // LAST_REGISTER ]);
            return locals[address - LAST_REGISTER].bornAgain();
        }
    }

    public long loadLocalInteger(int address)
        throws EvaluationException
    {
        switch (address) {
        case INT_REGISTER + 0:
             return registerInt0;
        case INT_REGISTER + 1:
            return registerInt1;
        case INT_REGISTER + 2:
            return registerInt2;
        case INT_REGISTER + 3:
            return registerInt3;
        case DOUBLE_REGISTER + 0:
            return (long) registerDouble0;
        case DOUBLE_REGISTER + 1:
            return (long) registerDouble1;
        case DOUBLE_REGISTER + 2:
            return (long) registerDouble2;
        case DOUBLE_REGISTER + 3:
            return (long) registerDouble3;
        case STRING_REGISTER + 0:
            return Conversion.toInteger(registerString0);
        case STRING_REGISTER + 1:
            return Conversion.toInteger(registerString1);
        case STRING_REGISTER + 2:
            return Conversion.toInteger(registerString2);
        case STRING_REGISTER + 3:
            return Conversion.toInteger(registerString3);
        case ITEM_REGISTER + 0:
            return registerItem0.getInteger();
        case ITEM_REGISTER + 1:
            return registerItem1.getInteger();
        case ITEM_REGISTER + 2:
            return registerItem2.getInteger();
        case ITEM_REGISTER + 3:
            return registerItem3.getInteger();
        case ITEM_REGISTER + 4:
            return registerItem4.getInteger();
        case ITEM_REGISTER + 5:
            return registerItem5.getInteger();
        case ITEM_REGISTER + 6:
            return registerItem6.getInteger();
        case ITEM_REGISTER + 7:
            return registerItem7.getInteger();
        default:
            XQValue v = locals[address - LAST_REGISTER].bornAgain();
            if (!v.next())
                throw new XQTypeException(XQType.ERR_EMPTY_UNEXPECTED);
            long item = v.getInteger();
            if (v.next())
                throw new XQTypeException(XQType.ERR_TOO_MANY);
            return item;
        }
    }

    public double loadLocalDouble(int address)
        throws EvaluationException
    {
        switch (address) {
        case INT_REGISTER + 0:
            return registerInt0;
        case INT_REGISTER + 1:
            return registerInt1;
        case INT_REGISTER + 2:
            return registerInt2;
        case INT_REGISTER + 3:
            return registerInt3;

        case DOUBLE_REGISTER + 0:
            return registerDouble0;
        case DOUBLE_REGISTER + 1:
            return registerDouble1;
        case DOUBLE_REGISTER + 2:
            return registerDouble2;
        case DOUBLE_REGISTER + 3:
            return registerDouble3;

        case STRING_REGISTER + 0:
            return Conversion.toDouble(registerString0);
        case STRING_REGISTER + 1:
            return Conversion.toDouble(registerString1);
        case STRING_REGISTER + 2:
            return Conversion.toDouble(registerString2);
        case STRING_REGISTER + 3:
            return Conversion.toDouble(registerString3);

        case ITEM_REGISTER + 0:
            return registerItem0.getDouble();
        case ITEM_REGISTER + 1:
            return registerItem1.getDouble();
        case ITEM_REGISTER + 2:
            return registerItem2.getDouble();
        case ITEM_REGISTER + 3:
            return registerItem3.getDouble();
        case ITEM_REGISTER + 4:
            return registerItem4.getDouble();
        case ITEM_REGISTER + 5:
            return registerItem5.getDouble();
        case ITEM_REGISTER + 6:
            return registerItem6.getDouble();
        case ITEM_REGISTER + 7:
            return registerItem7.getDouble();
        default:
            XQValue v = locals[address - LAST_REGISTER].bornAgain();
            if (!v.next())
                // throw new TypeException(Type.ERR_EMPTY_UNEXPECTED);
                throw EmptyException.instance();
            double item = v.getDouble();
            if (v.next())
                throw new XQTypeException(XQType.ERR_TOO_MANY);
            return item;
        }
    }

    public String loadLocalString(int address)
        throws EvaluationException
    {
        switch (address) {
        case INT_REGISTER + 0:
            return Conversion.toString(registerInt0);
        case INT_REGISTER + 1:
            return Conversion.toString(registerInt1);
        case INT_REGISTER + 2:
            return Conversion.toString(registerInt2);
        case INT_REGISTER + 3:
            return Conversion.toString(registerInt3);

        case DOUBLE_REGISTER + 0:
            return Conversion.toString(registerDouble0);
        case DOUBLE_REGISTER + 1:
            return Conversion.toString(registerDouble1);
        case DOUBLE_REGISTER + 2:
            return Conversion.toString(registerDouble2);
        case DOUBLE_REGISTER + 3:
            return Conversion.toString(registerDouble3);

        case STRING_REGISTER + 0:
            return registerString0;
        case STRING_REGISTER + 1:
            return registerString1;
        case STRING_REGISTER + 2:
            return registerString2;
        case STRING_REGISTER + 3:
            return registerString3;

        case ITEM_REGISTER + 0:
            return registerItem0.getString();
        case ITEM_REGISTER + 1:
            return registerItem1.getString();
        case ITEM_REGISTER + 2:
            return registerItem2.getString();
        case ITEM_REGISTER + 3:
            return registerItem3.getString();
        case ITEM_REGISTER + 4:
            return registerItem4.getString();
        case ITEM_REGISTER + 5:
            return registerItem5.getString();
        case ITEM_REGISTER + 6:
            return registerItem6.getString();
        case ITEM_REGISTER + 7:
            return registerItem7.getString();
        default:
            XQValue v = locals[address - LAST_REGISTER].bornAgain();
            if (!v.next())
                return null;
            String item = v.getString();
            if (v.next())
                throw new XQTypeException(XQType.ERR_TOO_MANY);
            return item;
        }
    }

    public XQItem loadLocalItem(int address)
        throws EvaluationException
    {
        switch (address) {
        case INT_REGISTER + 0:
            return new SingleInteger(registerInt0);
        case INT_REGISTER + 1:
            return new SingleInteger(registerInt1);
        case INT_REGISTER + 2:
            return new SingleInteger(registerInt2);
        case INT_REGISTER + 3:
            return new SingleInteger(registerInt3);

        case DOUBLE_REGISTER + 0:
            return new SingleDouble(registerDouble0);
        case DOUBLE_REGISTER + 1:
            return new SingleDouble(registerDouble1);
        case DOUBLE_REGISTER + 2:
            return new SingleDouble(registerDouble2);
        case DOUBLE_REGISTER + 3:
            return new SingleDouble(registerDouble3);

        case STRING_REGISTER + 0:
            return new SingleString(registerString0);
        case STRING_REGISTER + 1:
            return new SingleString(registerString1);
        case STRING_REGISTER + 2:
            return new SingleString(registerString2);
        case STRING_REGISTER + 3:
            return new SingleString(registerString3);

        case ITEM_REGISTER + 0:
            return registerItem0;
        case ITEM_REGISTER + 1:
            return registerItem1;
        case ITEM_REGISTER + 2:
            return registerItem2;
        case ITEM_REGISTER + 3:
            return registerItem3;
        case ITEM_REGISTER + 4:
            return registerItem4;
        case ITEM_REGISTER + 5:
            return registerItem5;
        case ITEM_REGISTER + 6:
            return registerItem6;
        case ITEM_REGISTER + 7:
            return registerItem7;
        default:
            XQValue v = locals[address - LAST_REGISTER].bornAgain();
            if (!v.next())
                return null;
            XQItem item = v.getItem();
            if (v.next())
                throw new XQTypeException(XQType.ERR_TOO_MANY);
            return item;
        }
    }

    // Used for let and function parameters
    public void storeLocal(LocalVariable var, 
                           Expression expr, XQType dynamicType,
                           boolean conversion, Focus focus, EvalContext calling)
        throws EvaluationException
    {
        // - calling.at(expr);
        switch (var.address) {
        case INT_REGISTER + 0:
            registerInt0 = expr.evalAsInteger(focus, calling);
            break;
        case INT_REGISTER + 1:
            registerInt1 = expr.evalAsInteger(focus, calling);
            break;
        case INT_REGISTER + 2:
            registerInt2 = expr.evalAsInteger(focus, calling);
            break;
        case INT_REGISTER + 3:
            registerInt3 = expr.evalAsInteger(focus, calling);
            break;

        case DOUBLE_REGISTER + 0:
            registerDouble0 = expr.evalAsDouble(focus, calling);
            break;
        case DOUBLE_REGISTER + 1:
            registerDouble1 = expr.evalAsDouble(focus, calling);
            break;
        case DOUBLE_REGISTER + 2:
            registerDouble2 = expr.evalAsDouble(focus, calling);
            break;
        case DOUBLE_REGISTER + 3:
            registerDouble3 = expr.evalAsDouble(focus, calling);
            break;

        case STRING_REGISTER + 0:
            registerString0 = expr.evalAsString(focus, calling);
            break;
        case STRING_REGISTER + 1:
            registerString1 = expr.evalAsString(focus, calling);
            break;
        case STRING_REGISTER + 2:
            registerString2 = expr.evalAsString(focus, calling);
            break;
        case STRING_REGISTER + 3:
            registerString3 = expr.evalAsString(focus, calling);
            break;

        case ITEM_REGISTER + 0:
            registerItem0 = expr.evalAsItem(focus, calling);
            break;
        case ITEM_REGISTER + 1:
            registerItem1 = expr.evalAsItem(focus, calling);
            break;
        case ITEM_REGISTER + 2:
            registerItem2 = expr.evalAsItem(focus, calling);
            break;
        case ITEM_REGISTER + 3:
            registerItem3 = expr.evalAsItem(focus, calling);
            break;
        case ITEM_REGISTER + 4:
            registerItem4 = expr.evalAsItem(focus, calling);
            break;
        case ITEM_REGISTER + 5:
            registerItem5 = expr.evalAsItem(focus, calling);
            break;
        case ITEM_REGISTER + 6:
            registerItem6 = expr.evalAsItem(focus, calling);
            break;
        case ITEM_REGISTER + 7:
            registerItem7 = expr.evalAsItem(focus, calling);
            break;

        default: // general case
            try {
                XQValue value = expr.eval(focus, calling);
                // -System.err.println(">>> value "+value+" type
                // "+dynamicType);

                // force checking and expansion into an ArraySequence:
                // BUT dont do that if there is only one use of the value
                // AND no dynamic type-checking
                if(dynamicType != null || var.uses > 1)
                    value = value.checkTypeExpand(dynamicType, this, 
                                                  conversion, true);
                if (value instanceof ErrorValue)
                    throw new XQTypeException(((ErrorValue) value).getReason());
                locals[var.address - LAST_REGISTER] = value;
            }
            catch (XQTypeException err) {
                error(expr, err);
            }
            break;
        }
    }

    // Store a value already evaluated: for, typeswitch
    public void storeLocal(int address, XQValue value,
                           boolean currentItem, XQType dynamicType)
        throws EvaluationException
    {
        // check non empty
        if (address < LAST_REGISTER && !currentItem && !value.next())
            throw new XQTypeException(XQType.ERR_EMPTY_UNEXPECTED);
        switch (address) {
        case INT_REGISTER + 0:
            registerInt0 = value.getInteger();
            break;
        case INT_REGISTER + 1:
            registerInt1 = value.getInteger();
            break;
        case INT_REGISTER + 2:
            registerInt2 = value.getInteger();
            break;
        case INT_REGISTER + 3:
            registerInt3 = value.getInteger();
            break;

        case DOUBLE_REGISTER + 0:
            registerDouble0 = value.getDouble();
            break;
        case DOUBLE_REGISTER + 1:
            registerDouble1 = value.getDouble();
            break;
        case DOUBLE_REGISTER + 2:
            registerDouble2 = value.getDouble();
            break;
        case DOUBLE_REGISTER + 3:
            registerDouble3 = value.getDouble();
            break;

        case STRING_REGISTER + 0:
            registerString0 = value.getString();
            break;
        case STRING_REGISTER + 1:
            registerString1 = value.getString();
            break;
        case STRING_REGISTER + 2:
            registerString2 = value.getString();
            break;
        case STRING_REGISTER + 3:
            registerString3 = value.getString();
            break;

        case ITEM_REGISTER + 0:
            registerItem0 = value.getItem();
            break;
        case ITEM_REGISTER + 1:
            registerItem1 = value.getItem();
            break;
        case ITEM_REGISTER + 2:
            registerItem2 = value.getItem();
            break;
        case ITEM_REGISTER + 3:
            registerItem3 = value.getItem();
            break;
        case ITEM_REGISTER + 4:
            registerItem4 = value.getItem();
            break;
        case ITEM_REGISTER + 5:
            registerItem5 = value.getItem();
            break;
        case ITEM_REGISTER + 6:
            registerItem6 = value.getItem();
            break;
        case ITEM_REGISTER + 7:
            registerItem7 = value.getItem();
            break;

        default:
            if (dynamicType != null && !dynamicType.acceptsItem(value))
                throw new XQTypeException("invalid type");

            locals[address - LAST_REGISTER] =
                currentItem ? new SingleItem(value.getItem()) : value;
            break;
        }
    }

    public void storeLocalItem(int address, XQItem value)
        throws EvaluationException
    {
        switch (address) {
        case INT_REGISTER + 0:
            registerInt0 = value.getInteger();
            break;
        case INT_REGISTER + 1:
            registerInt1 = value.getInteger();
            break;
        case INT_REGISTER + 2:
            registerInt2 = value.getInteger();
            break;
        case INT_REGISTER + 3:
            registerInt3 = value.getInteger();
            break;
            
        case DOUBLE_REGISTER + 0:
            registerDouble0 = value.getDouble();
            break;
        case DOUBLE_REGISTER + 1:
            registerDouble1 = value.getDouble();
            break;
        case DOUBLE_REGISTER + 2:
            registerDouble2 = value.getDouble();
            break;
        case DOUBLE_REGISTER + 3:
            registerDouble3 = value.getDouble();
            break;

        case STRING_REGISTER + 0:
            registerString0 = value.getString();
            break;
        case STRING_REGISTER + 1:
            registerString1 = value.getString();
            break;
        case STRING_REGISTER + 2:
            registerString2 = value.getString();
            break;
        case STRING_REGISTER + 3:
            registerString3 = value.getString();
            break;

        case ITEM_REGISTER + 0:
            registerItem0 = value;
            break;
        case ITEM_REGISTER + 1:
            registerItem1 = value;
            break;
        case ITEM_REGISTER + 2:
            registerItem2 = value;
            break;
        case ITEM_REGISTER + 3:
            registerItem3 = value;
            break;
        case ITEM_REGISTER + 4:
            registerItem4 = value;
            break;
        case ITEM_REGISTER + 5:
            registerItem5 = value;
            break;
        case ITEM_REGISTER + 6:
            registerItem6 = value;
            break;
        case ITEM_REGISTER + 7:
            registerItem7 = value;
            break;
        default: // if too many int variables
            locals[address - LAST_REGISTER] = new SingleItem(value);
            break;
        }
    }


    public void storeLocalInteger(int address, long value)
    {
        switch (address) {
        case INT_REGISTER + 0:
            registerInt0 = value;
            break;
        case INT_REGISTER + 1:
            registerInt1 = value;
            break;
        case INT_REGISTER + 2:
            registerInt2 = value;
            break;
        case INT_REGISTER + 3:
            registerInt3 = value;
            break;
        default: // if too many int variables
            locals[address - LAST_REGISTER] = new SingleInteger(value);
            break;
        }
    }

    public void storeScore(int address, double fulltextScore)
        throws EvaluationException
    {
        if(fulltextScore < 0)   // must be done somewhere
            fulltextScore = 0;
        switch (address) {
        case DOUBLE_REGISTER + 0:
            registerDouble0 = fulltextScore;
            break;
        case DOUBLE_REGISTER + 1:
            registerDouble1 = fulltextScore;
            break;
        case DOUBLE_REGISTER + 2:
            registerDouble2 = fulltextScore;
            break;
        case DOUBLE_REGISTER + 3:
            registerDouble3 = fulltextScore;
            break;
        default:
            locals[address - LAST_REGISTER] = new SingleDouble(fulltextScore);
        }
    }

    public XQValue getDocument(String uri)
        throws EvaluationException
    {
        // if doc not found an exception is raised
        try {
            return new SingleNode(dynCtx.getDocument(uri));
        }
        catch (DataModelException e) {
            throw new EvaluationException(e.getMessage(), e);
        }
    }

    public Collator getCollator(String uri)
    {
        return dynCtx.getCollator(uri);
    }

    public Collator getCollator()
    {
        return defaultCollator;
    }

    public DateTime getCurrentDate()
    {
        return dynCtx.getCurrentDate();
    }

    public int getImplicitTimezone()
    {
        return dynCtx.getImplicitTimezone();
    }
    
    public TraceObserver getTraceListener()
    {
        return dynCtx.traceListener;
    }
    

    public XQValue evalPath(PathExpr path, Focus focus)
        throws EvaluationException
    {
        return dynCtx.eval(path, focus, this);
    }

    public boolean emptyGreatest()
    {
        return dynCtx.mainQuery.getDefaultOrderEmptyGreatest();
    }

    public boolean sObs()
    {
        return dynCtx.mainQuery.getStrictCompliance();
    }

    public DynamicContext dynamicContext()
    {
        return dynCtx;
    }
    
    public Object getProperty(String name)
    {
        return dynCtx.getProperty(name);
    }


    public NamespaceContext getInScopeNS()
    {
        return constructorNS != null && constructorNS.size() > 0 ?
                constructorNS : getStaticContext().getInScopeNS();
    }

    public void setInScopeNS(NamespaceContext constructorNS)
    {
        this.constructorNS = constructorNS;
    }

    public Updates haveUpdateList()
    {
        return dynamicContext().haveUpdateList();
    }
}
