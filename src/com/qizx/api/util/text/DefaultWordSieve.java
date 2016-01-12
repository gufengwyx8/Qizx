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

package com.qizx.api.util.text;

import com.qizx.api.Indexing.WordSieve;
import com.qizx.api.util.fulltext.DefaultTextTokenizer;


/**
 * A basic text tokenizer suitable for most European languages.
 * <p>All methods can be redefined.
 * <ul>
 * <li>Words start with a letter, a digit, or an underscore. They can contain
 * additionally an hyphen '-', and a dot if it is not in last position.
 * <li>Unless specified by constructor argument, characters are converted to
 * lowercase.
 * <li>Unless specified by constructor argument, ISO-8859-1 characters with
 * accents (diacritics) are converted to accent-less equivalent (e.g 'é' is
 * converted to 'e'). More complex mappings such as German ß to "ss" are not
 * supported.
 * <li>No stemming is performed.
 * </ul>
 * @deprecated use {@link DefaultTextTokenizer}
 */
public class DefaultWordSieve extends DefaultTextTokenizer
    implements WordSieve, java.io.Serializable
{    
    /**
     * Default constructor.
     */
    public DefaultWordSieve()
    {
    }

    public WordSieve copy()
    {
        DefaultWordSieve sieve = new DefaultWordSieve();
        return sieve;
    }

}
