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
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.FullTextPullStream;
import com.qizx.queries.FullText.Selection;
import com.qizx.util.basic.Check;
import com.qizx.util.basic.IntSet;
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xdm.XMLPullStreamBase;

import java.util.ArrayList;

/**
 * Extracts a snippet of text from an XML node, attempting to show the key
 * terms of a full-text query.
 * <p>
 * As an implementation of the {@link FullTextPullStream} interface, this
 * object separates full-text term occurrences from surrounding plain text,
 * allowing to "highlight" by enclosing them within an XML element.
 * <p>
 * To get the results, the method moveToNextEvent has to be called. 
 * Returned events are of type FT_TERM and TEXT, finally followed by 
 * {@link FullTextPullStream#END}.
 * <p><b>Example:</b> the full-text query is: <code>. ftcontains 'romeo juliet'</code>,
 * and the FullTextSnippetExtractor is used on this document:
 * <pre>&lt;PLAY&gt;
 *  &lt;TITLE&gt;The Tragedy of Romeo and Juliet&lt;/TITLE&gt;
 *  &lt;FM&gt; ...
 *  </pre>
 * <p>Events generated could be successively:
 * <table cellspacing="0" border="0" cellpadding="3">
 * <tr>
 * <tr><td>TEXT<td>text='The Tragedy of '<td>wordCount=3
 * <tr><td>FT_TERM <td>text='Romeo' <td>wordCount=1 , termPosition=0
 * <tr><td>TEXT<td>text=' and '<td>wordCount=1
 * <tr><td>FT_TERM<td>text='Juliet'<td>wordCount=1 , termPosition=1
 * <tr><td>END<td><td><tr>
 * </table>
 * (Note that this is only an example: actual snippet could be different).
 * <p>A convenience method {@link #makeSnippet makeSnippet} provides simpler 
 * means of building a snippet. Example (assuming 'session' is a XQuerySession
 * or Library):
 * <pre>
 * Expression ftquery =
 *    session.compileExpression(". ftcontains 'romeo juliet' all words");
 * FullTextSnippetExtractor ftx = new FullTextSnippetExtractor(ftquery);
 * Node snippet = ftx.makeSnippet(node, session.getQName("div"),
 *                                session.getQName("span"),
 *                                session.getQName("class"), "ft_");</pre>
 * Results would be a node of the form:<pre>
 *  &lt;div>The Tragedy of &lt;span class="ft_0"><b>Romeo</b>&lt;/span> and &lt;span class="ft_1"><b>Juliet</b>&lt;/span>&lt;/div>
 * </pre>
 */
public class FullTextSnippetExtractor extends XMLPullStreamBase
    implements FullTextPullStream
{
    /** A pseudo-event that represents skipped words */
    public static final int GAP = 9; 
    static final String ELLIPSIS = " ... ";

    private int maxSnippetSize = 20;  // max length of snippet in words
    private int maxWorkSize = 500;  // max number of words scanned

    private FullTextHighlighter stream;
    private int termCount;
    private ArrayList chunks;
    
    private int bestScore;
    private int bestChunk;
    private ArrayList snippetChunks;
    private int currentChunk;

    
    /**
     * Creates a FullTextSnippetExtractor from a compiled expression.
     * The expression must be either of:
     * <ul>
     * <li>a full-text predicate <b>ftcontains</b>. The left-hand side of
     * ftcontains is ignored. Full-text options are taken into account. Example:
     * <pre>Expression e = session.compileExpression(
     *     ". ftcontains 'Romeo Juliet' all words case sensitive");
     *FullTextSnippetExtractor se = new FullTextSnippetExtractor(e);</pre>
     * <li>a call to the function <b>ft:contains</b>. The optional
     * <code>context</code> argument is ignored. Full-text options are taken
     * into account. Example:
     * <pre>Expression e = 
     *   session.compileExpression("ft:contains('+Romeo +Juliet', &lt;options case='sensitive'/>)");
     *FullTextSnippetExtractor se = new FullTextSnippetExtractor(e);</pre>
     * <li>An expression evaluating to a string, which is assumed to represent
     * a query using the simple full-text syntax. Full-text options cannot be
     * specified this way. Example:
     * <pre>Expression e = session.compileExpression("'+Romeo +Juliet'");
     *FullTextSnippetExtractor se = new FullTextSnippetExtractor(e);</pre>
     * <li>a call to the function <b>ft:contains</b>. The optional
     * <code>context</code> argument is ignored. Full-text options are taken
     * into account.
     * <li>An expression evaluating to a string, which is assumed to represent
     * a query using the simple full-text syntax. Full-text options cannot be
     * specified this way.
     * </ul>
     * @param query a compiled full-text predicate, or a string using the simple
     * full-text syntax.
     * @throws EvaluationException 
     * @see FullTextHighlighter
     */
    public FullTextSnippetExtractor(com.qizx.api.Expression query)
        throws EvaluationException
    {
        stream = new FullTextHighlighter(query);
    }

    /**
     * Creates a FullTextSnippetExtractor from a query string using the simple
     * full-text syntax.
     * @param simpleSyntaxQuery a query using the simple full-text syntax.
     * @param fulltextFactory a FullTextFactory used with the language parameter
     * to get a tokenizer (both at compile-time and run-time).
     * @param language language used for the options of the full-text query
     * @throws DataModelException if the query is incorrect
     */
    public FullTextSnippetExtractor(String simpleSyntaxQuery,
                                    FullTextFactory fulltextFactory, String language)
        throws DataModelException
    {
        stream = new FullTextHighlighter(simpleSyntaxQuery,
                                         fulltextFactory, language);
    }
    
    /**
     * For internal use.
     */
    public FullTextSnippetExtractor(Selection query, FullTextFactory ftFactory)
        throws EvaluationException
    {
        stream = new FullTextHighlighter(query, ftFactory);
    }

    /**
     * Gets the current maximum number of words in a snippet.
     */
    public int getMaxSnippetSize()
    {
        return maxSnippetSize;
    }

    /**
     * Sets the maximum number of words in a snippet. Default is 25.
     * @param maxSnippetSize a positive integer
     */
    public void setMaxSnippetSize(int maxSnippetSize)
    {
        this.maxSnippetSize = maxSnippetSize;
    }

    /**
     * Gets the maximum number of words examined to create a snippet.
     * Default is 500.
     */
    public int getMaxWorkSize()
    {
        return maxWorkSize;
    }

    /**
     * Sets the maximum number of words examined to create a snippet. 
     * <p>When the scanned document or node belongs to an indexed XML Library,
     * indexes are used to skip directly to occurrences of full-text terms, thus
     * reducing the work load. 
     * @param maxWorkSize a positive integer. 
     */
    public void setMaxWorkSize(int maxWorkSize)
    {
        this.maxWorkSize = maxWorkSize;
    }

    /**
     * Directly builds a snippet from a source Node.
     * @param node source document
     * @param wrapperElement name of an element used to wrap the whole snippet
     * @param hiliterElement name of an element used to wrap each highlighted term
     * @param styleAttribute optional name (can be null) of an attribute of the
     * hiliter element bearing a style indication
     * @param stylePrefix a prefix for the style indicator, if styleAttribute is
     * used.
     * @return a Node representing the full-text snippet
     * @throws DataModelException if there is a problem accessing the input node
     */
    public Node makeSnippet(Node node, QName wrapperElement, QName hiliterElement, 
                            QName styleAttribute, String stylePrefix)
        throws DataModelException
    {
        Check.nonNull(node, "node");
        Check.nonNull(wrapperElement, "wrapperElement");
        Check.nonNull(hiliterElement, "hiliterElement");

        start(node);
        CorePushBuilder out = new CorePushBuilder(null);
        out.putElementStart(wrapperElement);
        for( ; moveToNextEvent() != END; ) {
            if(getCurrentEvent() == TEXT || getCurrentEvent() == GAP)
                out.putText(getText());
            else {
                out.putElementStart(hiliterElement);
                if(styleAttribute != null)
                    out.putAttribute(styleAttribute,
                                     stylePrefix + getTermPosition(), null);
                out.putText(getText());
                out.putElementEnd(hiliterElement);
            }
        }
        out.putElementEnd(wrapperElement);
        return out.harvest();
    }

    /**
     * Searches snippet components in a source XML document or node. The method 
     * moveToNextEvent can then be used to extract those components.
     * @param node a node (document or element) from which to extract a snippet.
     * @throws DataModelException raised by problems accessing to the source node 
     */
    public void start(Node node) throws DataModelException
    {
        Check.nonNull(node, "node");

        stream.start(node);
        termCount = stream.getQueryTermCount();
        chunks = new ArrayList();        
        int totalWordCount = 0, wordCount;
        bestScore = 0;
        bestChunk = -1;
        
        // scan and accumulate text chunks, note best chunk
        for(; stream.moveToNextEvent() != END; ) {
            switch (stream.getCurrentEvent()) {
            case TEXT:
                wordCount = stream.getWordCount();
                totalWordCount += wordCount;
                //if (wordCount > 0)
                    storeChunk(TEXT, stream.getText(), wordCount, -1);
                break;
            case FT_TERM:
                wordCount = stream.getWordCount();
                totalWordCount += wordCount;
                storeChunk(FT_TERM, stream.getText(),
                           wordCount, stream.getTermPosition());
                break;
            case ELEMENT_START: // add gap
                Chunk last = chunks.size() == 0? 
                                 null : getChunk(chunks.size() - 1, chunks);
                if(last != null)
                    if(last.type != GAP) { // dummy type for a gap
                        last = new Chunk(GAP, ELLIPSIS, 1, -1);
                        chunks.add(last);
                    }
                    else ++ last.wordCount;
                break;
            }
            if(totalWordCount > maxWorkSize)
                break;
        }
//        for (int i = 0; i < chunks.size(); i++) {
//            Chunk c = getChunk(i, chunks);
//            System.err.println(i+"\tchunk "+c.type+" '"+c.text+"' "+c.wordCount+" "+c.queryPos);
//        }

        // heuristic: take best window, if it does not cover all terms then find
        // another window that adds terms and such that total size < windowMax
        
        if (bestChunk < 0) { // no term hit? take first chunk
            if(chunks.size() == 0)
                return;     // nothing we can do
            bestChunk = 0;
        }
        
        // adjust around best:
        Chunk chunk = getChunk(bestChunk, chunks); // always a FT
        snippetChunks = new ArrayList();
        snippetChunks.add(chunk);
        int wcnt = chunk.wordCount;

        // extend to right and left:
        int extent = (maxSnippetSize - wcnt + 1) / 2;
        if(bestChunk == 0)
            extent = (maxSnippetSize - wcnt);   // cannot extend lefts
        for(int n = bestChunk + 1 ; n < chunks.size() && extent > 0; n++) {
            Chunk next = getChunk(n, chunks);
            snippetChunks.add(next);
            extent -= next.wordCount;
        }
        
        extent = (maxSnippetSize - wcnt + 1) / 2;
        for(int p = bestChunk - 1 ; p >= 0 && extent > 0; p--) {
            Chunk prev = getChunk(p, chunks);
            snippetChunks.add(0, prev);
            extent -= prev.wordCount;
        }

        // trim GAP at ends
        if(getChunk(0, snippetChunks).type == GAP)
            snippetChunks.remove(0);
        int slast = snippetChunks.size() - 1;
        if(getChunk(slast, snippetChunks).type == GAP)
            snippetChunks.remove(slast);

        // if too long, simplify: (GAP,TEXT,GAP) -> GAP
        wcnt = countWords(snippetChunks);
        if(wcnt > maxSnippetSize) {
            for (int c = 0; c < snippetChunks.size(); c++) {
                Chunk ch = getChunk(c, snippetChunks);
                if(ch.type == GAP && c + 2 <  snippetChunks.size()
                   && getChunk(c + 2, snippetChunks).type == GAP
                   && getChunk(c + 1, snippetChunks).type == TEXT) {
                    snippetChunks.remove(c + 2);
                    snippetChunks.remove(c + 1); // careful with order!
                }
            }
        }
        
        // trim TEXT at ends if needed:
        wcnt = countWords(snippetChunks);
        int excess = wcnt - maxSnippetSize;
        if(excess > 0) {
            Chunk first = getChunk(0, snippetChunks);
            Chunk last = getChunk(snippetChunks.size() - 1, snippetChunks);
            // trim only last if possible:
            int leftFirst = Math.max(2, first.wordCount - excess / 3);
            if(bestChunk == 0) // dont cut best chunk unless very big
                leftFirst = Math.min(first.wordCount, maxSnippetSize);
            int leftLast = last.wordCount - (excess - first.wordCount + leftFirst);
            if(leftLast <= 0)
                snippetChunks.remove(snippetChunks.size() - 1);
            else
                last.text = stream.extractFirstWords(last.text, leftLast)+ ELLIPSIS;
            if(leftFirst < first.wordCount)
                first.text = ELLIPSIS + stream.extractLastWords(first.text, leftFirst);
        }        
        
        
        currentChunk = -1;
    }

    private Chunk getChunk(int index, ArrayList chunks)
    {
        return (Chunk) chunks.get(index);
    }
    
    private int countWords(ArrayList chunks)
    {
        int count = 0;
        for(int p = chunks.size(); --p >= 0; ) {
            Chunk c = getChunk(p, chunks);
            count += c.wordCount;
        }
        return count;
    }
    
    /* Add a chunk and look backwards for FT terms, computing a score.
     */
    private void storeChunk(int type, String text, int wordCount, int termPos)
    {
        Chunk chunk = new Chunk(type, text, wordCount, termPos);
        chunks.add(chunk);
        if(type != FT_TERM)
            return;
        IntSet termSet = new IntSet(termPos);
        int wcnt = wordCount, wspan = wordCount, termCnt = 1, gapCnt = 0;
        for(int p = chunks.size() - 1; --p >= 0; ) {
            Chunk prev = getChunk(p, chunks);
            wcnt += prev.wordCount;
            if(wcnt > maxSnippetSize)
                break;
            if(prev.type == FT_TERM) {
                termSet.add(prev.queryPos);
                wspan = wcnt;   // validate
                ++ termCnt;
            }
            else if(prev.type == START) 
                gapCnt ++;
        }
        chunk.score = 10 * termSet.size() + 4 * termCnt
                      + Math.min(5, maxSnippetSize - wspan) - 2 * gapCnt;
        if(chunk.score > bestScore) {
            bestScore = chunk.score;
            bestChunk = chunks.size() - 1;
        }
        
    }

    private static class Chunk
    {
        int type;
        int queryPos;
        int wordCount;
        String text;
        int score;

        public Chunk(int type, String text, int wordCount, int queryPos)
        {
            this.type = type;
            this.text = text;
            this.wordCount = wordCount;
            this.queryPos = queryPos;
        }
        
        public String toString() {
            return (type == GAP)? "..." : "|" + text + "|";
        }
    }

    public int moveToNextEvent()
        throws DataModelException
    {
        if(snippetChunks == null || ++ currentChunk >= snippetChunks.size())
            return setEvent(END);
        return setEvent(getChunk(currentChunk, snippetChunks).type);
    }

    public int getTermPosition()
    {
        return getChunk(currentChunk, snippetChunks).queryPos;
    }

    public int getWordCount()
    {
        return getChunk(currentChunk, snippetChunks).wordCount;
    }

    public String getText()
    {
        return getChunk(currentChunk, snippetChunks).text;
    }

    public QName getName()
    {
        return null;    // no ELEMENT event returned
    }

    public int getQueryTermCount()
    {
        return termCount;
    }

    public String[] getQueryTerms()
    {
        return stream.getQueryTerms();
    }

    public Node getCurrentNode()
    {
        return null;
    }
}
