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
import com.qizx.xquery.impl.XQueryPragma;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pragma
    implements XQueryPragma
{
    public QName name;

    public String contents;

    static final Pattern attrPattern =
        Pattern
            .compile("\\s*([\\w\\.-]+)\\s*=\\s*('[^']*'|\"[^\"]*\"|[^\\s]+)");

    public Pragma(QName name, String contents)
    {
        this.name = name;
        this.contents = contents;
    }

    public QName getName()
    {
        return name;
    }

    public String getContents()
    {
        return contents;
    }

    public XQueryPragma.Iterator contentIterator()
    {
        return new Iterator();
    }

    public class Iterator
        implements XQueryPragma.Iterator
    {
        Matcher matcher = attrPattern.matcher(contents);

        public boolean next()
        {
            return matcher.find();
        }

        public String getAttrName()
        {
            return matcher.group(1);
        }

        public String getAttrValue()
        {
            String g = matcher.group(2);
            return (g.charAt(0) == '\'' || g.charAt(0) == '"') ? g
                .substring(1, g.length() - 1) : g;
        }
    }
}
