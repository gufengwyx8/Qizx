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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.*;

/**
 * Basic dialog with configurable buttons (OK, Apply, Cancel).
 * 
 */
public abstract class DialogBase extends JDialog
{
    protected static final int VGAP = 8;
    protected static final int HGAP = 8;
    private static Insets BUTTON_INSETS = new Insets(1, 1, 0, 1);
    
    private Window window;
    private boolean firstShow;
    private boolean cancelled;
    // central area with dialog contents:
    protected JPanel form;
    private JLabel hintArea;
    private JLabel messageArea;
    
    private JPanel mainButtons;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    
    private BasicAction cancelAction;
    
    private ImageIcon hintIcon = GUI.getIcon(DialogBase.class, "hint.png");
    private ImageIcon errorIcon = GUI.getIcon(DialogBase.class, "error.png");
    private ImageIcon helpIcon = GUI.getIcon(DialogBase.class, "help.png");
    private boolean unpacked;

    public DialogBase()
    {
        this((Frame) null, "<title?>");
    }

    public DialogBase(JFrame owner)
    {
        this(owner, "<no title>");
    }

    public DialogBase(Frame parent, String title)
    {
        super(parent);
        init(parent, title);
    }

    public DialogBase(Dialog parent, String title)
    {
        super(parent);
        init(parent, title);
    }

    private void init(Window parent, String title)
    {
        this.window = parent;
        firstShow = true;
        setTitle(title);
        create();
        setModal(true);
        unpacked = true;
        // Swing doesnt do it for you...
        rootPane.registerKeyboardAction(cancelAction,
                                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                        JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void create()
    {
        // ----- always a hint area North:
        hintArea = new JLabel();
        getContentPane().add(hintArea, BorderLayout.NORTH);
        hintArea.setVerticalAlignment(SwingConstants.TOP);
        hintArea.setForeground(new Color(0x606080));
        hintArea.setBorder(BorderFactory.createEmptyBorder(VGAP, 2*HGAP, VGAP, 2*HGAP));
        hintArea.setIconTextGap(2*HGAP);
        
        // ----- central form with 
        JPanel formContainer = new JPanel();
        getContentPane().add(formContainer, BorderLayout.CENTER);
        
        GridBagger fgrid = new GridBagger(formContainer);
        fgrid.setInsets(HGAP, VGAP);
        form = new JPanel();
        fgrid.add(form, fgrid.prop("fill"));
        
        fgrid.newRow();
        messageArea = new JLabel();
        fgrid.add(messageArea, fgrid.prop("xfill"));
        
        // ----- buttons:
        JPanel allButtons = new JPanel();
        getContentPane().add(allButtons, BorderLayout.SOUTH);
    
        GridBagger grid = new GridBagger(allButtons);
        
        grid.newRow();
        // align left:
        grid.add(new JPanel(), grid.prop("xfill"));
        
        mainButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
        grid.add(mainButtons);
    
        helpButton = new JButton(new BasicAction(null, helpIcon , "cmdHelp", this));
        GUI.iconic(helpButton);
        helpButton.setToolTipText("Help");
        helpButton.setContentAreaFilled(false);
        addButton(helpButton, 0);
        
        mainButtons.add(Box.createHorizontalGlue());
    
        okButton = new JButton(new BasicAction("OK", null, "cmdOK", this));
        getRootPane().setDefaultButton(okButton);
        addButton(okButton, -1);
    
        cancelAction = new BasicAction("Cancel", null, "cmdCancel", this);
        cancelButton = new JButton(cancelAction);
        addButton(cancelButton, -1);
    }

    public void addButton(AbstractButton button, int position)
    {
        mainButtons.add(button, position);
    }

    public void removeButton(AbstractButton button)
    {
        mainButtons.remove(button);
    }

    public JComponent getForm() {
        return form;
    }

    /**
     * Makes the dialog visible.
     */
    public void showUp()
    {
        if(unpacked) {
            pack();
            unpacked = false;
        }
        if(firstShow && window != null) {
            centerIn(window);
            firstShow = false;
        }
        toFront();
        setVisible(true);
    }
    
    /**
     * Makes the dialog visible with a new title.
     */
    public void showUp(String title)
    {
        setTitle(title);
        showUp();
    }

    protected void vanish()
    {
        setVisible(false);
        dispose();
    }

    public void centerIn(Window window) {
        setRelativeLocation(this, window);
    }
    
    public JButton getOkButton() {
        return okButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getHelpButton() {
        return helpButton;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

    public void haveOnlyCloseButton()
    {
        removeButton(getOkButton());
        getCancelButton().setText("Close");
    }

    /**
     * Default OK command: just makes the dialog invisible.
     */
    public void cmdOK(ActionEvent e, BasicAction a)
    {
        cancelled = false;
        vanish();
    }

    public void cmdCancel(ActionEvent e, BasicAction a)
    {
        cancelled = true;
        vanish();
    }

    // intercept Window closing
    protected void processWindowEvent(WindowEvent e)
    {
        if(e.getID() == WindowEvent.WINDOW_CLOSING)
            cmdCancel(null, null);
    }

    public void cmdHelp(ActionEvent e, BasicAction a)
    {
        // 
    }
    
    public void setHint(String text, boolean withIcon)
    {
        hintArea.setText(text);
        hintArea.setIcon((withIcon && text != null)? hintIcon : null);
    }
    
    public void setMessage(String text)
    {
        messageArea.setText(text);
        messageArea.setIcon(null);
    }
    
    public static Window getToplevelContainer(Component comp)
    {
        if(comp == null)
            return null;
        Component p = comp.getParent(), gp;
        for( ; p != null && !(p instanceof Window); p = gp) {
            gp = p.getParent();
            if(gp == null)
                break;
        }
        return (Window) p;
    }
    
    public static void setRelativeLocation(Window tool, Window parent)
    {
        Dimension dim = tool.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle r = (parent != null)? parent.getBounds()
                      : new Rectangle(0, 0, screen.width, screen.height);
        
        double xc = r.getCenterX(), yc = r.getCenterY();
        xc -= dim.width / 2;
        if(xc < 0)
            xc = 0;
        if(xc + dim.width > screen.width)
            xc = screen.width - dim.width;

        yc -= dim.height / 2;
        if(yc < 0)
            yc = 0;
        if(yc + dim.height > screen.height)
            yc = screen.height - dim.height;
       
        tool.setLocation((int) xc, (int) yc);
    }
 
}
