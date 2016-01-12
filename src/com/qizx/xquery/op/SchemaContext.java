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

import com.qizx.api.DataModelException;
import com.qizx.api.EvaluationException;
import com.qizx.api.Node;
import com.qizx.api.QName;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.ExprDisplay;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

import java.util.ArrayList;

/**
 * class SchemaContext:
 */
public class SchemaContext extends LeafExpression
{

    public boolean startsFromType = false;

    public boolean endsWithSlash = false;

    public ArrayList steps = new ArrayList();

    public SchemaContext(boolean startsFromType)
    {
        this.startsFromType = startsFromType;
    }

    public void addStep(QName step)
    {
        steps.add(step);
    }

    public QName getStep(int rank)
    {
        return rank < 0 || rank >= steps.size() ? null : (QName) steps.get(rank);
    }

    public boolean isSimpleName()
    {
        return !startsFromType && steps.size() == 1;
    }

    /**
     * Checks the base-type of a Node.
     *  in absence of Schema accept only xs:untypedAtomic
     */
    public boolean acceptsNode(Node node)
    {
        // poor hack, just for XQUTS:
        try {
            if(steps.size() > 1)
                return false;
            QName typeName = getStep(0);
            int nat = node.getNodeNature();
            if(nat == Node.ELEMENT || nat == Node.DOCUMENT)
                return typeName == null ||
                       typeName == XQType.untyped || typeName == XQType.anyType;
            return typeName == null || typeName == XQType.untypedAtomic
                    || typeName == XQType.anySimpleType;
        }
        catch (DataModelException e) {
            ///e.printStackTrace();
            return false;
        }
    }

    public void dump(ExprDisplay d)
    {
        //d.println("SchemaContext");
        d.property("startsFromType", "" + startsFromType);
        //d.display("steps", steps);
    }

    public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
    {
        throw new RuntimeException("SchemaContext not implemented");
    }
}
