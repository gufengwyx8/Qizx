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
package com.qizx.api.fulltext;

import com.qizx.api.XMLPullStream;

/**
 * An extension of XMLPullStream that separates fragments of text matching the
 * key terms of a full-text query. This is typically used for "highlighting"
 * full-text terms, by wrapping full-text terms with some decoration, for
 * example a B or SPAN element in HTML.
 * <p>Note that this interface does not define how the full-text query is
 * specified.
 * <p>A Text node that contains one or several terms of a full-text query
 * generates several events of type TEXT and FT_TERM, instead of simply one.
 * <p><b>Example:</b> the full-text query is: <code>. ftcontains 'romeo juliet'</code>,
 * and the BasicFullTextPullStream is used on this document:
 * <pre>&lt;PLAY&gt;
 *  &lt;TITLE&gt;The Tragedy of Romeo and Juliet&lt;/TITLE&gt;
 *  &lt;FM&gt; ...
 *  </pre>
 * <p>Events generated are successively:
 * <table cellspacing="0" border="0" cellpadding="3">
 * <tr>
 * <td>DOCUMENT_START<td>&nbsp;<td>&nbsp;
 * <tr><td>ELEMENT_START<td>name=PLAY<td>
 * <tr><td>ELEMENT_START <td>name=TITLE<td>
 * <tr><td>TEXT<td>text='The Tragedy of '<td>wordCount=3
 * <tr><td>FT_TERM <td>text='Romeo' <td>wordCount=1 , termPosition=0
 * <tr><td>TEXT<td>text=' and '<td>wordCount=1
 * <tr><td>FT_TERM<td>text='Juliet'<td>wordCount=1 , termPosition=1
 * <tr><td>ELEMENT_END<td>name=TITLE<td><tr>
 * <td>...<td>...<td>
 * </table>
 * <p>Additional information is returned on each fragment TEXT or FT_TERM:
 * <ul>
 * <li>
 * <p>Query-position of the term, an integer value that represents the rank of
 * the term in the query [noted termPosition above].
 * <li>Number of words in the fragment (always 1 for FT_TERM, since phrases
 * are not recognized as a single piece).
 * </ul>
 */
public interface FullTextPullStream
    extends XMLPullStream
{
    /**
     * Event code returned when a query word or phrase is recognized.
     */
    int FT_TERM = 8;

    /**
     * On a FT_TERM event, returns the rank of the term (word, wildcard) in the
     * full-text query. Depends on the actual implementation of this interface.
     * <p>
     * Example: in the following query, terms 'romeo' has position 0, and term
     * 'juliet' has position 1.
     * <pre>
     * . ftcontains &quot;romeo juliet&quot; all words
     * </pre>
     * <p>
     * Note that excluded terms (following <code>ftnot</code> or <code>not
     * in</code>) are ignored.
     */
    int getTermPosition();
    
    /**
     * On a TEXT or FT_TERM event, returns the number of words in the text
     * chunk. For a FT_TERM, the value returned is 1, because phrases are not
     * recognized as a whole.
     */
    int getWordCount();
    
    /**
     * Returns the number of terms in the query.
     */
    int getQueryTermCount();
    
    /**
     * Returns the terms of the query as a String array.
     */
    String[] getQueryTerms();
}
