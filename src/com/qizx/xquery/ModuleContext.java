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
import com.qizx.api.Message;
import com.qizx.api.QName;
import com.qizx.api.QizxException;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Check;
import com.qizx.xdm.IQName;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;
import com.qizx.xquery.fn.UserFunction;
import com.qizx.xquery.fn.UserFunction.Signature;
import com.qizx.xquery.impl.ErrorValue;
import com.qizx.xquery.op.*;
import com.qizx.xquery.op.VarReference.Local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/*
 * Compiled Module and base class of MainQuery.
 */
public class ModuleContext extends BasicStaticContext
{    
    public static final int TY_PRESERVE = 0;   // preserve type info, nilled etc
    public static final int SP_PRESERVE = 1;   // preserve boundary space
    public static final int NS_XML11 = 2;      // supports XML1.1 (true by default)
    
    public static final int NS_COPY = 0;
    public static final int NS_PRES_INHERIT = 1;
    public static final int NS_NOPRES_INHERIT = 2;
    public static final int NS_PRES_NOINHERIT = 3;
    public static final int NS_NOPRES_NOINHERIT = 4;

    protected boolean checked = false;
    protected boolean checking;
    public boolean trace = !true;

    //protected NamespaceContext namespaceTable = new NamespaceContext();

    protected String physicalURI; // null for query
    protected String moduleNS; // null for query
    protected String source;

    protected ArrayList importedModules = new ArrayList();
    protected HashMap functionMap = new HashMap(); // quick access
    protected HashMap importedFunctions = new HashMap();
    protected HashMap importedGlobals = new HashMap();
    private GlobalVariable curInitVar;

    protected MatchOptions defaultFTOptions;
    
    // accumulates globals, functions and imported modules in declaration order
    protected ArrayList declarations = new ArrayList();
    protected Pragma[] pragmas;

    // Local variable declarations and bindings:
    protected LocalVariable locals, lastLocal;
    protected int maxLocalSize = 0;
    private int localCnt;
    private int localIntCnt, localDoubleCnt, localStringCnt, localItemCnt;
    protected LocalVars upLevels;
    
    private static class LocalVars
    {
        LocalVars up;
        LocalVariable locals;     // root of tree of locals (itself a dummy var)
        LocalVariable lastLocal;
        int maxLocalSize = 0;
    }

    // Used for static checking: access to enclosing expressions
    private Expression[] exprStack = new Expression[40];
    private int exprDepth = 0;

    // Type of expression '.':
    private DotType dotTypeStack;

    private int genId;

    private String latestErrorCode;

    private MessageReceiver messageTarget;
    private EvalContext cstEvalContext;

    /**
     * Creates a static context copying a template static context
     */
    public ModuleContext(BasicStaticContext parent)
    {
        defaultFTOptions = new MatchOptions();  // move to basic context?
        if(parent != null)
            copy(parent);
        // for dyn evaluation:
        if(parent instanceof ModuleContext)
            importedModules.add(parent);
    }

    public void dump(XMLExprDisplay display)
    {
        display.header("Module");
        for (int d = 0; d < declarations.size(); d++) {
            display.child((Expression) declarations.get(d));
        }
        display.end();
    }

    public void staticCheck(ModuleManager modman)
    {
        if (checked)
            return;
        resetLocals();
        // synchronize on the ModuleManager, not on module, to avoid
        // a deadlock on cyclical module import.
        synchronized (modman) {
            // block cyclical module import:
            checking = true;
            // process declarations in order:
            for (int d = 0; d < declarations.size(); d++) {
                Object decl = declarations.get(d);

                if (decl instanceof GlobalVariable)
                {
                    GlobalVariable global = (GlobalVariable) decl;
                    checkImportedGlobal(global, global.offset);

                    Expression ginit = global.init;
                    if (ginit != null) {
                        global.init = ginit = ginit.staticCheck(this, 0);
                        // FIX: allocate possible locals present in init expr
                        allocateLocalAddress(null);

                        if (global.declaredType == null)
                            global.setType(ginit.getType());
                        else {
                            if (!global.declaredType.accepts(ginit.getType()))
                                error(Expression.ERRC_BADTYPE, ginit.offset,
                                      "incompatible value type "
                                          + ginit.getType().toString(this)
                                          + " for initial value of variable $"
                                          + prefixedName(global.name) + ", "
                                          + global.declaredType.toString(this)
                                          + " required");
                            global.setType(global.declaredType);
                        }
                    }
                    else if (global.getType() == null)
                        global.setType(XQType.ANY);
                    if(ginit != null && ginit.isUpdating()) {
                        error("XUST0001", ginit,
                              "variable initialization must not be an updating expression");
                    }

                    GlobalVariable g =
                        (GlobalVariable) globalMap.get(global.name);
                    if (g != null)
                        error("XQST0049", global.offset,
                              "global variable " + prefixedName(global.name)
                                  + " already defined");
                    else {
                        globalMap.put(global.name, global);
                    }
                }
                else if (decl instanceof Signature) // function
                {
                    Signature fun = (Signature) decl;
                    fun.staticCheck(this, 0);
                    checkImportedFunction(fun, fun.offset);
                }
                else if (decl instanceof ModuleImport)
                {
                    checkModuleImport(modman, (ModuleImport) decl);
                }
            }
            checking = false;
            checked = true;
        }
        // -System.err.println("end check module "+moduleNS);
        // //env = null; // now needless, avoid pointing to temporary query
    }

    private void checkModuleImport(ModuleManager modman, ModuleImport mimport)
    {
        ModuleContext module = mimport.imported;
        if (module.checking) {
            error("XQST0073", mimport.offset, "circular import of module "
                                                + module.getPhysicalURI());
            return;
        }

        module.staticCheck(modman);
        // add to imported module list:
        // the module's declarations become visible
        importedModules.add(module);

        // merge declarations of imported module into visible declarations:
        for (int d = 0; d < module.declarations.size(); d++) {
            Object decl = module.declarations.get(d);

            if (decl instanceof GlobalVariable) {
                GlobalVariable global = (GlobalVariable) decl;
                if (checkImportedGlobal(global, mimport.offset))
                    importedGlobals.put(global.name, global);
            }
            else if (decl instanceof Signature) {
                Signature sig = (Signature) decl;
                if (checkImportedFunction(sig, mimport.offset)) {
                    // attention, use the signature as key (use argCnt)
                    importedFunctions.put(sig, sig);
                }
            }
        }

    }

    private boolean checkImportedGlobal(GlobalVariable global, int location)
    {
        GlobalVariable g = importedGlobal(global.name);
        if (g == null)
            return true;
        error("XQST0049", location,
              "global variable " + prefixedName(global.name)
              + " already defined in module " + g.module.getPhysicalURI());
        return false;
    }

    boolean checkImportedFunction(Signature fun, int location)
    {
        Signature s = importedFunction(fun);
        if (s == null)
            return true;
        error("XQST0034", fun.offset,
              "function " + prefixedName(fun.qname)
              + " already defined in module " + s.module.getPhysicalURI());
        return false;
    }

    /**
     * Called on start of an expression execution
     */
    void initGlobals(EvalContext context, HashMap globals)
        throws EvaluationException
    {
        if (predefined != null)
            predefined.initGlobals(context, globals);

        for (int g = 0, G = declarations.size(); g < G; g++)
        {
            if (declarations.get(g) instanceof ModuleImport) {
                ModuleImport mimport = (ModuleImport) declarations.get(g);
                ModuleContext module = mimport.imported;
                // recursive descent to imported modules.
                // CAUTION lock modules to avoid looping on circular imports
                // Hmmm well, circular imports are no more allowed anyway...
                if (globals.get(module) == null) { // not that beautiful but
                    globals.put(module, module);
                    module.initGlobals(context, globals);
                    globals.remove(module);
                }
            }
        }
        
        // Globals added by API in context are not declared: do it first
        for (Iterator iter = globalMap.values().iterator(); iter.hasNext();) {
            GlobalVariable var = (GlobalVariable) iter.next();
            XQValue init = (XQValue) globals.get(var.name);
            
            if(init == null)
                continue;
            // unfortunately required:
            init = init.bornAgain();
            // check the type if any
            if(var.declaredType != null)
                init = init.checkTypeExpand(var.declaredType, context, 
                                            false, true);
            context.setGlobal(var, init);
        }
        
        //
        for (int g = 0, G = declarations.size(); g < G; g++)
        {
            if (!(declarations.get(g) instanceof GlobalVariable))
                continue;
            GlobalVariable var = (GlobalVariable) declarations.get(g);
            XQType declaredType = var.declaredType;
            curInitVar = var;
            if (var.init != null) {
                XQValue v = var.init.eval(null, context);
                try { // expand with type checking
                    v = v.checkTypeExpand(declaredType, context, false, true);
                    if (v instanceof ErrorValue)
                        context.error(Expression.ERRC_BADTYPE,
                                      var.init, 
                                      ((ErrorValue) v).getReason());
                }
                catch (XQTypeException tex) {
                    context.error(var, tex);
                }
                context.setGlobal(var, v);
                curInitVar = null;
            }

            QName gname = var.name;
            // is there a value specified externally? 
            // use the full q-name of the variable
            //   (possible issue if $local:v allowed in modules)
            Object initValue = globals.get(gname);
            XQValue init = null;
            if(initValue instanceof ResultSequence) {
                ResultSequence seq = (ResultSequence) initValue;
                init = seq.getValues();
            }
            else 
                init = (XQValue) initValue;

            // glitch for compatibility: if $local:var, look for $var
            if (init == null && 
                 gname.getNamespaceURI() == NamespaceContext.LOCAL_NS) {
                QName redName = IQName.get((gname.getLocalPart()));
                init = (XQValue) globals.get(redName);
            }
            if (init == null) {
                // - System.err.println("no extern init for "+global.name);
                continue;
            }
            init = init.bornAgain(); // in case several executions
            if (declaredType != null) {
                try { // here we can promote: it helps with string values
                    init =
                        init.checkTypeExpand(declaredType, context, true,
                                             true);
                    if (init instanceof ErrorValue)
                        context.error(Expression.ERRC_BADTYPE, var,
                                      ((ErrorValue) init).getReason());
                }
                catch (XQTypeException tex) {
                    context.error(var, tex);
                }
            }
            context.setGlobal(var, init);
        }
    }

    public EvalContext getConstantEvalContext()
    {
        if(cstEvalContext == null) {
            MainQuery main = this instanceof MainQuery? (MainQuery) this : null;
            cstEvalContext = new EvalContext(new DynamicContext(main , null));
        }
        return cstEvalContext;
    }

    public Expression evalConstantExpr(Expression expr)
    {
        EvalContext ctx = getConstantEvalContext();
        try {
            XQValue res = expr.eval(null, ctx);
            if(res.next()) {
                switch(res.getItemType().quickCode()) {
                case XQType.QT_DOUBLE:
                    return new DoubleLiteral(res.getDouble());
                case XQType.QT_DEC:
                    return new DecimalLiteral(res.getDecimal());
                case XQType.QT_INT:
                    return new IntegerLiteral(res.getInteger());
                }
            }
        }
        catch (EvaluationException e) {
            ; 
        }
        return expr;
    }

    public int getLocalSize()
    {
        return maxLocalSize;
    };

    public void setSource(String source, String uri)
    {
        Check.nonNull(source, "source");
        this.source = source;
        this.physicalURI = uri;
    }

    public String getSource()
    {
        return source;
    }

    // ---------- compilation errors and messages -----------------------------

    public void error(String errCond, Expression place, String msg)
    {
        ModuleContext module = place.module;
        if(module == null)
            module = this;
        module.error(errCond, place.offset, msg);
    }

    public void error(String errCond, int offset, String msg)
    {
        error(xqueryErrorCode(errCond), offset, msg);
    }

    public void error(QName errorCode, Expression expr, String msg)
    {
        ModuleContext module = expr.module;
        if(module == null)
            module = this;
        module.error(errorCode, expr.offset, msg);
    }

    // The real one
    public void error(QName errorCode, int offset, String msg)
    {
         passMessage(Message.ERROR, errorCode, offset, msg);
    }


    public void warning(Expression expr, String msg)
    {
        ModuleContext module = expr.module;
        if(module == null)
            module = this;
        module.warning("", expr.offset, msg);
    }

    public void warning(int offset, String msg)
    {
        warning("", offset, msg);
    }

    public void warning(String errCond, int offset, String msg)
    {
        passMessage(Message.WARNING, xqueryErrorCode(errCond), offset, msg);
    }

    public void info(String text)
    {
        passMessage(Message.DETAIL, null, 0, text);
    }

    private void passMessage(int type, QName errorCode, int offset, String msg)
    {
        if(messageTarget == null)
            return;
        // convert offset into line and column:

        int lnum = 1;
        int eol = source.indexOf('\n'), sol = 0;    // Mac?
        for(; eol >= 0 && eol < offset; ++ lnum) {
            sol = eol + 1;
            eol = source.indexOf('\n', eol + 1);
        }
        String srcLine = source.substring(sol, eol < 0? source.length() : eol);

        messageTarget.receive(new Message(type, errorCode, msg,
                                          getPhysicalURI(), offset, 
                                          lnum, offset - sol, srcLine));
    }

    // --------------------------------------------------------------------

    public Expression simpleStaticCheck(Expression expr, int flags)
    {
        if(expr == null)
            return null;
        // push expr on context stack:
        if (exprDepth >= exprStack.length) {
            Expression[] old = exprStack;
            exprStack = new Expression[old.length * 2];
            System.arraycopy(old, 0, exprStack, 0, old.length);
        }
        exprStack[exprDepth++] = expr;
        if(expr.module == null)
            expr.module = this;
        Expression sc = expr.staticCheck(this, flags);
        --exprDepth;
        return sc;
    }

    // Used for most expressions; not suitable for legal updating expr
    public Expression staticCheck(Expression expr, int flags)
    {
        Expression e = simpleStaticCheck(expr, flags);
        if(UpdatingExpr.isUpdating(e))
            error("XUST0001", e, "updating expression is not allowed here");
        return e;
    }
    
    
    public Expression getEnclosing(int levels)  // TODO not used: ZAP
    {
        if (levels < 0 || levels > exprDepth - 1)
            return null;
        return exprStack[exprDepth - 1 - levels];
    }

    public boolean check(Prototype proto, int rank, Expression actualArgument)
    {
        // if(!proto.checkArg(rank, actualArgument)) {
        // error( actualArgument,
        // "type "+actualArgument.getType().toString(this)+
        // " not acceptable for argument '"+proto.getArgName(rank, this)+
        // "' in\n\t"+proto.toString(this));
        // return false;
        // }
        return true;
    }

    public Expression resolve(QName qname, Prototype[] protos,
                              Expression[] actualArguments, Expression call)
    {
        boolean errored = false;
        Prototype bestProto = null;
        int pr = 0, matches = 0;
        // count possible matches:
        for (pr = 0; pr < protos.length; pr++) {
            if(qname == protos[pr].qname
               && protos[pr].checkArgCount(actualArguments)) {
                ++ matches;
                bestProto = protos[pr];
            }
        }
        if(matches > 1) {   // overloading
            bestProto = null;
            int bestDist = Prototype.NO_MATCH;
            for (pr = 0; pr < protos.length; pr++) {
                // use a distance to find the best match
                int d = protos[pr].matchingDistance(actualArguments);
                if(d < bestDist) {
                    bestDist = d;
                    bestProto = protos[pr];
                }
            }
        }
        if (bestProto == null) { // no match
            errored = true;
            String msg = Prototype.displayName(qname, getInScopeNS())
                       + " cannot be applied to (";
            for (int a = 0; a < actualArguments.length; a++) {
                if (a > 0)
                    msg += ',';
                msg += ' ';
                msg += actualArguments[a].getType().toString(this);
            }
            msg += " )";
            call.module.error("XPST0017", call.offset, msg);
            info("known signature(s):");
            for (int p = 0; p < protos.length; p++)
                if (!protos[p].hidden && qname == protos[p].qname)
                    info(protos[p].toString(getInScopeNS()));

            // arbitrary prototype to avoid cast exceptions later
            bestProto = protos[0];
        }
        
        Function.Call newCall = bestProto.instanciate(actualArguments);
        newCall.setType(bestProto.returnType);
        newCall.module = call.module;
        newCall.offset = call.offset;
        if (!errored)
            newCall.compilationHook(); // optimizations
        return newCall;
    }

    public void staticTyping(Expression expr, SequenceType[] accepted,
                             String errorCode, String what)
    {
        if (!strictTyping)
            return;
        boolean ok = false;
        for (int i = accepted.length; --i >= 0;) {
            if (accepted[i].acceptsStatic(expr.getType()))
                ok = true;
        }

        if (!ok) {
            if (what == null)
                what = "expression";

            StringBuffer exp = new StringBuffer();
            for (int i = 0; i < accepted.length; ++i) {
                if (i > 0)
                    exp.append(" or ");
                exp.append(accepted[i].toString(this));
            }
            expr.module.error(errorCode, expr.offset,
                              "static type error for " + what +
                              ": expecting " + exp + " not " + expr.getType());
        }
    }

    // negative test: error if match
    public void staticTypingExclude(Expression expr, SequenceType[] accepted,
                                    String errorCode, String what)
    {
        if (!strictTyping)
            return;
        boolean ok = true;
        for (int i = accepted.length; --i >= 0;) {
            if (accepted[i].acceptsStatic(expr.getType()))
                ok = false;
        }

        if (!ok) {
            if (what == null)
                what = "expression";

            StringBuffer exp = new StringBuffer();
            for (int i = 0; i < accepted.length; ++i) {
                if (i > 0)
                    exp.append(" or ");
                exp.append(accepted[i].toString(this));
            }
            expr.module.error(errorCode, expr.offset,
                              "static type error for " + what +
                              ": must not match " + exp);
        }
    }

    // ----- Namespaces -------------------------------

    public void addDefaultNamespace(boolean forFun, String uri)
    {
        if (forFun) {
            setDefaultFunctionNamespace(uri);
        }
        else {
            setDefaultElementNamespace(uri);
            // We want it in mappings, because it is now how it is resolved
            declarePrefix("", uri);
        }
    }

    public boolean addNamespaceDecl(int location, String prefix, String uri)
    {
        NamespaceContext inScopeNS = getInScopeNS();
        // ??? cant redefine ??
        if (inScopeNS.isPrefixLocal(prefix))
            error("XQST0033", location, "namespace prefix '" + prefix
                                         + "' is already declared");
        declarePrefix(prefix, uri);
        return true;
    }

    // ----------------------------------------------------------------------

    /**
     * Gets the physical URI of the module (optionally defined in import).
     */
    public String getPhysicalURI()
    {
        return physicalURI;
    }

    public void setNamespaceURI(String uri)
    {
        moduleNS = NamespaceContext.unique(uri);
    }

    /**
     * Gets the namespace URI of the module.
     */
    public String getNamespaceURI()
    {
        return moduleNS;
    }

    // ---------------------------- a virer
    
    public int getCopyNSMode()
    {
        if (preserveNamespaces)
            return inheritNamespaces ? NS_PRES_INHERIT : NS_PRES_NOINHERIT;
        else
            return inheritNamespaces ? NS_NOPRES_INHERIT : NS_NOPRES_NOINHERIT;
    }

    
    
    public void storePragmas(Pragma[] pragmas)
    {
        this.pragmas = pragmas;
    }

    public Pragma[] getPragmas()
    {
        return pragmas;
    }

    public void setDefaultFtOptions(MatchOptions defaultFtOptions)
    {
        this.defaultFTOptions = defaultFtOptions;
    }

    public MatchOptions getDefaultFTOptions()
    {
        return defaultFTOptions;
    }

    public void addDeclaration(Object declaration)
    {
        declarations.add(declaration);
    }

    private ModuleContext importedModule(int m)
    {
        return (ModuleContext) importedModules.get(m);
    }

    public boolean alreadyImportedModule(String logicalURI)
    {
        for (int d = 0; d < declarations.size(); d++) {
            Object decl = declarations.get(d);
            if (!(decl instanceof ModuleContext))
                continue;
            ModuleContext mo = (ModuleContext) decl;
            
            if (logicalURI.equals(mo.getNamespaceURI()))
                return true;
        }
        return false;
    }

    public XQItemType lookForType(QName typeName)
    {
        // until implementation of schema import:
        return XQType.findItemType(typeName);
    }

    // ------------------ User Functions -----------------------------------

    public Function simpleFunctionLookup(QName name)
    {
        Function fun = (Function) functionMap.get(name);
        return fun;
    }

    public Function localFunctionLookup(QName name)
    {
        return simpleFunctionLookup(name);
    }

    public void declareFunction(Function function)
    {
        Prototype[] protos = function.getProtos();
        for (int i = 0; i < protos.length; i++) {
            functionMap.put(protos[i].qname, function);
        }
    }

    public Function functionLookup(QName name)
    {
        Function fun = localFunctionLookup(name);
        if (fun != null)
            return fun;
        // search in imported modules: (shallow search)
        for (int m = importedModules.size(); --m >= 0;) {
            fun = importedModule(m).localFunctionLookup(name);
            if (fun != null)
                return fun;
        }
        // finally search in predefined module:
        return predefined == null ? null : predefined.functionLookup(name);
    }

    // used only by function literals
    public Prototype functionLookup(QName name, int arity)
    {
        Function fun = functionLookup(name);
        if (fun == null)
            return null;
        for(Prototype proto : fun.getProtos()) {
            if(proto.qname == name && proto.accepts(arity))
                return proto;
        }
        return null;
    }

    public UserFunction.Signature importedFunction(Signature sig)
    {
        return (UserFunction.Signature) importedFunctions.get(sig);
    }

    public void declareFunction(QName name, Function function)
    {
        functionMap.put(name, function);
    }

//    public void authorizeJavaClass(String className)
//    {
//        if (predefined != null)
//            predefined.authorizeJavaClass(className);
//    }

    // ---------------- Global variables --------------------------------------

    public GlobalVariable lookforGlobalVariable(QName name)
    {
        GlobalVariable g = localGlobalVarLookup(name);
        if (g != null)
            return g;
        g = importedGlobal(name);
        if (g != null)
            return g;
        return predefined == null ? 
                null : predefined.lookforGlobalVariable(name);
    }

    public GlobalVariable importedGlobal(QName name)
    {
        return (GlobalVariable) importedGlobals.get(name);
    }

    /**
     * Defines a global variable in this module.
     */
    public GlobalVariable defineGlobal(QName qname, SequenceType type)
    {
        GlobalVariable g = declareGlobal(qname, type);
        addDeclaration(g); // otherwise cant be initialized
        return g;
    }

    // --------- local variable management during static analysis:

    public void resetLocals()
    {
        maxLocalSize = 0;
        localCnt = localIntCnt = localDoubleCnt = localStringCnt = localItemCnt = 0;
        locals = lastLocal = new LocalVariable(null, null, null);
    }

    public void popLocalVariables(LocalVariable mark)
    {
        lastLocal = mark;
        // System.err.println("pop locals -> "+lastLocal.name);
    }

    public LocalVariable defineLocalVariable(QName name, XQType type,
                                             Expression declaring)
    {
        // check for duplicates (again!)
        // if(lookforLocalVariable(name) != null)
        // error(declaring, "", "duplicate variable " + name);
        
        LocalVariable decl = newLocal(name, type, declaring);
        lastLocal.addAfter(decl);
        lastLocal = decl;
        return decl;
    }

    public LocalVariable newLocal(QName name, XQType type, Expression declaring)
    {
        int id = ++genId;
        if (name == null)
            name = IQName.get("_" + id);
        LocalVariable decl = new LocalVariable(name, type, declaring);
        decl.id = id;
        return decl;
    }

    public LocalVariable firstLocalVariable()
    {
        return locals.succ;
    }

    public LocalVariable latestLocalVariable()
    {
        // System.err.println("mark locals -> "+lastLocal.name);
        return lastLocal;
    }
    
    public void pushLocalsLevel()
    {
         LocalVars ctx = new LocalVars();
         ctx.locals = locals;
         ctx.lastLocal = lastLocal;
         ctx.up = upLevels;
         ctx.maxLocalSize = maxLocalSize;
         upLevels = ctx;
         resetLocals();
    }

    public void popLocalsLevel()
    {
        if(upLevels == null)
            throw new IllegalStateException("no closure");
        locals = upLevels.locals;
        lastLocal = upLevels.lastLocal;
        maxLocalSize = upLevels.maxLocalSize;
        upLevels = upLevels.up;
    }


    public LocalVariable lookforLocalVariable(QName name)
    {
        // - System.err.println("lookforLocalVariable "+name);
        for (LocalVariable v = lastLocal; v != null; v = v.pred) {
            if (v.name == name)
                return v;
        }
        return null;
    }

    public Local lookforClosureVariable(QName name)
    {
        LocalVariable decl = lookforLocalVariable(name);
        if (decl != null)
            return new Local(decl);
        int upLevel = 1;
        for(LocalVars ctx = upLevels; ctx != null; ctx = ctx.up)
        {
            for (LocalVariable v = ctx.lastLocal; v != null; v = v.pred) {
                if (v.name == name) {
                    Local var = new Local(v);
                    var.upLevel = upLevel;
                    return var;
                }
            }
            ++ upLevel;
        }
        return null; 
    }

    public void dumpLocals(String message)
    {
        System.err.println(message);
        recDump(locals, 0);
    }

    private void recDump(LocalVariable seq, int depth)
    {
        for (LocalVariable loc = seq; loc != null; loc = loc.replacing) {
            for (int d = 0; d < depth; d++)
                System.err.print("   ");
            System.err.println(depth + "  local $" + loc.name + " pos "
                               + loc.offset + " addr " + loc.address);
            recDump(loc.succ, depth + 1);
        }
    }

    public int allocateLocalAddress(LocalVariable root)
    {
        if (root == null)
            root = locals;
        int addrType = -1;
        XQType type = root.getType();
        if (root.name != null) {
            if (root.address < 0 || type.getOccurrence() != XQType.OCC_EXACTLY_ONE) {
                addrType = EvalContext.LAST_REGISTER; // in stack
                root.address = EvalContext.LAST_REGISTER + localCnt++;
                maxLocalSize = Math.max(root.address, maxLocalSize);
            }
            else if (XQType.INTEGER.equals(type)) {
                addrType = EvalContext.INT_REGISTER;
                root.address = EvalContext.INT_REGISTER + localIntCnt++;
                if (localIntCnt > EvalContext.MAX_REGISTER)
                    error("BUG", root, "BUG: too many 'integer registers: " + localIntCnt);
                if(trace) System.err.println("var "+root.name+" -> "+root.address+ " / " + localIntCnt);
            }
            else if (type == XQType.STRING) {
                addrType = EvalContext.STRING_REGISTER;
                root.address = EvalContext.STRING_REGISTER + localStringCnt++;
                if (localStringCnt > EvalContext.MAX_REGISTER)
                    error("BUG", root, "BUG: too many 'string registers");
            }
            else if (type == XQType.DOUBLE) {
                addrType = EvalContext.DOUBLE_REGISTER;
                root.address = EvalContext.DOUBLE_REGISTER + localDoubleCnt++;
                if (localDoubleCnt > EvalContext.MAX_REGISTER)
                    error("BUG", root, "BUG: too many 'double registers");
            }
            else {
                addrType = EvalContext.ITEM_REGISTER;
                root.address = EvalContext.ITEM_REGISTER + localItemCnt++;
                if (localItemCnt > EvalContext.LAST_REGISTER
                                   - EvalContext.ITEM_REGISTER)
                    error("BUG", root, "BUG: too many item registers");
            }
            // System.err.println("allocate "+root.name+" at "+root.address);
        }
        LocalVariable simult = root.succ;
        for (; simult != null; simult = simult.replacing) {
            allocateLocalAddress(simult);
        }
        // - System.err.println("address for "+root.name+" -> "+root.address);
        switch (addrType) {
        case EvalContext.INT_REGISTER:
            --localIntCnt;
            break;
        case EvalContext.STRING_REGISTER:
            --localStringCnt;
            break;
        case EvalContext.DOUBLE_REGISTER:
            --localDoubleCnt;
            break;
        case EvalContext.ITEM_REGISTER:
            --localItemCnt;
            break;
        case EvalContext.LAST_REGISTER:
            --localCnt;
            break;
        }
        return maxLocalSize;
    }

    // ---------------------- type of dot -------------------------------------
    
    public void pushDotType(XQType type)
    {
        dotTypeStack = new DotType(type.itemType(), dotTypeStack);
    }

    public void popDotType()
    {
        dotTypeStack = dotTypeStack.up;
    }

    public XQItemType getDotType()
    {
        return dotTypeStack == null ? null : dotTypeStack.type;
    }

    private static class DotType
    {
        DotType up;

        XQItemType type;

        DotType(XQItemType ty, DotType uptype)
        {
            type = ty;
            up = uptype;
        }
    }

    public boolean sObs()
    {
        return getStrictCompliance();
    }

    public static QName xqueryErrorCode(String xqcode)
    {
        return IQName.get(NamespaceContext.ERR, xqcode);
    }

    void setMessageTarget(MessageReceiver messageTarget)
    {
        this.messageTarget = messageTarget;
    }

    MessageReceiver getMessageTarget()
    {
        return messageTarget;
    }

    public void storeOption(IQName optionName, String value)
    {
        if(optionMap == null)
            optionMap = new HashMap<QName, String>();
        optionMap.put(optionName, value);
    }

    protected GlobalVariable getCurInitVar()
    {
        return curInitVar;
    }
}
