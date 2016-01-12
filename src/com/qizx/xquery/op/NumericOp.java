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

import com.qizx.api.EvaluationException;
import com.qizx.xquery.ModuleContext;
import com.qizx.xquery.XQItem;
import com.qizx.xquery.XQItemType;
import com.qizx.xquery.XQType;

/**
 * class NumericOp:
 */
public abstract class NumericOp extends Expression
{
    public Expression[] operands;

    public NumericOp(Expression expr1, Expression expr2)
    {
        operands = new Expression[] { expr1, expr2 };
    }

    public Expression child(int rank)
    {
        return rank < 2 ? operands[rank] : null;
    }

    // public Expression staticCheck( ModuleContext context ) {
    // operands[0] = context.staticCheck(operands[0], 0);
    // operands[1] = context.staticCheck(operands[1], 0);
    // return context.resolve(getProtos(), operands, this );
    // }

    // Static analysis of operands, then combine type codes.
    protected int combinedArgTypes(ModuleContext context)
    {
        operands[0] = context.staticCheck(operands[0], 0);
        operands[1] = context.staticCheck(operands[1], 0);
        XQItemType t1 = operands[0].getType().itemType();
        XQItemType t2 = operands[1].getType().itemType();
        int qt1 = t1.quickCode(), qt2 = t2.quickCode();
        if (qt1 == XQType.QT_UNTYPED)
            qt1 = XQType.QT_DOUBLE;
        if (qt2 == XQType.QT_UNTYPED)
            qt2 = XQType.QT_DOUBLE;
        return combinedTypes(qt1, qt2);
    }

    // Dynamic analysis of actual operands, then combine type codes.
    protected static int combinedArgTypes(XQItem op1, XQItem op2)
        throws EvaluationException
    {
        XQItemType t1 = op1.getItemType();
        XQItemType t2 = op2.getItemType();
        int qt1 = t1.quickCode(), qt2 = t2.quickCode();
        if (qt1 == XQType.QT_UNTYPED)
            qt1 = XQType.QT_DOUBLE;
        if (qt2 == XQType.QT_UNTYPED)
            qt2 = XQType.QT_DOUBLE;
        return combinedTypes(qt1, qt2);
    }
}
