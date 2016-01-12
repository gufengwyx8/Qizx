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
package com.qizx.xquery.op;

import com.qizx.api.EvaluationException;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.queries.FullText;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.queries.FullText.Selection;
import com.qizx.queries.FullText.SelectionList;
import com.qizx.queries.FullText.SimpleWord;
import com.qizx.queries.FullText.Wildcard;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQTypeException;
import com.qizx.xquery.XQValue;
import com.qizx.xquery.dt.StringArraySequence;

import java.util.regex.PatternSyntaxException;


/**
 * Primary FT Selection: evaluates to a list of word sequences 
 * with any/all option and occurrence option.
 */
public class FTWordsOp extends FTSelectionOp
{
    public int anyAll = FullText.ANY;
    public RangeExpr occs;
    
    public FTWordsOp(Expression words)
    {
        super(words);
    }
    
    public Expression staticCheck(ModuleContext context, int flags)
    {
        super.staticCheck(context, flags);        
        occs = (RangeExpr) context.staticCheck(occs, flags);
        return this;
    }

    public boolean isConstant()
    {
        return children[0].isConstant() && (occs == null || occs.isConstant());
    }
    
    public boolean hasExcludesOrOccurs()
    {
        if(occs != null)
            return true;
        return super.hasExcludesOrOccurs();
    }

    public Selection expand(Focus focus, EvalContext context,
                            MatchOptions inherited, float inheritedWeight)
        throws EvaluationException
    {
        SelectionList result = null;
           
        // compute words: the expression may return a sequence of strings
        XQValue res = children[0].eval(focus, context);
        String[] seq = null;
        try {
            seq = StringArraySequence.expand(res, true);
        }
        catch (XQTypeException e) {
            context.error(this, e);
        }
        
        // setup word tokenizer from options: case, diacritics, stemming etc
        MatchOptions mops = expandMatchOptions(inherited);
        float weight = expandWeight(focus, context, inheritedWeight);
        ModuleContext sctx = context.getStaticContext();
        FullTextFactory ftp = sctx.getFulltextFactory();
        
        TextTokenizer tokenizer = ftp.getTokenizer(mops.language);
        if(tokenizer == null)
            throw new EvaluationException(ModuleContext.xqueryErrorCode("FTST0009"),
                                          "no tokenizer available for language "
                                          + mops.language);
        tokenizer.setAcceptingWildcards(mops.wildcards == MatchOptions.WITH);
        tokenizer.setParsingSpecialChars(false);
        
        switch(anyAll) {
        case FullText.ALL_WORDS:   // single word list
            result = new FullText.All();
            for (int i = 0; i < seq.length; i++)
                expandTerms(seq[i], result, tokenizer, mops, weight);
            break;
        case FullText.ANY_WORD:   // single word list
            result = new FullText.Any();
            result.setWeight(weight);
            for (int i = 0; i < seq.length; i++)
                expandTerms(seq[i], result, tokenizer, mops, weight);
            break;
        case FullText.ALL: // list of phrases
            if(seq.length == 1) {
                result = new FullText.Phrase();
                expandTerms(seq[0], result, tokenizer, mops, weight);
            }
            else {
                result = new FullText.All();
                for (int i = 0; i < seq.length; i++)
                    result.addChild(expandTerms(seq[i], new FullText.Phrase(),
                                                tokenizer, mops, weight));
            }
            break;
        case FullText.ANY:
            if(seq.length == 1) {
                result = new FullText.Phrase();
                expandTerms(seq[0], result, tokenizer, mops, weight);
            }
            else {
                result = new FullText.Any();
                for (int i = 0; i < seq.length; i++)
                    result.addChild(expandTerms(seq[i], new FullText.Phrase(),
                                                tokenizer, mops, weight));
            }
            break;
        case FullText.PHRASE:
            result = new FullText.Phrase();
            for (int i = 0; i < seq.length; i++)
                expandTerms(seq[i], result, tokenizer, mops, weight);
            
            break;
            
        }
        
        // optional 'occurs' range:
        if(occs != null) {
            result.occRange = occs.evaluate(focus, context);
        }
        
        result.setMatchOptions(mops);
        result.setPosFilters(expandPositionFilters(focus, context));
        
        if(result.getChildCount() == 0)
            return FullText.NULL_QUERY;

        // simplify when only 1 child (frequent):
        if(result.getChildCount() == 1)
        {
            Selection child = result.getChild(0);
            if(child.getMatchOptions() == mops
               && child.occRange == null
               && child.getPosFilters() == null
               && // except if 'content' option (supported only by PolyIterators)
                  (result.getPosFilters() == null || 
                   result.getPosFilters().content == MatchOptions.UNSPECIFIED))
            {   // transfer filters to child:
                child.setPosFilters(result.getPosFilters());
                child.occRange = result.occRange;
                child.setWeight(result.getWeight());
                return child;
            }
        }
//        // ensure window filter on a phrase: (but after reduction)
//        if(result instanceof FullText.Phrase) {
//            FullText.Phrase phr = (FullText.Phrase) result;
//            if(phr.getPosFilters() == null) {
//                phr.setPosFilters(new PosFilters());
//                phr.getPosFilters().window = phr.getChildCount();
//            }
//        }
        return result;
    }

    private Selection expandTerms(String tokens, SelectionList result,
                                  TextTokenizer wordSieve,
                                  MatchOptions mops, float weight)
        throws EvaluationException
    {
        wordSieve.start(tokens);
        result.setWeight(weight);
        result.setMatchOptions(mops);
        
        try {
            int token = wordSieve.nextToken();
            for (; token != TextTokenizer.END; ) {
                if(token == TextTokenizer.WORD)
                {
                    char[] tokenChars = wordSieve.getTokenChars();
                    Selection tq = wordSieve.gotWildcard() ?
                                       (Selection) new Wildcard(tokenChars)
                                     : new SimpleWord(tokenChars);
                    tq.setWeight(result.getWeight());
                    tq.setMatchOptions(mops);
                    result.addChild(tq);
                }
                token = wordSieve.nextToken();
            }
        }
        catch (PatternSyntaxException e) {
            EvaluationException exc =
                new EvaluationException("wildcard pattern error: " + e.getMessage());
            exc.setErrorCode("FTDY0020");
            throw exc;
        }
        return result;
    }
}
