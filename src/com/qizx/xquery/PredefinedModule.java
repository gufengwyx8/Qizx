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

import com.qizx.api.QName;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Util;
import com.qizx.xdm.IQName;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.JavaFunction;
import com.qizx.xquery.fn.MathFun;
import com.qizx.xquery.fn._Boolean;
import com.qizx.xquery.fn._Empty;
import com.qizx.xquery.fn._String;

/**
 * Management of predefined functions and variables. 
 * Features an automatic registration of functions.
 * Because it manages Java binding security, one instance per XQuery session
 * (shared among Expressions).
 */
public class PredefinedModule extends ModuleContext
{
    private static final String funPackage;

    private FunctionPlugger[] pluggers = new FunctionPlugger[0];

    private JavaFunction.Plugger javaPlugger;

    static {
        // must be done creating predefined module (ie registering functions)
        String className = Function.class.getName();
        // Dont use getPackage here, as it appears to be unimplemented
        // in some J2EE environments:
        funPackage = className.substring(0, className.lastIndexOf('.'));
    }

    /**
     * Mechanism to dynamically load/plug functions: When an unknown function
     * is encountered by the parser, it calls
     * PredefinedModule.localFunctionLookup which in turn asks its registered
     * FunctionPluggers to look for this function. This mechanism manages
     * predefined functions fn: (with the side benefit that the system start-up
     * is faster because functions are lazily loaded), type functions (xs: and
     * xdt:), Java extensions, and application extensions.
     */
    public interface FunctionPlugger
    {
        Function plug(QName functionName, PredefinedModule target)
            throws SecurityException;
    }

    public PredefinedModule()
    {
        super(null);
        // fn:functions: name is camelCased,
        // eg get-year-from-date -> GetYearFromDate
        registerFunctionPlugger(new BasicFunctionPlugger(NamespaceContext.FN,
                                                         funPackage + ".%C"));
        // Type functions: like fn:functions but the name is left untouched
        registerFunctionPlugger(new BasicFunctionPlugger(NamespaceContext.XSD,
                                                         funPackage + ".XS_%n"));
        // registerFunctionPlugger(new BasicFunctionPlugger(Namespace.XDT,
        // funPackage + ".XS_%n"));
        // Java binding:
        registerFunctionPlugger(javaPlugger = new JavaFunction.Plugger());

        try {
            // explicit declaration of special functions:
            declareFunction(new _Boolean());
            declareFunction(new _String());
            declareFunction(new _Empty());
            declareFunction(new MathFun());
            // predefined global var $arguments contains command line options
            defineGlobal(IQName.get("arguments"), XQType.STRING.star);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        importedModules.clear(); // final
        predefined = null;
    }

    /**
     * Used by applications:
     */
    public void registerFunctionPlugger(FunctionPlugger plugger)
    {
        FunctionPlugger[] old = pluggers;
        pluggers = new FunctionPlugger[old.length + 1];
        System.arraycopy(old, 0, pluggers, 0, old.length);
        pluggers[old.length] = plugger;
    }

    /**
     * Explicitly authorizes a Java class to be used as extension.
     * <p>
     * Caution: this is a security feature. By default, all classes are
     * allowed. However, when this method is used once, the control becomes
     * explicit and each class whose methods are used as extension functions
     * must be declared.
     */
    public void authorizeJavaClass(String className)
    {
        javaPlugger.authorizeClass(className);
    }

    public Function localFunctionLookup(QName name)
        throws SecurityException
    {
        // caching:
        Function fun = (Function) functionMap.get(name);
        if (fun != null)
            return fun;
        // invoke pluggers:
        for (int p = 0; p < pluggers.length; p++)
            if ((fun = pluggers[p].plug(name, this)) != null) {
                declareFunction(fun);
                return fun;
            }
        // maybe failure: but try again, as a Java class might have declared
        // the function in its plug hook
        return (Function) functionMap.get(name);
    }

    /**
     * Basic implementation of function plugger: recognize function by NS and
     * instantiate a pattern with the localname. The pattern can contain %n
     * (unmodified name of the function), %c (camelcased name), %C (camelcased
     * and capitalized name).
     */
    public static class BasicFunctionPlugger
        implements FunctionPlugger
    {
        String ns;

        String pattern;

        public BasicFunctionPlugger(String ns, String pattern)
        {
            this.ns = NamespaceContext.unique(ns);
            this.pattern = pattern;
        }

        public Function plug(QName functionName, PredefinedModule target)
        {
            // System.err.println(ns+" plug " +functionName);
            if (functionName.getNamespaceURI() != ns) {
                return null;
            }
            String name = functionName.getLocalPart();
            StringBuffer className =
                new StringBuffer(pattern.length() + name.length());
            for (int c = 0, C = pattern.length(); c < C; c++) {
                char ch = pattern.charAt(c);
                if (ch != '%') {
                    className.append(ch);
                    continue;
                }
                ch = pattern.charAt(++c);
                if (ch == 'n')
                    className.append(name);
                else if (ch == 'C' || ch == 'c')
                    className.append(Util.camelCase(name, ch == 'C'));
            }
            try {
                Class fclass = Class.forName(className.toString());
                return (Function) fclass.newInstance();
            }
            catch (ClassNotFoundException e) {
                // e.printStackTrace();
            }
            catch (NoClassDefFoundError e) {    // bizarre: happens on Windoz
            }
            catch (Exception ie) {
                System.err.println("*** error instantiating function "
                                   + functionName);
                ie.printStackTrace(); // abnormal
            }
            return null;
        }
    }
}
