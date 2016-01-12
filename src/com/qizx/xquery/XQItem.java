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
package com.qizx.xquery;

import com.qizx.api.EvaluationException;
import com.qizx.api.Item;
import com.qizx.api.util.time.Date;
import com.qizx.api.util.time.DateTime;
import com.qizx.api.util.time.DateTimeBase;
import com.qizx.api.util.time.DateTimeException;
import com.qizx.api.util.time.Duration;
import com.qizx.xdm.BasicNode;

/**
 * XQuery abstract Item representation.
 * <p>
 * The type of the item (XQItemType) is returned by method getType().
 * <p>
 * Specialized methods getXXX() allow to retrieve or convert the actual item
 * value according to the type. An exception (XQTypeException) is raised if the
 * conversion is not possible.
 */
public interface XQItem
    extends Item
{
    /**
     * Gets the item as a Duration value.
     */
    Duration getDuration()
        throws EvaluationException;

    /**
     * Gets the item as a Duration value.
     */
    DateTimeBase getMoment()
        throws EvaluationException;

    /**
     * Specialized cast from string, used for node value conversion, possibly
     * using Indexing rules.
     */
    Date getDate()
        throws EvaluationException, DateTimeException;

    /**
     * Specialized cast from string, used for node value conversion, possibly
     * using Indexing rules.
     */
    DateTime getDateTime()
        throws EvaluationException, DateTimeException;

    BasicNode basicNode()
        throws EvaluationException;

    /**
     * Compares two items.
     * 
     * @param flags bitwise combination of flags for special comparisons:
     *        COMPAR_ORDER, COMPAR_VALUE
     * @return EQ GT LT COMPAR_ERROR or COMPAR_FAIL
     */
    int compareTo(XQItem that, ComparisonContext context, int flags)
        throws EvaluationException;

    int COMPAR_ORDER = 1;   // for lt gt le ge
    int COMPAR_VALUE = 2;

    /**
     * Deep equality of two items.
     * 
     * @throws EvaluationException
     */
    boolean deepEquals(XQItem item, ComparisonContext context)
        throws EvaluationException;

    abstract XQItemType getItemType()
        throws EvaluationException;
}
