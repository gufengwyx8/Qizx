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
package com.qizx.api.util.fulltext;

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.api.XMLPullStream;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.FullTextPullStream;
import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.queries.FullText;
import com.qizx.queries.SimpleFullText;
import com.qizx.queries.FullText.Any;
import com.qizx.queries.FullText.MatchOptions;
import com.qizx.queries.FullText.Selection;
import com.qizx.queries.iterators.OrIterator;
import com.qizx.queries.iterators.PostingIterator;
import com.qizx.queries.iterators.TokenStream;
import com.qizx.util.basic.Check;
import com.qizx.util.basic.IntSet;
import com.qizx.xdm.FONIDataModel;
import com.qizx.xdm.FONIDocument;
import com.qizx.xdm.NodePullStream;
import com.qizx.xdm.XMLPullStreamBase;
import com.qizx.xdm.FONIDataModel.FONINode;
import com.qizx.xquery.DynamicContext;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExpressionImpl;
import com.qizx.xquery.ext.FT_Contains;
import com.qizx.xquery.op.Expression;

/**
 * <p>
 * An implementation of {@link FullTextPullStream} that can be used to
 * highlight terms of a full-text query (more generally to distinguish these terms
 * from the rest of the XML source).
 * <p>
 * The source can be a node of the Data Model, or any stream of XML events
 * provided by a {@link XMLPullStream}.
 * <p>
 * The full-text query can be specified in diverse manners: see the
 * constructors.
 */
public class FullTextHighlighter extends XMLPullStreamBase
    implements FullTextPullStream
{
    private XMLPullStream source;

    private FullTextFactory ftFactory;
    private String language;

    // iterator on terms:
    protected FullText.Any allTerms;
    private PostingIterator termsIter;
    private TokenStream tokenStream;
    // to access context node
    private FONIDataModel dataModel;

    // when running on xlib
       
    // current text fragments. Characters are in 'currentText'.
    private String currentText;
    private TextPiece currentPiece;
    private TextPiece textPiece;
    private TextPiece termPiece;

    private int lastStart;
    private int lastPosting;

    private int curDocId;

    /**
     * Creates a FullTextHighlighter from a compiled XQuery Expression.
     * <p>
     * The expression must be either of:
     * <ul>
     * <li>a full-text predicate <b>ftcontains</b>. The left-hand side of
     * ftcontains is ignored. Full-text options are taken into account. Example:
     * <pre>Expression e = session.compileExpression(
     *      ". ftcontains 'Romeo Juliet' all words case sensitive");
     *FullTextHighlighter hiliter = new FullTextHighlighter(e);</pre>
     * <li>a call to the function <b>ft:contains</b>. The optional
     * <code>context</code> argument is ignored. Full-text options are taken
     * into account. Example:
     * <pre>Expression e = 
     *   session.compileExpression("ft:contains('+Romeo +Juliet', &lt;options case='sensitive'/>)");
     *FullTextHighlighter hiliter = new FullTextHighlighter(e);</pre>
     * <li>An expression evaluating to a string, which is assumed to represent
     * a query using the simple full-text syntax. Full-text options cannot be
     * specified this way. Example:
     * <pre>Expression e = session.compileExpression("'+Romeo +Juliet'");
     *FullTextHighlighter hiliter = new FullTextHighlighter(e);</pre>
     * </ul>
     * @param query a compiled full-text predicate, or a string using the simple
     * full-text syntax.
     * @exception EvaluationException
     */
    public FullTextHighlighter(com.qizx.api.Expression query)
        throws EvaluationException
    {
        Check.implementation(query, ExpressionImpl.class,
                             com.qizx.api.Expression.class);
        ExpressionImpl ftq = (ExpressionImpl) query;

        Expression expr = ftq.getExpr();
        DynamicContext dynCtx = ftq.getDynCtx();
        EvalContext ctx = new EvalContext(dynCtx);
        init (FT_Contains.compileQueryArgument(expr, null, ctx),
              dynCtx.getFulltextFactory());
    }

    /**
     * Creates a FullTextHighlighter from a query string using the simple
     * full-text syntax. Example:
     * <pre>FullTextHighlighter hiliter =
     *  new FullTextHighlighter("+Romeo +Juliet", ftfactory, "en");</pre>
     * @param simpleSyntaxQuery a query using the simple full-text syntax.
     * @param fulltextFactory a FullTextFactory used with the language parameter
     * to get a tokenizer (both at compile-time and run-time).
     * @param language language used for the options of the full-text query
     * @throws DataModelException if the query is incorrect
     */
    public FullTextHighlighter(String simpleSyntaxQuery,
                               FullTextFactory fulltextFactory, String language)
        throws DataModelException
    {
        Check.nonNull(simpleSyntaxQuery, "simpleSyntaxQuery");
        Check.nonNull(fulltextFactory, "fulltextFactory");
        
        this.ftFactory = fulltextFactory;
        this.language = language;

        MatchOptions mop = new MatchOptions();
        mop.language = language;

        SimpleFullText sft = new SimpleFullText(fulltextFactory.getTokenizer(language));

        init(sft.parseQuery(simpleSyntaxQuery, mop), fulltextFactory);
    }

    /**
     * Creates a FullTextHighlighter from a list of words. 
     * 
     * @param words an array of words, used as is (no tokenization applied).
     * @param fulltextFactory a FullTextFactory used to get a tokenizer
     * @param language language used for the options of the full-text query
     */
    public FullTextHighlighter(String[] words, FullTextFactory fulltextFactory,
                               String language)
    {
        Check.nonNull(words, "String[] words");
        Check.nonNull(fulltextFactory, "fulltextFactory");
        
        this.ftFactory = fulltextFactory;
        this.language = language;
        
        allTerms = new FullText.Any();
        allTerms.setPosFilters(new FullText.PosFilters(true));
        MatchOptions mop = new MatchOptions();
        mop.language = language;
        allTerms.setMatchOptions(mop);
        for (int i = 0; i < words.length; i++) {
            allTerms.addChild(new FullText.SimpleWord(words[i].toCharArray()));
        }
    }
    
    /**
     * For internal use.
     */
    public FullTextHighlighter(Selection query, FullTextFactory fulltextFactory)
    {
        Check.nonNull(query, "query");
        Check.nonNull(fulltextFactory, "fulltextFactory");
        
        init(query, fulltextFactory);
    }

    private void init(Selection selection, FullTextFactory ftFactory)
    {
        this.ftFactory = ftFactory;
        this.language = selection.getMatchOptions().language;
        
        allTerms = new FullText.Any();
        allTerms.setPosFilters(new FullText.PosFilters(true));
        allTerms.setMatchOptions(selection.getMatchOptions());
        // scan FT selection for basic terms:
        collectBasicTerms(selection, 1, allTerms);
    }

    private void collectBasicTerms(Selection sel, float weight, Any allTerms)
    {
        if(sel instanceof FullText.SimpleWord
                || sel instanceof FullText.Wildcard
                ////|| sel instanceof FullText.Phrase
          ) {
            try {
                sel.setWeight(weight);
            }
            catch (EvaluationException e) { ; // should not happen
            }
            allTerms.addChild(sel);
        }
        else if(sel instanceof FullText.SelectionList) {
            FullText.SelectionList sell = (FullText.SelectionList) sel;
            float subw = (sel instanceof FullText.Any)? (0.5f * weight) : weight;

            for(int c = 0, cnt = sell.getChildCount(); c < cnt; c++) {
                collectBasicTerms(sell.getChild(c), subw, allTerms);
            }
        }   
        else if(sel instanceof FullText.MildNot) { // only LHS
            collectBasicTerms(((FullText.MildNot) sel).what, weight, allTerms);
        }
        // ignore ftnot!
    }

    /**
     * Starts iteration on a Node tree. If the node belongs to an XML Library,
     * the iteration can be optimized using XML Library indexes.
     * 
     * @param node
     * @throws DataModelException 
     */
    public void start(Node node) throws DataModelException
    {
        curEvent = START;
        termsIter = null;
            start(new NodePullStream(node));

    }

    /**
     * Starts iteration using another XML Stream as source. This version
     * cannot be optimized using XML Library indexes.
     * 
     * @param source a pull stream. Text nodes (events of type TEXT) can be
     * split into several sections corresponding to recognized full-text terms
     * and plain text (resp. events FT_TERM and TEXT).
     */
    public void start(XMLPullStream source)
    {
        this.source = source;
        tokenStream = new TokenStream(ftFactory, language);
        termsIter = allTerms.realize(tokenStream);
        curEvent = START;
    }

    public int getQueryTermCount()
    {
        return allTerms.getChildCount();
    }

    public String[] getQueryTerms()
    {
        int size = getQueryTermCount();
        String[] res = new String[size];
        for (int i = 0; i < res.length; i++)
            res[i] = allTerms.getChild(i).asString();
        return res;
    }

    static class TextPiece
    {
        int eventType;
        int start;
        int length;
        int wordCount;
        int termPos;    // if FT term

        public TextPiece(int type, int start, int length,
                         int termPos, int wordCount) {
            this.eventType = type;
            this.start = start;
            this.length = length;
            this.termPos = termPos;
            this.wordCount = wordCount;
            
        }
    }

    public int moveToNextEvent()
        throws DataModelException
    {
        try {
            if(textPiece != null) {
                currentPiece = textPiece;
                textPiece = null;
                return setEvent(currentPiece.eventType);
            }
            if(termPiece != null) {
                currentPiece = termPiece;
                termPiece = null;
                nextTerm();
                return setEvent(currentPiece.eventType);
            }

            lastPosting = -1;

            curEvent = source.moveToNextEvent();
            switch (curEvent) {
            case XMLPullStream.TEXT:
                // slow way: tokenize and run the iterators
                currentText = source.getText();
                startTokenStream();
                if(nextTerm()) {
                    // retry: should not loop thanks to textPiece/termPiece
                    return moveToNextEvent();
                }
                break;
            case XMLPullStream.COMMENT:
            case XMLPullStream.PROCESSING_INSTRUCTION:
                currentText = source.getText();
                break;
            default:
                currentText = null;
                currentPiece = null;
                termPiece = null;
                break;
            }
            return curEvent;
        }
        catch (EvaluationException e) {
            throw new DataModelException(e.getErrorCode(), e.getMessage());
        }
    }

    private void startTokenStream()
    {
        tokenStream.reset();
        tokenStream.parseText(currentText, null);
        termsIter.resetDoc();
        termsIter.resetToNode(0);
        lastStart = 0;
        currentPiece = textPiece = termPiece = null;
    }

    public int getTermPosition()
    {
        return (currentPiece == null)? -1 : currentPiece.termPos;
    }

    public int getWordCount()
    {
        if(currentPiece != null)
            return currentPiece.wordCount;
        return (currentText == null)? 0 : tokenStream.countTokens(currentText);
    }

    // find a term in current text; if found, split into textPiece + termPiece
    private boolean nextTerm()
        throws DataModelException, EvaluationException
    {
        curEvent = TEXT;
        if (termsIter.nextNode()) {
            int lastHit = lastPosting;
            lastPosting = (int) termsIter.getNodeId();  // NId!
            int hitStart = tokenStream.getTokenStart(lastPosting);
            int hitLength = tokenStream.getTokenLength(lastPosting);
            if (hitStart > lastStart) {
                textPiece = new TextPiece(TEXT, lastStart, hitStart - lastStart,
                                          -1, lastPosting - lastHit - 1);
            }
            int qrank = (termsIter instanceof OrIterator)?
                    ((OrIterator) termsIter).getRankOfCurrent() : 0;
            termPiece = new TextPiece(FT_TERM, hitStart, hitLength, qrank, 1);
            lastStart = hitStart + hitLength;
            return true;
        }
        // leftover after hit:
        if (lastStart > 0) {
            textPiece =
                new TextPiece(TEXT, lastStart, currentText.length() - lastStart,
                              -1, tokenStream.getTokenCount() - lastPosting - 1);
        }
        return false;
    }


    protected void lazyGetAttrs()
    {
        attrCount = 0;
        if(source != null) {
            for (int a = 0, size = source.getAttributeCount(); a < size; a++) {
                addAttribute(source.getAttributeName(a), 
                             source.getAttributeValue(a));            
            }
        }
    }

    protected void lazyGetNS()
    {
        if(source != null) {
            nsCount = 0;
            for (int a = 0, size = source.getNamespaceCount(); a < size; a++) {
                addNamespace(source.getNamespacePrefix(a), 
                             source.getNamespaceURI(a));            
            }
        }
    }

    public QName getName()
    {
        return source.getName();
    }

    public String getText()
    {
        if(currentPiece != null) {
            return currentText.substring(currentPiece.start,
                                         currentPiece.start + currentPiece.length);
        }        
        return currentText;
    }

    public int getTextLength()
    {
        return (currentPiece != null)? currentPiece.length : currentText.length();
    }

    public String getTarget()
    {
        return source.getTarget();
    }

    /**
     * Internal use.
     */
    public String extractFirstWords(String text, int count)
    {
        TextTokenizer tok = tokenStream.getTokenizer();
        tok.start(text);
        for(int t; (t = tok.nextToken()) != TextTokenizer.END; )
            if(t == TextTokenizer.WORD && --count <= 0)
                break;
        return text.substring(0, tok.getTokenOffset() + tok.getTokenLength());
    }

    /**
     * Internal use.
     */
    public String extractLastWords(String text, int count)
    {
        TextTokenizer tok = tokenStream.getTokenizer();
        count = tokenStream.countTokens(text) - count;
        tok.start(text);
        for(int t; (t = tok.nextToken()) != TextTokenizer.END; )
            if(t == TextTokenizer.WORD && --count < 0)
                break;
        return text.substring(tok.getTokenOffset());
    }

    public Node getCurrentNode()
    {
        if(source != null)
            return source.getCurrentNode();
        return null;
    }
}
