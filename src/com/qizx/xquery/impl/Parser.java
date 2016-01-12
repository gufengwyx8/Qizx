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
public class Parser extends Lexer
{
    public final static String XQUERY_VERSION = "1.0";
    
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
    
    private ModuleManager moduleManager;
    
    private Pragma[] pragmas = new Pragma[0];
    private boolean preserveSpace = false;
    private int prologState;
    
    // declarations:
    private HashSet<String> declared = new HashSet<String>();
    private int declFlags;

    private ModuleResolver moduleResolver;


    
    /**
     * Parses input inside a properly built module context
     */
    public Parser(ModuleManager moduleManager)
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
        nextToken();

        parseOptVersion();
        
        parseNewProlog();
        
        // main query body:
        query.body = parseExpr();
        if (curToken != T_SemiColon && curToken != T_END)
            syntax("unrecognized characters at end of query");
        if (stateSP != 0)
            System.err.println("*** " + stateSP + " unpopped lexical states");
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
        nextToken();
        currentModule = module;
        setupModule(input, uri);

        parseOptVersion();
        // module namespace <prefix> = <StringLiteral> ;
        wantToken(T_Module);
        if (curToken != T_NCName)
            syntax("expecting prefix");
        int prefixHere = tokenStart;
        String prefix = makeString();
        wantToken(T_AssignEquals);
        if (curToken != T_URLLiteral)
            checkToken(T_StringLiteral);
        String moduleURI = makeString();

        currentModule.setNamespaceURI(moduleURI);
        currentModule.addNamespaceDecl(prefixHere, prefix, moduleURI);
        pickToken(T_SemiColon); // optional!

        // declarations:
        parseNewProlog();

        if (curToken != T_END)
            syntax("unrecognized characters at end of query");
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
        if(curToken != T_XQueryVersion)
            return;
        String version = makeString();
        if(version.compareTo(XQUERY_VERSION) != 0)	// mmm
            currentModule.error( "XQST0031", prevTokenLoc,
                                 "XML Query version " + version
                                 + " not supported, the current version is "
                                 + XQUERY_VERSION);
        if(pickKeyword("encoding")) {
            String encoding = makeString();
            if(!XMLUtil.isNCName(encoding))
                currentModule.error("XQST0087", prevTokenLoc,
                                    "improper encoding name: " + encoding);
            // What TODO with that rubbish?
            else
                currentModule.warning(prevTokenLoc, "Encoding currently ignored");
        }
        wantToken(T_SemiColon);
    }

    private void parseNewProlog() throws CompilationException
    {
        prologState = 0;
        for(;;)
        {
            switch(curToken) {
            case T_Declare: {
                nextToken();
                // swallow misc flags after 'declare'
                parseDeclFlags();
                // actual declaration: function, variable
                parseDeclaration();
                break;
            }
            case T_ImportModule:
                checkPrologState(1, "import module");
                parseImportModule();
                break;
                
            case T_ImportSchema:
                checkPrologState(1, "import schema");
                parseImportSchema();
                break;
            default:
                return;
            }
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
        int here = tokenStart;
        switch(curToken) {
        case T_DeclareNamespace:
            checkPrologState(1, "namespace");
            nextToken();
            parseNamespaceDecl();
            break;
        case T_DefaultElement:
        case T_DefaultFunction:
            checkPrologState(1, "default namespace");
            parseDefaultNamespaceDecl();
            break;
            
        case T_DeclareOrdering:
            checkPrologState(1, "ordering");
            nextToken();
            int order = checkOptionKeyword(Ordering_Keywords);
            checkDeclared("ordering", "XQST0065");
            currentModule.setOrderingMode(order == 0 ? 
                    XQueryContext.ORDERED : XQueryContext.UNORDERED);
            break;
        
        case T_DeclareDefaultOrder:
            checkPrologState(1, "ordering");
            nextToken();
            int emptyOrder = checkOptionKeyword(EmptyOrder_Keywords);
            checkDeclared("empty order", "XQST0065");
            currentModule.setDefaultOrderEmptyGreatest(emptyOrder == 1);
            break;
            
        case T_DeclareBoundarySpace:
            checkPrologState(1, "boundary-space");
            nextToken();
            checkDeclared("boundary-space", "XQST0068");
            preserveSpace = checkOptionKeyword(PreserveStrip_Keywords) == 0;
            currentModule.setBoundarySpacePolicy(preserveSpace ?
                            XQueryContext.PRESERVE : XQueryContext.NO_PRESERVE);
            break;
            
        case T_DeclareConstruction: // schemas
            checkPrologState(1, "construction");
            nextToken();
            int mode = checkOptionKeyword(PreserveStrip_Keywords); 
            checkDeclared("construction", "XQST0067");
            currentModule.setConstructionMode(mode == 0?
                    XQueryContext.PRESERVE : XQueryContext.NO_PRESERVE);
            break;
            
        case T_DeclareCopyNamespaces:
            checkPrologState(1, "copy-namespaces");
            nextToken();
            int preserve = checkOptionKeyword(CopyNS_Preserve_Keywords);
            wantToken(T_Comma);
            int inherit = checkOptionKeyword(CopyNS_Inherit_Keywords);
            checkDeclared("copy-namespaces", "XQST0055");
            currentModule.setNamespaceInheritMode(inherit == 0 ?
                    XQueryContext.INHERIT : XQueryContext.NO_INHERIT);
            currentModule.setNamespacePreserveMode(preserve == 0?
                    XQueryContext.PRESERVE : XQueryContext.NO_PRESERVE);
            break;
            
        case T_DefaultCollation:
            checkPrologState(1, "default collation");
            nextToken(); 
            checkToken(T_URLLiteral);
            String collation = makeString();
            checkDeclared("default collation", "XQST0038");
            try {
                currentModule.setDefaultCollation(collation);
            }
            catch (DataModelException e) {
                currentModule.error("XQST0038", prevTokenLoc,
                                    e.getMessage());
            }
            break;
            
        case T_DeclareBaseURI:
            checkPrologState(1, "base-uri");
            nextToken();
            checkToken(T_URLLiteral);
            checkDeclared("static base-uri", "XQST0032");
            currentModule.setBaseURI(makeString());
            break;
            
        case T_ValidationStrict:    // OBSOLETE
        case T_ValidationSkip:
        case T_ValidationLax:
            currentModule.error("XUST0026", here, "revalidation is not supported");
            nextToken();
            break;
            
        case T_DeclareFTOption:
            checkPrologState(1, "full-text option");
            nextToken();
            MatchOptions opt = new MatchOptions();
            parseFTMatchOptions(opt);
            currentModule.setDefaultFtOptions(opt);
            break;

        case T_DeclareOption:
            checkPrologState(2, "option");
            nextToken();
            checkToken(T_QName);
            IQName optionName = resolveQName(null);
            if(optionName.hasNoNamespace())
                currentModule.error("XPST0081", tokenStart, 
                                    "blank namespace not allowed");
            if( curToken != T_StringLiteral )
                wantToken(T_StringLiteral);
            String value = makeString();
            currentModule.storeOption(optionName, value);
            break;

        case T_DeclareVariable:
            checkPrologState(2, "variable");
            parseGlobalVarDecl();
            break;

        case T_DeclareFunction:
        case T_DeclareUpdatingFunction:
            checkPrologState(2, "function");
            parseFunctionDecl();
            break;

        default:
            syntax("expecting declaration");
            break;
        }
        pickToken(T_SemiColon); // optional
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
            switch(curToken) {
            case T_AssignableFlag:
                flag = DFLAG_ASSIGNABLE;
                break;
            case T_UnassignableFlag:
                flag = DFLAG_UNASSIGNABLE;
                break;
            case T_SimpleFlag:
                flag = DFLAG_SIMPLE;
                break;
            case T_PublicFlag:
                flag = DFLAG_PUBLIC;
                break;
            case T_PrivateFlag:
                flag = DFLAG_PRIVATE;
                break;
            case T_UpdatingFlag:
                flag = DFLAG_UPDATING;
                break;
            case T_SequentialFlag:
                flag = DFLAG_SEQUENTIAL;
                break;
            case T_DeterministicFlag:
                flag = DFLAG_DETERMINISTIC;
                break;
            case T_UndeterministicFlag:
                flag = DFLAG_UNDETERMINISTIC;
                break;
            default:
                return;
            }
            if((declFlags & flag) != 0)
                currentModule.error("XQ00?", prevTokenLoc, 
                                    "duplicate indicator '" + tokenName(curToken) +"'");
            declFlags |= flag;
            nextToken();
        }
    }

    // Returns the URL
    private String parseNamespaceDecl()
        throws CompilationException
    {
        if (curToken != T_NCName)
            syntax("expecting prefix");
        int prefixHere = tokenStart;
        String prefix = makeString();
        wantToken(T_AssignEquals);
        if (curToken != T_URLLiteral)
            wantToken(T_URLLiteral);
        String url = makeString();
        if (prefix.equals("xml") || prefix.equals("xmlns")
            || url.equals(NamespaceContext.XML))
            currentModule.error("XQST0070", tokenStart,
                                "illegal namespace declaration");
        currentModule.addNamespaceDecl(prefixHere, prefix, url);
        return url;
    }
    
    private String parseDefaultNamespaceDecl()
        throws CompilationException
    {
        boolean forFun = curToken == T_DefaultFunction;
        nextToken();
        int here = tokenStart;
        if (curToken != T_URLLiteral && curToken != T_StringLiteral)
            wantToken(T_URLLiteral);
        String url = makeString();
        if(checkDeclared(forFun ? "function namespace" : "element namespace",
                         "XQST0066"))
            currentModule.addDefaultNamespace(forFun, url);
        return url;
    }
    
    private void parseGlobalVarDecl()
        throws CompilationException
    {
        nextToken(); 
        int here = tokenStart;
        checkToken(T_VarName);
        SequenceType varType = null;
        QName name = resolveVarName();
        
        String moduleNS = currentModule.getNamespaceURI();
        if (moduleNS != NamespaceContext.LOCAL_NS
            && moduleNS != name.getNamespaceURI())
            currentModule.error("XQST0048", prevTokenLoc,
                                "namespace of variable name "
                                + currentModule.prefixedName(name)
                                + " should match the module namespace");
        
        if (pickToken(T_As))
            varType = parseSequenceType();
        Expression init = null;
        if (!pickToken(T_External))
            if (curToken == T_LbraceExprEnclosure)
                init = parseEnclosedExpr();
            else {
                wantToken(T_ColonEquals);
                init = parseExprSingle();
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
        nextToken();
        checkToken(T_QNameLpar);
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
    
        if (curToken != T_Rpar && curToken != T_RparAs) {
            //wantToken(T_VariableIndicator);
            checkToken(T_VarName);
            QName argName = resolveVarName();
            proto.arg(argName, pickToken(T_As)
                ? parseSequenceType() : XQType.ANY);
            while (pickToken(T_Comma)) {
                checkToken(T_VarName);
                argName = resolveVarName();
                proto.arg(argName, pickToken(T_As)
                    ? parseSequenceType() : XQType.ANY);
            }
        }
        if (pickToken(T_RparAs))
            proto.returnType = proto.declaredReturnType = parseSequenceType();
        else {
            wantToken(T_Rpar);
            if (pickToken(T_As))
                proto.returnType = proto.declaredReturnType = parseSequenceType();
        }
            
        if (!fun.addPrototype(proto))
            currentModule.error("XQST0034", proto.offset,
                            "duplicate prototype for function '" + name + "'");
    
        Expression body = null;
        if (!pickToken(T_External)) {
            body = parseEnclosedExpr();
        }
        proto.body = body;
        wantToken(T_SemiColon);            
        return fun;
    }

    private void parseImportModule()
        throws CompilationException
    {
        // 'import module' [ 'namespace' '=' ] ns [ 'at' url [ , url ]*]
        int here = tokenStart;
        nextToken(); 
        String prefix = null, nsuri, loc = null;
        int prefixHere = tokenStart;
        if(pickToken(T_Namespace)) {
            checkToken(T_NCName);
            prefix = makeString();
            wantToken(T_AssignEquals);
        }
        if(curToken != T_StringLiteral)
            checkToken(T_URLLiteral);
        
        nsuri = makeString();
        
        ArrayList atLocs = null;
        if(curToken == T_AtStringLiteral) {
            loc = makeString();
            //loc = FileUtil.resolve(baseUri, loc);
            atLocs = new ArrayList();
            atLocs.add(loc);
            while(pickToken(T_Comma)) {
                if(curToken != T_StringLiteral) 
                    checkToken(T_URLLiteral);
                loc = makeString();
                //loc = FileUtil.resolve(baseUri, loc);
                atLocs.add(loc);
            }
        }
        pickToken(T_SemiColon); 
        moduleImport(here, nsuri, atLocs);
        if(prefix != null)
            currentModule.addNamespaceDecl(prefixHere, prefix, nsuri);
    }

    private void parseImportSchema()
        throws CompilationException
    {
        int here = tokenStart;
        nextToken(); 
        String nsURL = "";
        switch(curToken) {
        case T_DefaultElement:
            nsURL = parseDefaultNamespaceDecl();
            break;
        case T_Namespace:
            nextToken();
            nsURL = parseNamespaceDecl();
            break;
        case T_StringLiteral:
        case T_URLLiteral:
            nsURL = makeString();
            // It seems that this islike 'default element namespace' ?
            currentModule.addDefaultNamespace(false, nsURL);
            break;
        default:
            syntax("improper schema import");
        }
        ArrayList locations = new ArrayList();
        if( curToken == T_AtStringLiteral) {
            locations.add(makeString());
            while(pickToken(T_Comma)) {
                if(curToken == T_StringLiteral || curToken == T_URLLiteral)
                    locations.add(makeString());
                else wantToken(T_StringLiteral);
            }
        }
        pickToken(T_SemiColon); 
        
        currentModule.error("XQST0009", here,
                              "schema import is not supported");
    }

    // SequenceExpr ::= Expr ( Comma Expr )*
    private Expression parseExpr()
        throws CompilationException
    {
        Expression e = parseExprSeq();
        if (!pickToken(T_SemiColon))
            return e;
        ApplyExpr seq = new ApplyExpr();
        locate(seq);
        seq.addExpr(e);
        while (curToken != T_Rpar && curToken != T_Rbrace
               && curToken != T_Rbrack && curToken != T_END) {
            seq.addExpr(parseExprSeq());
            wantToken(T_SemiColon);
        }
        return seq;
    }

    private Expression parseExprSeq()
        throws CompilationException
    {
        Expression e = parseExprSingle();
        if (curToken != T_Comma)
            return e;
        SequenceExpr seq = new SequenceExpr();
        locate(seq);
        seq.addExpr(e);
        while (pickToken(T_Comma))
            seq.addExpr(parseExprSingle());
        return seq;        
    }
    
    private Expression parseExprSingle()
        throws CompilationException
    {
        switch (curToken) {
        case T_ForVariable:
        case T_LetVariable:
        case T_LetScoreVariable:
        case T_ForTumbling:
        case T_ForSliding:
            return parseFLWRExpr();

        case T_Every:
        case T_Some:
            return parseQuantifiedExpr();
        case T_IfLpar:
            return parseIfExpr();
        case T_TypeswitchLpar:
            return parseTypeswitchExpr();
        case T_Switch:
            return parseSwitchExpr();

        case T_Block:
            nextToken();
            return parseBlock();
        case T_Exit:
            return parseExitExpr();
        case T_While:
            return parseWhileExpr();

            // XUpdate:
        case T_InsertNodes:
            return parseInsertNodes();
        case T_DeleteNodes:
            return parseDeleteNodes();
        case T_ReplaceNode:
        case T_ReplaceValueOfNode:
            return parseReplaceNode();
        case T_RenameNode:
            return parseRenameNode();
        case T_Copy:
            return parseTransform();
            
        default:
            Expression e = parseOrExpr();
            if(pickToken(T_ColonEquals))
                return parseAssign2(e);
            return e;
        }
    }
    
    // FLWRExpr ::=
    //    ( (ForClause | LetClause)+ WhereClause? OrderByClause?  Return )*  QuantifiedExpr 
    private Expression parseFLWRExpr()
        throws CompilationException
    {
        if (curToken != T_ForVariable &&
            curToken != T_LetVariable && curToken != T_LetScoreVariable &&
            curToken != T_ForTumbling  && curToken != T_ForSliding)
            return parseQuantifiedExpr();
        
        FLWRExpr flower = new FLWRExpr();
        locate(flower);
        // group of for let clauses at beginning
        for(;;) {
            if (pickToken(T_ForVariable)) {
                flower.addClause(parseForClause(true));
                while (pickCommaBeforeVar()) {
                    checkToken(T_VarName);
                    flower.addClause(parseForClause(true));
                }
            }
            else if (pickToken(T_LetVariable)) {
                flower.addClause(parseLetClause(false));
                while (pickCommaBeforeVar()) {
                    boolean score = pickToken(T_Score);
                    checkToken(T_VarName);
                    flower.addClause(parseLetClause(score));
                }
            }
            else if (pickToken(T_LetScoreVariable)) {
                flower.addClause(parseLetClause(true));
                while (pickCommaBeforeVar()) {
                    boolean score = pickToken(T_Score);
                    checkToken(T_VarName);
                    flower.addClause(parseLetClause(score));
                }
            }
            else if (pickToken(T_ForTumbling)) {
                flower.addClause(parseWindowClause(false));
            }
            else if (pickToken(T_ForSliding)) {
                flower.addClause(parseWindowClause(true));
            }
            else break;
        }

        // main where clause:
        if (pickToken(T_Where))
            flower.where = parseExprSingle();
        
        // XQ1.1: optional 'group by' clause, plus let* and another where
        if(pickToken(T_GroupBy)) {
            GroupingVariable keyVar = parseGroupBySpec();
            flower.groupingKeys = new GroupingVariable[] { keyVar };
            flower.postGroupingLets = new LetClause[0];
            
            while(pickToken(T_Comma)) {
                keyVar = parseGroupBySpec();
                
                VarReference[] old = flower.groupingKeys;
                flower.groupingKeys = new GroupingVariable[old.length + 1];
                System.arraycopy(old, 0, flower.groupingKeys, 0, old.length);
                flower.groupingKeys[old.length] = keyVar;
            }
            // list of let
            while (pickToken(T_LetVariable)) {
                LetClause let = parseLetClause(false);
                LetClause[] old = flower.postGroupingLets;
                flower.postGroupingLets = new LetClause[old.length + 1];
                System.arraycopy(old, 0, flower.postGroupingLets, 0, old.length);
                flower.postGroupingLets[old.length] = let;
            }
            // optional where:
            if (pickToken(T_Where))
                flower.postGroupingWhere = parseExprSingle();
        }
        
        // sorting:
        if (pickToken(T_OrderBy)) {
            flower.addOrderSpec(parseOrderSpec());
            while (pickToken(T_Comma))
                flower.addOrderSpec(parseOrderSpec());
        }
        else if (pickToken(T_OrderByStable)) {
            flower.stableOrder = true;
            flower.addOrderSpec(parseOrderSpec());
            while (pickToken(T_Comma))
                flower.addOrderSpec(parseOrderSpec());
        }
        wantToken(T_Return);
        flower.expr = parseExprSingle();
        return flower;
    }
    
    private boolean pickCommaBeforeVar() throws CompilationException
    {
        if(curToken != T_Comma)
            return false; 
        lexState = vardeclState;
        pickToken(T_Comma);
        return true;
    }
    
    // used for variable groups in Let
    private LetClause parseLetClause(boolean score)
        throws CompilationException
    {
        int here = prevTokenLoc;
        checkToken(T_VarName);
        LetClause clause = new LetClause(resolveVarName());
        clause.score = score;
        locate(clause, here);
        if (pickToken(T_As)) {
            if(score)
                currentModule.error("XPST0003", prevTokenLoc,
                                    "invalid type declaration after 'score'");
            clause.declaredType = parseSequenceType();
        }
        wantToken(T_ColonEquals);
        clause.expr = parseExprSingle();
        return clause;
    }
    
    // used for variable groups in For Some Every
    private ForClause parseForClause(boolean withPos)
        throws CompilationException
    {
        int here = prevTokenLoc;
        checkToken(T_VarName);
        ForClause clause = new ForClause(resolveVarName());
        locate(clause, here);
        if (pickToken(T_As))
            clause.declaredType = parseSequenceType();
        if (withPos && pickToken(T_At)) {
            checkToken(T_VarName);
            clause.position = resolveVarName();
        }
        if(pickToken(T_Score)) {
            checkToken(T_VarName);
            clause.score = resolveVarName();
        }
        wantToken(T_In);
        clause.expr = parseExprSingle();
        return clause;
    }
    
    private VarClause parseWindowClause(boolean sliding)
        throws CompilationException
    {
        int here = prevTokenLoc;
        checkToken(T_VarName);
        QName mainVar = resolveVarName();
        WindowClause clause = new WindowClause(mainVar, sliding);
        locate(clause, here);
        if (pickToken(T_As))
            clause.declaredType = parseSequenceType();
        wantToken(T_In);
        clause.expr = parseExprSingle();
        
        // start/end conditions:
        wantToken(T_Start);
        parseWindowCond(clause.startCond);
        
        if(pickToken(T_OnlyEnd))
            clause.onlyEnd = true;
        else if(curToken != T_End && !sliding) {
            clause.endCond = null;
            return clause;  // tumbling: optional end clause
        }
        else
            wantToken(T_End);
        parseWindowCond(clause.endCond);
        return clause;
    }

    private void parseWindowCond(Condition cond)
        throws CompilationException
    {
         if(curToken == T_VarName) {
             cond.itemVarName = resolveVarName();
         }
         if(pickToken(T_At)) {
             checkToken(T_VarName);
             cond.atVarName = resolveVarName();
         }
         if(pickToken(T_Previous)) {
             checkToken(T_VarName);
             cond.previousVarName = resolveVarName();
         }
         if(pickToken(T_Next)) {
             checkToken(T_VarName);
             cond.nextVarName = resolveVarName();
         }
         wantToken(T_When);
         cond.cond = parseExprSingle();
    }

    private OrderSpec parseOrderSpec( ) throws CompilationException 
    {
        int here = tokenStart;
        OrderSpec spec = new OrderSpec( parseExprSingle() );
        locate(spec, here);
        if( pickToken(T_Descending) )
            spec.descending = true;
        else pickToken(T_Ascending);
        spec.emptyGreatest = currentModule.getDefaultOrderEmptyGreatest();
        if( pickToken(T_EmptyGreatest) )
            spec.emptyGreatest = true;
        else if(pickToken(T_EmptyLeast))
            spec.emptyGreatest = false;
        if( pickToken(T_Collation)) {
            checkToken(T_StringLiteral);
            spec.collation = makeString();
        }
        return spec;
    }
    
    private GroupingVariable parseGroupBySpec() throws CompilationException
    {
        checkToken(T_VarName);
        QName name = resolveVarName();
        String collation = null;
        if(pickToken(T_Collation)) {
            checkToken(T_StringLiteral);
            collation = makeString();
        }
        GroupingVariable var = new GroupingVariable(name, collation);
        locate2(var);
        return var;
    }

    // QuantifiedExpr ::=
    //	    ( (Some | Every) VarClause ( Comma VarClause )*  Satisfies )* ExprSingle
    // VarClause ::= VarName ( TypeDeclaration )?  In Expr
    private Expression parseQuantifiedExpr( ) throws CompilationException 
    {
        if( curToken != T_Some && curToken != T_Every )
            return parseTypeswitchExpr();
        QuantifiedExpr q = new QuantifiedExpr(curToken == T_Every);
        locate(q);
        nextToken();
        q.addVarClause( parseForClause(false) );
        while(pickCommaBeforeVar()) {
            checkToken(T_VarName);
            q.addVarClause( parseForClause(false) );
        }
        wantToken(T_Satisfies);
        q.cond = parseExprSingle();
        return q;
    }
    
    private Expression parseSwitchExpr()
        throws CompilationException
    {
        int here = tokenStart;
        pickToken(T_Switch);
        SwitchExpr sw = new SwitchExpr(parseExpr());
        locate(sw, here);

        wantToken(T_Rpar);
        if(curToken != T_Default)
            lexState = defState; // scary hack because syntax is ambiguous
        
        while (pickToken(T_Case)) {
            SwitchExpr.Case cc = new SwitchExpr.Case();
            locate2(cc);
            sw.addCase(cc);
            cc.key = parseExprSingle();
            // empty return if followed by 'case' 
            if(pickToken(T_Return)) {
                cc.expr = parseExprSingle();
            }
            if(curToken != T_Default)
                lexState = defState; // scary hack because syntax is ambiguous
        }
        
        // default:
        wantToken(T_Default);
        SwitchExpr.Case def = new SwitchExpr.Case();
        locate2(def);
        wantToken(T_Return);
        def.expr = parseExprSingle();
        sw.addCase(def);
        return sw;
    }

    // TypeswitchExpr ::= 
    //  (TypeswitchLpar Expr Rpar CaseClause+ Default ( VariableIndicator VarName )?  Return  )*  IfExpr 
    // CaseClause ::= Case (
    private Expression parseTypeswitchExpr()
        throws CompilationException
    {
        int here = tokenStart;
        if (!pickToken(T_TypeswitchLpar))
            return parseIfExpr();
        TypeswitchExpr sw = new TypeswitchExpr(parseExpr());
        locate(sw, here);
        wantToken(T_Rpar);
        while (pickToken(T_Case)) {
            TypeCaseClause cc = new TypeCaseClause();
            locate2(cc);
            sw.addCaseClause(cc);
            
            if (curToken == T_VarName) {
                cc.variable = resolveVarName();
                wantToken(T_As);
            }
            cc.declaredType = parseSequenceType();
            wantToken(T_Return);
            cc.expr = parseExprSingle();
        }
        wantToken(T_Default);
        TypeCaseClause defc = new TypeCaseClause();
        locate2(defc);
        sw.addCaseClause(defc);
        if (curToken == T_VarName) {
            defc.variable = resolveVarName();
        }
        wantToken(T_Return);
        defc.expr = parseExprSingle();
        return sw;
    }

    private Expression parseWhileExpr() throws CompilationException
    {
        int here = tokenStart;
        nextToken();
        Expression cond = parseExprSingle();
        wantToken(T_Rpar);
        wantToken(T_LbraceExprEnclosure);
        Expression block = parseBlock();
        return locate(new WhileExpr(cond, block), here);
    }

    // a block after the leading brace
    private Expression parseBlock() throws CompilationException
    {
        BlockExpr block = new BlockExpr();
        locate(block, prevTokenLoc);
        // variables:
        while(pickToken(T_Declare)) {
            block.addClause(parseBlockVar());
            while(pickToken(T_Comma)) {
                block.addClause(parseBlockVar());
            }
            wantToken(T_SemiColon);
        }
        // body:
        block.body = parseExpr();
        wantToken(T_Rbrace);
        return block;
    }
    
    private LetClause parseBlockVar()
        throws CompilationException
    {
        int here = tokenStart;
        checkToken(T_VarName);
        LetClause clause = new LetClause(resolveVarName());
        locate(clause, here);
        if (pickToken(T_As)) {
            clause.declaredType = parseSequenceType();
        }
        if(pickToken(T_ColonEquals)) 
            clause.expr = parseExprSingle();
        return clause;
    }
    
//    private Expression parseAssign()
//        throws CompilationException
//    {
//        int here = tokenEnd - 2;
//        checkToken(T_VarAssign);
//        IQName var = resolveVarName();
//        return locate(new AssignExpr(var, parseExprSingle()), here);
//    }

    private Expression parseAssign2(Expression lhs)
        throws CompilationException
    {
        int here = tokenStart;
        //IQName var = resolveVarName();
        if(lhs instanceof VarReference) {
            VarReference ref = (VarReference) lhs;
            return locate(new AssignExpr(ref.name, parseExprSingle()), here);
        }
        tokenStart = lhs.offset;
        syntax("invalid left handside of assignment");
        return null;
    }

    private Expression parseExitExpr() throws CompilationException
    {
        int here = tokenStart;
        nextToken();
        Expression ret = parseExprSingle();
        return locate(new ExitExpr(ret), here);
    }

    private Expression parseIfExpr()
        throws CompilationException
    {
        int here = tokenStart;
        if (!pickToken(T_IfLpar))
            return parseInstanceofExpr();
        Expression e1 = parseExpr();
        wantToken(T_Rpar);
        wantToken(T_Then);
        Expression e2 = parseExprSingle();
        wantToken(T_Else);
        return locate(new IfExpr(e1, e2, parseExprSingle()), here);
    }
    
    private Expression parseOrExpr()
        throws CompilationException
    {
        Expression e = parseAndExpr();
        if (curToken != T_Or)
            return e;
        OrExpr or = new OrExpr(e);
        locate(or);
        while (pickToken(T_Or))
            or.addExpr(parseAndExpr());
        return or;
    }

    private Expression parseAndExpr()
        throws CompilationException
    {
        Expression e = parseComparisonExpr();
        if (curToken != T_And)
            return e;
        AndExpr and = new AndExpr(e);
        locate(and);
        while (pickToken(T_And))
            and.addExpr(parseComparisonExpr());
        return and;
    }

    // associativity is not clear here, anyway it does not make sense
    private Expression parseComparisonExpr()
        throws CompilationException
    {
        Expression e = parseFTContainsExpr();
        loop: for (;;) {
            int here = tokenStart;
            switch (curToken) {
            case T_FortranLt:
                nextToken();
                e = new ValueLtOp(e, parseFTContainsExpr());
                break;
            case T_FortranEq:
                nextToken();
                e = new ValueEqOp(e, parseFTContainsExpr());
                break;
            case T_FortranLe:
                nextToken();
                e = new ValueLeOp(e, parseFTContainsExpr());
                break;
            case T_FortranNe:
                nextToken();
                e = new ValueNeOp(e, parseFTContainsExpr());
                break;
            case T_FortranGt:
                nextToken();
                e = new ValueGtOp(e, parseFTContainsExpr());
                break;
            case T_FortranGe:
                nextToken();
                e = new ValueGeOp(e, parseFTContainsExpr());
                break;
            case T_Gt:
                nextToken();
                e = new GtOp(e, parseFTContainsExpr());
                break;
            case T_GtEquals:
                nextToken();
                e = new GeOp(e, parseFTContainsExpr());
                break;
            case T_Lt:
                nextToken();
                e = new LtOp(e, parseFTContainsExpr());
                break;
            case T_LtEquals:
                nextToken();
                e = new LeOp(e, parseFTContainsExpr());
                break;
            case T_Equals:
                nextToken();
                e = new EqOp(e, parseFTContainsExpr());
                break;
            case T_NotEquals:
                nextToken();
                e = new NeOp(e, parseFTContainsExpr());
                break;
            case T_IsNot:
                nextToken();
                e = new IsNotOp(e, parseFTContainsExpr());
                break;
            case T_Is:
                nextToken();
                e = new IsOp(e, parseFTContainsExpr());
                break;
            case T_LtLt:
                nextToken();
                e = new BeforeOp(e, parseFTContainsExpr());
                break;
            case T_GtGt:
                nextToken();
                e = new AfterOp(e, parseFTContainsExpr());
                break;

            default:
                break loop;
            }
            locate(e, here);
        }
        return e;
    }
    
    private Expression parseFTContainsExpr()
        throws CompilationException
    {
        Expression e = parseRangeExpr();
        if(pickToken(T_FTContains)) {
            FTContainsOp ftc = new FTContainsOp(e, parseFTSelection());
            e = ftc;
            if(pickToken(T_WithoutContent))
                ftc.ignore = parseUnionExpr();
        }
        return e;
    }
    
    private Expression parseRangeExpr()
        throws CompilationException
    {
        Expression e = parseAdditiveExpr();
        int here = tokenStart;
        if (pickToken(T_To)) {
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
            int here = tokenStart;
            if (pickToken(T_Plus))
                e = new PlusOp(e, parseMultiplicativeExpr());
            else if (pickToken(T_Minus))
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
            int here = tokenStart;
            if( pickToken(T_Multiply))
                e = new MulOp(e, parseUnionExpr() );
            else if( pickToken(T_Div) )
                e = new DivOp(e, parseUnionExpr() );
            else if( pickToken(T_Idiv) )
                e = new IDivOp(e, parseUnionExpr() );
            else if( pickToken(T_Mod) )
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
        while( pickToken(T_Union) || pickToken(T_Vbar) ) {
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
            int here = tokenStart;
            if( pickToken(T_Intersect))
                e = new IntersectOp(e, parseInstanceofExpr() );
            else if(pickToken(T_Except))
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
        int here = tokenStart;
        if(pickToken(T_Instanceof)) {
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
        int here = tokenStart;
        if( pickToken(T_TreatAs) ) {
            locate(e = new TreatExpr(e, parseSequenceType()), here);
        }
        return e;
    }
    
    // CastableExpr ::= CastExpr ( <Castable As> SingleType  )*  
    //
    Expression parseCastableExpr( ) throws CompilationException 
    {
        Expression e = parseCastExpr();
        int here = tokenStart;
        if(pickToken(T_CastableAs))
            locate(e = new CastableExpr(e, parseSingleType()), here);
        return e;
    }
    
    // CastExpr ::= ComparisonExpr ( <Cast As> SingleType  )*  
    //
    Expression parseCastExpr( ) throws CompilationException 
    {
        Expression e = parseUnaryExpr();
        int here = tokenStart;
        if(pickToken(T_CastAs))
            locate(e = new CastExpr(e, parseSingleType()), here);
        return e;
    }
    
    // UnaryExpr ::= ( ( Minus | Plus) )*  UnionExpr 
    Expression parseUnaryExpr( ) throws CompilationException 
    {
        if( pickToken(T_Minus)) {
            int here = prevTokenLoc;
            return locate( new NegateOp(parseUnaryExpr()), here );
        }
        else if( pickToken(T_Plus)) {
            Expression expr = parseUnaryExpr();
            int here = prevTokenLoc;
            if(expr instanceof StringLiteral) // hack for xqts...
                currentModule.error("XPTY0004", here,
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
        if (curToken == T_Validate)
            return parseValidateExpr();
        else if (curToken == T_Try) // *** EXTENSION ***
            return parseTryCatchExpr();
        else if (curToken == T_Pragma) {
            do {
                QName extName = checkPragma();
                // for the moment, ignore all pragmas...
                  
                nextToken();
            }
            while (curToken == T_Pragma);
            wantToken(T_LbraceExprEnclosure);
            Expression e = parseExpr();
            wantToken(T_Rbrace);
            return e;
        }
        else
            return parsePathExpr();
    }

    private QName checkPragma() throws CompilationException
    {
        // check prefix/namespace: assumes prefixValue+localName set
        String ns = expandPrefix(prefixValue);
        if (ns == NamespaceContext.EMPTY)
            currentModule.error("XPST0081", tokenEnd + 3,
                                "blank namespace not allowed for extension");
        return IQName.get(ns, localName);
    }
    
    Expression parseTryCatchExpr()
        throws CompilationException
    {
        int here = tokenStart;
        nextToken();
        Expression caught = parseEnclosedExpr();
        wantToken(T_Catch);
        
        TryCatchExpr trycat = null;
        if(pickToken(T_Lpar)) {
            checkToken(T_VarName);
            QName name = resolveVarName();
            wantToken(T_Rpar);
            trycat = new TryCatchExpr(caught, name, parseEnclosedExpr());
        }
        else {  // support of XQ1.1
            TryCatchExpr.Catch handler = parseCatch();
            trycat = new TryCatchExpr(caught, handler);

            while(pickToken(T_Catch)) {
                handler = parseCatch();
                trycat.addCatch(handler);
            }
        }
        return locate(trycat, here);
    }

    private TryCatchExpr.Catch parseCatch() throws CompilationException 
    {
        int here = tokenStart;
        BaseNodeFilter test = parseNameTest(false);
        TryCatchExpr.Catch cat = new TryCatchExpr.Catch(test);
        locate(cat, here);
        while(pickToken(T_Vbar)) {
            test = parseNameTest(false);
            cat.addTest(test);
        }
        // variables:
        if(pickToken(T_Lpar)) {
            checkToken(T_VarName);
            cat.codeVarName = resolveVarName();
            if(pickToken(T_Comma)) {
                checkToken(T_VarName);
                cat.descVarName = resolveVarName();
                if(pickToken(T_Comma)) {
                    checkToken(T_VarName);
                    cat.valueVarName = resolveVarName();
                }
            }
            wantToken(T_Rpar);
        }
        cat.handler = parseEnclosedExpr();
        return cat;
    }

    // PathExpr ::= Root RelativePathExpr? |
    // 		    RootDescendants RelativePathExpr | RelativePathExpr 
    // RelativePathExpr ::= StepExpr ( ( Slash | SlashSlash) StepExpr )*  
    Expression parsePathExpr( ) throws CompilationException 
    {
        int here = tokenStart;
        boolean atRoot = false, atRootDesc = false;
        if( pickToken(T_Root) )
            atRoot = true;
        else if( pickToken(T_RootDescendants) )
            atRoot = atRootDesc = true;
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
        if( curToken != T_Slash && curToken != T_SlashSlash )
            return p != null ? p : step;
        // definitely a path expr:
        if( p == null ) {
            locate(p = new PathExpr(), here);
            p.addStep(step);
        }
        while( curToken == T_Slash || curToken == T_SlashSlash ) {
            if( pickToken(T_SlashSlash) )
                p.addStep( locate(new DescendantOrSelfStep(null), here) );
            else nextToken();
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
        int here = tokenStart;
        switch(curToken) {
        case T_DotDot:
            nextToken();
            step = new ParentStep(null);
            break;
        case T_AxisAncestor:
            nextToken();
            step = new AncestorStep(parseNodeTest());
            break;
        case T_AxisAncestorOrSelf:
            nextToken();
            step = new AncestorOrSelfStep(parseNodeTest());
            break;
        case T_AxisAttribute:
            nextToken();
            step = new AttributeStep(parseNodeTest(true));
            break;
        case T_At:
            nextToken();
            step = new AttributeStep(parseNameTest(true));
            break;
        case T_AxisChild:
            nextToken();
            step = new ChildStep(parseNodeTest());
            break;
        case T_AxisDescendant:
            nextToken();
            step = new DescendantStep(parseNodeTest());
            break;
        case T_AxisDescendantOrSelf:
            nextToken();
            step = new DescendantOrSelfStep(parseNodeTest());
            break;
        case T_AxisFollowing:
            nextToken();
            step = new FollowingStep(parseNodeTest());
            break;
        case T_AxisFollowingSibling:
            nextToken();
            step = new FollowingSiblingStep(parseNodeTest());
            break;
            // 	    case T_AxisNamespace:
            // 		nextToken(); step = new NamespaceStep( parseNodeTest() );
            // 		break;
        case T_AxisSelf:
            nextToken(); step = new SelfStep( parseNodeTest() );
            break;
        case T_AxisParent:
            nextToken(); step = new ParentStep( parseNodeTest() );
            break;
        case T_AxisPrecedingSibling:
            nextToken(); step = new PrecedingSiblingStep( parseNodeTest() );
            break;
        case T_AxisPreceding:
            nextToken(); step = new PrecedingStep( parseNodeTest() );
            break;
        case T_Dot:
            nextToken(); step = new SelfStep( null );
            break;
        case T_AttributeLpar:
            step = new AttributeStep( parseNodeTest(true) );
            break;
        case T_DocumentNodeLpar:
        case T_ElementLpar:
        case T_CommentLpar:
        case T_TextLpar:
        case T_NodeLpar:
        case T_ProcessingInstructionLpar:
        case T_NCNameColonStar:
        case T_QName:
        case T_Star:
        case T_StarColonNCName:
            step = new ChildStep( parseNodeTest() );
            break;
        case T_Lbrack:
            syntax("unexpected '['");
        default:
            step = parsePrimaryExpr();
            break;
        }
        if(step != null)
            locate(step, here);
        // predicates:
        FilterExpr filtered = (step instanceof FilterExpr)? (FilterExpr) step : null;
        while( pickToken(T_Lbrack) ) {
            here = prevTokenLoc;
            if(filtered == null)
                filtered = new FilterExpr( step );
            filtered.addPredicate(parseExpr());
            locate(filtered, here);
            wantToken(T_Rbrack);
        }
        return filtered == null ? step : filtered;
    }
    
    // ValidateExpr ::= 'validate' SchemaMode ? SchemaContext? '{' Expr '}'
    //
    Expression parseValidateExpr( ) throws CompilationException 
    {
        int here = tokenStart;
        wantToken(T_Validate);
        int mode = -1;
        if( pickKeyword("lax") )
            mode = ValidateExpr.LAX_MODE;
        else if( pickKeyword("strict") )
            mode = ValidateExpr.STRICT_MODE;
        else if( pickKeyword("skip") )
            mode = ValidateExpr.SKIP_MODE;
        Expression sc = null;
        if( !pickToken(T_LbraceExprEnclosure) ) {
            sc = parseSchemaContext();
            wantToken(T_LbraceExprEnclosure);
        }
        Expression e = parseExpr();
        wantToken(T_Rbrace);
        e = new ValidateExpr(mode, null, e);
        locate(e, here);
        return e;
    }
    
    
    // PrimaryExpr ::= ( Literal | FunctionCall | VariableIndicator VarName  | ParenthesizedExpr) 
    //
    Expression parsePrimaryExpr( ) throws CompilationException 
    {
        switch(curToken) {
        case T_DecimalLiteral:
            return locate2( new DecimalLiteral(makeDecimal()) );
        case T_DoubleLiteral:
            return locate2( new DoubleLiteral(makeNumber()) );
        case T_IntegerLiteral:
            return locate2( new IntegerLiteral(makeInteger()) );
        case T_StringLiteral:
            return locate2( makeStringLiteral() );
        case T_QNameLpar:
            return parseFunctionCall();
//        case T_CallTemplate:
//            return parseCallTemplate();
        case T_VarName:
            return locate2(new VarReference( resolveVarName()));
        case T_Lpar:
            return parseParenthesizedExpr();
            
        case T_StartTagOpenRoot:
        case T_StartTagOpen:
        case T_XmlComment:
        case T_ProcessingInstruction:
            
        case T_DocumentLbrace:
        case T_ElementQNameLbrace:
        case T_ElementLbrace:
        case T_AttributeQNameLbrace:
        case T_AttributeLbrace:
        case T_TextLbrace:
        case T_CommentLbrace:
        case T_NamespaceLbrace:
        case T_PILbrace:
        case T_PINameLbrace:
            return parseConstructor( );

        case T_Ordered:
        case T_Unordered:
            nextToken(); 
            Expression encl = parseExpr();
            wantToken(T_Rbrace);
            return encl;    // downright ignored!
            

        default:	// dont protest immediately
            return null;
        }
    }
    
    Expression parseParenthesizedExpr( ) throws CompilationException 
    {
        wantToken(T_Lpar);
        if( pickToken(T_Rpar) )
            return new SequenceExpr();
        Expression e = parseExpr();
        wantToken(T_Rpar);
        return e;
    }
    
    Expression parseFunctionCall()
        throws CompilationException
    {
        int here = tokenStart;
        checkToken(T_QNameLpar);
        QName fname = resolveQName(currentModule.getDefaultFunctionNamespace());
        // standard function
        FunctionCall call = new FunctionCall(fname);
        locate(call, here);
        if (!pickToken(T_Rpar)) {
            call.addArgument(parseExprSingle());
            while (pickToken(T_Comma))
                call.addArgument(parseExprSingle());
            wantToken(T_Rpar);
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
        checkToken(T_QName);
        XQItemType itype = checkTypeName( resolveElementName() );
        return pickToken(T_QMark) ? (XQType) itype.opt : itype;
    }
    
    // SequenceType ::=  ItemType OccurrenceIndicator  |  Empty 
    SequenceType parseSequenceType( ) throws CompilationException 
    {
        if(pickToken(T_Empty))
            return XQType.NONE.opt; // optional by essence!
        XQItemType itemType = parseItemType();
        if(pickToken(T_Star) || pickToken(T_Multiply))
            return itemType.star;
        else if(pickToken(T_Plus))
            return itemType.plus;
        else if(pickToken(T_QMark))
            return itemType.opt;
        return itemType.one;
    }
    
    // ItemType ::= ( ( ElementType | AttributeType) ( ElemOrAttrType )?  
    //		  | Node | ProcessingInstruction | Comment | Text | Document
    //		  | Item | AtomicType | Untyped | AtomicValue
    //
    XQItemType parseItemType( ) throws CompilationException 
    {
        switch(curToken) {
        case T_DocumentNodeLpar:
        case T_ElementLpar:
        case T_TextLpar:
        case T_ProcessingInstructionLpar:
        case T_CommentLpar:
        case T_NodeLpar:
            return new NodeType(parseNodeTest());
        case T_AttributeLpar:
            return new NodeType(parseNodeTest(true));
        case T_QName:
            XQItemType itype = checkTypeName( resolveElementName() );
            // wish it were "default type NS" resolveQName(Namespace.XSD)
            return itype;
        case T_ItemLparRpar:
            nextToken();
            return XQType.ITEM;
        default:
            syntax("expecting item type");
            return null;
        }
    }
    
    NodeFilter parseNodeTest( ) throws CompilationException {
        return parseNodeTest(false);
    }
    
    NodeFilter parseNodeTest(boolean forAttr)
        throws CompilationException
    {
        int here = tokenStart;
        switch (curToken) {
        case T_DocumentNodeLpar:
            nextToken();
            NodeFilter nt = curToken == T_Rpar ? null : parseElemAttrTest(false);
            wantToken(T_Rpar);
            if (nt == null)
                nt = new BaseNodeFilter(Node.ELEMENT, null, null);
            return locate(here, new DocumentTest(nt));
        case T_ElementLpar:
            return locate(here, parseElemAttrTest(false));
        case T_AttributeLpar:
            return locate(here, parseElemAttrTest(true));
        case T_TextLpar:
            nextToken();
            wantToken(T_Rpar);
            return locate(here, new BaseNodeFilter(Node.TEXT, null, null));
        case T_ProcessingInstructionLpar:
            nextToken();
            String target = null;
            if (curToken == T_StringLiteral)
                target = makeString();
            else if (curToken == T_QName)
                target = makeString(); // ignore NS!!
            wantToken(T_Rpar);
            return locate(here, new BaseNodeFilter(Node.PROCESSING_INSTRUCTION,
                                                 null, target));
        case T_CommentLpar:
            nextToken();
            wantToken(T_Rpar);
            return locate(here, new BaseNodeFilter(Node.COMMENT, null, null));
        case T_NodeLpar:
            nextToken();
            wantToken(T_Rpar);
            return new BaseNodeFilter(-1, null, null); // not null: we want
                                                        // nodes
        default:
            return parseNameTest(forAttr);
        }
    }
    
    BaseNodeFilter parseElemAttrTest(boolean forAttr)
        throws CompilationException
    {
        wantToken(forAttr ? T_AttributeLpar : T_ElementLpar);
        String ns = null;
        String name = null;
        SchemaContext path = null;
        if (curToken != T_Rpar && !pickToken(T_Star)) {
            path = parseSchemaContextPath(forAttr);
            if (path.isSimpleName()) {
                QName qname = path.getStep(0);
                path = null;
                ns = qname.getNamespaceURI();
                name = qname.getLocalPart();
            }
        }
        if (path == null && pickToken(T_Comma)) {
            QName typeName = parseStarName();
            locate2(path = new SchemaContext(true));
            path.addStep(typeName);
            if (typeName != null && 
                 typeName.getNamespaceURI() != NamespaceContext.XSD)
                path.module.error("XPST0001", path.offset,
                                  "undefined type " + typeName);
        }
        wantToken(T_Rpar);
        if (path != null)
            path.module.warning("XPST0008", path.offset,
                                "Schema Type test not supported");
        return new BaseNodeFilter(forAttr ? Node.ATTRIBUTE : Node.ELEMENT, 
                                  ns, name, path);
    }
    
    
    QName parseStarName() throws CompilationException  {
        if(curToken == T_QName)
            return resolveElementName();
        else if(!pickToken(T_Star))
            syntax("expecting name or '*'");
        return null;
    }
    
    BaseNodeFilter parseNameTest(boolean forAttr)
        throws CompilationException
    {
        int here = tokenStart;
        String dns = forAttr ? "" : currentModule.getDefaultElementNamespace();
        int kind = forAttr ? Node.ATTRIBUTE : Node.ELEMENT;
        BaseNodeFilter test = null;
        switch (curToken) {
        case T_QName:
            test = new BaseNodeFilter(kind, expandPrefix(prefixValue, dns),
                                      makeString());
            break;
        case T_StarColonNCName:
            test = new BaseNodeFilter(kind, null, makeString());
            break;
        case T_Star:
            nextToken();
            test = new BaseNodeFilter(kind, null, null);
            break;
        case T_NCNameColonStar:
            test = new BaseNodeFilter(kind, expandPrefix(makeString(), dns), null);
            break;
        default:
            syntax("expecting name test");
            return null;
        }
        locate(here, test);
        return test;
    }
    
    // SchemaContext ::= 'global' | 'context' SchemaContextLocation 
    SchemaContext parseSchemaContext( ) throws CompilationException 
    {
        if(pickKeyword("global"))
            return null;
        if(pickKeyword("context"))
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
        SchemaContext sc = new SchemaContext(curToken == T_TypeQName);
        String defNS =
            forAttr ? null : currentModule.getDefaultElementNamespace();
        if (curToken != T_TypeQName && curToken != T_QName)
            wantToken(T_QName);
        sc.addStep(resolveQName(defNS));
        while (pickToken(T_Slash))
            if (curToken == T_QName)
                sc.addStep(resolveQName(defNS));
            else {
                sc.endsWithSlash = true;
                break;
            }
        return sc;
    }
    
    Expression parseConstructor( ) throws CompilationException 
    {
        int here = tokenStart;
        Expression e = null;
        NamedConstructor nc;
        
        switch(curToken) {

        
        case T_StartTagOpenRoot:
        case T_StartTagOpen:
            return parseElementConstructor();
        case T_XmlComment:
            return locate(new AtomConstructor( Node.COMMENT, makeStringLiteral() ), here);
        case T_ProcessingInstruction:
            return locate(new PIConstructor( makeString() ), here);

        case T_TextLbrace:
            nextToken();
            if(!pickToken(T_Rbrace)) {
                e = parseExpr();
                wantToken(T_Rbrace);
            }
            return new AtomConstructor(Node.TEXT, e);
            
            
            
        case T_DocumentLbrace:
            nextToken();
            e = new DocumentConstructor(parseExpr());
            wantToken(T_Rbrace);
            return e;
            
        case T_ElementLbrace:
        case T_AttributeLbrace:
            nc = (curToken == T_ElementLbrace)
                      ? (NamedConstructor) new ElementConstructor(null)
                      : new AttributeConstructor(null);
            nextToken();
            nc.name = parseExpr();
            wantToken(T_Rbrace);
            wantToken(T_LbraceExprEnclosure);
            if(!pickToken(T_Rbrace)) {
                nc.addItem( parseExpr() );
                wantToken(T_Rbrace);
            }
            return locate(nc, here);
            
        case T_ElementQNameLbrace:
        case T_AttributeQNameLbrace:
            int prevToken = curToken;
            // QName must be expanded using inscope mappings 
            // attention specs are UNCLEAR for attrs: use same rule as non computed
            String ns = "";
            if(curToken == T_ElementQNameLbrace || prefixValue.length() > 0)
                ns = expandPrefix(prefixValue);
            QName qName = IQName.get(ns, makeString());
            Expression qname = new QNameLiteral(qName);
            locate(qname, prevToken);
            nc = (prevToken == T_ElementQNameLbrace)
                    ? (NamedConstructor) new ElementConstructor(qname)
                    : new AttributeConstructor(qname);
            if(!pickToken(T_Rbrace)) {
                nc.addItem( parseExpr() );
                wantToken(T_Rbrace);
            }
            return locate(nc, here);
            
        case T_CommentLbrace:
            nextToken();
            e = parseExpr();
            wantToken(T_Rbrace);
            return locate(new AtomConstructor( Node.COMMENT, e ), here);
            
        case T_NamespaceLbrace:
            String prefix = makeString();
            e = parseExpr();
            wantToken(T_Rbrace);
            return locate(new NamespaceConstructor(new StringLiteral(prefix),
                                                   e), here);
            
        case T_PILbrace:
            nextToken();
            Expression name = parseExpr();
            wantToken(T_Rbrace);
            wantToken(T_LbraceExprEnclosure);
            if(!pickToken(T_Rbrace)) {
                e = parseExpr();
                wantToken(T_Rbrace);
            }
            return locate(new PIConstructor(name, e), here);
            
        case T_PINameLbrace:
            String target = makeString();
            e = parseExpr();
            wantToken(T_Rbrace);
            return locate(new PIConstructor(new StringLiteral(target), e),
                          here);
            
        default:
            syntax("illegal constructor"); return null;
        }
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
        checkToken(T_StartTagOpen);

        //checkToken(T_TagQName);
        String tagPrefix = prefixValue, tagName = makeString();
        ElementConstructor c = new ElementConstructor(null);
        c.setDirect();
        locate(c, here);

        pickToken(T_S);
        NamespaceContext nsMapping = currentModule.getInScopeNS();
        nsMapping.newLevel();

        while (curToken == T_TagQName) {
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

        if (pickToken(T_EmptyTagClose)) {
            nsMapping.popLevel();
            return c;
        }
        wantToken(T_TagClose);
        StringBuffer textBuf = new StringBuffer();
        // parse contents:
        loop: for (;;) {
            textBuf.setLength(0);
            here = tokenStart;
            boolean keepAnyway = false;
            while (curToken == T_Char || curToken == T_CharRef
                    || curToken == T_Cdata)
            {
                textBuf.append(saveBuffer);
                if (curToken == T_CharRef || curToken == T_Cdata)
                    keepAnyway = true;
                else if (!keepAnyway && !preserveSpace)
                    for (int ic = saveBuffer.length(); --ic >= 0;)
                        if (!Character.isWhitespace(saveBuffer.charAt(ic))) {
                            keepAnyway = true;
                            break;
                        }
                nextToken();
            }
            if (textBuf.length() > 0)
                if (preserveSpace || keepAnyway)
                    locate(c.addTextItem(textBuf.toString()), here);
            switch (curToken) {
            case T_Lbrace:
            case T_LbraceExprEnclosure:
                c.addItem(parseEnclosedExpr());
                break;
            case T_StartTagOpen:
                c.addItem(parseElementConstructor());
                break;
            case T_XmlComment:
                c.addItem(locate2(new AtomConstructor(Node.COMMENT,
                                                      makeStringLiteral())));
                break;
            case T_ProcessingInstruction:
                c.addItem(locate2(new PIConstructor(makeString())));
                break;
            case T_EndTag:
            case T_END:
                break loop;
            }
        }

        checkToken(T_EndTag);
        //checkToken(T_TagQName);
        String etagPrefix = prefixValue;
        if (!tagPrefix.equals(etagPrefix))
            currentModule.error(ERR_SYNTAX, tokenStart,
                                "mismatched prefix on end-tag: "
                                    + "must be equal to start tag prefix");
        tagName = makeString();

        ens = expandPrefix(etagPrefix);
        QName etag = IQName.get(ens == null ? "" : ens, tagName);
        if (etag != stag)
            syntax("tag mismatch: " + etag + " encountered when expecting "
                   + stag);

        nsMapping.popLevel();
        return c;
    }

    // returns true if really attribute and not NS
    private boolean parseAttribute(ElementConstructor c)
        throws CompilationException
    {
        AttributeConstructor ac = new AttributeConstructor( null );
        ac.prefix = prefixValue;	// temporary
        ac.value = makeString();
        locate2(ac);
        pickToken(T_S);
        wantToken(T_ValueIndicator);
        pickToken(T_S);
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
    
    // AttributeValue ::=  OpenQuot ( ( EscapeQuot | AttributeValueContent) )*  CloseQuot 
    //			 | OpenApos ( ( EscapeApos | AttributeValueContent) )*  CloseApos 
    private void parseAttributeValue( AttributeConstructor c ) throws CompilationException 
    {
        int delimiter = curToken == T_OpenQuot ? T_CloseQuot : T_CloseApos;
        if(!pickToken(T_OpenQuot) && !pickToken(T_OpenApos))
            syntax("bad attribute delimiter");
        for( ;; )
            if(curToken == T_Char) {
                String frag = makeString();
                locate2( c.addTextItem(XMLUtil.replaceWhiteSpace(frag)));
            }
            else if(curToken == T_CharRef) {
                // no normalization:
                locate2( c.addTextItem(makeString()));
            }
            else if( curToken == T_Lbrace || curToken == T_LbraceExprEnclosure )
                c.addItem( parseEnclosedExpr() );
            else
                break;
        wantToken(delimiter);
        if(c.contents.length == 0)
            c.addTextItem("");
    }
    
    Expression parseEnclosedExpr( ) throws CompilationException 
    {
        if(!pickToken(T_LbraceExprEnclosure))
            wantToken(T_Lbrace);
        Expression e = parseExpr();
        wantToken(T_Rbrace);
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
        if(pickToken(T_Weight)) {
            if(s.weight != null)
                currentModule.error("XPTY0004", s,
                                    "conflicting weight " + CPF_TEXT );
            if(currentModule.sObs()) {
                wantToken(T_LbraceExprEnclosure);    // NEW junk
                s.weight = parseRangeExpr();
                wantToken(T_Rbrace);
            }
            else {
                boolean braces = pickToken(T_LbraceExprEnclosure);
                s.weight = parseRangeExpr();
                if (braces)
                    wantToken(T_Rbrace);
            }
        }
        return s;
    }

    private FTSelectionOp parseFTOr() throws CompilationException
    {
        FTSelectionOp e = parseFTAnd();
        if (curToken != T_FTOr)
            return e;
        FTOrOp or = new FTOrOp(e);
        locate(or);
        while (pickToken(T_FTOr))
            or.addChild(parseFTAnd());
        return or;
    }
    
    private FTSelectionOp parseFTAnd() throws CompilationException
    {
        FTSelectionOp e = parseFTMildNot();
        if (curToken != T_FTAnd)
            return e;
        FTAndOp and = new FTAndOp(e);
        locate(and);
        while (pickToken(T_FTAnd))
            and.addChild(parseFTMildNot());
        return and;
    }
    
    private FTSelectionOp parseFTMildNot() throws CompilationException
    {
        FTSelectionOp e = parseFTNot();
        if( pickToken(T_FTNotIn)) {
            int here = tokenStart;
            e = new FTMildNotOp(e, parseFTNot());
            locate(e, here);
        }
        return e;
    }
    
    private FTSelectionOp parseFTNot() throws CompilationException
    {
        int here = tokenStart;
        if(pickToken(T_FTNot)) {
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
        if(pickToken(T_Lpar)) {
            FTSelectionOp e = parseFTSelection();
            wantToken(T_Rpar);
            return e;
        }
        else if (curToken == T_Pragma) {
            do {
                QName extName = checkPragma();
                // for the moment, ignore all pragmas...
                nextToken();
            }
            while (curToken == T_Pragma);
            wantToken(T_LbraceExprEnclosure);
            FTSelectionOp e = parseFTSelection();
            wantToken(T_Rbrace);
            return e;
        }

        FTWordsOp words = null;
        if(pickToken(T_LbraceExprEnclosure)) {
            words = new FTWordsOp(parseExpr());
            wantToken(T_Rbrace);
        }
        else if(curToken == T_StringLiteral) {
            words = new FTWordsOp(makeStringLiteral());
        }
        else syntax("expecting literal string or '{' or '('");
        
        if(pickToken(T_Any)) {
            words.anyAll = FullText.ANY;
        }
        else if(pickToken(T_AnyWord)) {
            words.anyAll = FullText.ANY_WORD;
        }
        else if(pickToken(T_All)) {
            words.anyAll = FullText.ALL;
        }
        else if(pickToken(T_AllWords)) {
            words.anyAll = FullText.ALL_WORDS;
        }
        else if(pickToken(T_Phrase)) {
            words.anyAll = FullText.PHRASE;            
        }
        
        // occurs X times ... 
        if(pickToken(T_Occurs)) {
            words.occs = parseFTRange();
            wantToken(T_Times);
        }
        return words;
    }
    

    private FTPosFilters parseFTPosFilters(FTSelectionOp sel)
        throws CompilationException
    {
        FTPosFilters filters = new FTPosFilters(false);
        HashSet set = new HashSet();
        for(;;) {
            if(pickToken(T_Ordered)) {
                checkOption(set, "ordered");
                filters.ordered = true;
            }
            else if(pickToken(T_Window)) {
                checkOption(set, "window");
                filters.windowExpr = parseAdditiveExpr();
                filters.windowUnit = parseDistanceUnit();
                // current limitation:
                if(filters.windowUnit != FTPosFilters.WORDS)
                    currentModule.error("FTST0003", sel, 
                                        "unsupported full-text window unit");
            }
            else if(pickToken(T_Distance)) {
                checkOption(set, "distance");
                filters.distance = parseFTRange();
                filters.distanceUnit = parseDistanceUnit();
                // current limitation:
                if(filters.distanceUnit != FTPosFilters.WORDS)
                    currentModule.error("FTST0003", sel, 
                                        "unsupported full-text distance unit");
            }
            else if(pickToken(T_SameSentence)) {
                checkOption(set, "scope");
                filters.scope = FTPosFilters.SAME_SENTENCE;
            }
            else if(pickToken(T_SameParagraph)) {
                checkOption(set, "scope");
                filters.scope = FTPosFilters.SAME_PARAGRAPH;
            }
            else if(pickToken(T_DifferentParagraph)) {
                checkOption(set, "scope");
                filters.scope = FTPosFilters.DIFF_SENTENCE;
            }
            else if(pickToken(T_DifferentSentence)) {
                checkOption(set, "scope");
                filters.scope = FTPosFilters.DIFF_PARAGRAPH;
            }
            else if(pickToken(T_AtStart)) {
                checkOption(set, "anchor");
                filters.content = FTPosFilters.AT_START;
            }
            else if(pickToken(T_AtEnd)) {
                checkOption(set, "anchor");
                filters.content = FTPosFilters.AT_END;
            }
            else if(pickToken(T_EntireContent)) {
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
        if(pickToken(T_Words))
            return FTPosFilters.WORDS;
        if(pickToken(T_Sentences))
            return FTPosFilters.SENTENCES;
        if(pickToken(T_Paragraphs))
            return FTPosFilters.PARAGRAPHS;
        syntax("expect 'words', 'sentences' or 'paragraphs'");
        return -1;
    }

    private void parseFTMatchOptions(MatchOptions opt)
        throws CompilationException
    {
        HashSet set = new HashSet();
        for(;;) {
            int here = tokenStart;
            if(pickToken(T_Language)) {
                checkOption(set, "language");
                if(curToken != T_StringLiteral)
                    syntax("expecting language literal");
                opt.language = makeString();
                if(!((LanguageType) XQType.LANGUAGE).checkValue(opt.language))
                    currentModule.error("FTST0013", here, 
                                        "invalid language " + opt.language);
            }
            else if(pickToken(T_CaseSensitive)) {
                checkOption(set, "case");
                opt.caseSensitivity = MatchOptions.SENSITIVE;
            }
            else if(pickToken(T_CaseInsensitive)) {
                checkOption(set, "case");
                opt.caseSensitivity = MatchOptions.INSENSITIVE;
            }
            else if(pickToken(T_Lowercase)) {
                checkOption(set, "case");
                opt.caseSensitivity = MatchOptions.LOWERCASE;
            }
            else if(pickToken(T_Uppercase)) {
                checkOption(set, "case");
                opt.caseSensitivity = MatchOptions.UPPERCASE;
            }
            else if(pickToken(T_DiacriticsSensitive)) {
                checkOption(set, "diacritics");
                opt.diacritics = MatchOptions.SENSITIVE;
            }
            else if(pickToken(T_DiacriticsInsensitive)) {
                checkOption(set, "diacritics");
                opt.diacritics = MatchOptions.INSENSITIVE;
            }
            else if(pickToken(T_WithWildcards)) {
                checkOption(set, "wildcards");
                opt.wildcards = MatchOptions.WITH;
            }
            else if(pickToken(T_WithoutWildcards)) {
                checkOption(set, "wildcards");
                opt.wildcards = MatchOptions.WITHOUT;
            }
            else if(pickToken(T_WithStemming)) {
                checkOption(set, "stemming");
                opt.stemming = MatchOptions.WITH;
            }
            else if(pickToken(T_WithoutStemming)) {
                checkOption(set, "stemming");
                opt.stemming = MatchOptions.WITHOUT;
            }
            else if(pickToken(T_WithThesaurus)) {
                checkOption(set, "thesaurus");
                if(pickToken(T_Lpar)) {
                    parseThesaurus(opt);
                    while(pickToken(T_Comma)) {
                        parseThesaurus(opt);
                    }
                    wantToken(T_Rpar);
                }
                else parseThesaurus(opt);
            }
            else if(pickToken(T_WithoutThesaurus)) {
                checkOption(set, "thesaurus"); // ignored: default
            }
            else if(pickToken(T_WithoutStopWords)) {
                checkOption(set, "stopwords"); // ignored: default
            }
            else if(pickToken(T_WithStopWords)) {
                checkOption(set, "stopwords");
                currentModule.error("FTST0008", here,
                                    "stop-words are not supported");
                parseStopWords(opt.language);
                // opt.addStopWords(parseStopWords(opt.language));
                for(;;) {
                    if(pickToken(T_Union))
                        parseStopWords(opt.language);
                        //opt.addStopWords(parseStopWords(opt.language));
                    else if(pickToken(T_Except))
                        parseStopWords(opt.language);
                        // opt.exceptStopWords(parseStopWords(opt.language));
                    else break;
                }
            }
            else if(pickToken(T_WithDefaultStopWords)) {
                checkOption(set, "stopwords");
                currentModule.error("FTST0008", here,
                                    "stop-words are not supported");
                for(;;) {
                    if(pickToken(T_Union))
                        parseStopWords(opt.language);
                        //opt.addStopWords(parseStopWords(opt.language));
                    else if(pickToken(T_Except))
                        parseStopWords(opt.language);
                        //opt.exceptStopWords(parseStopWords(opt.language));
                    else break;
                }
            }
            else if (pickToken(T_FTExtension)) {
                QName option = resolveVarName();
                
            }
            else break;
        }
    }

    private void parseStopWords(String languageCode)
        throws CompilationException
    {
        if(curToken == T_AtStringLiteral) {
            // URI: immediately resolved through static context
            String uri = makeString();
//            currentModule.error("FTST0008", prevTokenLoc,
//                                "unreachable stop-word list '" + uri + "'");
        }
        else {
            //DefaultStopWordList sw2 = new DefaultStopWordList();
            wantToken(T_Lpar);
            if(curToken != T_StringLiteral)
                syntax("expect string for stop word");
            String word = makeString();
            //sw2.addWord(word.toCharArray());
            while(pickToken(T_Comma)) {
                if(curToken != T_StringLiteral)
                    syntax("expect string for stop word");
                word = makeString();
                //sw2.addWord(word.toCharArray());
            }
            wantToken(T_Rpar);
        }
    }

    private void parseThesaurus(MatchOptions options)
        throws CompilationException
    {
        if(pickToken(T_Default) || pickKeyword("default")) {
            options.addThesaurus(getThesaurus("default", null, null, null));
            return;
        }
        if(curToken != T_AtStringLiteral)
            syntax("expecting thesaurus URI");

        String uri = makeString(), relationship = null;
        if(curToken == T_Relationship) {
            relationship = makeString();
        }
        RangeExpr levels = parseFTRange();
        if(levels != null)
            wantToken(T_Levels);
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
            currentModule.error("FTST0008", prevTokenLoc,
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
        
        if(pickToken(T_Exactly))
            range.lower = range.upper = parseAdditiveExpr();
        else if(pickToken(T_AtLeast))
            range.lower = parseAdditiveExpr();
        else if(pickToken(T_AtMost))
            range.upper = parseAdditiveExpr();
        else if(pickToken(T_From)) {
            range.lower = parseAdditiveExpr();
            wantToken(T_To);
            range.upper = parseAdditiveExpr();
        }
        else return null;
        return range;
    }

    private void checkOption(HashSet set, String name)
    {
         if(set.contains(name))
             currentModule.error("FTST0019", prevTokenLoc,
                                 "duplicate " + name + " option");
         set.add(name);
    }
    
    // ------ XUpdate -----------------------------------------------------
    
    private InsertExpr parseInsertNodes() throws CompilationException
    {
        InsertExpr insert = new InsertExpr();
        locate(insert);
        pickToken(T_InsertNodes);
        
        insert.what = parseExprSingle();
        switch(curToken) {
        case T_Into:
            insert.mode = InsertExpr.INTO;
            break;
        case T_AsFirstInto:
            insert.mode = InsertExpr.FIRST;
            break;
        case T_AsLastInto:
            insert.mode = InsertExpr.LAST;
            break;
        case T_Before:
            insert.mode = InsertExpr.BEFORE;
            break;
        case T_After:
            insert.mode = InsertExpr.AFTER;
            break;
        default:
            syntax("invalid insertion mode: " +
            		"expecting 'into' or 'as' or 'before' or 'after'");
        }
        nextToken();
        insert.where = parseExprSingle();
        
        return insert;
    }

    private Expression parseDeleteNodes() throws CompilationException
    {
        DeleteExpr delete = new DeleteExpr();
        locate(delete);
        pickToken(T_DeleteNodes);
        
        delete.where = parseExprSingle();
        return delete;
    }

    private ReplaceExpr parseReplaceNode() throws CompilationException
    {
        ReplaceExpr replace = new ReplaceExpr();
        locate(replace);
        replace.mode =
            (curToken == T_ReplaceValueOfNode)? ReplaceExpr.VALUE : 0;
        nextToken();
        
        replace.where = parseExprSingle();
        wantToken(T_With);
        replace.what = parseExprSingle();
        return replace;
    }

    private RenameExpr parseRenameNode() throws CompilationException
    {
        RenameExpr rename = new RenameExpr();
        locate(rename);
        pickToken(T_RenameNode);
        rename.where = parseExprSingle();
        wantToken(T_As);
        rename.what = parseExprSingle();
        return rename;
    }

    private Expression parseTransform() throws CompilationException
    {
        TransformExpr trans = new TransformExpr();
        locate(trans);
        pickToken(T_Copy);
        
        LetClause vcl = parseLetClause(false);
        trans.copies = new LetClause[] { vcl };
        while(pickToken(T_Comma)) {
            checkToken(T_VarName);
            vcl = parseLetClause(false);
            trans.copies = (LetClause[]) Expression.addExpr(trans.copies, vcl);
        }
        wantToken(T_Modify);
        trans.modify = parseExprSingle();
        wantToken(T_Return);
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
        if (curToken != T_QName || prefixValue.length() > 0)
            wantToken(T_NCName);
        String opt = makeString();
        for (int k = options.length; --k >= 0;)
            if (options[k].equals(opt))
                return k;
        String msg = "expecting " + options[0];
        for (int k = 1; k < options.length; k++)
            msg += " or " + options[k];
        syntax(msg);
        return 0;// dummy
    }

    private Expression makeStringLiteral()
        throws CompilationException
    {
        return locate2(new StringLiteral(makeString()));
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
        return IQName.get(expandPrefix(prefixValue, defaultNS), makeString());
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

    private String makeString()
        throws CompilationException
    {
        String v = saveBuffer.toString();
        nextToken();
        return v;
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
        nextToken();
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
        nextToken();
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
        nextToken();
        return v;
    }

    boolean pickKeyword(String kw)
        throws CompilationException
    {
        if (curToken != T_QName || prefixValue.length() > 0
            || !kw.equals(saveBuffer.toString()))
            return false;
        nextToken();
        return true;
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
