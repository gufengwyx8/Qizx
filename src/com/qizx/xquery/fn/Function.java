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

import com.qizx.api.CompilationException;
import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.util.basic.Util;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xdm.IQName;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.*;
import com.qizx.xquery.impl.EmptyException;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.FunctionCall;

import java.text.Collator;

/**
 * Function descriptor. 
 * Each built-in function is a subclass of Function and
 * defines a list of implemented signatures aka Prototypes.
 */
public abstract class Function
{
    public static String ERR_ARGTYPE = "FORG0006";

    /**
     * Checks the arguments and return a function call instantiated with actual
     * arguments.
     * <p>
     * Specific to each function.
     */
    public Expression staticCheck(QName actualName, ModuleContext context,
                                  Expression[] arguments, Expression call)
    {
        return context.resolve(actualName, getProtos(), arguments, call);
    }

    public QName getName()
    {
        return getProtos()[0].qname;
    }

    // redefined in each implementation
    public abstract Prototype[] getProtos();


    /**
     * Instantiated function/operator call.
     */
    public abstract static class Call extends Expression
    {
        public Expression[] args; // actual args
        public Prototype prototype;
        
        public Expression child(int rank)
        {
            return rank < args.length ? args[rank] : null;
        }

        public void dump(ExprDisplay d)
        {
            if (prototype != null) {
                d.header("function");
                d.property("signature", prototype.toString(null));
            }
            else d.header("operator");
            d.property("rt", Util.shortClassName(getClass()));
            d.headerInfo(this);
            d.children(args);
        }

        public void compilationHook()
        {
        }

        // utility: eval an argument of type xdt:object(argClass)
        public Object objArg(Expression[] args, int rank, Class argClass,
                             Focus focus, EvalContext context)
            throws EvaluationException
        {
            try {
                XQItem v = args[rank].evalAsItem(focus, context);
                Object obj = null;
                if (!(v instanceof WrappedObjectValue)
                    || !argClass.isAssignableFrom((obj =
                        ((WrappedObjectValue) v).getObject()).getClass())) {
                    String cname = argClass.getName();
                    context.error(ERRC_BADTYPE, this, "bad type for argument "
                                                   + (rank + 1)
                                                   + ": expecting "
                                                   + cname.substring(cname
                                                       .lastIndexOf('.') + 1));
                }
                return obj;
            }
            catch (EvaluationException e) {
                context.error(ERRC_BADTYPE, args[rank],
                              new EvaluationException("error on argument "
                                                  + (rank + 1) + ": "
                                                  + e.getMessage(), e));
                return null;
            }
        }

        public Collator getCollator(Expression arg, Focus focus,
                                    EvalContext context)
            throws EvaluationException
        {
            if(arg == null)
                return context.getCollator();
            String uri = arg.evalAsString(focus, context);
            Collator coll = context.getCollator(uri);
            if (coll == null)
                context.error("FOCH0002", arg, "unknown collation: " + uri);
            return coll;
        }
        
        public static IQName toQName(String qname, Node holder, EvalContext ectx)
            throws DataModelException
        {
            String localPart;
            String ns;
            String prefix = IQName.extractPrefix(qname);
            localPart = IQName.extractLocalName(qname);
            ns = holder.getNamespaceUri(prefix);
            if (ns == null)
                if (prefix.length() == 0)
                    ns = "";
                else if(ectx != null && ectx.getStaticContext() != null) {
                    ns = ectx.getStaticContext().getInScopeNS().getNamespaceURI(prefix);
                }            
            if(ns == null)
                throw new DataModelException("undefined namespace prefix: " + prefix);
            return IQName.get(ns, localPart);
        }

        protected Expression checkFunction(QName funName, Expression[] args,
                                           EvalContext ectx)
            throws CompilationException
        {
            FunctionCall call = new FunctionCall(funName, args);
            return ectx.dynamicContext().checkExpression(call);
        }
    }

    public abstract static class BoolCall extends Call
    {
        protected BoolCall()
        {
            type = XQType.BOOLEAN;
        }

        public abstract boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException;

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return new SingleBoolean(evalAsBoolean(focus, context));
        }

        public boolean evalAsEffectiveBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return evalAsBoolean(focus, context);
        }

    }

    // give a general static type to numeric operators
    public abstract static class NumericCall extends Call
    {
        public NumericCall()
        {
            type = XQType.NUMERIC.opt;
        }
    }

    public abstract static class IntegerCall extends Call
    {
        protected IntegerCall()
        {
            type = XQType.INTEGER;
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return new SingleInteger(evalAsInteger(focus, context));
        }

        public abstract long evalAsInteger(Focus focus, EvalContext context)
            throws EvaluationException;
    }

    public abstract static class OptIntegerCall extends Call
    {
        public OptIntegerCall()
        {
            type = XQType.INTEGER.opt;
        }

        public abstract long evalAsOptInteger(Focus focus, EvalContext context)
            throws EvaluationException;

        public long evalAsInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            try {
                return evalAsOptInteger(focus, context);
            }
            catch (EmptyException e) {
                errorEmpty(context);
                return 0;
            }
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            try {
                return new SingleInteger(evalAsInteger(focus, context));
            }
            catch (EmptyException e) {
                return XQValue.empty;
            }
        }
    }

    public abstract static class FloatCall extends Call
    {
        public FloatCall()
        {
            type = XQType.FLOAT.opt;
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return new SingleFloat(evalAsFloat(focus, context));
        }

        public abstract float evalAsFloat(Focus focus, EvalContext context)
            throws EvaluationException;
    }

    public abstract static class OptFloatCall extends Call
    {
        public OptFloatCall()
        {
            type = XQType.FLOAT.opt;
        }

        public abstract float evalAsOptFloat(Focus focus, EvalContext context)
            throws EvaluationException;

        public float evalAsFloat(Focus focus, EvalContext context)
            throws EvaluationException
        {
            try {
                return evalAsOptFloat(focus, context);
            }
            catch (EmptyException e) {
                errorEmpty(context);
                return 0;
            }
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            try {
                return new SingleFloat(evalAsFloat(focus, context));
            }
            catch (EmptyException e) {
                return XQValue.empty;
            }
        }
    }

    public abstract static class DoubleCall extends Call
    {
        protected DoubleCall()
        {
            type = XQType.DOUBLE;
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return new SingleDouble(evalAsDouble(focus, context));
        }

        public abstract double evalAsDouble(Focus focus, EvalContext context)
            throws EvaluationException;
    }

    public abstract static class OptDoubleCall extends Call
    {
        protected OptDoubleCall()
        {
            type = XQType.DOUBLE.opt;
        }

        public abstract double evalAsOptDouble(Focus focus, EvalContext context)
            throws EvaluationException;

        public double evalAsDouble(Focus focus, EvalContext context)
            throws EvaluationException
        {
            try {
                return evalAsOptDouble(focus, context);
            }
            catch (EmptyException e) {
                errorEmpty(context);
                return 0;
            }
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            try {
                return new SingleDouble(evalAsOptDouble(focus, context));
            }
            catch (EmptyException e) {
                return XQValue.empty;
            }
        }
    }

    public abstract static class StringCall extends Call
    {
        protected StringCall()
        {
            type = XQType.STRING;
        }

        public abstract String evalAsString(Focus focus, EvalContext context)
            throws EvaluationException;

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            return new SingleString(evalAsString(focus, context));
        }
    }

    public abstract static class OptStringCall extends Call
    {
        protected OptStringCall()
        {
            type = XQType.STRING.opt;
        }

        public abstract String evalAsOptString(Focus focus, EvalContext context)
            throws EvaluationException;

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            String s = evalAsOptString(focus, context);
            return s == null ? XQValue.empty : new SingleString(s);
        }

        public String evalAsString(Focus focus, EvalContext context)
            throws EvaluationException
        {
            String s = evalAsOptString(focus, context);
            if (s == null)
                errorEmpty(context);
            return s;
        }
    }

    public abstract static class TreeCall extends Call
    {
        protected TreeCall()
        {
            type = XQType.NODE;
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            BasicNode node = evaluateAsNode(focus, context);
            return node == null ? XQValue.empty : new SingleNode(node);
        }

        public BasicNode evaluateAsNode(Focus focus, EvalContext context)
            throws EvaluationException
        {
            CorePushBuilder builder =
                new CorePushBuilder(context.getStaticContext().getBaseURI());
            evalAsEvents(builder, focus, context);
            return builder.harvest();
        }

        public abstract void evalAsEvents(XMLPushStreamBase output, Focus focus,
                                          EvalContext context)
            throws EvaluationException;
    }

}
