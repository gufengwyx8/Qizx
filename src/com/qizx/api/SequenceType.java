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
package com.qizx.api;

/**
 * Type of an Item sequence.
 */
public interface SequenceType
{
    /**
     * Occurrence indicator corresponding with one optional item.
     */
    int OCC_ZERO_OR_ONE = 0;

    /**
     * Occurrence indicator corresponding with one required item.
     */
    int OCC_EXACTLY_ONE = 1;

    /**
     * Occurrence indicator corresponding with '*', any number of items.
     */
    int OCC_ZERO_OR_MORE = 2;

    /**
     * Occurrence indicator corresponding with '+', at least one item.
     */
    int OCC_ONE_OR_MORE = 3;

    /**
     * Returns the base Item Type of the sequence type.
     * @return the base Item Type of the Sequence type.
     */
    public ItemType getItemType();

    /**
     * Returns the occurrence indicator associated with this type
     * (OCC_ZERO_OR_ONE, OCC_EXACTLY_ONE, OCC_ONE_OR_MORE, OCC_ZERO_OR_MORE).
     * @return an int representing the occurrence indicator value
     */
    public int getOccurrence();
}
