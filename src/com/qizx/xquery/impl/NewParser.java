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
package com.qizx.xquery.impl;

import com.qizx.api.*;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.Thesaurus;
import com.qizx.queries.FullText;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.util.NamespaceContext;
import com.qizx.util.basic.Util;
import com.qizx.util.basic.XMLUtil;
import com.qizx.xdm.BaseNodeFilter;
import com.qizx.xdm.Conversion;
import com.qizx.xdm.DocumentTest;
import com.qizx.xdm.IQName;
import com.qizx.xdm.NodeFilter;
import com.qizx.xquery.*;
import com.qizx.xquery.SequenceType;
import com.qizx.xquery.dt.FunctionType;
import com.qizx.xquery.dt.LanguageType;
import com.qizx.xquery.dt.NodeType;
import com.qizx.xquery.fn.UserFunction;
import com.qizx.xquery.op.*;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.WindowClause.Condition;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Parser for XPath 2.0 / XQuery 1.0 expressions.
 */
public class NewParser extends NewLexer
{
    public final static String XQUERY_VERSION = "1.1";
    
    static final String XMLNS = "xmlns";
    private static final QName UNORDERED =
        IQName.get(NamespaceContext.FN, "unordered");

    private static final String[] PreserveStrip_Keywords =
        { "preserve", "strip" };
    private static final String[] Ordering_Keywords =
        { "ordered", "unordered" };
    private static final String[] EmptyOrder_Keywords =
        { "least", "greatest" };
    private static final String[] CopyNS_Preserve_Keywords =
        { "preserve", "no-preserve" };
    private static final String[] CopyNS_Inherit_Keywords =
        { "inherit", "no-inherit" };

    private static final String CPF_TEXT =
        "on full-text expression: should be either inside or outside parentheses";

    private static final int DFLAG_ASSIGNABLE = 1;
    private static final int DFLAG_UNASSIGNABLE = 1 << 1;
    private static final int DFLAG_SIMPLE = 1 << 2;
    private static final int DFLAG_PUBLIC = 1 << 3;
    private static final int DFLAG_PRIVATE = 1 << 4;
    private static final int DFLAG_UPDATING = 1 << 5;
    private static final int DFLAG_SEQUENTIAL = 1 << 6;
    private static final int DFLAG_DETERMINISTIC = 1 << 7;
    private static final int DFLAG_UNDETERMINISTIC = 1 << 8;

    // common lexical patterns:
    private static final String COMMA = ", ";
    private static final String SEMICOLON = ";";
    private static final String MORE_VAR = ", $ %Q";

    
    private ModuleManager moduleManager;
    
    private Pragma[] pragmas = new Pragma[0];
    private boolean preserveSpace = false;
    private int prologState;
    
    // declarations:
    private HashSet<String> declared = new HashSet<String>();
    private int declFlags;

    private ModuleResolver moduleResolver;

    private int lastTokenStart;
    private StringBuilder textBuf = new StringBuilder();

    
    /**
     * Parses input inside a properly built module context
     */
    public NewParser(ModuleManager moduleManager)
    {
        this.moduleManager = moduleManager;
        moduleResolver = moduleManager.getResolver();
    }

    /**
     * Parses and returns a main Query.
     * 
     * @param input the actual text to parse
     * @param uri of the query source (for messages), or null if not
     *        applicable.
     * @param query prebuilt Query (static analysis is not performed).
     * @throws CompilationException on first lexical or syntactic error (no
     *         recovery)
     */
    public void parseQuery(MainQuery query, String input, String uri)
        throws CompilationException
    {
        currentModule = query;
        startLexer(input);
        setupModule(input, uri);

        parseOptVersion();
        
        parseNewProlog();
        
        // main query body:
        query.body = parseExpr();
        eat(SEMICOLON);
        if (!atEndOfInput())
            syntax("unrecognized characters at end of query");
        currentModule.storePragmas(pragmas);
    }
    
    /**
     * Parses a library module (used by the Module Manager).
     * 
     * @param module being parsed (already created by the calling ModuleManager
     *  to cope with cyclical references.)
     * @param input the actual text to parse
     * @param uri of the query source (for messages), or null if not
     *        applicable.
     * @return a parsed Module (static analysis is not performed).
     */
    public BasicStaticContext parseModule(ModuleContext module, String input,
                                          String uri)
        throws CompilationException
    {
        startLexer(input);
        currentModule = module;
        setupModule(input, uri);

        parseOptVersion();
        // module namespace <prefix> = <StringLiteral> ;
        want("module namespace %N = %S ; ");
        String prefix = savedName;
        String moduleURI = extractStringToken();

        currentModule.setNamespaceURI(moduleURI);
        currentModule.addNamespaceDecl(lastTokenStart, prefix, moduleURI);

        // declarations:
        parseNewProlog();

        if (!atEndOfInput())
            syntax("unrecognized characters at end of module");
        currentModule.storePragmas(pragmas);
        return currentModule;
    }
    
    private void setupModule( String input, String uri )
    {
        currentModule.setSource(input, uri);
    }
    
    //  This code was generated from the XML specification, then
    //  manually completed and cleaned.
    
    private void parseOptVersion() throws CompilationException
    {
        if(!eat("xquery version %S "))
            return;
        String version = extractStringToken();
        if(version.compareTo(XQUERY_VERSION) > 0 || // hmmm
           version.compareTo("1.0") < 0)
            currentModule.error( "XQST0031", prevTokenLoc,
                                 "XML Query version " + version
                                 + " not supported, the current version is "
                                 + XQUERY_VERSION);
        if(eat("encoding %S ")) {
            String encoding = extractStringToken();
            if(!XMLUtil.isNCName(encoding))
                currentModule.error("XQST0087", prevTokenLoc,
                                    "improper encoding name: " + encoding);
            // What TODO with that rubbish?
            else
                currentModule.warning(prevTokenLoc, "Encoding currently ignored");
        }
        want(SEMICOLON);
    }

    private void parseNewProlog() throws CompilationException
    {
        prologState = 0;
        for(;;)
        {
            if(eat("declare ")) {
                // swallow misc flags after 'declare'
                parseDeclFlags();
                // actual declaration: function, variable
                parseDeclaration();
            }
            else if(eat("import module ")) {
                checkPrologState(1, "import module");
                parseImportModule();
            }
            else if(eat("import schema ")) {
                checkPrologState(1, "import schema");
                parseImportSchema();
            }
            else break;
        }
    }

    private void checkPrologState(int state, String decl)
    {
        if(state >= prologState)
            prologState = state;
        else
            currentModule.error(ERR_SYNTAX, tokenStart,
                                decl + " declaration may not appear after functions, variables");
    }

    // new prolog parsing for support of Scripting and all XQ 1.1
    private void parseDeclaration() throws CompilationException
    {
        if(eat("namespace")) {
            checkPrologState(1, "namespace");
            parseNamespaceDecl();
        }
        else if(eat("default element namespace")) {
            checkPrologState(1, "default element namespace ");
            parseDefaultNamespaceDecl(false);
        }
        else if(eat("default function namespace")) {
            checkPrologState(1, "default function namespace ");
            parseDefaultNamespaceDecl(true);
        }
        else if(eat("default order empty")) {
            checkPrologState(1, "ordering");
            int emptyOrder = checkOptionKeyword(EmptyOrder_Keywords);
            checkDeclared("empty order", "XQST0065");
            currentModule.setDefaultOrderEmptyGreatest(emptyOrder == 1);
        }
        else if(eat("ordering")) {
            checkPrologState(1, "ordering");
            int order = checkOptionKeyword(Ordering_Keywords);
            checkDeclared("ordering", "XQST0065");
            currentModule.setOrderingMode(order == 0 ? 
                    XQueryContext.ORDERED : XQueryContext.UNORDERED);
        }
        else if(eat("boundary-space")) {
            checkPrologState(1, "boundary-space");
            checkDeclared("boundary-space", "XQST0068");
            preserveSpace = checkOptionKeyword(PreserveStrip_Keywords) == 0;
            currentModule.setBoundarySpacePolicy(preserveSpace ?
                            XQueryContext.PRESERVE : XQueryContext.NO_PRESERVE);
        }
        else if(eat("construction")) {
            checkPrologState(1, "construction");
            int mode = checkOptionKeyword(PreserveStrip_Keywords); 
            checkDeclared("construction", "XQST0067");
            currentModule.setConstructionMode(mode == 0 ? XQueryContext.PRESERVE
                                                     : XQueryContext.NO_PRESERVE);
        }
        else if(eat("copy-namespaces")) {
            checkPrologState(1, "copy-namespaces");
            int preserve = checkOptionKeyword(CopyNS_Preserve_Keywords);
            want(COMMA);
            int inherit = checkOptionKeyword(CopyNS_Inherit_Keywords);
            checkDeclared("copy-namespaces", "XQST0055");
            currentModule.setNamespaceInheritMode(inherit == 0 ?
                    XQueryContext.INHERIT : XQueryContext.NO_INHERIT);
            currentModule.setNamespacePreserveMode(preserve == 0?
                    XQueryContext.PRESERVE : XQueryContext.NO_PRESERVE);
        }
        else if(eat("default collation %S")) {
            checkPrologState(1, "default collation");
            String collation = extractStringToken();
            checkDeclared("default collation", "XQST0038");
            try {
                currentModule.setDefaultCollation(collation);
            }
            catch (DataModelException e) {
                currentModule.error("XQST0038", prevTokenLoc,
                                    e.getMessage());
            }
        }
        else if(eat("base-uri %S")) {
            checkPrologState(1, "base-uri");
            checkDeclared("static base-uri", "XQST0032");
            currentModule.setBaseURI(extractStringToken());
        }
        else if(eat("revalidation lax") ||
                eat("revalidation strict") ||
                eat("revalidation skip")) {
            currentModule.error("XUST0026", tokenStart,
                                "revalidation is not supported");
        }
        else if(eat("ft-option")) {
            checkPrologState(1, "full-text option");
            MatchOptions opt = new MatchOptions();
            parseFTMatchOptions(opt);
            currentModule.setDefaultFtOptions(opt);
        }
        else if(eat("option %Q %S")) {
            checkPrologState(2, "option");
            IQName optionName = resolveQName(null);
            if(optionName.hasNoNamespace())
                currentModule.error("XPST0081", tokenStart, 
                                    "blank namespace not allowed");
            String value = extractStringToken();
            currentModule.storeOption(optionName, value);
        }
        else if(eat("variable")) {
            checkPrologState(2, "variable");
            parseGlobalVarDecl();
        }
        else if(eat("function")) {
            // flags already swallowed
            checkPrologState(2, "function");
            parseFunctionDecl();
        }
        else {
            syntax("expecting declaration");
        }
        eat(" ; "); // optional
    }

    private boolean checkDeclared(String decl, String errorCode)
    {
        if(declared.contains(decl)) {
            currentModule.error(errorCode, prevTokenLoc,
                                decl + " already declared");
            return false;
        }
        declared.add(decl);
        return true;
    }

    private void parseDeclFlags()
        throws CompilationException
    {
        declFlags = 0;
        for(;;) {
            int flag = 0;
            if(eat("assignable")) {
                flagAdd(DFLAG_ASSIGNABLE);
                flagConflict(DFLAG_UNASSIGNABLE);
            }
            else if(eat("unassignable")) {
                flagAdd(DFLAG_UNASSIGNABLE);
                flagConflict(DFLAG_ASSIGNABLE);
            }
            else if(eat("simple")) {
                flagAdd(DFLAG_SIMPLE);
            }
            else if(eat("updating")) {
                flagAdd(DFLAG_UPDATING);
            }
            else if(eat("sequential")) {
                flagAdd(DFLAG_SEQUENTIAL);
            }
            else if(eat("public")) {
                flagAdd(DFLAG_PUBLIC);
                flagConflict(DFLAG_PRIVATE);
            }
            else if(eat("private")) {
                flagAdd(DFLAG_PRIVATE);
                flagConflict(DFLAG_PUBLIC);
            }
            else if(eat("deterministic")) {
                flagAdd(DFLAG_DETERMINISTIC);
                flagConflict(DFLAG_UNDETERMINISTIC);
            }
            else if(eat("nondeterministic")) {
                flagAdd(DFLAG_UNDETERMINISTIC);
                flagConflict(DFLAG_DETERMINISTIC);
            }
            else break;
        }
    }
    
    private void flagConflict(int flag)
    {
        if((declFlags & flag) != 0)
            currentModule.error("XQST0106", prevTokenLoc, 
                                "'" + lastToken() +"' conflicts with previous indicators");
         // TODO
    }

    private void flagAdd(int flag)
    {
        if((declFlags & flag) != 0)
            currentModule.error("XQST0106", prevTokenLoc, 
                                "duplicate indicator '" + lastToken() +"'");
        declFlags |= flag;
    }

    // Returns the URL
    private String parseNamespaceDecl()
        throws CompilationException
    {
        want("%N = %S");
        String prefix = savedName;
        String url = extractStringToken();

        if (prefix.equals("xml") || prefix.equals("xmlns")
            || url.equals(NamespaceContext.XML))
            currentModule.error("XQST0070", tokenStart,
                                "illegal namespace declaration");
        currentModule.addNamespaceDecl(lastTokenStart, prefix, url);
        return url;
    }
    
    private String parseDefaultNamespaceDecl(boolean forFun)
        throws CompilationException
    {
        want("%S");
        String url = extractStringToken();
        if(checkDeclared(forFun ? "function namespace" : "element namespace",
                         "XQST0066"))
            currentModule.addDefaultNamespace(forFun, url);
        return url;
    }
    
    private void parseGlobalVarDecl()
        throws CompilationException
    {
        want("$ %Q");
        int here = tokenStart;
        QName name = resolveVarName();
        
        String moduleNS = currentModule.getNamespaceURI();
        if (moduleNS != NamespaceContext.LOCAL_NS
            && moduleNS != name.getNamespaceURI())
            currentModule.error("XQST0048", prevTokenLoc,
                                "namespace of variable name "
                                + currentModule.prefixedName(name)
                                + " should match the module namespace");
        
        SequenceType varType = null;
        if (eat("as "))
            varType = parseSequenceType();
        
        Expression init = null;
        if (!eat("external"))
            if(eat(":=")) {
                init = parseExprSingle();
            }
            else {
                want("{");
                init = parseEnclosedExpr();
            }
        
        GlobalVariable global =
            new GlobalVariable(name, varType, init);
        currentModule.addDeclaration(locate(global, here));
    }

    // FunctionDefn ::=
    //   DeclareFunction QNameLpar ParamList? (Rpar | RparAs SequenceType) EnclosedExpr
    //
    private UserFunction parseFunctionDecl()
        throws CompilationException
    {
        want("%Q (");
        QName name = resolveQName(currentModule.getDefaultFunctionNamespace());
        String ns = name.getNamespaceURI();
        if (ns == NamespaceContext.EMPTY)
            currentModule.error("XQST0060", prevTokenLoc,
                                "function name has no namespace: " + name);
        if (currentModule.getNamespaceURI() == NamespaceContext.LOCAL_NS) {
            // main: allow any namespace but predefined ones
            if (ns == NamespaceContext.FN || ns == NamespaceContext.XSD
                || ns == NamespaceContext.XML || ns == NamespaceContext.XSI)
                currentModule.error("XQST0045", prevTokenLoc,
                                    "illegal namespace for function "
                                        + currentModule.prefixedName(name));
        }
        else // module:
        if (ns != currentModule.getNamespaceURI()) {
            currentModule.error("XQST0048", prevTokenLoc,
                                "namespace of function name "
                                    + currentModule.prefixedName(name)
                                    + " does not match module namespace");
        }
        
        UserFunction fun =
            (UserFunction) currentModule.localFunctionLookup(name);
        if (fun == null) {
            fun = new UserFunction();
        }
    
        UserFunction.Signature proto = new UserFunction.Signature(name);
        proto.module = currentModule;
        proto.offset = prevTokenLoc;
        proto.updating = (declFlags & DFLAG_UPDATING) != 0;

        // the function is immediately visible (forward references accepted):
        // NB: done here because we need the name in proto.
        // Doesnt harm to be put twice in map.
        currentModule.declareFunction(name, fun);
        currentModule.addDeclaration(proto);
    
        parseFunctionProto(proto);
            
        if (!fun.addPrototype(proto))
            currentModule.error("XQST0034", proto.offset,
                            "duplicate prototype for function '" + name + "'");
    
        Expression body = null;
        if (!eat("external")) {
            want("{");
            body = parseEnclosedExpr();
        }
        proto.body = body;
        want(SEMICOLON);            
        return fun;
    }

    private void parseFunctionProto(UserFunction.Signature proto)
        throws CompilationException
    {
        if (!eat(")")) {
            for(;;) {
                want("$ %Q");
                QName argName = resolveVarName();
                XQType argType = XQType.ANY;
                if(eat("as "))
                    argType = parseSequenceType();
                proto.arg(argName, argType);
                if(!eat(COMMA))
                    break;
            }
            want(")");
        }        
        
        if (eat("as "))
            proto.returnType = proto.declaredReturnType = parseSequenceType();
    }

    private void parseImportModule()
        throws CompilationException
    {
        // 'import module' [ 'namespace' prefix '=' ] ns [ 'at' url [ , url ]*]
        int here = tokenStart;
        String prefix = null, nsuri, loc = null;
        int prefixHere = here;
        if(eat("namespace %N = ")) {
            prefixHere = tokenStart;
            prefix = savedName;
        }
        if(!eatStringLiteral())
            errorExpect("namespace URI");
        
        nsuri = extractStringToken();
        
        ArrayList atLocs = null;
        if(eat("at %S")) {
            loc = extractStringToken();
            //loc = FileUtil.resolve(baseUri, loc);
            atLocs = new ArrayList();
            atLocs.add(loc);
            while(eat(", %S")) {
                loc = extractStringToken();
                atLocs.add(loc);
            }
        }
        eat(SEMICOLON); 
        moduleImport(here, nsuri, atLocs);
        if(prefix != null)
            currentModule.addNamespaceDecl(prefixHere, prefix, nsuri);
    }

    private void parseImportSchema()
        throws CompilationException
    {
        int here = tokenStart;
        String nsURL = "";
        if(eat("default element namespace")) {
            nsURL = parseDefaultNamespaceDecl(false);
        }
        else if(eat("namespace %?%N")) {
            nsURL = parseNamespaceDecl();
        }
        else if(eat("%S")) {
            nsURL = extractStringToken();
            // It seems that this islike 'default element namespace' ?
            currentModule.addDefaultNamespace(false, nsURL);
        }
        else {
            syntax("improper schema import");
        }
        
        ArrayList locations = new ArrayList();
        if(eat("at %S")) {
            locations.add(extractStringToken());
            while(eat(", %S")) {
                locations.add(extractStringToken());
            }
        }
        eat(SEMICOLON); 
        currentModule.error("XQST0009", here, "schema import is not supported");
    }

    // SequenceExpr ::= Expr ( Comma Expr )*
    private Expression parseExpr()
        throws CompilationException
    {
        Expression e = parseExprSeq();
        if (!eat(SEMICOLON))
            return e;
        ApplyExpr seq = new ApplyExpr();
        locate(seq);
        seq.addExpr(e);
        // rotten syntax: must look-ahead
        while (!atEndOfInput() && !see(")") && !see("]") && !see("}")) {
            seq.addExpr(parseExprSeq());
            want(SEMICOLON);
        }
        return seq;
    }

    private Expression parseExprSeq()
        throws CompilationException
    {
        Expression e = parseExprSingle();
        if (!eat(COMMA))
            return e;
        SequenceExpr seq = new SequenceExpr();
        locate(seq);
        seq.addExpr(e);
        seq.addExpr(parseExprSingle());
        while (eat(COMMA))
            seq.addExpr(parseExprSingle());
        return seq;        
    }
    
    private Expression parseExprSingle()
        throws CompilationException
    {
        if(eat("for $ %Q"))
            return parseFLWRExpr(parseForClause(true));
        if(eat("let $ %Q"))
            return parseFLWRExpr(parseLetClause(false));
        if(eat("let score $ %Q"))
            return parseFLWRExpr(parseLetClause(true));
        if(eat("for tumbling window $ %Q"))
            return parseFLWRExpr(parseWindowClause(false));
        if(eat("for sliding window $ %Q"))
            return parseFLWRExpr(parseWindowClause(true));

        if(eat("every $ %Q"))
            return parseQuantifiedExpr(true);
        if(eat("some $ %Q"))
            return parseQuantifiedExpr(false);

        if(eat("if ("))
            return parseIfExpr();
        if(eat("typeswitch ("))
            return parseTypeswitchExpr();
        if(eat("switch ("))
            return parseSwitchExpr();

        if(eat("block {"))
            return parseBlock();
        if(eat("exit returning"))
            return parseExitExpr();
        if(eat("while ("))
            return parseWhileExpr();

            // XUpdate:
        if(eat("insert nodes") || eat("insert node"))
            return parseInsertNodes();
        if(eat("delete nodes") || eat("delete node"))
            return parseDeleteNodes();
        if(eat("replace node"))
            return parseReplaceNode(false);
        if(eat("replace value of node"))
            return parseReplaceNode(true);
        if(eat("rename node"))
            return parseRenameNode();
        if(eat("copy $ %Q"))
            return parseTransform();
        // default: or expression, can be followed by assign
        Expression e = parseOrExpr();
        if(eat(":="))
            return parseAssign2(e);
        return e;
    }
    
    // FLWRExpr ::= HeadClause IntermediateClause* ReturnClause
    private Expression parseFLWRExpr(VarClause firstClause)
        throws CompilationException
    {
        FLWRExpr flower = new FLWRExpr();
        locate(flower, firstClause.offset);
        flower.addClause(firstClause);
        
        // we are at end of first for/let clause: swallow additional clauses
        if(eat(MORE_VAR)) {
            if(firstClause instanceof ForClause) {
                flower.addClause(parseForClause(true));
                while(eat(MORE_VAR))
                    flower.addClause(parseForClause(true));
            }
            else if(firstClause instanceof LetClause) {
                LetClause let = (LetClause) firstClause;
                flower.addClause(parseLetClause(let.score));
                while(eat(MORE_VAR))
                    flower.addClause(parseLetClause(let.score));
            }
            else
                syntax("unexpected comma");
        }
        
        boolean afterGroupBy = false;
        
        // from 1.1 intermediate clauses are in any number and order
        for(;;) {
            if(eat("for $ %Q")) {
                flower.addClause(parseForClause(true));
                while(eat(MORE_VAR))
                    flower.addClause(parseForClause(true));
            }
            else if(eat("let $ %Q")) {
                if(afterGroupBy)
                    flower.addPostLetClause(parseLetClause(false)); // FIXME
                else
                    flower.addClause(parseLetClause(false));
                while(eat(MORE_VAR))
                    if(afterGroupBy)
                        flower.addPostLetClause(parseLetClause(false)); // FIXME
                    else
                        flower.addClause(parseLetClause(false));
            }
            else if(eat("let score $ %Q")) {
                flower.addClause(parseLetClause(true));
                while(eat(", "))
                    flower.addClause(parseLetClause(true));
            }
            else if(eat("for tumbling window $ %Q"))
                flower.addClause(parseWindowClause(false));
            else if(eat("for sliding window $ %Q"))
                flower.addClause(parseWindowClause(true));
            else if (eat("where")) { // FIXME no spcial field 'where'
                if(afterGroupBy)
                    flower.postGroupingWhere = parseExprSingle();
                else
                    flower.where = parseExprSingle();
            }
            else if(eat("group by")) {
                GroupingVariable keyVar = parseGroupBySpec();
                flower.groupingKeys = new GroupingVariable[] { keyVar };
                flower.postGroupingLets = new LetClause[0];
            
                while(eat(COMMA)) {
                    keyVar = parseGroupBySpec();

                    VarReference[] old = flower.groupingKeys;
                    flower.groupingKeys = new GroupingVariable[old.length + 1];
                    System.arraycopy(old, 0, flower.groupingKeys, 0, old.length);
                    flower.groupingKeys[old.length] = keyVar;
                }
                afterGroupBy = true;
            }
            else if((flower.stableOrder = eat("stable order by"))
                    || eat("order by")) {
                flower.addOrderSpec(parseOrderSpec());
                while (eat(COMMA))
                    flower.addOrderSpec(parseOrderSpec());
            }
            else if(eat("count $ %Q")) {
                
            }
            else break;
        }
        want("return");
        flower.expr = parseExprSingle();
        return flower;
    }
    
    // variable name already crunched
    // used in For, Some, Every
    private ForClause parseForClause(boolean withAt)
        throws CompilationException
    {
        int here = tokenStart;
        ForClause clause = new ForClause(resolveVarName());
        locate(clause, here);
        if (eat("as"))
            clause.declaredType = parseSequenceType();
        if (withAt && eat("at $ %Q")) {
            clause.position = resolveVarName();
        }
        if(eat("score $ %Q")) {
            clause.score = resolveVarName();
        }
        want("in");
        clause.expr = parseExprSingle();
        return clause;
    }

    // variable name already crunched
    private LetClause parseLetClause(boolean score)
        throws CompilationException
    {
        int here = tokenStart;
        LetClause clause = new LetClause(resolveVarName());
        clause.score = score;
        locate(clause, here);
        if (eat("as")) {
            if(score)
                currentModule.error("XPST0003", prevTokenLoc,
                                    "invalid type declaration after 'score'");
            clause.declaredType = parseSequenceType();
        }
        want(":=");
        clause.expr = parseExprSingle();
        return clause;
    }
    
    private VarClause parseWindowClause(boolean sliding)
        throws CompilationException
    {
        int here = tokenStart;
        QName mainVar = resolveVarName();
        WindowClause clause = new WindowClause(mainVar, sliding);
        locate(clause, here);
        if (eat("as"))
            clause.declaredType = parseSequenceType();
        want("in");
        clause.expr = parseExprSingle();
        
        // start/end conditions:
        want("start");
        parseWindowCond(clause.startCond);
        
        if(eat("only end")) {
            clause.onlyEnd = true;
            parseWindowCond(clause.endCond);
        }
        else if(eat("end")) {
            parseWindowCond(clause.endCond);
        }
        else if(!sliding) { // tumbling: optional end clause
            clause.endCond = null;
        }
        else
            errorExpect("end condition");
        return clause;
    }

    private void parseWindowCond(Condition cond)
        throws CompilationException
    {
         if(eat("$ %Q")) {
             cond.itemVarName = resolveVarName();
         }
         if(eat("at $ %Q")) {
             cond.atVarName = resolveVarName();
         }
         if(eat("previous $ %Q")) {
             cond.previousVarName = resolveVarName();
         }
         if(eat("next $ %Q")) {
             cond.nextVarName = resolveVarName();
         }
         want("when");
         cond.cond = parseExprSingle();
    }

    private OrderSpec parseOrderSpec( ) throws CompilationException 
    {
        int here = currentPos();
        OrderSpec spec = new OrderSpec(parseExprSingle());
        locate(spec, here);
        if (eat("descending"))
            spec.descending = true;
        else
            eat("ascending");
        spec.emptyGreatest = currentModule.getDefaultOrderEmptyGreatest();
        if (eat("empty greatest"))
            spec.emptyGreatest = true;
        else if (eat("empty least"))
            spec.emptyGreatest = false;
        if (eat("collation %S")) {
            spec.collation = extractStringToken();
        }
        return spec;
    }
    
    private GroupingVariable parseGroupBySpec() throws CompilationException
    {
        want("$ %Q");
        QName name = resolveVarName();
        String collation = null;
        if(eat("collation %S")) {
            collation = extractStringToken();
        }
        GroupingVariable var = new GroupingVariable(name, collation);
        locate2(var);
        return var;
    }

    // QuantifiedExpr ::=
    //	    ( (Some | Every) VarClause ( Comma VarClause )*  Satisfies )* ExprSingle
    // VarClause ::= VarName ( TypeDeclaration )?  In Expr
    private Expression parseQuantifiedExpr(boolean every)
        throws CompilationException 
    {
        QuantifiedExpr q = new QuantifiedExpr(every);
        locate(q);
        q.addVarClause( parseForClause(false) );
        while(eat(MORE_VAR)) {
            q.addVarClause( parseForClause(false) );
        }
        want("satisfies");
        q.cond = parseExprSingle();
        return q;
    }
    
    private Expression parseSwitchExpr()
        throws CompilationException
    {
        int here = tokenStart;
        SwitchExpr sw = new SwitchExpr(parseExpr());
        locate(sw, here);

        want(")");
        
        while (eat("case")) {
            SwitchExpr.Case cc = new SwitchExpr.Case();
            locate2(cc);
            sw.addCase(cc);
            cc.key = parseExprSingle();
            // empty return if followed by 'case' 
            if(eat("return")) {
                cc.expr = parseExprSingle();
            }
        }
        
        // default:
        want("default");
        SwitchExpr.Case def = new SwitchExpr.Case();
        locate2(def);
        want("return");
        def.expr = parseExprSingle();
        sw.addCase(def);
        return sw;
    }

    // TypeswitchExpr ::= 
    //  (TypeswitchLpar Expr ) CaseClause+ Default ( VariableIndicator VarName )?  Return  )*  IfExpr 
    // CaseClause ::= Case (
    private Expression parseTypeswitchExpr()
        throws CompilationException
    {
        int here = tokenStart;
        TypeswitchExpr sw = new TypeswitchExpr(parseExpr());
        locate(sw, here);
        want(")");
        while (eat("case ")) {
            TypeCaseClause cc = new TypeCaseClause();
            locate2(cc);
            sw.addCaseClause(cc);
            
            if (eat("$ %Q as")) {
                cc.variable = resolveVarName();
            }
            cc.declaredType = parseSequenceType();
            want("return");
            cc.expr = parseExprSingle();
        }
        want("default");
        TypeCaseClause defc = new TypeCaseClause();
        locate2(defc);
        sw.addCaseClause(defc);
        if (eat("$ %Q")) {
            defc.variable = resolveVarName();
        }
        want("return");
        defc.expr = parseExprSingle();
        return sw;
    }

    private Expression parseWhileExpr() throws CompilationException
    {
        int here = tokenStart;
        Expression cond = parseExprSingle();
        want(") {");
        Expression block = parseBlock();
        return locate(new WhileExpr(cond, block), here);
    }

    // a block after the leading brace
    private Expression parseBlock() throws CompilationException
    {
        BlockExpr block = new BlockExpr();
        locate(block, prevTokenLoc);
        // variables:
        while(eat("declare")) {
            block.addClause(parseBlockVar());
            while(eat(COMMA)) {
                block.addClause(parseBlockVar());
            }
            want(SEMICOLON);
        }
        // body:
        block.body = parseExpr();
        want("}");
        return block;
    }
    
    private LetClause parseBlockVar()
        throws CompilationException
    {
        int here = currentPos();
        want("$ %Q");
        LetClause clause = new LetClause(resolveVarName());
        locate(clause, here);
        if (eat("as")) {
            clause.declaredType = parseSequenceType();
        }
        if(eat(":=")) 
            clause.expr = parseExprSingle();
        return clause;
    }

    private Expression parseAssign2(Expression lhs)
        throws CompilationException
    {
        int here = tokenStart;  // on :=
        //IQName var = resolveVarName();
        if(lhs instanceof VarReference) {
            VarReference ref = (VarReference) lhs;
            return locate(new AssignExpr(ref.name, parseExprSingle()), here);
        }
        syntax("invalid left handside of assignment");
        return null;
    }

    private Expression parseExitExpr() throws CompilationException
    {
        int here = tokenStart;
        Expression ret = parseExprSingle();
        return locate(new ExitExpr(ret), here);
    }

    private Expression parseIfExpr()
        throws CompilationException
    {
        int here = tokenStart;
        Expression e1 = parseExpr();
        want(")");
        want("then");
        Expression e2 = parseExprSingle();
        want("else");
        return locate(new IfExpr(e1, e2, parseExprSingle()), here);
    }
    
    private Expression parseOrExpr()
        throws CompilationException
    {
        Expression e = parseAndExpr();
        if (!eat("or"))
            return e;
        OrExpr or = new OrExpr(e);
        locate(or);
        or.addExpr(parseAndExpr());
        while (eat("or"))
            or.addExpr(parseAndExpr());
        return or;
    }

    private Expression parseAndExpr()
        throws CompilationException
    {
        Expression e = parseComparisonExpr();
        if (!eat("and"))
            return e;
        AndExpr and = new AndExpr(e);
        locate(and);
        and.addExpr(parseComparisonExpr());
        while (eat("and"))
            and.addExpr(parseComparisonExpr());
        return and;
    }

    // associativity is not clear here, anyway it does not make much sense
    private Expression parseComparisonExpr()
        throws CompilationException
    {
        Expression e = parseFTContainsExpr();
        loop: for (;;) {
            eatSpace();
            int here = currentPos();
            if(eat("lt")) {
                e = new ValueLtOp(e, parseFTContainsExpr());
            }
            else if(eat("le")) {
                e = new ValueLeOp(e, parseFTContainsExpr());
            }
            else if(eat("eq")) {
                e = new ValueEqOp(e, parseFTContainsExpr());
            }
            else if(eat("ne")) {
                e = new ValueNeOp(e, parseFTContainsExpr());
            }
            else if(eat("gt")) {
                e = new ValueGtOp(e, parseFTContainsExpr());
            }
            else if(eat("ge")) {
                e = new ValueGeOp(e, parseFTContainsExpr());
            }
            else if(eat(">=")) {
                e = new GeOp(e, parseFTContainsExpr());
            }
            else if(eat(">>")) {
                e = new AfterOp(e, parseFTContainsExpr());
            }
            else if(eat(">")) {
                e = new GtOp(e, parseFTContainsExpr());
            }
            else if(eat("<<")) {
                e = new BeforeOp(e, parseFTContainsExpr());
            }
            else if(eat("<=")) {
                e = new LeOp(e, parseFTContainsExpr());
            }
            else if(eat("<")) {
                e = new LtOp(e, parseFTContainsExpr());
            }
            else if(eat("=")) {
                e = new EqOp(e, parseFTContainsExpr());
            }
            else if(eat("!=")) {
                e = new NeOp(e, parseFTContainsExpr());
            }
            else if(eat("is not")) {
                e = new IsNotOp(e, parseFTContainsExpr());
            }
            else if(eat("is")) {
                e = new IsOp(e, parseFTContainsExpr());
            }
            else
                break loop;
            locate(e, here);
        }
        return e;
    }
    
    private Expression parseFTContainsExpr()
        throws CompilationException
    {
        Expression e = parseRangeExpr();
        if(eat("ftcontains") || eat("contains text")) {
            FTContainsOp ftc = new FTContainsOp(e, parseFTSelection());
            e = ftc;
            if(eat("without content"))
                ftc.ignore = parseUnionExpr();
        }
        return e;
    }
    
    private Expression parseRangeExpr()
        throws CompilationException
    {
        Expression e = parseAdditiveExpr();
        if (eat("to")) {
            int here = tokenStart;
            e = new RangeExpr(e, parseAdditiveExpr());
            locate(e, here);
        }
        return e;
    }

    // AdditiveExpr ::= MultiplicativeExpr (( Plus | Minus)
    // MultiplicativeExpr)*
    private Expression parseAdditiveExpr()
        throws CompilationException
    {
        Expression e = parseMultiplicativeExpr();
        for (;;) {
            int here = currentPos();
            if (eat("+"))
                e = new PlusOp(e, parseMultiplicativeExpr());
            else if (eat("-"))
                e = new MinusOp(e, parseMultiplicativeExpr());
            else
                break;
            locate(e, here);
        }
        return e;
    }
    
    // MultiplicativeExpr ::= UnaryExpr ((Multiply|Div|Idiv|Mod) UnaryExpr)*
    Expression parseMultiplicativeExpr( ) throws CompilationException 
    {
        Expression e = parseUnionExpr();
        for( ;; ) {
            int here = currentPos();
            if( eat("*"))
                e = new MulOp(e, parseUnionExpr() );
            else if( eat("div") )
                e = new DivOp(e, parseUnionExpr() );
            else if( eat("idiv") )
                e = new IDivOp(e, parseUnionExpr() );
            else if( eat("mod") )
                e = new ModOp(e, parseUnionExpr() );
            else
                break;
            locate(e, here);
        }
        return e;
    }
    
    // UnionExpr ::= IntersectExceptExpr ((Union | Vbar) IntersectExceptExpr)* 
    Expression parseUnionExpr( ) throws CompilationException 
    {
        Expression e = parseIntersectExceptExpr();
        while( eat("union") || eat("|") ) {
            int here = prevTokenLoc;
            e = new UnionOp(e, parseIntersectExceptExpr() );
            locate(e, here);
        }
        return e;
    }
    
    // IntersectExceptExpr ::= ValueExpr ( (Intersect | Except) ValueExpr )*  
    Expression parseIntersectExceptExpr( ) throws CompilationException 
    {
        Expression e = parseInstanceofExpr();
        for( ;; ) {
            int here = currentPos();
            if( eat("intersect"))
                e = new IntersectOp(e, parseInstanceofExpr() );
            else if(eat("except"))
                e = new ExceptOp(e, parseInstanceofExpr() );
            else
                break;
            locate(e, here);
        }
        return e;
    }
    
    // InstanceofExpr ::= TreatExpr ( Instanceof SequenceType  )?   //CHANGED  
    //
    Expression parseInstanceofExpr( ) throws CompilationException 
    {
        Expression e = parseTreatExpr();
        int here = currentPos();
        if(eat("instance of")) {
            e = new InstanceofExpr(e, parseSequenceType());
            locate(e, here);
        }
        return e;
    }
    
    // TreatExpr ::= CastableExpr ( <TreatAs> SequenceType )?
    //
    Expression parseTreatExpr( ) throws CompilationException 
    {
        Expression e = parseCastableExpr();
        int here = currentPos();
        if( eat("treat as") ) {
            locate(e = new TreatExpr(e, parseSequenceType()), here);
        }
        return e;
    }
    
    // CastableExpr ::= CastExpr ( <Castable As> SingleType  )*  
    //
    Expression parseCastableExpr( ) throws CompilationException 
    {
        Expression e = parseCastExpr();
        int here = currentPos();
        if(eat("castable as"))
            locate(e = new CastableExpr(e, parseSingleType()), here);
        return e;
    }
    
    // CastExpr ::= ComparisonExpr ( <Cast As> SingleType  )*  
    //
    Expression parseCastExpr( ) throws CompilationException 
    {
        Expression e = parseUnaryExpr();
        if(eat("cast as")) {
            int here = tokenStart;
            locate(e = new CastExpr(e, parseSingleType()), here);
        }
        return e;
    }
    
    // UnaryExpr ::= ( ( Minus | Plus) )*  UnionExpr 
    Expression parseUnaryExpr( ) throws CompilationException 
    {
        if (eat("-")) {
            int here = tokenStart;
            return locate(new NegateOp(parseUnaryExpr()), here);
        }
        else if( eat("+")) {
            Expression expr = parseUnaryExpr();
            if(expr instanceof StringLiteral) // hack for xqts...
                currentModule.error("XPTY0004", tokenStart,
                                    "improper operand type for +");
            return expr;
        }
        else
            return parseValueExpr();
    }
    
    // ValueExpr ::= ( ValidateExpr | PathExpr) 
    Expression parseValueExpr()
        throws CompilationException
    {
        if (eat("try {"))
            return parseTryCatchExpr();
        else if (eat("validate %? %N") || eat("validate %? {"))
            return parseValidateExpr();
        else if (eatPragma()) {
            do {
                QName extName = checkPragma();
                // for the moment, ignore all pragmas...
                  
            }
            while (eatPragma());
            want("{");
            Expression e = parseExpr();
            want("}");
            return e;
        }
        else
            return parsePathExpr();
    }

    private QName checkPragma() throws CompilationException
    {
        // check prefix/namespace: assumes prefixValue+localName set
        String ns = expandPrefix(savedPrefix);
        if (ns == NamespaceContext.EMPTY)
            currentModule.error("XPST0081", tokenEnd + 3,
                                "blank namespace not allowed for extension");
        return IQName.get(ns, savedName);
    }
    
    Expression parseTryCatchExpr()
        throws CompilationException
    {
        int here = tokenStart;
        Expression caught = parseEnclosedExpr();
        want("catch");
        
        TryCatchExpr trycat = null;
        if(eat("( $ %Q")) { // Qizx old style
            QName name = resolveVarName();
            want(") {");
            trycat = new TryCatchExpr(caught, name, parseEnclosedExpr());
        }
        else {  // support of XQuery 1.1
            TryCatchExpr.Catch handler = parseCatch();
            trycat = new TryCatchExpr(caught, handler);

            while(eat("catch")) {
                handler = parseCatch();
                trycat.addCatch(handler);
            }
        }
        return locate(trycat, here);
    }

    private TryCatchExpr.Catch parseCatch() throws CompilationException 
    {
        int here = tokenStart;
        BaseNodeFilter test = parseNameTest(false, false);
        TryCatchExpr.Catch cat = new TryCatchExpr.Catch(test);
        locate(cat, here);
        while(eat("|")) {
            test = parseNameTest(false, false);
            cat.addTest(test);
        }
        // variables:
        if(eat("( $ %Q")) {
            cat.codeVarName = resolveVarName();
            if(eat(MORE_VAR)) {
                cat.descVarName = resolveVarName();
                if(eat(MORE_VAR)) {
                    cat.valueVarName = resolveVarName();
                }
            }
            want(")");
        }
        want("{");
        cat.handler = parseEnclosedExpr();
        return cat;
    }

    // PathExpr ::= Root RelativePathExpr? |
    // 		    RootDescendants RelativePathExpr | RelativePathExpr 
    // RelativePathExpr ::= StepExpr ( ( Slash | SlashSlash) StepExpr )*  
    Expression parsePathExpr( ) throws CompilationException 
    {
        int here = currentPos();
        boolean atRoot = false, atRootDesc = false;
        if( eat("//") )
            atRoot = atRootDesc = true;
        else if( eat("/") )
            atRoot = true;
        // first expression
        Expression step = parseStepExpr();
        PathExpr p = null;
        // need a PathExpr even with only one step, depending on the step kind 
        if(atRoot || step instanceof ReverseStep) {
            p = new PathExpr();
            locate(p, here);
            if(atRoot)
                p.addStep( locate(new RootStep(null), here) );
            if( atRootDesc )
                if( step != null )
                    p.addStep( locate(new DescendantOrSelfStep(null), here) );
                else syntax("unterminated path '//'");
            if(step != null)
                p.addStep(step);
        }
        else if(step == null)
            syntax("expecting expression"); 
        
        // if there is no following / or //, finish here:
        if( !see("/"))
            return p != null ? p : step;
        // really a path expr:
        if( p == null ) {
            locate(p = new PathExpr(), here);
            p.addStep(step);
        }
        for(;;) {
            if( eat("//") )
                p.addStep( locate(new DescendantOrSelfStep(null), here) );
            else if(!eat("/") )
                break;
            step = parseStepExpr();
            if( step == null )
                syntax("unterminated path expression");
            p.addStep(step);
        }
        locate(p, here);
        return p;
    }
    
    // StepExpr ::= ( ForwardStep | ReverseStep | PrimaryExpr) Predicates 
    Expression parseStepExpr( ) throws CompilationException 
    {
        Expression step = null;
        eatSpace();
        int here = currentPos();
        if(eat("..")) {
            step = new ParentStep(null);
        }
        else if(!see("%u") && eat(".")) { 
            step = new SelfStep( null );
        }
        else if(eat("ancestor ::")) {
            step = new AncestorStep(parseNodeTest(false));
        }
        else if(eat("ancestor-or-self ::")) {
            step = new AncestorOrSelfStep(parseNodeTest(false));
        }
        else if(eat("attribute ::")) {
            step = new AttributeStep(parseNodeTest(true));
        }
        else if(eat("@")) {
            step = new AttributeStep(parseNameTest(true, false));
        }
        else if(eat("child ::")) {
            step = new ChildStep(parseNodeTest(false));
        }
        else if(eat("descendant ::")) {
            step = new DescendantStep(parseNodeTest(false));
        }
        else if(eat("descendant-or-self ::")) {
            step = new DescendantOrSelfStep(parseNodeTest(false));
        }
        else if(eat("following-sibling ::")) {
            step = new FollowingSiblingStep(parseNodeTest(false));
        }
        else if(eat("following ::")) {
            step = new FollowingStep(parseNodeTest(false));
        }
        else if(eat("self ::")) {
            step = new SelfStep( parseNodeTest(false) );
        }
        else if(eat("parent ::")) {
            step = new ParentStep( parseNodeTest(false) );
        }
        else if(eat("preceding-sibling ::")) {
            step = new PrecedingSiblingStep( parseNodeTest(false) );
        }
        else if(eat("preceding ::")) {
            step = new PrecedingStep( parseNodeTest(false) );
        }
        else { // implicit child step?
            step = parsePrimaryExpr();
            if(step == null) {
                NodeFilter test = parseNodeTest(false, true);
                if(test == null) {
                    return null;
                }
                step = new ChildStep(test);
            }
        }
        if(step != null)
            locate(step, here);
        // predicates:
        FilterExpr filtered = (step instanceof FilterExpr)? (FilterExpr) step : null;
        for( ; ; ) {
            if (eat("[") ) {
                here = tokenStart;
                if(filtered == null)
                    filtered = new FilterExpr( step );
                filtered.addPredicate(parseExpr());
                locate(filtered, here);
                want("]");
            }
            else if (eat("(") ) {
                FunctionItemCall call = new FunctionItemCall(step);
                locate(call, tokenStart);
                if (!eat(")")) {
                    call.addArgument(parseExprSingle());
                    while (eat(COMMA))
                        call.addArgument(parseExprSingle());
                    want(")");
                }
                filtered = null;
                step = call;
            }
            else break;
        }
        return filtered == null ? step : filtered;
    }

    // ValidateExpr ::= 'validate' SchemaMode ? SchemaContext? '{' Expr '}'
    //
    Expression parseValidateExpr( ) throws CompilationException 
    {
        int here = tokenStart;
        int mode = -1;
        QName type = null;
        if(eat("as %Q")) {
            type = resolveElementName();
        }
        else if (eat("lax"))
            mode = ValidateExpr.LAX_MODE;
        else if (eat("strict"))
            mode = ValidateExpr.STRICT_MODE;
            
        want("{");
        Expression e = parseExpr();
        want("}");
        e = new ValidateExpr(mode, type, e);
        locate(e, here);
        return e;
    }    
    
    // PrimaryExpr ::= ( Literal | FunctionCall | VariableIndicator VarName  | ParenthesizedExpr) 
    //
    Expression parsePrimaryExpr( ) throws CompilationException 
    {
        int here = currentPos();
        if(eatNumber()) {
            switch(numberToken) {
            case INTEGER_LITERAL:
                return locate( new IntegerLiteral(makeInteger()) );
            case DECIMAL_LITERAL:
                return locate( new DecimalLiteral(makeDecimal()) );
            case DOUBLE_LITERAL:
                return locate( new DoubleLiteral(makeNumber()) );
            }
        }
        else if(eatStringLiteral())
            return locate( new StringLiteral(extractStringToken()) );
        
        // reject reserved call-like constructs:
        if(see("item (") || see("node (") || see("document-node (")
           || see("element (") || see("text (") || see("attribute (")
           || see("processing-instruction (") || see("comment ("))
            return null;
        
        if(eat("function (")) {   // before simple call
            here = tokenStart;
            return locate(parseLambdaFunction(), here);
        }
        if(eat("%Q ("))
            return parseFunctionCall();
        if(eat("%Q # %u"))
            return parseFunctionLiteral();
        
//        if(crunch("call template":
//            return parseCallTemplate();
        if(eat("$ %Q"))
            return locate2(new VarReference( resolveVarName()));
        if(eat("("))
            return parseParenthesizedExpr();
        if(eat("ordered {") || eat("unordered {")) { 
            Expression encl = parseExpr();
            want("}");
            return encl;    // downright ignored!
        }
        
        
        if(eat("<%Q"))
            return parseElementConstructor();
        if(eatXmlComment())
            return newCommentConstructor();
        if(eatPI())
            return locate2(new PIConstructor( extractStringToken() ));

        
        
        if(eat("text {")) {
            Expression e = new AtomConstructor(Node.TEXT, parseExpr());
            want("}");
            return locate(e, here);
        }      
            
        if(eat("document {")) {
            Expression e = new DocumentConstructor(parseExpr());
            want("}");
            return locate(e, here);
        }
        
        if(eat("element {")) {
            Expression name = parseExpr();
            want("} {");
            return parseNamedConstructorBody(here, new ElementConstructor(name));
        }
        if(eat("element %Q {")) {
            Expression name = parseNamedConstructorName(true);
            return parseNamedConstructorBody(here, new ElementConstructor(name));
        }
        
        if(eat("attribute {")) {
            Expression name = parseExpr();
            want("} {");
            return parseNamedConstructorBody(here, new AttributeConstructor(name));
        }
        if(eat("attribute %Q {")) {
            Expression name = parseNamedConstructorName(false);
            return parseNamedConstructorBody(here, new AttributeConstructor(name));
        }
            
        if(eat("comment {")) {
            Expression e = parseExpr();
            want("}");
            return locate(new AtomConstructor( Node.COMMENT, e ), here);
        }
        
        if(eat("namespace {")) {
            Expression prefix = parseExpr();
            want("} {");
            Expression e = parseExpr();
            want("}");
            return locate(new NamespaceConstructor(prefix, e), here);
        }
        
        if(eat("namespace %N {")) {
            StringLiteral prefix = new StringLiteral(savedName);
            Expression e = parseExpr();
            want("}"); 
            return locate(new NamespaceConstructor(prefix, e), here);
        }
       
        if(eat("processing-instruction {")) {
            Expression name = parseExpr(), body = null;
            want("} {");
            if(!eat("}")) {
                body = parseExpr();
                want("}");
            }
            return locate(new PIConstructor(name, body), here);
        }
        
        if(eat("processing-instruction %N {")) {
            String target = savedName;
            Expression body = parseExpr();
            want("}");
            return locate(new PIConstructor(new StringLiteral(target), body),
                          here);
        }
        
        // dont protest immediately
        return null;
    }
    
    Expression parseParenthesizedExpr( ) throws CompilationException 
    {
        if( eat(")") )
            return new SequenceExpr();
        Expression e = parseExpr();
        want(")");
        return e;
    }
    
    Expression parseFunctionCall()
        throws CompilationException
    {
        int here = tokenStart;
        // name already read
        QName fname = resolveQName(currentModule.getDefaultFunctionNamespace());
        // standard function
        FunctionCall call = new FunctionCall(fname);
        locate(call, here);
        if (!eat(")")) {
            call.addArgument(parseExprSingle());
            while (eat(COMMA))
                call.addArgument(parseExprSingle());
            want(")");
        }
        // special functions:
        if (fname == UNORDERED) {
            if (call.getArgCount() != 1)
                currentModule.error("XPST0017", here,
                                    "unordered wants exactly 1 argument");
            return locate(new Unordered(call.child(0)), here);
        }
        return call;
    }
    
    private Expression parseFunctionLiteral()
        throws CompilationException
    {
        QName fname = resolveQName(currentModule.getDefaultFunctionNamespace());
        return new FunctionLiteral(fname, makeInteger());
    }

    private Expression parseLambdaFunction() throws CompilationException
    {
        UserFunction.Signature proto = new UserFunction.Signature(null);
        proto.module = currentModule;
        proto.offset = tokenStart;
        parseFunctionProto(proto);
        want("{");
        proto.body = parseEnclosedExpr();
        return new InlineFunction(proto);
    }

    XQItemType checkTypeName(QName name)
    {
        XQItemType itype = currentModule.lookForType(name);
        if (itype == null) {
            currentModule.error("XPST0051", prevTokenLoc,
                                "unknown type "
                                    + currentModule.prefixedName(name));
            return XQType.ITEM;
        }
        else if (!XQType.ATOM.accepts(itype))
            currentModule.error("XPTY0004", prevTokenLoc,
                                "non atomic type "
                                    + currentModule.prefixedName(name));
        return itype;
    }
    
    // SingleType ::= AtomicType ( QMark )?  
    XQType parseSingleType( ) throws CompilationException
    {
        want("%Q");
        XQItemType itype = checkTypeName( resolveElementName() );
        return eat("?") ? (XQType) itype.opt : itype;
    }
    
    // SequenceType ::=  ItemType OccurrenceIndicator  |  Empty 
    SequenceType parseSequenceType( ) throws CompilationException 
    {
        if(eat("empty-sequence ( )"))
            return XQType.NONE.opt; // optional by essence!
        XQItemType itemType = parseItemType();
        if(eat("*"))
            return itemType.star;
        else if(eat("+"))
            return itemType.plus;
        else if(eat("?"))
            return itemType.opt;
        return itemType.one;
    }
    
    XQItemType parseItemType( ) throws CompilationException 
    {
        if(eat("item ( )")) {
            return XQType.ITEM;
        }
        
        if(eat("function (")) {  
            if(eat("* )"))
                return new FunctionType(null);
            UserFunction.Signature proto = new UserFunction.Signature(null);
            if (!eat(")")) {
                for(;;) {
                    XQType argType = parseSequenceType();
                    proto.arg((QName) null, argType);
                    if(!eat(COMMA))
                        break;
                }
                want(")");
            }        
            if (eat("as "))
                proto.returnType = proto.declaredReturnType = parseSequenceType();
            return new FunctionType(proto);
        }
        
        if(see("%N (")) {   // document( element( etc
            NodeFilter type = parseNodeTest(false);
            return new NodeType(type);
        }
        
        if(eat("%Q")) {
            XQItemType itype = checkTypeName( resolveElementName() );
            // wish it were "default type NS" resolveQName(Namespace.XSD)
            return itype;
        }
        syntax("expect type");
        return null;
    }
    
    NodeFilter parseNodeTest() throws CompilationException
    {
        return parseNodeTest(false);
    }
    
    NodeFilter parseNodeTest(boolean forAttr) throws CompilationException
    {
        return parseNodeTest(forAttr, false);
    }
    
    NodeFilter parseNodeTest(boolean forAttr, boolean lenient)
        throws CompilationException
    {
        eatSpace();
        int here = currentPos();
        if(eat("document-node (")) {
            NodeFilter nt = null;
            if(!eat(")")) {
                want("element (");
                nt = parseElemAttrTest(false);
                want(")");
            }
            if (nt == null)
                nt = new BaseNodeFilter(Node.ELEMENT, null, null);
            return locate(here, new DocumentTest(nt));
        }
        if(eat("element ("))
            return locate(here, parseElemAttrTest(false));
        if(eat("attribute ("))
            return locate(here, parseElemAttrTest(true));
        if(eat("text ( )")) {
            return locate(here, new BaseNodeFilter(Node.TEXT, null, null));
        }
        if(eat("comment ( )")) {
            return locate(here, new BaseNodeFilter(Node.COMMENT, null, null));
        }
        if(eat("processing-instruction (")) {
            String target = null;
            if (eatStringLiteral())
                target = extractStringToken();
            else if (eat("%N"))
                target = savedName;
            want(")");
            return locate(here, new BaseNodeFilter(Node.PROCESSING_INSTRUCTION,
                                                   null, target));
        }
        
        if(eat("node ( )")) {
            return new BaseNodeFilter(-1, null, null); // not null: we want nodes
        }
        return parseNameTest(forAttr, lenient);
    }
    
    BaseNodeFilter parseElemAttrTest(boolean forAttr)
        throws CompilationException
    {
        String ns = null;
        String name = null;
        SchemaContext path = null;
        eatSpace();
        if (!see(")") && !eat("*")) {
            path = parseSchemaContextPath(forAttr);
            if (path.isSimpleName()) {
                QName qname = path.getStep(0);
                path = null;
                ns = qname.getNamespaceURI();
                name = qname.getLocalPart();
            }
        }
        if (path == null && eat(COMMA)) {
            QName typeName = parseStarName();
            locate2(path = new SchemaContext(true));
            path.addStep(typeName);
            if (typeName != null && 
                 typeName.getNamespaceURI() != NamespaceContext.XSD)
                path.module.error("XPST0001", path.offset,
                                  "undefined type " + typeName);
            if (typeName == XQType.ANY.getName())
                path = null;    // no trouble
        }
        want(")");
        if (path != null)
            currentModule.warning("XPST0008", path.offset,
                                "Schema Type test not supported");
        return new BaseNodeFilter(forAttr ? Node.ATTRIBUTE : Node.ELEMENT, 
                                  ns, name, path);
    }
    
    
    QName parseStarName() throws CompilationException
    {
        if(eat("%Q"))
            return resolveElementName();
        else if(eat("*"))
            return null;
        syntax("expecting name or '*'");
        return null;
    }
    
    BaseNodeFilter parseNameTest(boolean forAttr, boolean lenient)
        throws CompilationException
    {
        int here = currentPos();
        String dns = forAttr ? "" : currentModule.getDefaultElementNamespace();
        int kind = forAttr ? Node.ATTRIBUTE : Node.ELEMENT;
        BaseNodeFilter test = null;

//        if(see("%Q ("))      // dont take function call
//            return null;
        if(eat("* : %N")) {
            test = new BaseNodeFilter(kind, null, savedName);
        }
        else if(eat("*")) {
            test = new BaseNodeFilter(kind, null, null);
        }
        else if(eat("%N : *")) {
            test = new BaseNodeFilter(kind, expandPrefix(extractStringToken(),
                                                         dns), null);
        }
        else if(eat("%Q")) {
            test = new BaseNodeFilter(kind, expandPrefix(savedPrefix, dns),
                                      savedName);
        }
        else {
            if (!lenient)
                syntax("expecting name test");
            return null;
        }
        locate(here, test);
        return test;
    }
    
    // SchemaContext ::= 'global' | 'context' SchemaContextLocation 
    SchemaContext parseSchemaContext( ) throws CompilationException 
    {
        if(eat("global"))
            return null;
        if(eat("context"))
            return parseSchemaContextPath(false);	// TODO reject simple name
        syntax( "expecting schema context");
        return null;
    }
    
    // SchemaContextPath ::= ( QName | TypeQName) ( Slash QName )*  
    // This is different than the standard, because the grammar is largely ambiguous
    // this can accept a simple QName, or no final '/', which is not OK for a SchemaContextPath:
    // this is tested afterwards by SchemaContext.length() and SchemaContext.endsWithSlash
    SchemaContext parseSchemaContextPath(boolean forAttr)
        throws CompilationException
    {
        SchemaContext sc = new SchemaContext(false);
        String defNS =
            forAttr ? null : currentModule.getDefaultElementNamespace();
        want("%Q");
        sc.addStep(resolveQName(defNS));
        while (eat("/"))
            if (eat("%Q"))
                sc.addStep(resolveQName(defNS));
            else {
                sc.endsWithSlash = true;
                break;
            }
        return sc;
    }

    private Expression parseNamedConstructorName(boolean forElement)
        throws CompilationException
    {
        // QName must be expanded using inscope mappings 
        // attention specs are UNCLEAR for attrs: use same rule as non computed
        String ns = "";
        if(forElement || savedPrefix.length() > 0)
            ns = expandPrefix(savedPrefix);
        QName qName = IQName.get(ns, savedName);
        Expression qname = new QNameLiteral(qName);
        
        locate(qname, tokenStart);
        return qname;
    }

    private Expression parseNamedConstructorBody(int here, NamedConstructor nc)
        throws CompilationException
    {
        if(!eat("}")) {
            nc.addItem( parseExpr() );
            want("}");
        }
        return locate(nc, here);
    }
    
    private Expression newCommentConstructor() throws CompilationException
    {
        return locate2(new AtomConstructor(Node.COMMENT,
                                           new StringLiteral(extractStringToken() )));
    }

    // Direct element constructor
    // ElementConstructor ::= ( StartTagOpenRoot | StartTagOpen) TagQName AttributeList 
    //     ( EmptyTagClose |
    //	     StartTagClose ElementContent*  EndTagOpen TagQName S?  EndTagClose ) 
    //
    ElementConstructor parseElementConstructor()
        throws CompilationException
    {
        int here = tokenStart;
        String tagPrefix = savedPrefix, tagName = savedName;
        ElementConstructor c = new ElementConstructor(null);
        c.setDirect();
        locate(c, here);

        NamespaceContext nsMapping = currentModule.getInScopeNS();
        nsMapping.newLevel();

        setSkipSpace(true);
        allowComments = false;
        while (eat("%Q =")) {
            parseAttribute(c);
        }
        
        // Only now the Qnames can be resolved:
        // the default elem NS is known through static mappings
        String ens = expandPrefix(tagPrefix);
        QName stag = IQName.get(ens, tagName);
        c.name = new QNameLiteral(stag);
        locate(c.name, here);

        for (int a = 0, AN = c.attributes.size(); a < AN; a++) {
            AttributeConstructor ac = c.getAttribute(a);
            if (ac instanceof NamespaceConstructor)
                continue;
            // no default NS for attributes:
            String ns =
                (ac.prefix.length() == 0) ? "" : expandPrefix(ac.prefix);
            ac.name = new QNameLiteral(IQName.get(ns, ac.value));
            ac.prefix = ac.value = null; // recover memory...
        }

        if (eat("/>")) {
            allowComments = true;
            nsMapping.popLevel();
            return c;
        }
        want(">");
        setSkipSpace(false);
        allowComments = true;
        whiteSpace = true;
        StringBuilder buf = new StringBuilder();
        
        // parse contents:
        for (;;) {
            here = tokenStart;
            boolean keepAnyway = false;
            buf.setLength(0);
            for(;;) {
                if(eatXmlChars((char) 0)) {   // sets whiteSpace
                    if(!whiteSpace)
                        keepAnyway = true;
                    buf.append(saveBuffer);
                }
                else if(eatCDATASection() || eatCharRef()) {
                    buf.append(saveBuffer);
                    keepAnyway = true;
                }
                else break;
            }
            if(preserveSpace || keepAnyway)
                locate(c.addTextItem(buf.toString()), here);
            
            if(eat("{")) {   // {{ is swallowed by crunchXMlChars
                setSkipSpace(true);
                c.addItem(parseEnclosedExpr());
                setSkipSpace(false);
            }
            else if(eat("<%Q"))
                c.addItem(parseElementConstructor());
            else if(eatXmlComment())
                c.addItem(newCommentConstructor());
            else if(eatPI())
                c.addItem(locate2(new PIConstructor(extractStringToken())));
            else if(eatNoSkip("</%Q"))
                break;
            else
                syntax("invalid element content");
        }

        String etagPrefix = savedPrefix;
        if (!tagPrefix.equals(etagPrefix))
            currentModule.error(ERR_SYNTAX, tokenStart,
                                "mismatched prefix on end-tag: "
                                    + "must be equal to start tag prefix");
        tagName = extractStringToken();

        ens = expandPrefix(etagPrefix);
        QName etag = IQName.get(ens == null ? "" : ens, tagName);
        if (etag != stag)
            syntax("tag mismatch: " + etag + " encountered when expecting "
                   + stag);
        setSkipSpace(true);
        allowComments = false;
        want(">");
        nsMapping.popLevel();
        allowComments = true;
        return c;
    }


    // returns true if really attribute and not NS
    private boolean parseAttribute(ElementConstructor c)
        throws CompilationException
    {
        AttributeConstructor ac = new AttributeConstructor( null );
        ac.prefix = savedPrefix;	// temporary
        ac.value = extractStringToken();
        locate(ac);

        parseAttributeValue(ac);

        // detect and process xmlns attributes
        String nsPrefix = null;
        if (ac.prefix.equals(XMLNS))
            nsPrefix = ac.value;
        else if (ac.prefix.length() == 0 && ac.value.equals(XMLNS))
            nsPrefix = "";
        if (nsPrefix == null) {
            c.addAttribute(ac);
            return true;
        }
        // new ExprDump().display("attr value", ac.contents);
        if (ac.contents.length != 1
            || !(ac.contents[0] instanceof StringLiteral)) {
            ac.module.error("XQST0022", ac.offset,
                            "namespaces must have a literal value");
        }
        else {
            if ("xml".equals(nsPrefix) || "xmlns".equals(nsPrefix))
                ac.module.error("XQST0070", ac.offset,
                                "reserved namespace prefix");

            String nsuri = ((StringLiteral) ac.contents[0]).value;
            currentModule.getInScopeNS().addMapping(nsPrefix, nsuri);
            NamespaceConstructor attC =
                new NamespaceConstructor(nsPrefix, nsuri);
            locate2(attC);
            c.addAttribute(attC);
        }
        return false;
    }
    
    private void parseAttributeValue( AttributeConstructor c )
        throws CompilationException 
    {
        String delimiter = null;
        eatSpace();
        if(eatRaw("\""))
            delimiter = "\"";
        else if(eatRaw("'"))
            delimiter = "'";
        else
           syntax("bad attribute delimiter");
        for( ; curChar != 0; ) {
            if(eatRaw(delimiter)) {
                if(!eatRaw(delimiter))
                    break;
                else
                    c.addTextItem(delimiter);
            }
            else if(eatXmlChars(delimiter.charAt(0))) {
                String frag = extractStringToken();
                locate( c.addTextItem(XMLUtil.replaceWhiteSpace(frag)));
            }
            else if(eatCharRef()) {
                // no normalization:
                locate( c.addTextItem(extractStringToken()));
            }
            else if(eat("{"))
                c.addItem( parseEnclosedExpr() );
            else
                syntax("invalid attribute content");
        }
        //want(delimiter);
        if(c.contents.length == 0)
            c.addTextItem("");
    }
    
    Expression parseEnclosedExpr( ) throws CompilationException 
    {
        Expression e = parseExpr();
        want("}");
        return e;
    }
    
    // parses the FT selection and options after the container expression
    private FTSelectionOp parseFTSelection()
        throws CompilationException
    {
        FTSelectionOp s = parseFTOr();
        // Attention: if expression s has parentheses around, it is possible
        // to have pos filters inside AND outside parenths
        FTPosFilters filters = parseFTPosFilters(s);
        if(filters != null) {
            if(s.posFilters == null)
                s.posFilters = filters;
            else // should be combined, but that's a lot of work
                currentModule.error("XPTY0004", s,
                                    "conflicting position filters " + CPF_TEXT );
        }
        // weight:
        if(eat("weight")) {
            if(s.weight != null)
                currentModule.error("XPTY0004", s,
                                    "conflicting weight " + CPF_TEXT );
            if(currentModule.sObs()) {
                want("{");    // NEW junk
                s.weight = parseRangeExpr();
                want("}");
            }
            else { // optional
                boolean braces = eat("{");
                s.weight = parseRangeExpr();
                if (braces)
                    want("}");
            }
        }
        return s;
    }

    private FTSelectionOp parseFTOr() throws CompilationException
    {
        FTSelectionOp e = parseFTAnd();
        if (!eat("ftor"))
            return e;
        FTOrOp or = new FTOrOp(e);
        or.addChild(parseFTAnd());
        locate(or);
        while (eat("ftor"))
            or.addChild(parseFTAnd());
        return or;
    }
    
    private FTSelectionOp parseFTAnd() throws CompilationException
    {
        FTSelectionOp e = parseFTMildNot();
        if (!eat("ftand"))
            return e;
        FTAndOp and = new FTAndOp(e);
        and.addChild(parseFTMildNot());
        locate(and);
        while (eat("ftand"))
            and.addChild(parseFTMildNot());
        return and;
    }
    
    private FTSelectionOp parseFTMildNot() throws CompilationException
    {
        FTSelectionOp e = parseFTNot();
        if( eat("not in")) {
            int here = tokenStart;
            e = new FTMildNotOp(e, parseFTNot());
            locate(e, here);
        }
        return e;
    }
    
    private FTSelectionOp parseFTNot() throws CompilationException
    {
        int here = tokenStart;
        if(eat("ftnot")) {
            FTNotOp e = new FTNotOp(parseFTPrimaryWithOptions());
            locate(e, here);
            return e;
        }
        else return parseFTPrimaryWithOptions();
    }

    private FTSelectionOp parseFTPrimaryWithOptions() throws CompilationException
    {
        FTSelectionOp sel = parseFTPrimary();
        // difficulties due to poor FT syntax: when we have
        //  "(primary options1) options2", then options1 and options2 clash
        if(sel.matchOptions == null)
            sel.matchOptions = new MatchOptions();
        parseFTMatchOptions(sel.matchOptions);
        if(sel.matchOptions.likeDefault())
            sel.matchOptions = null;
        return sel;
    }
    
    private FTSelectionOp parseFTPrimary() throws CompilationException
    {
        if (eatPragma()) {
            do {
                QName extName = checkPragma();
                // for the moment, ignore all pragmas...
            } while (eatPragma());
            want("{");
            FTSelectionOp e = parseFTSelection();
            want("}");
            return e;
        }
        else if(eat("(")) {
            FTSelectionOp e = parseFTSelection();
            want(")");
            return e;
        }
        
        FTWordsOp words = null;
        if(eat("{")) {
            words = new FTWordsOp(parseExpr());
            want("}");
        }
        else if(eatStringLiteral()) {
            words = new FTWordsOp(makeStringLiteral());
        }
        else syntax("expecting literal string or '{' or '('");
        
        if(eat("any word")) {
            words.anyAll = FullText.ANY_WORD;
        }
        else if(eat("any")) {
            words.anyAll = FullText.ANY;
        }
        else if(eat("all words")) {
            words.anyAll = FullText.ALL_WORDS;
        }
        else if(eat("all")) {
            words.anyAll = FullText.ALL;
        }
        else if(eat("phrase")) {
            words.anyAll = FullText.PHRASE;            
        }
        
        // occurs X times ... 
        if(eat("occurs")) {
            words.occs = parseFTRange();
            want("times");
        }
        return words;
    }
    

    private FTPosFilters parseFTPosFilters(FTSelectionOp sel)
        throws CompilationException
    {
        FTPosFilters filters = new FTPosFilters(false);
        HashSet set = new HashSet();
        for(;;) {
            if(eat("ordered")) {
                checkOption(set, "ordered");
//                // ugly HACK to comply with rubbish specs: 
//                // in practice 'ordered' is ignored if coming AFTER other filters
//                if(filters.windowExpr == null && filters.distance == null)
                    filters.ordered = true;
            }
            else if(eat("window")) {
                checkOption(set, "window");
                filters.windowExpr = parseAdditiveExpr();
                filters.windowUnit = parseDistanceUnit();
                // current limitation:
                if(filters.windowUnit != FTPosFilters.WORDS)
                    currentModule.error("FTST0003", sel, 
                                        "unsupported full-text window unit");
            }
            else if(eat("distance")) {
                checkOption(set, "distance");
                filters.distance = parseFTRange();
                filters.distanceUnit = parseDistanceUnit();
                // current limitation:
                if(filters.distanceUnit != FTPosFilters.WORDS)
                    currentModule.error("FTST0003", sel, 
                                        "unsupported full-text distance unit");
            }
            else if(eat("same sentence")) {
                checkOption(set, "scope");
                filters.scope = FTPosFilters.SAME_SENTENCE;
            }
            else if(eat("same paragraph")) {
                checkOption(set, "scope");
                filters.scope = FTPosFilters.SAME_PARAGRAPH;
            }
            else if(eat("different paragraph")) {
                checkOption(set, "scope");
                filters.scope = FTPosFilters.DIFF_SENTENCE;
            }
            else if(eat("different sentence")) {
                checkOption(set, "scope");
                filters.scope = FTPosFilters.DIFF_PARAGRAPH;
            }
            else if(eat("at start")) {
                checkOption(set, "anchor");
                filters.content = FTPosFilters.AT_START;
            }
            else if(eat("at end")) {
                checkOption(set, "anchor");
                filters.content = FTPosFilters.AT_END;
            }
            else if(eat("entire content")) {
                checkOption(set, "anchor");
                filters.content = FTPosFilters.ENTIRE_CONTENT;
            }
            else break;
        }
        // current limitation:
        if(filters.scope != FTPosFilters.UNSPECIFIED)
            currentModule.error("FTST0004", sel, 
                    "unsupported full-text feature 'same' / 'different'");
        return set.isEmpty() ? null : filters;
    }

    private int parseDistanceUnit() throws CompilationException
    {
        if(eat("words"))
            return FTPosFilters.WORDS;
        if(eat("sentences"))
            return FTPosFilters.SENTENCES;
        if(eat("paragraphs"))
            return FTPosFilters.PARAGRAPHS;
        syntax("expect 'words', 'sentences' or 'paragraphs'");
        return -1;
    }

    private void parseFTMatchOptions(MatchOptions opt)
        throws CompilationException
    {
        HashSet set = new HashSet();
        for(;;) {
            int here = currentPos();
            if(eat("using language %S") || eat("language %S")) {
                checkOption(set, "language");
                opt.language = extractStringToken();
                if(!((LanguageType) XQType.LANGUAGE).checkValue(opt.language))
                    currentModule.error("FTST0013", here, 
                                        "invalid language " + opt.language);
            }
            else if(eat("using case sensitive") || eat("case sensitive")) {
                checkOption(set, "case");
                opt.caseSensitivity = MatchOptions.SENSITIVE;
            }
            else if(eat("using case insensitive") || eat("case insensitive")) {
                checkOption(set, "case");
                opt.caseSensitivity = MatchOptions.INSENSITIVE;
            }
            else if(eat("using lowercase") || eat("lowercase")) {
                checkOption(set, "case");
                opt.caseSensitivity = MatchOptions.LOWERCASE;
            }
            else if(eat("using uppercase") || eat("uppercase")) {
                checkOption(set, "case");
                opt.caseSensitivity = MatchOptions.UPPERCASE;
            }
            else if(eat("using diacritics sensitive") || eat("diacritics sensitive")) {
                checkOption(set, "diacritics");
                opt.diacritics = MatchOptions.SENSITIVE;
            }
            else if(eat("using diacritics insensitive") || eat("diacritics insensitive")) {
                checkOption(set, "diacritics");
                opt.diacritics = MatchOptions.INSENSITIVE;
            }
            else if(eat("using wildcards") || eat("with wildcards")) {
                checkOption(set, "wildcards");
                opt.wildcards = MatchOptions.WITH;
            }
            else if(eat("using no wildcards") || eat("without wildcards")) {
                checkOption(set, "wildcards");
                opt.wildcards = MatchOptions.WITHOUT;
            }
            else if(eat("using stemming") || eat("with stemming")) {
                checkOption(set, "stemming");
                opt.stemming = MatchOptions.WITH;
            }
            else if(eat("using no stemming") || eat("without stemming")) {
                checkOption(set, "stemming");
                opt.stemming = MatchOptions.WITHOUT;
            }
            else if(eat("using thesaurus") || eat("with thesaurus")) {
                checkOption(set, "thesaurus");
                if(eat("(")) {
                    parseThesaurus(opt);
                    while(eat(COMMA)) {
                        parseThesaurus(opt);
                    }
                    want(")");
                }
                else parseThesaurus(opt);
            }
            else if(eat("using no thesaurus") || eat("without thesaurus")) {
                checkOption(set, "thesaurus"); // ignored: default
            }
            else if(eat("using no stop words")) {
                checkOption(set, "stopwords"); // ignored: default
            }
            else if(eat("using stop words")) {
                checkOption(set, "stopwords");
                currentModule.error("FTST0008", tokenStart,
                                    "stop-words are not supported");
                parseStopWords(opt.language);
                // opt.addStopWords(parseStopWords(opt.language));
                for(;;) {
                    if(eat("union"))
                        parseStopWords(opt.language);
                        //opt.addStopWords(parseStopWords(opt.language));
                    else if(eat("except"))
                        parseStopWords(opt.language);
                        // opt.exceptStopWords(parseStopWords(opt.language));
                    else break;
                }
            }
            else if(eat("using default stop words")) {
                checkOption(set, "stopwords");
                currentModule.error("FTST0008", tokenStart,
                                    "stop-words are not supported");
                for(;;) {
                    if(eat("union"))
                        parseStopWords(opt.language);
                        //opt.addStopWords(parseStopWords(opt.language));
                    else if(eat("except"))
                        parseStopWords(opt.language);
                        //opt.exceptStopWords(parseStopWords(opt.language));
                    else break;
                }
            }
            else if (eat("using option %Q %S")) {
                QName option = resolveVarName();
                
            }
            else break;
        }
    }

    private void parseStopWords(String languageCode)
        throws CompilationException
    {
        if(eat("at %S")) {
            // URI: immediately resolved through static context
            String uri = extractStringToken();
//            currentModule.error("FTST0008", prevTokenLoc,
//                                "unreachable stop-word list '" + uri + "'");
        }
        else {
            want("( %S");
            String word = extractStringToken();
            while(eat(", %S")) {
                word = extractStringToken();
            }
            want(")");
        }
    }

    private void parseThesaurus(MatchOptions options)
        throws CompilationException
    {
        if(eat("default")) {
            options.addThesaurus(getThesaurus("default", null, null, null));
            return;
        }
        if(!eat("at %S"))
            syntax("expecting thesaurus URI");

        String uri = extractStringToken(), relationship = null;
        if(eat("relationship %S")) {
            relationship = extractStringToken();
        }
        RangeExpr levels = parseFTRange();
        if(levels != null)
            want("levels");
        options.addThesaurus(getThesaurus(uri, options.language, relationship, levels));
    }


    private Thesaurus getThesaurus(String uri, String language, 
                                   String relationship, RangeExpr levels)
    {
        FullTextFactory ftp = currentModule.getFulltextFactory();
        int minLev = 0, maxLev = Integer.MAX_VALUE;
        if(levels != null) {
            // only constant expressions allowed:
            if(levels.lower != null)
                minLev = evalConstantLevel(levels.lower);
            if(levels.upper != null)
                maxLev = evalConstantLevel(levels.upper);
        }
        Thesaurus th =
            ftp.getThesaurus(uri, language, relationship, minLev, maxLev);
        if(th == null)
            currentModule.error("FTST0008", tokenStart,
                                "unreachable thesaurus '" + uri + "'");
        return th;
    }

    private int evalConstantLevel(Expression expr)
    {
        Expression upper = currentModule.evalConstantExpr(expr);                
        if(!(upper instanceof IntegerLiteral)) {
            currentModule.error("FTST0008", prevTokenLoc,
                           "only constant bounds accepted for thesaurus level");
            return 0;
        }
        return (int) ((IntegerLiteral) upper).value;
    }

    private RangeExpr parseFTRange() throws CompilationException
    {
        RangeExpr range = new RangeExpr(null, null);
        
        if(eat("exactly"))
            range.lower = range.upper = parseAdditiveExpr();
        else if(eat("at least"))
            range.lower = parseAdditiveExpr();
        else if(eat("at most"))
            range.upper = parseAdditiveExpr();
        else if(eat("from")) {
            range.lower = parseAdditiveExpr();
            want(" to");
            range.upper = parseAdditiveExpr();
        }
        else return null;
        return range;
    }

    private void checkOption(HashSet set, String name)
    {
         if(set.contains(name))
             currentModule.error("FTST0019", tokenStart,
                                 "duplicate " + name + " option");
         set.add(name);
    }
    
    // ------ XUpdate -----------------------------------------------------
    
    private InsertExpr parseInsertNodes() throws CompilationException
    {
        InsertExpr insert = new InsertExpr();
        locate(insert);
        
        insert.what = parseExprSingle();
        if(eat("into"))
            insert.mode = InsertExpr.INTO;
        else if(eat("as first into"))
            insert.mode = InsertExpr.FIRST;
        else if(eat("as last into"))
            insert.mode = InsertExpr.LAST;
        else if(eat("before"))
            insert.mode = InsertExpr.BEFORE;
        else if(eat("after"))
            insert.mode = InsertExpr.AFTER;
        else
            syntax("invalid insertion mode: " +
            		"expecting 'into' or 'as' or 'before' or 'after'");

        insert.where = parseExprSingle();
        return insert;
    }

    private Expression parseDeleteNodes() throws CompilationException
    {
        DeleteExpr delete = new DeleteExpr();
        locate(delete);
        delete.where = parseExprSingle();
        return delete;
    }

    private ReplaceExpr parseReplaceNode(boolean valueOf)
        throws CompilationException
    {
        ReplaceExpr replace = new ReplaceExpr();
        locate(replace);
        replace.mode = valueOf? ReplaceExpr.VALUE : 0;
        replace.where = parseExprSingle();
        want("with");
        replace.what = parseExprSingle();
        return replace;
    }

    private RenameExpr parseRenameNode() throws CompilationException
    {
        RenameExpr rename = new RenameExpr();
        locate(rename);
        rename.where = parseExprSingle();
        want("as");
        rename.what = parseExprSingle();
        return rename;
    }

    private Expression parseTransform() throws CompilationException
    {
        TransformExpr trans = new TransformExpr();
        locate(trans);
        
        LetClause vcl = parseLetClause(false);
        trans.copies = new LetClause[] { vcl };
        while(eat(MORE_VAR)) {
            vcl = parseLetClause(false);
            trans.copies = (LetClause[]) Expression.addExpr(trans.copies, vcl);
        }
        want("modify");
        trans.modify = parseExprSingle();
        want("return");
        trans.result = parseExprSingle();
        
        return trans;
    }
    
    // ------ utilities: --------------------------------------------------

    private void moduleImport(int location, String logicalURI,
                              ArrayList physicalURIs)
    {
        if (logicalURI == null || logicalURI.length() == 0) {
            currentModule.error("XQST0088", location,
                                "empty namespace URI in module import");
            return;
        }
        // check unicity of NS here: several module pieces can share the same NS
        // but only one import is allowed...
        if (currentModule.alreadyImportedModule(logicalURI))
            currentModule.error("XQST0047", location,
                                "module already imported '" + logicalURI + "'");

        String[] locationHints = Util.toStringArray(physicalURIs);
        URL[] urls = null;
        try {
            if(moduleResolver != null)
                urls = moduleResolver.resolve(logicalURI, locationHints);
        }
        catch(MalformedURLException e) {
            currentModule.error("XQST0059", location, "error during resolution"
                                + " of module " + logicalURI + ": " + e);
        }
        if (urls == null || urls.length == 0) {
            currentModule.error("XQST0059", location, "module "
                                    + logicalURI + " cannot be resolved");
            return;
        }
        
        for (int i = 0; i < urls.length; i++) {
            ModuleContext importedModule;
            try {
                importedModule =
                    moduleManager.loadModule(currentModule, urls[i]);
                if (!logicalURI.equals(importedModule.getNamespaceURI()))
                    currentModule.error("XQST0059", location,
                                        "module imported from '" + logicalURI
                                        + "' declares a different URI ("
                                        + importedModule.getNamespaceURI() + ")");
                ModuleImport modImport = new ModuleImport(importedModule);
                currentModule.addDeclaration(locate(modImport, location));
            }
            catch (IOException e) {
                Exception ee = e;
                if (e.getMessage() == null
                    && e.getCause() instanceof Exception)
                    ee = (Exception) e.getCause();
                currentModule.error("XQST0059", location,
                               "in import of module " + logicalURI + ": " + ee);
            }
            catch (CompilationException e) {
                currentModule.error("XQST0059", location,
                               "in import of module " + logicalURI + ": " + e);
            }

        }
    }

    private int checkOptionKeyword(String[] options)
        throws CompilationException
    {
        want("%N");
        String opt = savedName;
        for (int k = options.length; --k >= 0;)
            if (options[k].equals(opt))
                return k;
        String msg = "expecting " + options[0];
        for (int k = 1; k < options.length; k++)
            msg += " or " + options[k];
        syntax(msg);
        return 0;// dummy
    }

    /**
     * Gets the NS for a prefix, using statically known namespaces.
     */
    private String expandPrefix(String prefix)
    {
        String ns = currentModule.convertPrefixToNamespace(prefix);
        if (ns == null) {
            if (prefix.length() > 0)
                currentModule.error("XPST0081", tokenStart,
                                    "unknown prefix '" + prefix + "'");
            ns = "";
        }
        return ns;
    }

    /**
     * Gets the NS for a prefix, using first a default NS (function) then the
     * statically known namespaces.
     */
    private String expandPrefix(String prefix, String defaultNS)
    {
        if (prefix == null || prefix.length() == 0)
            return defaultNS;
        return expandPrefix(prefix);
    }

    // assuming that the current token is QName,
    // return the QName after moving to next token.
    private IQName resolveQName(String defaultNS)
        throws CompilationException
    {
        if (defaultNS == null)
            defaultNS = "";
        return IQName.get(expandPrefix(savedPrefix, defaultNS), savedName);
    }

    private IQName resolveElementName()
        throws CompilationException
    {
        return resolveQName(currentModule.getDefaultElementNamespace());
    }

    private IQName resolveVarName()
        throws CompilationException
    {
        // normally blank NS, should be module NS
        return resolveQName(null);
        // return resolveQName( currentModule.getNamespace() );
    }

    private Expression makeStringLiteral()
        throws CompilationException
    {
        return locate2(new StringLiteral(extractStringToken()));
    }

    private BigDecimal makeDecimal()
        throws CompilationException
    {
        BigDecimal v;
        try {
            v = Conversion.toDecimal(saveBuffer.toString(), false);
        }
        catch (EvaluationException e) {
            currentModule.error(ERR_SYNTAX, tokenStart,
                                "invalid value of decimal literal '"
                                    + saveBuffer + "'");
            v = new BigDecimal(0);
        }
        return v;
    }

    private double makeNumber()
        throws CompilationException
    {
        double v = 0;
        try {
            v = Double.parseDouble(saveBuffer.toString());
        }
        catch (java.lang.NumberFormatException e) {
            currentModule.error(ERR_SYNTAX, tokenStart,
                                "invalid value of double literal '"
                                    + saveBuffer + "'");
        }
        return v;
    }
    
    private long makeInteger()
        throws CompilationException
    {
        long v = 0;
        try {
            v = Conversion.toInteger(saveBuffer.toString());
        }
        catch (EvaluationException e) {
            currentModule.error(ERR_SYNTAX, tokenStart,
                                "value of integer literal '" + saveBuffer
                                    + "' out of bounds");
        }
        return v;
    }


    Expression locate(Expression e, int where)
    {
        e.offset = where;
        e.module = currentModule;
        return e;
    }

    Expression locate(Expression e)
    {
        return locate(e, tokenStart);
    }

    Expression locate2(Expression e)
    {
        return locate(e, prevTokenLoc);
    }

    NodeFilter locate(int where, BaseNodeFilter nt)
    {
        nt.srcLocation = where;
        return nt;
    }

    public void setModuleResolver(ModuleResolver moduleResolver)
    {
         this.moduleResolver = moduleResolver;
    }
}
