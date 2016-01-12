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

import com.qizx.api.Node;

/**
 * Abstract conversion service for node values.
 * <p>Provides a way to convert string values of nodes (element and attributes)
 * to double or Date values. Implemented by the Indexing specification of
 * XML Libraries.
 */
public interface DataConversion
{
    /**
     * Attempts to convert the string to a double value.
     * @param value an alleged numeric value in text form.
     * @return the converted value, or NaN if the conversion is not possible.
     *         Should raise no exception
     */
    double convertNumber(Node node);

    /**
     * Attempts to convert the date or date-time contained in the text fragment
     * to a double value (in milliseconds from 1970-01-01 00:00:00 UTC).
     * 
     * @param value an alleged date-time value in text form.
     * @return the converted value, or NaN if the conversion is not possible.
     */
    double convertDate(Node node);
}
