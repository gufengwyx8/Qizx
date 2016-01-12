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
package com.qizx.xquery.fn;

import com.qizx.api.EvaluationException;
import com.qizx.xquery.EvalContext;
import com.qizx.xquery.Focus;
import com.qizx.xquery.XQType;
import com.qizx.xquery.XQValue;

/**
 *  Implementation of function fn:insert-before.
 */
public class InsertBefore extends Function {

    private static Prototype[] protos = { 
        Prototype.fn("insert-before", XQType.ITEM.star, Exec.class)
            .arg("target", XQType.ITEM.star)
            .arg("position", XQType.INTEGER)
            .arg("inserted", XQType.ITEM.star)
    };
    public Prototype[] getProtos() { return protos; }

    public static class Exec extends Function.Call
    {
        public XQValue eval(Focus focus, EvalContext context)
        throws EvaluationException
        {
            context.at(this);
            XQValue src = args[0].eval(focus, context);
            long pos = args[1].evalAsInteger(focus, context);
            XQValue inserted = args[2].eval(focus, context);
            context.at(this);
            return new Sequence(src, pos, inserted);
        }
    }

    public static class Sequence extends FilteredSequence
    {
        protected long insertPos;
        protected XQValue inserted;
        protected boolean inInsertion;
        
        public Sequence(XQValue source, long insertPos,
                        XQValue inserted)
        {
            super(source);
            this.insertPos = Math.max(insertPos, 1);
            this.inserted = inserted;
        }

        public XQValue bornAgain()
        {
            return new Sequence(source.bornAgain(), insertPos,
                                inserted.bornAgain());
        }

        public boolean next() throws EvaluationException
        {
            for (;;) {
                ++ position;
                if(inInsertion) {
                    if(inserted.next()) {
                        item = inserted.getItem();
                        return true;
                    }
                    inInsertion = false;
                }
                else {
                    if(position != insertPos) {
                        if(!source.next()) {
                            // Seems finished, but is
                            // insertion position beyond ?
                            if(position >= insertPos)
                                return false;
                            inInsertion = true;
                            continue;
                        }
                        item = source.getItem();
                        return true;
                    }
                    inInsertion = true;
                }
            }
        }
    }

}
