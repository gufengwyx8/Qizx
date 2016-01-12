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
import com.qizx.util.NamespaceContext;
import com.qizx.xdm.NodeFilter;
import com.qizx.xdm.XMLPushStreamBase;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.NodeType;
import com.qizx.xquery.impl.ErrorValue;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.LocalVariable;
import com.qizx.xquery.op.UpdatingExpr;

/**
 * User Function Definition. As a subclass of Function, this descriptor holds
 * several prototypes if user functions are overloaded.
 */
public class UserFunction extends Function
{
    private static final EvaluationException STACK_OVF =
        new EvaluationException("XQuery stack overflow");

    protected Signature[] protos = new Signature[1];

    public UserFunction()
    {
    }

    public static class Signature extends Prototype
    {
        public boolean updating;
        public Expression body;
        // address of arguments: register or plain local
        LocalVariable[] argDecls;
        public int maxStackSize;

        // For future use: XSLT2
        public Expression[] argInit; // default value
        public boolean[] tunnelArg; // tunnel parameter

        public Signature(QName qname)
        {
            super(qname, null, Call.class);
            argInit = new Expression[argNames.length];
            tunnelArg = new boolean[argNames.length];
        }

        public void arg(QName name, XQType argType, Expression init,
                        boolean tunnel)
        {
            super.arg(name, argType);
            if (argCnt >= argInit.length) {
                Expression[] oldi = argInit;
                argInit = new Expression[argNames.length];
                System.arraycopy(oldi, 0, argInit, 0, oldi.length);

                boolean[] old = tunnelArg;
                tunnelArg = new boolean[argNames.length];
                System.arraycopy(old, 0, tunnelArg, 0, old.length);
            }
            argInit[argCnt - 1] = init;
            tunnelArg[argCnt - 1] = tunnel;
        }

        // Static analysis of the definition
        public Expression staticCheck(ModuleContext context, int flags)
        {
            context.resetLocals();

            argDecls = new LocalVariable[argCnt];
            for (int p = 0; p < argCnt; p++) {
                QName param = argNames[p];
                // check duplication:
                int dup = p;
                for (; --dup >= 0;)
                    if (argNames[dup] == param)
                        break;
                if (dup >= 0)
                    module.error("XQST0039", offset, "duplicate parameter $"
                                                       + param);
                // declare locals for parameters:
                argDecls[p] = context.defineLocalVariable(param, argTypes[p], null);
                argDecls[p].storageType(argTypes[p], context);
            }

            if (body == null) {
                module.error("XQST0000", offset, "unbound external function "
                                                 + module.prefixedName(qname));
                return this;
            }
            body = context.simpleStaticCheck(body, 0);
            XQType retType = body.getType();
            if (declaredReturnType == null) // unspecified
                returnType = (retType != null)? retType : XQType.ANY;

            // Allocate addresses/registers for locals, compute stack size
            if(context.trace)
                System.err.println("------- allocate register for "+qname);
            maxStackSize = context.allocateLocalAddress(null);
            
            if(UpdatingExpr.isUpdating(body) != updating)
                if(updating) {
                    if(!UpdatingExpr.isVacuous(body))
                        context.error("XUST0002", body,
                                    "function is declared updating but is not");
                }
                else
                    context.error("XUST0001", body,
                                  "function is not declared updating but is");
            return this;
        }

        @Override
        public Call instanciate(Expression[] actualArguments)
        {
            int actArgCnt = actualArguments.length;

            Call call = (Call) super.instanciate(actualArguments);
            call.signature = this;
            call.argChecks = new XQType[actArgCnt];
            call.module = module;
            call.offset = offset;
            
            
            int argc = Math.min(actArgCnt, argCnt); // protect
            for (int a = 0; a < argc; a++) {
                // if declared arg of type item* or anytype: no check
                XQType declared = argTypes[a];
                XQType actualType = actualArguments[a].getType();
                
                boolean dynCheck = needsDynamicCheck(declared.itemType());
                
                // if we are sure that actual arg has occ 1: 
                //    no check, unless specific node tests
                // because actual occ 1 satisfies any formal occ 
                if (actualType.getOccurrence() == XQType.OCC_EXACTLY_ONE
                        && dynCheck)
                    call.argChecks[a] = declared;

                // if occ != *, need to check the occ count: Type.accepts(value)
                if (declared.getOccurrence() != XQType.OCC_ZERO_OR_MORE || dynCheck)
                    call.argChecks[a] = declared;
                
            }

            return call;
        }
        
        protected EvalContext createContext(Focus focus, EvalContext context,
                                            Expression[] args, XQType[] argChecks)
            throws EvaluationException
        {
            // due to deferred evaluation in many constructs (Flower, sequence,
            // paths...), the function context (arguments and locals) has to
            // be allocated (cant be in a classical stack)
            // and can even *persist after exit* of the function.
            EvalContext called = context.subContext(this);

            // eval and bind actual arguments to declared arguments:
            for (int a = 0; a < args.length; a++) {
                try {
                    called.storeLocal(argDecls[a], args[a], argChecks[a],
                                      true/* cast */, focus, context);
                }
                catch (EvaluationException err) {
                    NamespaceContext nsCtx =
                        context.getStaticContext().getInScopeNS();
                    context.error(err.getErrorCode(), args[a],
                                  "type mismatch on argument $" + argNames[a]
                                  + " of function " + displayName(nsCtx)
                                  + " : " + err.getMessage());
                }
            }
            context.at(this);
            return called;
        }

        @Override
        public XQValue invoke(Expression[] args, Expression call,
                              EvalContext lexicalContext, 
                              Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(call);
            if(!accepts(args.length))
                context.error("XPTY0004", call, "invalid number of arguments");
            // push context and populate its parameters
            // TODO arg type check + fun item coercion
            EvalContext newCtx = createContext(focus, context, args, argTypes);
            newCtx.setClosure(lexicalContext);
            
            // context item not inherited (focus := null)
            XQValue result = body.eval(null, newCtx);
            // check return type if required:
            XQType rtype = declaredReturnType;
            if (rtype != null) {
                result = result.checkTypeExpand(rtype, context, 
                                                true/* cast */, true);
                if (result instanceof ErrorValue)
                    context.error(ERRC_BADTYPE, this,
                                  ((ErrorValue) result).getReason());
            }
            return result;
        }

        // means specific node type
        private boolean needsDynamicCheck(XQType type)
        {
            NodeFilter nt = null;
            return type instanceof NodeType
                   && (nt = ((NodeType) type).nodeFilter) != null
                   && !nt.staticallyCheckable();
        }

        public boolean equals(Object obj)
        {
            if(obj instanceof Signature) {
                Signature sig = (Signature) obj;
                return sig.qname == qname && sig.argCnt == argCnt;
            }
            return false;
        }

        public int hashCode()
        {
            if(qname == null)
                return argCnt;
            return qname.hashCode() ^ argCnt;
        }

        public void dump(ExprDisplay d)
        {
            d.header("UserFunction");
            d.property("signature", toString(null));
            d.child("body", body);
        }
    }

    public Prototype[] getProtos() // never called
    {
        return protos;
    }

    public boolean addPrototype(Signature proto)
    {
        if (protos[0] == null)
            protos[0] = proto;
        else {
            for (int i = 0; i < protos.length; i++) {
                // detect duplicate: based only on arg count
                if (protos[i].argCnt == proto.argCnt)
                    return false;
            }
            Signature[] old = protos;
            protos = new Signature[old.length + 1];
            System.arraycopy(old, 0, protos, 0, old.length);
            protos[old.length] = proto;
        }
        return true;
    }

    public XQValue eval(Focus focus, EvalContext context)
    {
        throw new RuntimeException("UserFunction not evaluable");
    }

    /**
     * Run-time User Function implementation.
     */
    public static class Call extends Function.Call
    {
        Signature signature;
        XQType[] argChecks;       // type actually used for dynamic checking

        public void dump(ExprDisplay d)
        {
            d.header(this);
            d.property("prototype", prototype.toString());
            d.property("localSize", "" + getLocalSize());
            StringBuilder chkBuf = new StringBuilder();
            for (int i = 0; i < argChecks.length; i++)
                chkBuf.append(argChecks[i] != null? "Y," : " ,");
            d.property("checks", chkBuf.toString());
            d.children(args);
        }

        public XQType getType()
        {
            if (type == null) {
                // can happen with recursive functions
                if (signature.body != null) {
                    type = XQType.ANY;      // anti rec loop
                    type = signature.body.getType();
                }
            }
            if (type == null)
                type = XQType.ANY;
            return type;
        }

        public int getLocalSize()
        {
            return ((Signature) prototype).maxStackSize;
        }

        public int getFlags()
        {
            return super.getFlags()
                   + (((Signature) prototype).updating? UPDATING : 0);
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            try {
                // push context and populate its parameters
                EvalContext newCtx = signature.createContext(focus, context,
                                                             args, argChecks);
                // context item not inherited (focus := null)
                XQValue result = signature.body.eval(null, newCtx);
                // check return type if required:
                XQType rtype = prototype.declaredReturnType;
                if (rtype != null) {
                    result = result.checkTypeExpand(rtype, context, 
                                                    true/* cast */, true);
                    if (result instanceof ErrorValue)
                        context.error(ERRC_BADTYPE, this,
                                      ((ErrorValue) result).getReason());
                }
                return result;
            }
            catch(StackOverflowError stove) {
                return context.error(this, STACK_OVF);
            }
        }

        // ----------- optimizations:

        public void evalAsEvents(XMLPushStreamBase output, Focus focus,
                                 EvalContext context)
            throws EvaluationException
        {
            context.at(this);

            EvalContext newContext =
                signature.createContext(focus, context, args, argChecks);

            // context item not inherited
            signature.body.evalAsEvents(output, null, newContext);
        }

        public long evalAsInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            EvalContext newContext =
                signature.createContext(focus, context, args, argChecks);

            // context item not inherited
            return signature.body.evalAsInteger(null, newContext);
        }

        public long evalAsOptInteger(Focus focus, EvalContext context)
            throws EvaluationException
        {
            EvalContext newContext =
                signature.createContext(focus, context, args, argChecks);

            // context item not inherited
            return signature.body.evalAsOptInteger(null, newContext);
        }

        public double evalAsDouble(Focus focus, EvalContext context)
            throws EvaluationException
        {
            EvalContext newContext =
                signature.createContext(focus, context, args, argChecks);

            // context item not inherited
            return signature.body.evalAsDouble(null, newContext);
        }

        public double evalAsOptDouble(Focus focus, EvalContext context)
            throws EvaluationException
        {
            EvalContext newContext = 
                signature.createContext(focus, context, args, argChecks);

            // context item not inherited
            return signature.body.evalAsOptDouble(null, newContext);
        }
    }
}
