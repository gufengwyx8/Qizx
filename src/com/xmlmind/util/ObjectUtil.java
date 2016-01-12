/*
 *    Qizx/open 4.1
 *
 * This code is part of the Qizx application components
 * Copyright (C) 2004-2010 Axyana Software -- All rights reserved.
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
/*
 * Copyright (c) 2002-2008 Pixware. 
 *
 * Author: Hussein Shafie
 *
 * This file is part of several XMLmind projects.
 * For conditions of distribution and use, see the accompanying legal.txt file.
 */
package com.xmlmind.util;

/**
 * A collection of utility functions (static methods) operating on Objects.
 */
public final class ObjectUtil {
    private ObjectUtil() {}

    /**
     * Tests if specified objects are equal using <code>equals()</code>.
     * Unlike with <code>equals()</code>, both objects may be
     * <code>null</code> and two <code>null</code> objects are always
     * considered to be equal.
     * <p>This function is missing in java.util.Arrays which, on the other
     * hand, has all the other functions: <code>Arrays.equals(boolean[],
     * boolean[])</code>, <code>Arrays.equals(byte[], byte[])</code>, ...,
     * <code>Arrays.equals(Object[], Object[])</code>.
     */
    public static final boolean equals(Object o1, Object o2) {
        return ((o1 == null && o2 == null) ||
                (o1 != null && o1.equals(o2)));
    }
}
