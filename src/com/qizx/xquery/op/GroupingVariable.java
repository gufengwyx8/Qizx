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

import com.qizx.api.QName;
import com.qizx.xquery.ComparisonContext;
import com.qizx.xquery.ModuleContext;

import java.text.Collator;

/**
 * Variable used as a key in group-by. Can be associated with a collation.
 */
public class GroupingVariable extends VarReference.Local
    implements ComparisonContext
{
    public String collation;
    public Collator collator;

    public GroupingVariable(QName name, String collation)
    {
        super(name);
        this.collation = collation;
    }

    public Expression staticCheck(ModuleContext context, int flags)
    {
        decl = context.lookforLocalVariable(name);
        if (decl == null)
            // cannot be a global: (not specified, but woulnt make sense)
            context.error("XQST0094", this,
                          "grouping variable " + context.prefixedName(name)
                          + " not declared (must be local)");
        else {
            type = decl.type;
            decl.use();
        }
        if(collation != null) {
            collator = context.getCollator(collation);
            if (collator == null && collation != null)
                context.error("XQST0076", this, "unknown collation " + collation);
        }
        return this;
    }

    public Collator getCollator()
    {
        return collator;
    }

    public boolean emptyGreatest() {
        return false;
    }

    public int getImplicitTimezone() {
        return 0;
    }    
}
