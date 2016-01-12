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
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.SingleDouble;

/**
 *  Implementation of function fn:floor.
 */
public class MathFun extends Function
{
    static Prototype[] protos = {
        Prototype.fn("pi", XQType.NUMERIC.opt, PI.class),
        
        Prototype.fn("tan", XQType.NUMERIC.opt, TanRT.class)
                 .arg("arg", XQType.NUMERIC.opt),
        Prototype.fn("atan", XQType.NUMERIC.opt, AtanRT.class)
                 .arg("arg", XQType.NUMERIC.opt),
        Prototype.fn("cos", XQType.NUMERIC.opt, CosRT.class)
                 .arg("arg", XQType.NUMERIC.opt),
        Prototype.fn("acos", XQType.NUMERIC.opt, AcosRT.class)
                 .arg("arg", XQType.NUMERIC.opt),
        Prototype.fn("sin", XQType.NUMERIC.opt, SinRT.class)
                 .arg("arg", XQType.NUMERIC.opt),
        Prototype.fn("asin", XQType.NUMERIC.opt, AsinRT.class)
                 .arg("arg", XQType.NUMERIC.opt)
    };
    
    public Prototype[] getProtos() { return protos; }

    public static abstract class BaseRT extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            XQItem arg = args[0].evalAsOptItem(focus, context);
            if (arg == null)
                return XQValue.empty;
            context.at(this);
            switch (arg.getItemType().quickCode()) {
            case XQType.QT_DOUBLE:
            case XQType.QT_UNTYPED:
            case XQType.QT_DEC:
            case XQType.QT_INT:
            case XQType.QT_FLOAT:
                return new SingleDouble( exec( arg.getDouble() ) );
            default:
                context.badTypeForArg(arg.getItemType(), args[0], -1, "numeric");
                return null;
            }
        }
        
        protected abstract double exec(double value);
    }

    public static class PI extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
        {
            return new SingleDouble(Math.PI);
        }
    }
    
    public static class SinRT extends BaseRT
    {
        protected double exec(double value) {
            return Math.sin(value);
        }
    }
    
    public static class CosRT extends BaseRT
    {
        protected double exec(double value) {
            return Math.cos(value);
        }
    }
    
    public static class TanRT extends BaseRT
    {
        protected double exec(double value) {
            return Math.tan(value);
        }
    }
    
    public static class AsinRT extends BaseRT
    {
        protected double exec(double value) {
            return Math.asin(value);
        }
    }
    
    public static class AcosRT extends BaseRT
    {
        protected double exec(double value) {
            return Math.acos(value);
        }
    }
    
    public static class AtanRT extends BaseRT
    {
        protected double exec(double value) {
            return Math.atan(value);
        }
    }
}
