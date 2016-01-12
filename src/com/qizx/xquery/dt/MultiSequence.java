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
package com.qizx.xquery.dt;

import com.qizx.api.EvaluationException;
import com.qizx.api.Item;
import com.qizx.xquery.XQValue;

import java.util.ArrayList;

/**
 * Sequence of Sequences.
 */
public class MultiSequence extends GenericValue
{
    XQValue curComp = XQValue.empty;
    int index = -1;
    ArrayList list = new ArrayList();

    public void add(XQValue seq)
    {
        list.add(seq);
    }

    public boolean next()
        throws EvaluationException
    {
        for (;;) {
            if (curComp.next()) {
                item = curComp.getItem();
                return true;
            }
            if (++index >= list.size())
                return false;
            curComp = (XQValue) list.get(index);
        }
    }

    public double getFulltextScore(Item item) throws EvaluationException
    {
        if(item == null)
            item = getNode();
        for (int i = 0, asize = list.size(); i < asize; i++) {
            double score = ((XQValue) list.get(i)).getFulltextScore(item);
            if(score >= 0)
                return score;
        }
        return super.getFulltextScore(item); // fallback
    }

//    public void getFulltextSelections(java.util.Collection collect)
//    {
//        for (int i = 0; i < list.size(); i++)
//            ((XQValue) list.get(i)).getFulltextSelections(collect);
//    }

    public XQValue bornAgain()
    {
        MultiSequence museq = new MultiSequence();
        for (int s = 0; s < list.size(); s++)
            museq.list.add(((XQValue) list.get(s)).bornAgain());
        return museq;
    }
}
