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
 * Copyright (c) 2002-2003 Pixware. Author: Hussein Shafie This file is part of
 * the XMLmind XML Editor project. For conditions of distribution and use, see
 * the accompanying legal.txt file.
 */
package com.qizx.apps.studio.gui;

import com.qizx.util.basic.PlatformUtil;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

/**
 * A collection of utility functions (static methods) used to help building
 * dialog boxes.
 */
public final class DialogUtil
{
    private static final char MNEMO_CHAR = '&';

    /**
     * Absence of mnemonic for button or menu item.
     */
    public static final char NO_MNEMONIC = '\0';

    private static boolean addMnemonics = !PlatformUtil.IS_MAC_OS;

    /**
     * Specifies whether underscore characters used in labels to specify the
     * position of a mnemonic are discarded or not.
     * @param add if <code>false</code>, underscores are discarded; if
     *        <code>true</code> underscores are used to configure mnemonics
     *        for buttons
     * @see #setMnemonic
     */
    public static void setAddingMnemonics(boolean add)
    {
        addMnemonics = add;
    }

    /**
     * Returns <code>false</code> if underscore characters used in labels to
     * specify the position of a mnemonic are discarded. Returns
     * <code>true</code> if underscores are used to configure mnemonics for
     * buttons.
     * @see #setMnemonic
     */
    public static boolean isAddingMnemonics()
    {
        return addMnemonics;
    }

    /**
     * Returns specified label after removing underscore character used to
     * specify the position of a mnemonic (if any).
     */
    public static String buttonLabel(String msg)
    {
        int underscore = msg.indexOf(MNEMO_CHAR);
        if (underscore >= 0)
            msg = (new StringBuffer(msg)).deleteCharAt(underscore).toString();

        return msg;
    }

    /**
     * Returns character in specified label which must be used as a mnemonic.
     * Returns {@link #NO_MNEMONIC} if there is no such character.
     */
    public static char buttonMnemonic(String msg)
    {
        if (!addMnemonics)
            return NO_MNEMONIC;

        int underscore = msg.indexOf(MNEMO_CHAR);
        if (underscore >= 0)
            return msg.charAt(underscore - 1);
        else
            return NO_MNEMONIC;
    }

    /**
     * Returns index within specified label of character which must be used as
     * a mnemonic. Returns -1 if there is no such character.
     */
    public static int buttonMnemonicIndex(String msg)
    {
        if (!addMnemonics)
            return -1;

        int underscore = msg.indexOf(MNEMO_CHAR);
        if (underscore >= 0)
            return underscore;
        else
            return -1;
    }

    /**
     * Configures specified button to have a mnemonic. Specified button label
     * must have an & after character used as a mnemonic (examples:
     * <tt>F&ile</tt>, <tt>Save A&s</tt>) in order to do so.
     */
    public static void setMnemonic(AbstractButton button, String msg)
    {
        char mnemo = buttonMnemonic(msg);
        if (mnemo != NO_MNEMONIC) {
            button.setMnemonic(mnemo);
            button.setDisplayedMnemonicIndex(buttonMnemonicIndex(msg));
        }
    }

    /**
     * Configures specified label to have a mnemonic. Specified text of label
     * must have an underscore before character used as a mnemonic (examples:
     * <tt>_Name:</tt>, <tt>Name _List:</tt>) in order to do so.
     */
    public static void setDisplayedMnemonic(JLabel label, String msg)
    {
        char mnemo = buttonMnemonic(msg);
        if (mnemo != NO_MNEMONIC) {
            label.setDisplayedMnemonic(mnemo);
            label.setDisplayedMnemonicIndex(buttonMnemonicIndex(msg));
        }
    }

    // -----------------------------------------------------------------------

    private static final Insets buttonInsets = new Insets(1, 1, 1, 1);

    public static void setIconic(AbstractButton button)
    {
        if (!PlatformUtil.IS_MAC_OS)
            button.setMargin(buttonInsets);
    }

    // -----------------------------------------------------------------------

    /**
     * Beeps to report the error, moves focus to specified field and selects
     * all its content.
     */
    public static void badField(JTextField field)
    {
        field.getToolkit().beep();
        field.selectAll();
        field.requestFocus();
    }

    // -----------------------------------------------------------------------

    /**
     * Shows specified menu positioned relatively to specified button.
     */
    public static void showMenu(JPopupMenu menu, JButton button)
    {
        Dimension dim = menu.getPreferredSize();
        menu.show(button, button.getWidth() - dim.width, button.getHeight());
    }
}
