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
package com.qizx.apps.studio.gui;

import java.applet.Applet;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.*;

/**
 * GUI Utilities.
 */
public class GUI
{
    private static final Insets BUTTON_INSETS = new Insets(2, 4, 1, 4);
    private static final Insets SMALL_INSETS = new Insets(1, 1, 1, 1);
    
    private static ResourceBundle resourceBundle = 
        ResourceBundle.getBundle(GUI.class.getName());

    public static void nativeLookAndFeel()
    {
        try {
            String lafName = UIManager.getSystemLookAndFeelClassName();
            if(lafName.indexOf("GTK") > 0)
                return; // too ugly!
            UIManager.setLookAndFeel(lafName);
        }
        catch (Exception shouldNotHappen) {
            shouldNotHappen.printStackTrace();
        }
    }
    
    public static String loc(String id) {
        try {
            String msg = resourceBundle.getString(id);
            return msg;
        }
        catch (MissingResourceException e) {
            return id.replace('_', ' ');
        }
    }

    public static final Component getDialogAnchor(Component component)
    {
        while (component != null) {
            if ((component instanceof Applet) ||
                (component instanceof Dialog) ||
                (component instanceof Frame))
                return component;

            component = component.getParent();
        }
        return null;
    }

    public static void setHeader(JScrollPane pane, JComponent header) {
        JViewport hport = new JViewport();
        hport.setView(header);
        pane.setColumnHeader(hport);
    }

    public static Font changeSize(Font font, int delta)
    {
        return font.deriveFont(font.getSize() + (float) delta);
    }

    public static ImageIcon getIcon(String name)
    {
        if(name == null)
            return null;
        URL iconURL = GUI.class.getResource(name);
        return iconURL == null ? null : new ImageIcon(iconURL, name);
    }
    
    public static ImageIcon getIcon(Class classe, String name) {
        
        URL iconURL = classe.getResource(name);
        return iconURL == null? null : new ImageIcon(iconURL, name);
    }

    public static Image getImage(Class classe, String name) {
        
        URL iconURL = classe.getResource(name);
        return iconURL == null? null 
                              : Toolkit.getDefaultToolkit().getImage(iconURL);
    }
    
    public static void bindAction(JComponent component,
                                  String keyStroke, String id, Action action)
    {
        KeyStroke ks = KeyStroke.getKeyStroke(keyStroke);
        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, id);
        component.getActionMap().put(id, action);
    }
    
    public static Action getAction(JComponent component, String id)
    {
        ActionMap map = component.getActionMap();
        return (map != null)? map.get(id) : null;
    }

    public static void paintImmediately(JComponent comp) {
        comp.paintImmediately(0, 0, comp.getWidth(), comp.getHeight());
    }
    
    public static void notBigger(JComponent comp) {
        comp.setMaximumSize(comp.getPreferredSize());
    }
    
    public static void notSmaller(JComponent comp) {
        comp.setMinimumSize(comp.getPreferredSize());
    }
    
    public static void keepMinWidth(JComponent comp) {
        Dimension d = comp.getPreferredSize();
        d.height = 2;
        comp.setMinimumSize(d);
    }
    
    public static void keepMinHeight(JComponent comp) {
        Dimension d = comp.getPreferredSize();
        d.width = 2;
        comp.setMinimumSize(d);
    }

    public static void setMinWidth(JComponent comp, int size) {
        Dimension min = comp.getMinimumSize();
        if(min == null)
            min = new Dimension(size, 0);
        else min.width = size;
        comp.setMinimumSize(min);        
    }

    public static void setMinHeight(JComponent comp, int size) {
        Dimension min = comp.getMinimumSize();
        if(min == null)
            min = new Dimension(size, 0);
        else min.height = size;
        comp.setMinimumSize(min);        
    }

    public static void setPreferredWidth(JComponent comp, int width) {
        Dimension d = comp.getPreferredSize();
        if(d != null) {
            d.width = width;
            comp.setPreferredSize(d);
        }
    }

    public static void setPreferredHeight(JComponent comp, int height) {
        Dimension d = comp.getPreferredSize();
        if(d != null) {
            d.height = height;
            comp.setPreferredSize(d);
        }
    }
    
    public static int getFontWidth(JComponent comp)
    {
        Font font = comp.getFont();
        Graphics2D g2d = (Graphics2D) comp.getGraphics();
        if(g2d == null) {
            return (int)(font.getSize() * 0.66);  // OOPS whatever
        }
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = font.getMaxCharBounds(frc);
        return (int) (bounds.getWidth() + 0.5);
    }
    
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }
    
    public static void waitCursor(JComponent comp, boolean busy) {
        comp.setCursor(busy? 
                Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
    }

    /**
     * Shows a red message.
     * @param title
     * @param message
     */
    public static void alert(String title, String message)
    {
        JTextArea text = new JTextArea(3, 20);
        text.setOpaque(false);
        text.setForeground(Color.RED);
        text.setText(message);
        text.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        JOptionPane.showMessageDialog(null, text, title, JOptionPane.ERROR_MESSAGE);
    }
    
    public static void warning(String title, String message)
    {
        JOptionPane.showMessageDialog(null, message,
                                      title, JOptionPane.WARNING_MESSAGE);
    }
    
    public static void message(String title, String message)
    {
        JOptionPane.showMessageDialog(null, message,
                                      title, JOptionPane.ERROR_MESSAGE);
    }
    
    public static void error(String message)
    {
        message("Error", message);
    }
    
    public static void error(Throwable t)
    {
        t.printStackTrace();
        String msg = "<html><pre><b style='color: red;'>" + t.getMessage() + "\ntrace:";
        StackTraceElement[] st = t.getStackTrace();
        for (int s = 0; s < st.length && s < 20; s++) {
            msg += "\n" + st[s];
        }
        msg += "\n...";
        
        message("Unexpected error", msg);
    }
    
    /**
     * Asks for confirmation (Yes/Cancel).
     * @param title
     * @param message
     */
    public static boolean confirmation(String title, String message)
    {
        return JOptionPane.showConfirmDialog(null, message, title,
                                             JOptionPane.YES_NO_OPTION)
               == JOptionPane.YES_OPTION;
    }

    
    public static String browseFile(String title, String initialPath, 
                                    boolean forOpen,
                                    boolean acceptDir, boolean acceptFile)
    {
        return null;
    }

    public static void iconic(AbstractButton button)
    {
         button.setMargin(SMALL_INSETS);
    }

    public static void betterLookButton(JButton button)
    {
        button.setMargin(BUTTON_INSETS);
    }

    public static JButton newBasicButton(String label,
                                         String actionMethod, Object actionTarget)
    {
        return new JButton(new BasicAction(label,
                                           actionMethod, actionTarget));
    }
    

}
