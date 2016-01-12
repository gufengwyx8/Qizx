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

import com.qizx.apps.studio.Help;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.*;

/**
 * Basic services for an application.
 */
public abstract class AppFrame extends JFrame
{   
    protected PreferenceKeeper settings;
    private StatusBar statusBar;
    
    private ResourceBundle resourceBundle;

    public boolean traceExceptions;

    
    public AppFrame(String appName)
    {
        super(appName);
        
        resourceBundle = ResourceBundle.getBundle(getClass().getName());
        
        settings = new PreferenceKeeper(getClass());
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e)
            {
                commandQuit(null, null);
            }
        });
        
        JPanel workArea = (JPanel) getContentPane(); 
        
        // status bar (first)
        statusBar = new StatusBar();
        workArea.add(statusBar, BorderLayout.SOUTH);        
        statusBar.addArea(new MemoryStatus(1000), true);
    }
    
    public StatusBar getStatusBar()
    {
        return statusBar;
    }

    // ----------------------- utility methods --------------------------------
    
    public String getSetting(String key)
    {
        return settings.get(key);
    }

    public boolean getBoolSetting(String key, boolean defaultValue)
    {
        return settings.getBool(key, defaultValue);
    }

    public int getIntSetting(String key, int defaultValue)
    {
        return settings.getInt(key, defaultValue);
    }

    public void saveSetting(String key, String value)
    {
         settings.put(key, value);
    }

    public void saveSetting(String key, boolean value)
    {
        settings.put(key, value);
    }
    public void saveSetting(String key, int value)
    {
        settings.put(key, value);
    }

    protected final void initGlassPane()
    {
        final Component glassPane = getGlassPane();
        glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // let mouse events go through:
        glassPane.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                redispatchMouseEvent(e, true);
            }

            public void mousePressed(MouseEvent e) {
                redispatchMouseEvent(e, true);
            }

            public void mouseClicked(MouseEvent e) {
                redispatchMouseEvent(e, true);
            }

            public void mouseReleased(MouseEvent e) {
                redispatchMouseEvent(e, true);
            }

            public void mouseDragged(MouseEvent e) {
                redispatchMouseEvent(e, true);
            }

            public void mouseExited(MouseEvent e) {
                redispatchMouseEvent(e, true);
            }

            public void mouseMoved(MouseEvent e) {
                redispatchMouseEvent(e, true);
            }

            public void mouseWheelMoved(MouseWheelEvent e) {
                redispatchMouseEvent(e, true);
            }

            private void redispatchMouseEvent(MouseEvent e, boolean repaint)
            {
                Point ept = e.getPoint();
                Container cp = getContentPane();
                Point ctPt = SwingUtilities.convertPoint(glassPane, ept, cp);
                Component gadget = SwingUtilities.getDeepestComponentAt(cp, ctPt.x, ctPt.y);
                if (gadget != null) {
                    Point gpt = SwingUtilities.convertPoint(glassPane, ept, gadget);
                    gadget.dispatchEvent(new MouseEvent(
                        gadget, e.getID(), e.getWhen(), e.getModifiers(),
                        gpt.x, gpt.y, e.getClickCount(), e.isPopupTrigger()));
                }
            }
        });
        
        glassPane.addMouseMotionListener(new MouseMotionAdapter() {});
        glassPane.addKeyListener(new KeyAdapter() {});
    }

    public void waitCursor(boolean on)
    {
        Component glassPane = ((RootPaneContainer) this).getGlassPane();

        if (on && !glassPane.isVisible())
            glassPane.setVisible(true);
        else if (!on && glassPane.isVisible())
            glassPane.setVisible(false);
    }

    public ImageIcon getIcon(String name)
    {
        ImageIcon icon = GUI.getIcon(getClass(), "icons/" + name);
        return (icon != null) ? icon : GUI.getIcon(name);
    }
    
    protected void fatal(String message)
    {
        System.err.println("*** " + message);
        System.exit(2);
    }

    public void showError(String msg)
    {
        JOptionPane.showMessageDialog(this, msg, local("Error"),
                                      JOptionPane.ERROR_MESSAGE);
    }

    public void showError(Throwable e)
    {
        String msg = e.getMessage();
        if(msg == null)
            msg = e.toString();
        JOptionPane.showMessageDialog(this, msg, local("Error"),
                                      JOptionPane.ERROR_MESSAGE);
        if(traceExceptions)
            e.printStackTrace();
    }

    public void showError(String msg, Throwable e)
    {
        JOptionPane.showMessageDialog(this, msg + ": " + e.getMessage(), local("Error"), 
                                      JOptionPane.ERROR_MESSAGE);
        if(traceExceptions)
            e.printStackTrace();        
    }

    public boolean confirm(String question)
    {
        int rep = JOptionPane.showConfirmDialog(this, question, local("Confirm"),
                                                JOptionPane.YES_NO_OPTION);
        return (rep == JOptionPane.YES_OPTION);
    }

    protected JMenu newMenu(String header)
    {
        String lheader = local(header);
        JMenu menu = new JMenu(DialogUtil.buttonLabel(lheader));
        char mnemo = DialogUtil.buttonMnemonic(lheader);
        if (mnemo != DialogUtil.NO_MNEMONIC)
            menu.setMnemonic(mnemo);
        return menu;
    }
    
    protected JMenuItem addMenuItem(JMenu menu, Action action, String accel)
    {
        JMenuItem item = new JMenuItem(action);
        if (accel != null)
            item.setAccelerator(KeyStroke.getKeyStroke(accel));
        menu.add(item);
        return item;
    }
    
    protected JMenuItem addMenuItem(JMenu menu, String icon, String header,
                                    String accel, String method, Object target)
    {
        String lheader = local(header);
        Action a = new BasicAction(DialogUtil.buttonLabel(lheader),
                                   getIcon(icon), method, target);
        JMenuItem item = new JMenuItem(a);

        char mnemo = DialogUtil.buttonMnemonic(lheader);
        if (mnemo != DialogUtil.NO_MNEMONIC)
            item.setMnemonic(mnemo);

        if (accel != null)
            item.setAccelerator(KeyStroke.getKeyStroke(accel));
        menu.add(item);
        return item;
    }

    public BasicAction newBasicAction(String label, String icon,
                                      Object target, String targetMethod)
    {
        BasicAction action = new BasicAction(local(label), getIcon(icon),
                                             targetMethod, target);
        return action;
    }

    public String local(String id)
    {
        if (id == null)
            return null;
        try {
            String msg = resourceBundle.getString(id);
            return msg;
        }
        catch (MissingResourceException e) {
            return id.replace('_', ' ');
        }
    }
    
    protected String local(String id, String arg1) {
        try {
            String msg = resourceBundle.getString(id);
            return MessageFormat.format(msg, new Object[] { arg1 });
        }
        catch (MissingResourceException e) {
            return id;
        }
    }

    public JFileChooser newFileChooser(String name)
    {
        JFileChooser chooser = new JFileChooser();
        // title is resource:
        String id = "Select_" + name + "_file";
        String prefKey = "current_" + id;
        chooser.setName(prefKey);
        chooser.setDialogTitle(local(id));
        
        // get current path from user settings
        chooser.setCurrentDirectory(new File(settings.get(prefKey, ".")));
        return chooser;
    }

    public boolean showOpenDialog(JFileChooser chooser, Component comp)
    {
        boolean ok = chooser.showOpenDialog(comp) == JFileChooser.APPROVE_OPTION; 
        // remember path in user settings:
        try {
            settings.put(chooser.getName(), 
                      chooser.getCurrentDirectory().getCanonicalPath());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return ok;
    }
    

    protected abstract boolean readyToQuit();

    public void commandQuit(ActionEvent ev, BasicAction a)
    {
        if(readyToQuit())
            System.exit(0);
    }

    public void cmdHelp(ActionEvent ev, BasicAction a)
    {
        Help.showHelp();
    }

    public void cmdHelpContextual(ActionEvent ev, BasicAction a)
    {
        Help.contextualHelp(ev.getSource());
    }
}
