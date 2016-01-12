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

public class LanguageType extends TokenType
{
    public String getShortName()
    {
        return "language";
    }

    public boolean checkValue(String value)
    {
        if (value.length() < 2
            || !Character.isLetter(value.charAt(0))
            || !Character.isLetter(value.charAt(1)))
            return false;
        if (value.length() > 2 && value.charAt(2) != '-')
            return false;
        // we dont go further
        return true;
    }
}
