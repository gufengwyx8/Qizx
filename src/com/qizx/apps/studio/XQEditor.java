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
package com.qizx.apps.studio;

import com.qizx.api.QizxException;
import com.qizx.api.util.text.LexicalTokenizer;
import com.qizx.apps.studio.gui.AppFrame;
import com.qizx.apps.studio.gui.BasicAction;
import com.qizx.apps.studio.gui.GUI;
import com.qizx.apps.studio.gui.TextPort;
import com.qizx.util.basic.FileUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Style;

public class XQEditor extends TextPort
    implements DocumentListener
{
    private static final String BANNER = " Query editor";
    private static final int RECOLOR_DELAY = 300;

    private AppFrame app;
    private File currentFile;
    private String shortFileName;
    private boolean modified;
    private boolean locked;    
    private Style[] tokenStyles;

    
    public XQEditor(AppFrame app)
    {
        super(BANNER, 80);
        this.app = app;

        enableUndo();
        changeFont(Font.PLAIN, true);
        text.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                          BorderFactory.createEmptyBorder(0, 2, 0, 2)));
       
        JButton clearButton = new JButton(new BasicAction(null, app.getIcon("clear.png"),
                                                          "cmdClear", this));
        clearButton.setToolTipText("Clear the query edition area");
        Help.setHelpId(clearButton, "xquery_erase");
        GUI.iconic(clearButton);
        
        addTool(new JToolBar.Separator(), -1);
        addTool(clearButton, -1);

        doc.addDocumentListener(this);

        tokenStyles = new Style[] {
                addStyle("dummy", Color.black),
                addStyle("TAG", new Color(0x009080)),
                addStyle("SPACE", Color.white), // whatever
                addStyle("NUMBER", new Color(0x7b4018)),
                addStyle("STRING", new Color(0xc08020)),
                addStyle("MISC", new Color(0x303030)),
                addStyle("NAME", new Color(0x3050e0)),
                addStyle("KEYWORD", new Color(0x004090)),
                addStyle("COMMENT", new Color(0x4e9960)),
                addStyle("PRAGMA", new Color(0xc09070)),
                addStyle("FUNC", new Color(0x7030f0))
        };
        
        cmdClear(null, null);
    }

    public void saveSettings()
    {
        //app.saveSetting(RESULTS_USE_MARKUP, useMarkup);
    }

    public void cmdClear(ActionEvent e, BasicAction a)
    {
        setText("");
        modified = false;
        shortFileName = null;
        currentFile = null;
        redoBanner(null);
    }

    public File getFile()
    {
        return currentFile;
    }

    public boolean isModified()
    {
        return modified;
    }

    public void loadQuery(File file, String encoding) throws IOException
    {
        String query = FileUtil.loadString(file, encoding);
        text.setText(query);
        
        modified = false;
        redoBanner(file);
    }

    public void saveQuery(File file, String encoding)
        throws IOException
    {
        FileUtil.saveString(text.getText(), file, encoding);
        modified = false;
        redoBanner(file);
    }

    public void save(String encoding) throws IOException
    {
        saveQuery(currentFile, encoding);
    }

    final ActionListener redoSyntax = new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
            try {
                synColorTimer.stop();
                // Strip troublemaker CR from text: 
                String text2 = text.getText();
                StringBuffer buf = new StringBuffer(text2.length());
                for (int i = 0, size = text2.length(); i < size; i++) {
                    char ch = text2.charAt(i);
                    if (ch != 13)
                        buf.append(ch);
                }
                text2 = buf.toString();
                enableUndoEvents(false);
                LexicalTokenizer scolo = new LexicalTokenizer(text2);
                int token = scolo.nextToken();
                for( ; token > 0; token = scolo.nextToken()) {
                    int start = scolo.getTokenStart();
                    int tlen = scolo.getTokenLength();
                    doc.setCharacterAttributes(start, tlen,
                                               tokenStyles[token], true);
                }
            }
            catch (QizxException ex) { // syntax errors can happen while typing
                
            }
            finally {
                enableUndoEvents(true);
            }
            text.revalidate();
        }
    };

    Timer synColorTimer = new Timer(RECOLOR_DELAY, redoSyntax);


    private void redoBanner(File file)
    {
        if(file != null) {
            shortFileName = file.getName();
            currentFile = file;
        }
        changeTitle(BANNER + ": " + 
                    (shortFileName != null? shortFileName : "")
                    + (modified? " *" : ""));
    }

    void gotChange()
    {
        if (!locked) {
            modified = true;
            redoBanner(null);
        }
        synColorTimer.restart();
    }

    // Document events:
    public void changedUpdate(DocumentEvent e) { }
    public void insertUpdate(DocumentEvent e) { gotChange(); }
    public void removeUpdate(DocumentEvent e) { gotChange(); } 
}
