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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;


/**
 *	Scrollable text view, with tool bar.
 *<p> Supports text styles.
 */
public class TextPort extends ToolView
    implements CaretListener, UndoableEditListener
{
    private static final String LINK_ATTR = "#link#";
    private static final int MIN_CHAR_WIDTH = 7;
    
    protected JTextPane  text;
    protected StyledDocument doc;
    protected StyleContext styles = new StyleContext();
    protected UndoManager undoer;
    protected BasicAction undoAction, redoAction;
    protected JPopupMenu popup;
    
    private boolean recordUndoEdits;

    private int columns;
    
    public TextPort(String title)
    {
        this(title, -1);
    }
    
    /**
     * @param columns if negative: variable width w/ wrapping.
     */
    public TextPort(String title, final int columns)
    {
        super(title, null);
        doc = new DefaultStyledDocument();
        this.columns = columns;
        text = new JTextPane(doc) {
           
            // just crappy this way of telling it wraps...
            public boolean getScrollableTracksViewportWidth()
            {
                if( TextPort.this.columns <= 0)
                    return true;
                //getParent().setBackground(Color.white);
                return false;
            }
            
            public Dimension getPreferredSize()
            {
                int cols = TextPort.this.columns;
                if(cols <= 0)
                    return super.getPreferredSize();
                
                View view = (View) getUI().getRootView(this);

                float w = Math.max(cols * MIN_CHAR_WIDTH,
                                   view.getPreferredSpan(View.X_AXIS));
                float h = view.getPreferredSpan(View.Y_AXIS);

                return new Dimension((int) Math.ceil(w + 2 * MIN_CHAR_WIDTH),
                                     (int) Math.ceil(h));
            }

        };
        
        setView(text);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(Color.white);
    }
    
    public void changeFont(int style, boolean fixedWidth) {
        Font ft = text.getFont();
        if(fixedWidth)
             ft = new Font("Monospaced", style, ft.getSize());
        else ft = ft.deriveFont(style);
        text.setFont(ft);
        //resetWidth();
    }
    
//    private void resetWidth()
//    { 
//        if(columns <= 0)
//            return;
//        Dimension size = new Dimension(columns * GUI.getFontWidth(text), 0);
//        text.setMinimumSize(size);
//        text.setPreferredSize(size);
//    }

    public void clearText()
    {
        try {
            doc.remove(0, doc.getLength());
        }
        catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    public String getText()
    {
        String txt = text.getText();
        return txt == null? "" : txt;
    }
    
    public void setText(String value)
    {
        text.setText(value);
    }
    
    public int appendText( String txt, AttributeSet style ) {
        try {
            int pos = doc.getLength();
            doc.insertString( pos, txt, style );
            //Rectangle r = text.modelToView(doc.getLength());
            //getViewport().scrollRectToVisible(r);
            pos = doc.getLength();
            selectPosition(pos);
            return pos;
        }
        catch(BadLocationException e) {
            e.printStackTrace();
            return doc.getLength();
        }
    }

    public void selectPosition(int pos)
    {
        text.select(pos, pos);
    }

    public void select(int position, int position2)
    {
        text.requestFocusInWindow();
        text.select(position, position2);
    }

    public JButton addButton(Action action, int position)
    {
        JButton button = new JButton(action);
        addTool(button, position);
        return button;
    }

    public Style addStyle(String name, Color fontColor) {
        return addStyle(name, fontColor, true);
    }
    
    public Style addStyle(String name, Color fontColor, boolean bold) {
        Style style = doc.addStyle(name, null);
        StyleConstants.setForeground( style, fontColor);
        StyleConstants.setBold(style, bold);
        return style;
    }
    
    public MutableAttributeSet mutableStyle( Style parent ) {
        return new SimpleAttributeSet(parent);
    }
    
    public JTextPane getTextPane() {
        return text;
    }

    public void hyperlinkMessage(String text, Style style, Object link)
    {
        MutableAttributeSet must = mutableStyle(style);
        must.addAttribute(LINK_ATTR, link);
        appendText(text, must);
    }

    public void caretUpdate(CaretEvent e)
    {
        Element elem = doc.getCharacterElement(e.getDot());
        if(elem == null)
            return;
        Object attr = elem.getAttributes().getAttribute(LINK_ATTR);
        if(attr != null)
            linkAction(attr, e.getDot());
    }

    // To be redefined
    protected void linkAction(Object attr, int dot) {
    }
    
    public void enableUndo()
    {
        undoer = new UndoManager();
        doc.addUndoableEditListener(this);
        undoAction = new BasicAction("Undo", GUI.getIcon("undo.png"), "cmdUndo", this);
        GUI.bindAction(this, "control Z", "undo", undoAction);
        redoAction = new BasicAction("Redo", GUI.getIcon("redo.png"), "cmdRedo", this);
        GUI.bindAction(this, "control Y", "redo", redoAction);
    }
    
    public Action getAction(String name)
    {
        if("undo".equals(name))
            return undoAction;
        if("redo".equals(name))
            return redoAction;
        return GUI.getAction(text, name);
    }

    protected void enableUndoEvents(boolean enable)
    {
        recordUndoEdits = enable;
    }

    public void undoableEditHappened(UndoableEditEvent e)
    {
        UndoableEdit edit = e.getEdit();
        if(!recordUndoEdits)
            return;
        undoer.addEdit(edit);
        undoActionsEnable();
    }

    public void cmdUndo(ActionEvent e, BasicAction a)
    {
        if(undoer.canUndo())
            undoer.undo();
        undoActionsEnable();
    }
    
    public void cmdRedo(ActionEvent e, BasicAction a)
    {
        if(undoer.canRedo())
            undoer.redo();
        undoActionsEnable();    
    }

    private void undoActionsEnable()
    {
        undoAction.setEnabled(undoer.canUndo());
        redoAction.setEnabled(undoer.canRedo());
    }
}
