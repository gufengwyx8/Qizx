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

import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 * Helper class used to create localized, formatted, messages.
 */
public final class Localizer {
    private ResourceBundle resourceBundle;

    /**
     * Constructs a Localizer which uses <code>java.text.MessageFormat</code>s
     * found in <tt><i>package_name</i>.Messages<i>_locale</i>.properties</tt>
     * <code>java.util.ResourceBundle</code>s to create its localized,
     * formatted, messages.
     * <p>Example: <tt>com.xmlmind.foo.Messages_fr.properties</tt>, which is
     * used to create localized, formatted, messages for all classes in the
     * <tt>com.xmlmind.foo</tt> package, contains 2 message formats having
     * <tt>cannotOpen</tt> and <tt>cannotSave</tt> as their IDs.
     * <pre>cannotOpen=Ne peut ouvrir le fichier "{0}":\n{1}
     *
     *cannotSave=Ne peut enregistrer le fichier "{0}":\n{1}</pre>
     * 
     * @param cls Class used to specify <i>package_name</i>.
     */
    public Localizer(Class cls) {
        String packageName = cls.getName();
        int pos = packageName.lastIndexOf('.');
        if (pos >= 0)
            packageName = packageName.substring(0, pos);

        resourceBundle = ResourceBundle.getBundle(packageName + ".Messages");
    }

    /**
     * Returns localized message having specified ID. Returns specified ID if
     * the message is not found.
     */
    public String msg(String id) {
        String message = null;
        try {
            message = resourceBundle.getString(id);
        } catch (Exception e) {
            return id;
        }
        return message;
    }

    /**
     * Returns localized message having specified ID and formatted using
     * specified arguments. Returns specified ID if the message is not found.
     */
    public String msg(String id, Object... args) {
        String message = null;
        try {
            message = resourceBundle.getString(id);
        } catch (Exception e) {
            return id;
        }
        return MessageFormat.format(message, args);
    }
}
