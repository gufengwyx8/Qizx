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

import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.fulltext.Scorer;
import com.qizx.api.fulltext.Stemmer;
import com.qizx.api.fulltext.TextTokenizer;
import com.qizx.api.fulltext.Thesaurus;

/**
 * Fulltext service provider plugged by default.
 * <p>Provides a generic TextTokenizer and a standard Scorer. Might be extended
 * in future versions to provide stemmer and thesaurus.
 */
public class DefaultFullTextFactory
    implements FullTextFactory
{
    public TextTokenizer getTokenizer(String languageCode)
    {
        return new DefaultTextTokenizer();
    }

    public Stemmer getStemmer(String languageCode)
    {
        return null; // not available by default
    }

    public Thesaurus getThesaurus(String uri, String languageCode,
                                  String relationship,
                                  int levelMin, int levelMax)
    {
        return null; // not available by default
    }
    

    public Scorer createScorer()
    {
        return new DefaultScorer();
    }
}
