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

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.xdm.BasicNode;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

/**
 * Performs atomization on an input sequence. A Node is potentially expanded
 * into a sequence of atoms, which is inlined. Schema are in fact not
 * implemented, so we take the string-value of the node.
 */
public class Atomizer extends GenericValue
{
    XQValue input;

    public Atomizer(XQValue value)
    {
        this.input = value;
    }

    public boolean next()
        throws EvaluationException
    {
        if (!input.next())
            return false;
        // Too simple when using Schema: can be a sequence to inline
        item = toSingleAtom(input.getItem()); // TODO SCHEMA
        return true;
    }

    // Simplified version, sufficient in absence of Schema
    public static XQItem toSingleAtom(XQItem item)
        throws EvaluationException
    {
        if (!item.isNode())
            return item;

        Node node = item.getNode();

        try {
            int nat = node.getNodeNature();
            XQItemType ityp = XQType.UNTYPED_ATOMIC;
            if (nat == Node.COMMENT || nat == Node.PROCESSING_INSTRUCTION)
                ityp = XQType.STRING;
            return new SingleString(node.getStringValue(), ityp);
        }
        catch (DataModelException e) {
            throw BasicNode.wrapDMException(e);
        }
    }

    public XQValue bornAgain()
    {
        return new Atomizer(input.bornAgain());
    }

}
