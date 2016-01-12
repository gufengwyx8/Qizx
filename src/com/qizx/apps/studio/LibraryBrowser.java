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
import com.qizx.apps.studio.gui.AppFrame;
import com.qizx.apps.studio.gui.TreePort;
import com.qizx.apps.util.QizxConnector;
import com.qizx.apps.util.QizxConnector.MemberIterator;
import com.qizx.util.basic.PathUtil;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.event.TreeSelectionEvent;

/**
 * Naked XML Library browser.
 * All logic is implemented through ActionHandler.
 */
public class LibraryBrowser extends TreePort
{
    protected AppFrame app;
    protected QizxConnector connector;
    protected ActionHandler handler;
    
    protected Icon serverIcon;
    protected Icon libIcon;
    protected Icon collectionIcon;
    protected Icon documentIcon;
    protected Icon nxdataIcon;


    public LibraryBrowser(AppFrame app)
    {
        super(null, true, null);
        this.app = app;
        
        serverIcon = app.getIcon("server.png");
        libIcon    = app.getIcon("library.png");
        collectionIcon = app.getIcon("collection.png");
        documentIcon = app.getIcon("document.png");
        nxdataIcon = app.getIcon("nxdoc.png");
        changeRoot(new LibManagerNode());
    }

    public QizxConnector getConnector()
    {
        return connector;
    }

    public void setConnector(QizxConnector connector)
    {
        this.connector = connector;
        changeRoot(new LibManagerNode());
    }
    
    public ActionHandler getHandler()
    {
        return handler;
    }

    public void setHandler(ActionHandler handler)
    {
        this.handler = handler;
    }

    /**
     * Redisplays the tree inside the node specified by library:path.
     * Lateral nodes are kept in the same expanded/collapsed state.
     */
    public void refresh(String library, String path)
    {
        // creates problems: so inhibit handlers
        ActionHandler saveHandler = handler;
        try {
            handler = null;
            if(connector != null)
                connector.refresh(library);
            smartRefresh(null); // TODO
        }
        catch (Exception e) {
            e.printStackTrace();    // what TODO?
        }
        finally {
            handler = saveHandler;
        }
    }
    
    public interface ActionHandler
    {   
        void selectedLibrary(LibraryBrowser source, String libraryName);
        
        void selectedCollection(LibraryBrowser source, String libraryName,
                                String path);
        
        void selectedDocument(LibraryBrowser source, String libraryName,
                              String path);

        JPopupMenu getDatabaseMenu(LibraryBrowser source);

        JPopupMenu getLibraryMenu(LibraryBrowser source,
                                  String libraryName);
        
        JPopupMenu getCollectionMenu(LibraryBrowser source,
                                     String libraryName, String path);
        JPopupMenu getDocumentMenu(LibraryBrowser source,
                                   String libraryName, String path);
    }

    public class LibManagerNode extends TreePort.TNode
    {
        LibManagerNode() {
        }
        
        public Icon getIcon(boolean selected, boolean expanded) {
            return serverIcon;
        }
        
        public String toString()
        {
            if(connector == null)
                return "[No XML Libraries]";
            return connector.getDisplay();
        }

        public boolean equals(Object obj)
        {
            if(!(obj instanceof LibManagerNode))
                return false;
            return true;
        }

        public void procreate()
        {
            if(connector == null)
                return;
            try {
                try {
                    String[] libs = connector.listLibraries();
                    if(libs != null) {
                        for(int li = 0; li < libs.length; li++) {
                            add( new LibraryNode(libs[li]) );
                        }
                    }
                }
                catch (QizxException e) { // no privileges?
                    children.clear();
                }
            }
            catch (Exception e) {
                app.showError("Cannot list XML Libraries: ", e);
            }
        }

        protected JPopupMenu getPopupMenu()
        {
            return (handler == null)?
                     null : handler.getDatabaseMenu(LibraryBrowser.this);
        }
    }
    
    public class LibraryNode extends TreePort.TNode
    {
        public String name;
        
        LibraryNode(String name)
        {
            super();
            this.name = name;
        }

        public Icon getIcon(boolean selected, boolean expanded) {
            return libIcon;
        }
        
        public String toString() {
            try {
                return "XML library '" + name +"'";
            }
            catch (Exception e) { return e.toString(); }
        }

        public boolean equals(Object obj)
        {
            if(obj == this)
                return true;
            if(!(obj instanceof LibraryNode))
                return false;
            LibraryNode that = (LibraryNode) obj;
            return that.name.equals(name);
        }

        public void procreate()
        {
            add(new CollectionNode("/", name));
        }
        
        protected void selected(TreeSelectionEvent e)
        {
            if (handler != null)
                handler.selectedLibrary(LibraryBrowser.this, name);
        }

        protected JPopupMenu getPopupMenu()
        {
            return (handler == null)? 
                       null : handler.getLibraryMenu(LibraryBrowser.this, name);            
        }
    }
    
    class CollectionNode extends TreePort.TNode
    {
        String libraryName;
        String path;

        CollectionNode(String path, String libName)
        {
            this.path = path;
            this.libraryName = libName;
        }

        public Icon getIcon(boolean selected, boolean expanded)
        {
            return collectionIcon;
        }

        protected String getToolTip()
        {
            return "Collection('" + path + "')";
        }

        public String toString()
        {
            return PathUtil.getBaseName(path);
        }

        public boolean equals(Object obj)
        {
            if(obj == this)
                return true;
            if(!(obj instanceof CollectionNode))
                return false;
            CollectionNode that = (CollectionNode) obj;
            return that.path.equals(path);
        }

        public void procreate()
        {
            try {
                MemberIterator iter = connector.collectionIterator(libraryName, path);

                for (; iter.next(); ) {
                    String path = iter.getPath();
                    if(iter.isDocument()) {
                        add(new DocumentNode(path, libraryName, iter.isNonXML()));
                    }
                    else {
                        add(new CollectionNode(path, libraryName));
                    }
                }
            }
            catch (Exception e) {
                app.getStatusBar().transientMessage("Got exception " + e);
                app.showError(e);
            }
        }

        protected void selected(TreeSelectionEvent e)
        {
            if (handler != null)
                handler.selectedCollection(LibraryBrowser.this, libraryName, path);            
        }

        protected JPopupMenu getPopupMenu()
        {
            return (handler == null)? null
                      : handler.getCollectionMenu(LibraryBrowser.this, libraryName, path);                        
        }
    }
    
    
    public class DocumentNode extends TreePort.TLeaf
    {
        String libraryName;
        String path;
        boolean isNonXML;

        public DocumentNode(String path, String libName, boolean isNonXML)
        {
            this.path = path;
            this.libraryName = libName;
            this.isNonXML = isNonXML;
        }

        public Icon getIcon(boolean selected, boolean expanded)
        {
            return isNonXML? nxdataIcon : documentIcon;
        }

        public String toString()
        {
            return PathUtil.getBaseName(path);
        }

        public boolean equals(Object obj)
        {
            if(obj == this)
                return true;
            if(!(obj instanceof DocumentNode))
                return false;
            DocumentNode that = (DocumentNode) obj;
            return that.path.equals(path);
        }

        protected String getToolTip()
        {
            return "Document('" + path + "')";
        }

        protected JPopupMenu getPopupMenu()
        {
            return (handler == null)? null
               : handler.getDocumentMenu(LibraryBrowser.this, libraryName, path);  
        }

        protected void selected(TreeSelectionEvent e)
        {
            if (handler != null)
                handler.selectedDocument(LibraryBrowser.this, libraryName, path);            
        }
    }
}
