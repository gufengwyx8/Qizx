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
package com.qizx.queries.iterators;


/**
 * A Phrase is simply an ordered All with a window constraint.
 * 
 */
public class PhraseIterator extends AllIterator
{
    /**
     * 
     * @param variable true if items of phrase are not simple words, but Or's
     * of words or phrases due to Thesaurus expansion. In that case the test
     * based on window does not work.
     */
    public PhraseIterator(PostingIterator[] iterators, boolean variable)
    {
        super(iterators);
        setOrdered(true);
        if(variable)
            setDistanceConstraint(0, 0);
        else
            // less expensive than distance = 0: 
            setWindowConstraint(iterators.length);
    }

    public PostingIterator bornAgain()
    {
        return copyConstraints(new PhraseIterator(copy(iterators),
                                                  constraints.window < 0));
    }
}
