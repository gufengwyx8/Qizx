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
package com.qizx.xdm;

import com.qizx.api.EvaluationException;
import com.qizx.api.Item;
import com.qizx.api.Node;
import com.qizx.xquery.XQValue;

import java.util.HashSet;

/**
 * Node Filter removing duplicates.
 */
public class DistinctNodeSequence extends NodeSequenceBase
{
    XQValue source;
    BasicNode node;
    HashSet seenSet = new HashSet();

    public DistinctNodeSequence(XQValue source)
    {
        this.source = source;
    }

    public Node getNode()
    {
        return node;
    }

    public BasicNode basicNode()
    {
        return node;
    }

    public boolean next() throws EvaluationException
    {
        for (; source.next();) {
            node = source.basicNode();
            // equals and HashCode on Node are based on identity.
            if (!seenSet.contains(node)) {
                seenSet.add(node);
                return true;
            }
        }
        return false;
    }

    public double getFulltextScore(Item item) throws EvaluationException
    {
        if(item == null)
            item = getNode();
        return source.getFulltextScore(item);
    }

    public XQValue bornAgain()
    {
        return new DistinctNodeSequence(source.bornAgain());
    }
}
