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
import com.qizx.util.basic.Util;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.JavaMapping;
import com.qizx.xquery.op.Expression;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;

/**
 *   Function bound to a Java method.
 *   <p>The binding mechanism is based on the qname of the function: the uri part
 *   has the form java:<fully_qualified_class_name> (e.g. "java:java.lang.Math")
 *   and the local part matches the Java method name. For example the following
 *   code calls java.lang.Math.log :
 *	<pre>declare namespace math = "java:java.lang.Math"
 *	math:log(1)</pre>
 *   <p>The function name can be in XPath style with hyphens: it is converted
 *   into 'camelCase' style automatically. For example "list-files" is converted
 *   to "listFiles".
 *   <p>
 */
public class JavaFunction extends Function
{
    public static boolean trace = false;
    private static Class[] hookProto = { PredefinedModule.class };
    // more than 1 if overloaded:
    protected Signature[] prototypes;
   
    public static class Plugger
        implements PredefinedModule.FunctionPlugger
    {
        HashSet<String> allowedClasses = new HashSet<String>(); // disabled by default

        public void authorizeClass(String className)
        {
            if(className == null) {
                allowedClasses = null;
            }
            else {
                if (allowedClasses == null)
                    allowedClasses = new HashSet<String>();
                allowedClasses.add(className);
            }
        }

        public Function plug(com.qizx.api.QName qname, PredefinedModule target)
            throws SecurityException
        {
            String uri = qname.getNamespaceURI();
            if (!uri.startsWith("java:"))
                return null;
            String className = uri.substring(5);

            // detect auto arg0 specification: java:className?localname=uri
            QName autoArg0 = null;
            int mark = className.indexOf('?');
            if (mark > 0) {
                // DEPRECATED
                throw new SecurityException("auto arg0 is deprecated");
            }

            // the function name can be itself a qualified name (eg
            // io.File.new)
            String functionName = qname.getLocalPart();
            int dot = functionName.lastIndexOf('.');
            if (dot >= 0) {
                className = className + '.' + functionName.substring(0, dot);
                functionName = functionName.substring(dot + 1);
            }

            if (allowedClasses != null && !allowedClasses.contains(className))
                throw new SecurityException(
                              "Java extension: security restriction on class "
                              + className);

            try {
                Class fclass = Class.forName(className);
                // find a plug hook in the class: can declare extra stuff
                try {
                    Method hook = fclass.getMethod("plugHook", hookProto);
                    hook.invoke(null, new Object[] {
                        target
                    });
                }
                catch (Exception diag) {
                    if (trace)
                        System.err.println("no hook for " + fclass + " "
                                           + diag);
                }

                // look for the function or constructor
                Signature[] protos;
                int kept = 0;
                String name = Util.camelCase(functionName, false);
                if (trace)
                    System.err.println("found Java class " + fclass
                                       + " for function " + name);

                if (name.equals("new")) {
                    // look for constructors
                    int mo = fclass.getModifiers();
                    if (!Modifier.isPublic(mo) || Modifier.isAbstract(mo)
                        || Modifier.isInterface(mo))
                        return null; // instantiation will fail

                    Constructor[] constructors = fclass.getConstructors();
                    protos = new Signature[constructors.length];
                    for (int c = 0; c < constructors.length; c++) {
                        protos[kept] =
                            convertConstructor(qname, constructors[c]);
                        if (protos[kept] != null) {
                            if (trace)
                                System.err.println("detected constructor "
                                                   + protos[kept]);
                            ++kept;
                        }
                    }
                }
                else {
                    Method[] methods = fclass.getMethods();
                    protos = new Signature[methods.length];
                    for (int m = 0; m < methods.length; m++)
                        // match name without conversion:
                        if (methods[m].getName().equals(name)) {
                            protos[kept] =
                                convertMethod(qname, methods[m], autoArg0);
                            if (protos[kept] != null) {
                                if (trace)
                                    System.err.println("detected method "
                                                       + protos[kept]);
                                ++kept;
                            }
                        }
                }
                if (kept > 0)
                    return new JavaFunction(protos, kept);
            }
            catch (ClassNotFoundException e) {
                if (trace)
                    System.err.println("*** class not found " + className);
            }
            catch (Exception ie) {
                ie.printStackTrace(); // abnormal: report
            }
            return null;
        }
    }

    static Signature convertMethod(QName qname, Method method, QName autoArg0)
    {
        // method must be public:
        if (!Modifier.isPublic(method.getModifiers()))
            return null;
        
        Class[] params = method.getParameterTypes();
        Signature proto = new Signature(qname, null, method, null);
        proto.isStatic = Modifier.isStatic(method.getModifiers());

        if (!Modifier.isStatic(method.getModifiers()))
            // add a first argument 'this'
            proto.arg("this", convertClass(method.getDeclaringClass()));
        int param = 0;
        if(params.length > 0 && params[0] == EvalContext.class) {
            // new way of implementing builtin functions which are Java methods
            // first param has EvalContext class => hidden
            ++ param;
            proto.ctxArg = true;
        }
        
        for ( ; param < params.length; param++)
        {
            XQType type = convertClass(params[param]);
            if (type != XQType.ITEM.star)
                proto.arg("p" + (param + 1), type);
            else { // hack:
                proto.vararg = true;
            }
        }
        proto.returnType = convertClass(method.getReturnType());
        return proto;
    }

    static Signature convertConstructor(QName qname, Constructor constr)
    {
        if (!Modifier.isPublic(constr.getModifiers()))
            return null;
        Class[] params = constr.getParameterTypes();
        Signature proto = new Signature(qname, null, null, constr);
        for (int p = 0; p < params.length; p++) {
            proto.arg("p" + (p + 1), convertClass(params[p]));
        }
        proto.returnType = convertClass(constr.getDeclaringClass());
        return proto;
    }

    public static synchronized XQType convertClass(Class javaType)
    {
        return JavaMapping.getSequenceType(javaType);
    }

    /**
     * Convert Java object to XQuery item or sequence.
     * @throws XQTypeException 
     */
    public static XQValue convertObject(Object object) throws XQTypeException
    {
        if (object == null)
            return XQValue.empty;
        return convertClass(object.getClass()).convertFromObject(object);
    }

    // -------------------------------------------------------------------------

    public JavaFunction(Signature[] protos, int count)
    {
        prototypes = new Signature[count];
        System.arraycopy(protos, 0, prototypes, 0, count);
    }

    public com.qizx.xquery.fn.Prototype[] getProtos() 
    {
        return prototypes;
    }

    public static class Signature extends com.qizx.xquery.fn.Prototype
    {
        // what is executed:
        Method method; // method and constructor are exclusive
        Constructor constructor;
        
        boolean isStatic; // true for static method
        boolean ctxArg;   // true if 1st arg is EvalContext
    
        public Signature(QName qname, XQType returnType, Method method,
                         Constructor<Object> constructor)
        {
            super(qname, returnType, Call.class);
            this.method = method;
            this.constructor = constructor;
        }
    
        public Function.Call instanciate(Expression[] actualArguments)
        {
            Call call = (Call) super.instanciate(actualArguments);

            return call;
        }
    
        public String toString()
        {
            return "JavaFunction " + method;
        }
    }

    public static class Call extends Function.Call
    {
        public void dump(ExprDisplay d)
        {
            d.header(this);
            d.property("prototype", prototype.toString());
            d.children("actual arguments", args);
        }

        public XQValue eval(Focus focus, EvalContext context)
            throws EvaluationException
        {
            context.at(this);
            Signature proto = (Signature) prototype;
            NamespaceContext nsCtx = context.getStaticContext().getInScopeNS();
            
            if (proto.method != null) { // method:
                Object target = null; 
                int argcnt = proto.argCnt, shift = 0;
                
                if (!proto.isStatic) { // target is first arg
                    target = proto.argTypes[0].convertToObject(args[0], 
                                                               focus, context);
                    --argcnt;
                    shift = 1;
                }
                else { // add EvalContext arg?
                    if(proto.ctxArg) {
                        ++ argcnt;
                    }
                }
                // if vararg we add a final Object[] arg
                Object[] params = new Object[proto.vararg ? (argcnt + 1) : argcnt];

                if(!proto.ctxArg) {
                    for (int a = 0; a < argcnt; a++)
                        params[a] = convertArgument(focus, context,
                                                    proto, a + shift);
                }
                else {
                    params[0] = context;
                    for (int a = 1; a < argcnt; a++)
                        params[a] = convertArgument(focus, context, proto, a - 1);
                }

                if (proto.vararg) {
                    int vcnt = args.length - argcnt + 1 - shift;
                    XQItem[] vargs = new XQItem[vcnt];
                    // eval and convert extra args into Item or null (type item?)
                    // like WrappedObjectType but treat also int double boolean
                    for (int va = 0; va < vcnt; va++)
                        vargs[va] = args[argcnt - 1 + va].evalAsOptItem(focus, context);
                    params[argcnt] = vargs;
                }
               
                // finally call the method
                try {
                    if (trace)
                        System.err.println("calling Java method: "
                                           + proto.method + "\n for " + proto);
                    Object result = proto.method.invoke(target, params);
                    return proto.returnType.convertFromObject(result);
                }
                catch (InvocationTargetException iex) {
                    // ex.printStackTrace();
                    Throwable cause = iex.getCause();
                    if (!(cause instanceof Exception))
                        cause = new RuntimeException(cause);
                    if(cause instanceof EvaluationException) {
                        EvaluationException evx = (EvaluationException) cause;
                        context.error(evx.getErrorCode(), this, cause.getMessage());
                    }
                    else
                        context.error("FXJA0001", this,
                                      new EvaluationException(cause.getMessage(),
                                                              (Exception) cause));
                }
                catch (Exception ex) {
                    context.error("FXJA0002", this,
                                  new EvaluationException(
                                        "invocation of extension function "
                                        + proto.toString(nsCtx) + "\n throws " + ex,
                                        ex));
                }
            }
            else { // constructor:
                Object[] params = new Object[proto.argCnt];
                for (int a = 0; a < proto.argCnt; a++)
                    params[a] = convertArgument(focus, context, proto, a);
                try {
                    if (trace)
                        System.err.println("calling Java constructor: "
                                           + proto.constructor + "\n for "
                                           + proto);
                    Object result = proto.constructor.newInstance(params);
                    return proto.returnType.convertFromObject(result);
                }
                catch (Exception ex) {
                    // -System.err.println("Param0: " + params[0].getClass());
                    ex.printStackTrace();
                    context.error(ERR_ARGTYPE, this,
                                  "invocation of extension constructor: " + ex);
                }
            }
            return XQValue.empty;
        }

        private Object convertArgument(Focus focus, EvalContext context,
                                       Signature proto, int rank)
            throws EvaluationException
        {
            return proto.argTypes[rank].convertToObject(args[rank], focus, context);
        }

//      try {
//      }
//      catch (EvaluationException e) {
//          context.error(ERR_ARGTYPE, args[rank],
//                        "computing argument " + (rank + 1)
//                        + " of Java method " + prototype.toString(context.getInScopeNS())
//                        + ": " + e.getMessage());
//          return null;
//      }
    }
    
//    public static void main(String[] args)
//    {
//        try {
//            Class klass = org.w3c.dom.Element[].class;
//            System.err.println(" " + convertClass(klass));
//            System.err.println(" " + JavaMapping.getSequenceType(klass));
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
