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
 * A collection of utility functions (static methods) related to Exceptions.
 */
public final class ThrowableUtil {
    private ThrowableUtil() {}

    /**
     * Returns the message of specified exception. Returns the class name of
     * specified exception if this exception has no message.
     */
    public static String reason(Throwable e) {
        String reason = e.getMessage();
        if (reason == null)
            reason = e.getClass().getName();

        return reason;
    }

    /**
     * Equivalent to {@link #detailedReason(Throwable, int)
     * detailedReason(e, 1)}.
     */
    public static String detailedReason(Throwable e) {
        return detailedReason(e, 1);
    }

    /**
     * Same as {@link #reason} except that the stack trace of specified
     * exception is integrated to returned message.
     *
     * @param e the Throwable to be formatted
     * @param maxCauses in case of a chained Throwable,
     * maximum number of causes ({@link Throwable#getCause})
     * to be included in the formatted string
     * @return formatted string representation of specified Throwable
     *
     */
    public static String detailedReason(Throwable e, int maxCauses) {
        StringBuilder buffer = new StringBuilder();
        detailedReason(e, buffer);

        for (int i = 0; i < maxCauses; ++i) {
            Throwable cause = e.getCause();
            if (cause == null)
                break;

            buffer.append("\nCAUSE:\n");
            detailedReason(cause, buffer);

            e = cause;
        }

        return buffer.toString();
    }

    /**
     * Copies both the message and the stack trace of specified exception to
     * specified buffer.
     */
    public static void detailedReason(Throwable e, StringBuilder buffer) {
        buffer.append(e.getClass().getName());
        if (e.getMessage() != null) {
            buffer.append(": ");
            buffer.append(e.getMessage());
        }

        buffer.append("\n+---------------------------------------\n");

        StackTraceElement[] traces = e.getStackTrace();
        int traceCount = Math.min(20, traces.length);
        for (int i = 0; i < traceCount; ++i) {
            buffer.append("| ");
            buffer.append(traces[i]);
            buffer.append('\n');
        }

        buffer.append("+---------------------------------------");
    }
}
