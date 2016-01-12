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
package com.qizx.api.util.text;

import com.qizx.api.Indexing;

/**
 * Base class of Sieve implementations.
 */
public abstract class SieveBase implements Indexing.Sieve
{
    protected String[] parameters;

    /**
     * Utility method to specify an individual parameter.
     * @param name of parameter
     * @param value of parameter
     */
    public void addParameter(String name, String value)
    {
        if(parameters == null) {
            parameters = new String[] { name, value };
            return;
        }
        for(int pos = parameters.length - 2; pos >= 0; pos -= 2) {
            if(name.equals(parameters[pos])) {
                parameters[pos + 1] = value;
                return;
            }
        }
        // add:
        String[] old = parameters;
        parameters = new String[old.length + 2];
        System.arraycopy(old, 0, parameters, 0, old.length);
        parameters[old.length] = name;
        parameters[old.length + 1] = value;
    }

    // @see com.qizx.api.Indexing.Sieve#getParameters()
    public String[] getParameters()
    {
        return parameters;
    }

    // @see java.lang.Object#toString()
    public String toString()
    {
        return toString(getClass(), parameters);
    }

    /**
     * Utility that can be used by different implementations.
     */
    protected static String toString(Class classe, String[] parameters)
    {
        StringBuffer s = new StringBuffer(classe.getName());
        s.append("(");
        if (parameters != null)
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0)
                    s.append(' ');
                s.append(parameters[i]);
            }
        s.append(")");
        return s.toString();
    }
}
