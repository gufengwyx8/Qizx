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
 * Interface implemented by objects which are required to report information,
 * warnings and non-fatal errors to the user.
 * <p>The implementation is generally expected to be thread-safe.
 */
public interface Console {
    /**
     * Message types.
     */
    enum MessageType {
        /**
         * A non-fatal error message.
         */
        ERROR,

        /**
         * A warning message.
         */
        WARNING,

        /**
         * An information message.
         */
        INFO,

        /**
         * A low-level information message.
         */
        VERBOSE,

        /**
         * A debugging or trace message.
         */
        DEBUG
    }

    /**
     * Show specified message to user.
     * 
     * @param message a possibly multi-line message
     * @param messageType type of message: {@link MessageType#INFO}, {@link
     * MessageType#WARNING}, {@link MessageType#ERROR} or {@link
     * MessageType#DEBUG}.
     */
    void showMessage(String message, MessageType messageType);
}
