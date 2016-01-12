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
package com.qizx.xquery.ext;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.api.fulltext.Thesaurus;
import com.qizx.queries.FullText;
import com.qizx.queries.SimpleFullText;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.queries.FullText.Selection;
import com.qizx.xdm.BasicNode;
import com.qizx.xdm.IQName;
import com.qizx.xquery.*;
import com.qizx.xquery.dt.SingleNode;
import com.qizx.xquery.fn.Function;
import com.qizx.xquery.fn.Prototype;
import com.qizx.xquery.op.Expression;
import com.qizx.xquery.op.FTContainsOp;


/**
 *  Implementation of simplified full-text function ft:matches.
 */
public class FT_Contains extends ExtensionFunction
{
    static QName qfname = IQName.get(FULLTEXT_EXT_NS, "contains");
    static Prototype[] protos = { 
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("query", XQType.STRING)
            .arg("context", XQType.NODE.star)
            .arg("options", XQType.ELEMENT),
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("query", XQType.STRING)
            .arg("options", XQType.ELEMENT),
        new Prototype(qfname, XQType.BOOLEAN.opt, Exec.class)
            .arg("query", XQType.STRING)
    };
    
    public Prototype[] getProtos() { return protos; }
    
    private static final QName FT_OPT_ERR = ModuleContext.xqueryErrorCode("FTDY0030");
    private static final QName OP_CASE = IQName.get("case");
    private static final QName OP_DIAC = IQName.get("diacritics");
    private static final QName OP_LANGUAGE = IQName.get("language");
    private static final QName OP_STEMMING = IQName.get("stemming");
    private static final QName OP_THESAURUS = IQName.get("thesaurus");
    private static final QName OP_REL = IQName.get("relationship");

    
    public static Selection compileQuery(String query, EvalContext context)
        throws EvaluationException
    {
        DynamicContext dynCtx = context.dynamicContext();
        FullTextFactory ftf = dynCtx.getFulltextFactory();
        MatchOptions defaultOptions = dynCtx.getDefaultFTOptions();
        TextTokenizer tokenizer =
            ftf.getTokenizer(defaultOptions.language);
        try {
            SimpleFullText parser = new SimpleFullText(tokenizer);
            return parser.parseQuery(query, defaultOptions);
        }
        catch (DataModelException e) {
            throw new EvaluationException(e.getErrorCode(), e.getMessage());
        }
    }

    public static void parseOptions(Focus focus, EvalContext context,
                                    Expression optionArg, MatchOptions dstOptions)
        throws EvaluationException, DataModelException
    {
        BasicNode options = optionArg.evalAsNode(focus, context);
        FullTextFactory ff = context.dynamicContext().getFulltextFactory();
        
        Node[] attrs = options.getAttributes();
        if(attrs != null)
          for (int i = 0; i < attrs.length; i++) {
            QName name = attrs[i].getNodeName();
            String value = attrs[i].getStringValue();
            if(name == OP_CASE) {
                if("insensitive".startsWith(value))
                    dstOptions.caseSensitivity = MatchOptions.INSENSITIVE;
                else if("sensitive".startsWith(value))
                    dstOptions.caseSensitivity = MatchOptions.SENSITIVE;
                else context.error(FT_OPT_ERR, optionArg,
                                   "invalid value of option 'case'");
            }
            else if(name == OP_DIAC) {
                if("insensitive".startsWith(value))
                    dstOptions.diacritics = MatchOptions.INSENSITIVE;
                else if("sensitive".startsWith(value))
                    dstOptions.diacritics = MatchOptions.SENSITIVE;
                else context.error(FT_OPT_ERR, optionArg,
                                   "invalid value of option 'diacritics'");
            }
            else if(name == OP_LANGUAGE) {
                dstOptions.language = value;
            }
            else if(name == OP_STEMMING) {
                if("true".equalsIgnoreCase(value))
                    dstOptions.stemming = MatchOptions.WITH;
                else if("false".equalsIgnoreCase(value))
                    dstOptions.stemming = MatchOptions.WITHOUT;
                else context.error(FT_OPT_ERR, optionArg,
                        "invalid value of option 'stemming'");
            }
            else if(name == OP_THESAURUS) {
                Thesaurus thesaurus =
                    ff.getThesaurus(value, dstOptions.language, null,
                                    0, Integer.MAX_VALUE);
                if(thesaurus == null)
                    context.error(FT_OPT_ERR, optionArg,
                                  "unknown thesaurus '"+ value +"'");
                dstOptions.addThesaurus(thesaurus);
            }
            else context.error(FT_OPT_ERR, optionArg,
                               "unknown option " + name);
        }
    }

    /**
     * Utility for FT_functions: compiles an argument into a FT selection
     * @param queryArg may be operator ftcontains, function ft:contains (with
     *  possible options), or a string expression using the simple syntax.
     *  In the first two cases, the 'context' operand is ignored.
     */
    public static 
      FullText.Selection compileQueryArgument(Expression queryArg,
                                              Focus focus, EvalContext context)
        throws EvaluationException
    {
        ModuleContext sctx = context.getStaticContext();
        FullText.Selection query = null;
        if(queryArg instanceof FTContainsOp) {
            // . ftcontains query :
            FTContainsOp ftc = (FTContainsOp) queryArg;
            query = ftc.expandSelection(focus, context,
                                        sctx.getDefaultFTOptions(), 1);              
        }
        else if(queryArg instanceof FT_Contains.Exec) {
            // ft:contains(query, options?)
            FT_Contains.Exec ftc = (FT_Contains.Exec) queryArg;

            String querySrc = ftc.args[0].evalAsString(focus, context);
            query = FT_Contains.compileQuery(querySrc, context);
            
            // options: just modify the matchOptions of the Selection
            if(ftc.args.length > 1) {
                Expression opArg = ftc.args[ftc.args.length - 1];
                try {
                    FT_Contains.parseOptions(focus, context, opArg,
                                             query.getMatchOptions());
                }
                catch (DataModelException e) {
                    context.error(e.getErrorCode(), queryArg, e.getMessage());
                }
            }
        }
        else if(XQType.STRING.isSuperType(queryArg.getType().itemType())) {
            // simple query:
            String qs = queryArg.evalAsString(focus, context);
            query = FT_Contains.compileQuery(qs, context);
        }
        else 
            context.error("XQFT0100", queryArg,
                          "expecting either operator ftcontains or " +
                          "function ft:contains, or " +
                          "string representing a simplified full-text query");
        if(query != null)   // should not happen
            query = query.applyThesauri();
        else System.err.println("OOPS null expanded query in ftcontains");
        return query;
    }

    public static class Exec extends Function.BoolCall
    {
        boolean isConstant;
        // initialized on first call if constant
        FullText.Selection cachedQuery;
        
        
        // detect constant query/options and precompile
        public void compilationHook()
        {
            Expression query = args[0];
            Expression options = args[args.length - 1];
            if(query.isConstant() &&
               (args.length < 2 || options.isConstant())) {
                isConstant = true;
            }
        }
        
        public synchronized Selection prepareQuery(Focus focus, EvalContext context)
            throws EvaluationException
        {
            if(cachedQuery != null)
                return cachedQuery;
            try {
                String query = args[0].evalAsString(focus, context);
                Selection q = compileQuery(query, context);
                
                // options: just modify the matchOptions of the Selection
                if(args.length > 1) {
                    Expression opArg = args[args.length - 1];
                    parseOptions(focus, context, opArg, q.getMatchOptions());
                }
                
                if(isConstant)
                    cachedQuery = q;
                return q;
            }
            catch (DataModelException e) {
                context.error(e.getErrorCode(), this, e.getMessage());
                return null; // dummy
            }
        }

        public boolean evalAsBoolean(Focus focus, EvalContext context)
            throws EvaluationException
        {
            Selection query = prepareQuery(focus, context);
            FullTextFactory ff = context.dynamicContext().getFulltextFactory();
            try {
                if(args.length == 3) {
                    XQValue seq = args[1].eval(focus, context);
                    // stop on first matching node:
                    return query.matches(seq, null, ff);
                }
                else {
                    XQItem curItem = checkFocus(focus, context);
                    return query.matches(new SingleNode(curItem.basicNode()),
                                         null, ff);
                }
            }
            catch (DataModelException e) {
                dmError(context, e);
                return false; // dummy
            }
        }
    }
}
