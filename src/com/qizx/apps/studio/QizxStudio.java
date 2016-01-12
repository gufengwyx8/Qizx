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

import com.qizx.api.*;
import com.qizx.api.fulltext.FullTextFactory;
import com.qizx.api.util.XMLSerializer;
import com.qizx.api.util.fulltext.DefaultFullTextFactory;
import com.qizx.apps.studio.dialogs.*;
import com.qizx.apps.studio.gui.*;
import com.qizx.apps.util.Property;
import com.qizx.apps.util.QizxConnector;
import com.qizx.restclient.RESTConnection;
import com.qizx.util.basic.CLOptions;
import com.qizx.util.basic.FileUtil;
import com.qizx.util.basic.PathUtil;
import com.qizx.xdm.CorePushBuilder;
import com.qizx.xquery.ExpressionImpl;
import com.qizx.xquery.XMLExprDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;


/**
  *	A visual interface for Qizx.
  */
public class QizxStudio extends AppFrame
    implements XQResultView.StatusHandler,
               LibraryBrowser.ActionHandler, MetadataView.ActionHandler
{
    private static final String QUERY_ENCODING = "query-encoding";
    private static final String SETT_TOP_TAB = "topTab";
    private static final String SETT_XTAB_SPLIT = "xtab.split";
    private static final String SETT_XTAB_SPLIT2 = "xtab.split2";
    private static final String SETT_QTAB_SPLIT = "qtab.split";
    private static final String SETT_QTAB_SPLIT2 = "qtab.split2";
    
    private static final int QHIST_MAX = 50;
    
    private static final String[] ENCODINGS = new String[] { 
                "(default)", "UTF-8", "UTF-16", "ISO-8859-1", "ISO-8859-15", "Shift_JIS"
             };
    private static final String MODIFIED_PROCEED = "Query_is_modified:_proceed?";
    
    private static String APP_NAME = "Qizx Studio";
    private static String APP_NAME_V = APP_NAME + " " + Product.FULL_VERSION;
    private static final String DEFAULT_SERVER_URL = "http://localhost:8080/qizx/api";
    
    
    static CLOptions options = new CLOptions(APP_NAME);
    static {
        options.define(null, "<xquery file>...", "=queryFile",
                        "file containing a query to execute.");
        
        options.define("-login_", "<username[:password]>", "!setLoginAction",
                       "define the login name. If authentication is required and\n" +
                       "the password is not present, it will be read on the console.\n" +
                       "See also -auth");
        options.define("-auth_", "<file>", "!loadSecrets",
                       "get the login credentials from a file for convenience.\n" +
                       "If authentication is required, credentials will be read from this file.\n");
        options.define("-group_", "<directory>", "=groupRoot",
                       "select a group of XML Libraries specified by its root directory");
        options.define("-g_", "<directory>", "=groupRoot",
                       "short form of -group");


        options.define("-i_", "<collection>", "=implicitDomain",
          "define implicit root for Path expressions: a path expression\n" +
          "without explicit root, like \"//elem\", takes this value as root.\n" +
          "This allows writing queries independent of the input data.\n" +
          "The value is the same as the argument of function fn:collection():\n" +
          "- a file path or an URL: for example \"dir1/doc1.xml\"\n" +
          "- a file pattern: \"dir/*.xml\"\n" +
          "- a semicolon-separated list of the above elements: \"dizxir1/*.xml;dir2/doc2.xsl\"\n"
        );

        options.define("-domain_", "<collection>", "=implicitDomain",
                       "alias for option -i (implicit path root)");

        options.define("-timezone_", "<time zone>", "=timezone",
                       "Define the implicit timezone, in duration format");
        options.define("-collation_", "<collation>", "=collation",
                       "Define the name of default collation");

        options.define("-doc.cache_", "<size_in_Kb>", "=docCache",
                       "define the parsed document cache size (default 8 Mb)");

        options.define("-tex.", "", "=traceExceptions",
                       "verbose display of exceptions");
        
        // undocumented options:
        options.define("-wiz.", "", "=wizard", null);
        options.define("-dq.", "",  "=traceQuery", null);
        options.define("-lob.", "", "=observer", null);
        options.define("-help@", "", "?", "print this help");        
    }
    
    // --- command line options:
    public String  queryFile;
    
    public boolean traceQuery;
    
    public String groupRoot;
    public String libName;
    public String implicitDomain;
    public String baseURI;
    public String moduleBaseURI;
    public String timezone;
    public String collation;
    public int docCache;
    public boolean wizard;
    public boolean observer = true;

    // ----- GUI stuff -------------------------------------------------------

    protected ImageIcon saveIcon = getIcon("save.gif");
    protected ImageIcon saveAsIcon = getIcon("save.gif");
    protected ImageIcon errorIcon = getIcon("error.gif");
   
    // tabs:
    protected JTabbedPane toolTabs;   // tabbed tools (top-level)
    private JSplitPane xtab;
    private JSplitPane xsplit;
    private JSplitPane qtab;
    private JSplitPane qsplit;
    
    // ----- views -------------------------------
    private LibraryBrowser libBrowser;
    private XQDataView docView;
    private MetadataView metadataView;

    private XQEditor xqEditor;
    private XQResultView resultsView;
    
    private MessageView messageView;    
    
    private JMenuBar mainMenuBar;
    private JMenu filesMenu, helpMenu;

    // ---- specialized dialogs: -----------------
    private CatalogDialog catalogDialog;
    private String initialCatalogs;
    private ErrorLogDialog errorLogDialog;
    private JFileChooser libGroupChooser;
    private GroupOpenDialog groupOpenDialog;
    private ServerConnectDialog serverOpenDialog;
    private ImportDialog importDialog;
    private ImportDialog importNXDialog;
    private ExportDialog exportDialog;
    private MetaEditDialog metaEditor;
    
    // ---- actions    
    private BasicAction newQueryAction;
    private BasicAction loadQueryAction;
    private BasicAction saveQueryAction;
    private BasicAction saveQueryAsAction;
    private BasicAction execAction;
    private BasicAction cancelExecAction;
    private BasicAction finishExecAction;
    private BasicAction dumpExprAction;
    
    private MemberAction docExportAction;
    private MemberAction openGroupAction;
    private MemberAction connectServerAction;
    private MemberAction closeGroupAction;
    
    private FileFilter xqFileFilter = new GlobFileFilter("xq");
    private JFileChooser fileChooser;
    private JComboBox fileChooserEncodings;
    private String currentQueryEncoding;
    
    private HistoryModel recentGroups = new HistoryModel(4, "recentGroup");
    private HistoryModel recentServers = new HistoryModel(4, "recentServer");
    private HistoryModel recentQueryFiles = new HistoryModel(10, "recentQueries");
    
    // ---- Qizx related stuff -----------
    public FullTextFactory fulltextFactory = new DefaultFullTextFactory();

    private QizxConnector connector;
    private String currentLibrary;
    private String currentQueryDomain;
    
    private Properties userSecrets;
    private String userName;
    private String userPassword;
    private HistoryModel qhistory;
    private JButton currentExecButton;
    private JMenu recentFilesMenu;


    // ---------------------------------------------------------------------

    static public void main( String args[] )
    {
        GUI.nativeLookAndFeel();
    
        QizxStudio app = new QizxStudio(APP_NAME_V);
    
        try {
            options.parse(args, app);
            app.createGUI();
            app.setVisible(true);
            app.start();
        }
        catch (CLOptions.Error e) {
            System.exit(0);
        }
        catch (Exception e) {
            if(app.traceExceptions)
                e.printStackTrace();
            else System.err.println("*** " + e);
            System.exit(1);
        }
    }

    public QizxStudio(String appName)
    {
        super(appName);
    }
   
    public void setTitle(String groupLocation)
    {
        if(groupLocation != null)
            super.setTitle(APP_NAME_V + ": " + groupLocation);
        else
            super.setTitle(APP_NAME_V);
    }

    public void start()
    {
        buildRecentFileMenu();

        try {
            waitCursor(true);
            if(groupRoot != null)
                openLibraries(groupRoot);
            if(queryFile != null)
                loadQuery(new File(queryFile));
            waitCursor(false);
        }
        catch (Exception e) {
            waitCursor(false);
            if(traceExceptions)
                e.printStackTrace();
            GUI.error(e.getMessage());
        }
        
        // XML catalogs
        initialCatalogs = CatalogDialog.getCatalogProperty();
        if(initialCatalogs == null || initialCatalogs.length() == 0) {
            String cataPref = getSetting("xml.catalogs");
            if(cataPref != null)
                CatalogDialog.setCatalogProperty(cataPref);
        }
    }

    public void setLoginAction(String cred) throws Exception
    {
        int colon = cred.indexOf(':');
        if(colon > 0) {
            userPassword = cred.substring(colon + 1);
            userName = cred.substring(0, colon);
        }
        else {
            userName = cred;
        }       
    }

    // --------------- GUI ------------------------------------------------
    
    private void createActions()
    {
        connectServerAction = new MemberAction("Connect_to_Server...", "cmdOpenServer",
                                               this, null, null, null);
        closeGroupAction = new MemberAction("Disconnect", "cmdCloseLibGroup",
                                            this, null, null, null);

        newQueryAction = newBasicAction("New_XQuery", "querynew.png",
                                         this, "cmdNewQuery");
        loadQueryAction = newBasicAction("Open_XQuery_File...", "queryload.png",
                                         this, "cmdOpenQuery");
        saveQueryAction = newBasicAction("Save_XQuery_File...", "querysave.png",
                                         this, "cmdSaveQuery");
        saveQueryAsAction = newBasicAction("Save_XQuery_File_As...", "querysave.png",
                                           this, "cmdSaveQueryAs");
        dumpExprAction = newBasicAction(null, "dumpexpr.png",
                                        this, "cmdDumpExpr");
    
        execAction = newBasicAction("Execute", "cog_go.png",
                                     this, "cmdExecute");
        cancelExecAction = newBasicAction(" Stop ", "cog_stop.png",
                                          this, "cmdCancelExec");
        finishExecAction = newBasicAction(null, null, this, "cmdFinishExec");
        
        docExportAction = new MemberAction(null, "cmdDocExport", this,
                                           null, null, null);
        docExportAction.setEnabled(false);
    }

    private void createGUI()
    {
        setIconImage(GUI.getImage(getClass(), "icons/logob16.png"));

        System.setProperty("swing.aatext", "true");
        
        // init help before components:
        Help.initialize();
        Help.setHelpId(this, "startPoint");
        
        initGlassPane();

        // --------- preferences:
        
        Rectangle geom = settings.getGeometry("main.geometry");
        if(geom != null)
            setBounds(geom);
        else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            final int xpadding = 100, ypadding = 60;
            setBounds(xpadding, ypadding, 
                      screen.width - 2 * xpadding,
                      screen.height - 2 * ypadding);
        }

        
        JPanel workArea = (JPanel) getContentPane(); 

        // top-level tabbed pane with main tools:
        toolTabs = new JTabbedPane(JTabbedPane.TOP,
                                   JTabbedPane.WRAP_TAB_LAYOUT);
        workArea.add(toolTabs, BorderLayout.CENTER);

        qhistory = new HistoryModel(QHIST_MAX, "queries");
        qhistory.loadFromPrefs(settings);
        
        recentGroups.loadFromPrefs(settings);
        recentServers.loadFromPrefs(settings);
        recentQueryFiles.loadFromPrefs(settings);
        currentQueryEncoding = settings.get(QUERY_ENCODING);
        
        // --------- views ---------------------------------------------------
        
        createActions();

        libBrowser = new LibraryBrowser(this);
        libBrowser.setHandler(this);
        Help.setHelpId(libBrowser, "xlibs_browser");
        
        docView = new XQDataView(this, "Document", true);
        Help.setHelpId(docView, "xlibs_docport");
        JButton b = docView.addToolbarButton(docExportAction,
                                       "Export document to file", "doc_export");
        b.setIcon(getIcon("export.png"));
        
        metadataView = new MetadataView(this);
        Help.setHelpId(metadataView, "xlibs_metadata");
        metadataView.setHandler(this);

        xqEditor = createXQEditor();
        
        resultsView = new XQResultView(this, "Query results", true);
        resultsView.setStatusHandler(this);
        Help.setHelpId(resultsView, "xquery_results");
        
        messageView = new MessageView();

        errorLogDialog = new ErrorLogDialog(this);
        
        // --------- tabs: temporary -----------------------------------------

        qtab = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        Help.setHelpId(qtab, "xquery_tab");
        qsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        qtab.setLeftComponent(qsplit);
        qsplit.setTopComponent(xqEditor);
        qsplit.setBottomComponent(messageView);
        
        qtab.setRightComponent(resultsView);
        
        qtab.setResizeWeight(0.5);
        qsplit.setResizeWeight(0.6);

        toolTabs.addTab("XQuery", null, qtab, "Edit and execute a XQuery expression");
        toolTabs.setSelectedComponent(qtab);

        
        xtab = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        xtab.setLeftComponent(libBrowser);
        Help.setHelpId(xtab, "xlibs_tab");
        
        xsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        xtab.setRightComponent(xsplit);
        
        xsplit.setTopComponent(metadataView);
        xsplit.setBottomComponent(docView);

        xtab.setResizeWeight(0.3);
        xsplit.setResizeWeight(0.2);

        toolTabs.addTab("XML Libraries", null, xtab, "Manage and browse a group of XML Libraries");
//\end
        int topTab = getIntSetting(SETT_TOP_TAB, 0);
        if(topTab >= toolTabs.getComponentCount())
            topTab = 0;
        toolTabs.setSelectedIndex(topTab);
        
        int pos = getIntSetting(SETT_XTAB_SPLIT, -1);
        if(pos > 0)
            xtab.setDividerLocation(pos);
        pos = getIntSetting(SETT_XTAB_SPLIT2, -1);
        if(pos > 0)
            xsplit.setDividerLocation(pos);
        pos = getIntSetting(SETT_QTAB_SPLIT, -1);
        if(pos > 0)
            qtab.setDividerLocation(pos);
        pos = getIntSetting(SETT_QTAB_SPLIT2, -1);
        if(pos > 0)
            qsplit.setDividerLocation(pos);
        
        createMenus();
    }

    private void createMenus()
    {
        mainMenuBar = new JMenuBar();
        
        filesMenu = newMenu("F&ile");
        mainMenuBar.add( filesMenu );
        
        addMenuItem(filesMenu, newQueryAction, "control N");
        addMenuItem(filesMenu, loadQueryAction, "control O");
        addMenuItem(filesMenu, saveQueryAction, "control S");
        addMenuItem(filesMenu, saveQueryAsAction, "control shift S");
        recentFilesMenu = new JMenu("Recent XQuery files");
        filesMenu.add(recentFilesMenu);
        buildRecentFileMenu();
        
        filesMenu.addSeparator();
        addMenuItem(filesMenu, null, "Q&uit", "control Q",
                    "commandQuit", this);
        
        
        JMenu editMenu = newMenu("E&dit");
        mainMenuBar.add( editMenu );

        addMenuItem(editMenu, GUI.getAction(xqEditor, "undo"), "control Z");
        addMenuItem(editMenu, GUI.getAction(xqEditor, "redo"), "control Y");
        editMenu.addSeparator();
        
        JMenuItem it;
        DefaultEditorKit.CutAction cuteAction = new DefaultEditorKit.CutAction();
        cuteAction.putValue(Action.SMALL_ICON, getIcon("cut.png"));
        it = addMenuItem(editMenu, cuteAction, "control X");
        it.setText("Cut");
        it.setMnemonic(KeyEvent.VK_T);
        
        DefaultEditorKit.CopyAction copyAction = new DefaultEditorKit.CopyAction();
        copyAction.putValue(Action.SMALL_ICON, getIcon("copy.png"));
        it = addMenuItem(editMenu, copyAction, "control C");
        it.setText("Copy");
        it.setMnemonic(KeyEvent.VK_C);

        DefaultEditorKit.PasteAction pasteAction = new DefaultEditorKit.PasteAction();
        pasteAction.putValue(Action.SMALL_ICON, getIcon("paste.png"));
        it = addMenuItem(editMenu, pasteAction, "control V");
        it.setText("Paste");
        it.setMnemonic(KeyEvent.VK_P);
        
        // ---- Tools ---------------
        JMenu toolsMenu = newMenu("T&ools");
        mainMenuBar.add( toolsMenu );
        addMenuItem(toolsMenu, connectServerAction, null);
        addMenuItem(toolsMenu, closeGroupAction, null);

        toolsMenu.addSeparator();
        addMenuItem(toolsMenu, "settings.gif", "X&ML_Catalogs...", null,
                    "cmdXMLCatalogs", this);
        addMenuItem(toolsMenu, null, "Show Log", null, "cmdShowLog", this);
        
//        addMenuItem(toolsMenu, null, "C&lear Document Cache", null,
//                    "cmdClearDocCache", this);
        
        // ---- Help ---------------
        helpMenu = newMenu("H&elp");
        mainMenuBar.add(helpMenu);
        
        addMenuItem(helpMenu, "help.png", "H&elp", "F1", 
                    "cmdHelp", this);
        addMenuItem(helpMenu, "contextual_help.gif", "C&ontextual_Help", null, 
                    "cmdHelpContextual", this);

        helpMenu.addSeparator();
        addMenuItem(helpMenu, null, "A&bout " + APP_NAME_V, null, 
                    "cmdAbout", this);

        setJMenuBar(mainMenuBar);
        getContentPane().invalidate();
    }

    // ----------------- callbacks --------------------------------------------

    private XQEditor createXQEditor()
    {
        final XQEditor editor = new XQEditor(this);

        if(wizard) {
            // for debugging: not official
            JButton dumpButton = editor.addButton(dumpExprAction, 0);
            GUI.iconic(dumpButton);
            dumpButton.setToolTipText("Dump expression");

            HistoryMenu historyMenu = new HistoryMenu("Query history", qhistory) {
                protected void selected(Object item) {
                    editor.setText(item.toString());
                }
            };
            historyMenu.setIcon(getIcon("history.gif"));
            GUI.iconic(historyMenu);
            Help.setHelpId(historyMenu, "xquery_history");
            editor.addTool(historyMenu, 0);
            editor.addToolSpace(10, 0);
        }

        JButton execButton = editor.addButton(execAction, 0);
        execButton.setToolTipText("Compile and execute query");
        Help.setHelpId(execButton, "xquery_execute");
        GUI.iconic(execButton);

        return editor;
    }

    /**
     *  Displays compilation and execution messages.
     */
    public class MessageView extends TextPort
        implements TraceObserver
    {
        Style errorStyle, noStyle, linkStyle, traceStyle;

        MessageView()
        {
            super("Messages", -1);
            Help.setHelpId(this, "xquery_messages");

            BasicAction clearAction =
                new BasicAction(null, getIcon("clear.png"), "cmdClear", this);
            JButton clearButton = addButton(clearAction, -1);
            GUI.iconic(clearButton);
            clearButton.setToolTipText("Clear messages");

            errorStyle = addStyle("ERROR", new Color(0xC00000));
            noStyle = addStyle("PLAIN", new Color(0x303030));
            linkStyle = addStyle("LINK", new Color(0x000090));
            traceStyle = addStyle("TRACE", new Color(0x408000));
            StyleConstants.setUnderline(linkStyle, true);
            text.addCaretListener(this);
        }

        public void cmdClear(ActionEvent ev, BasicAction a) {
            text.setText("");
        }

        protected void linkAction(Object link, int dot)
        {
            int position = 0;
            if(link instanceof Message) {
                position = ((Message) link).getPosition();
            }
            else if(link instanceof EvaluationStackTrace) {
                position = ((EvaluationStackTrace) link).getPosition();
            }
            
            xqEditor.select(position, position + 1);
        }

        public void trace(EvaluationStackTrace location, ItemSequence value,
                          String label)
        {
            StringBuffer out = new StringBuffer("TRACE: label=");
            out.append(label);
            out.append(", value=");
            int cnt = 0;
            try {
                //value.moveTo(0);
                for(; value.moveToNextItem() && cnt < 10; cnt++) {
                    out.append(' ');
                    out.append(value.getString());
                }
                if(value.moveToNextItem())
                    out.append("...");
            }
            catch (EvaluationException e) {
                out.append("!" + e.toString() + "!");
            }
            out.append("\n at ");
            //            message(out.toString(), traceStyle);
            //            showFrame(location);             
        }
    }

    // --------------- controller --------------------------------------------

    protected boolean readyToQuit()
    {
        try {
            if(xqEditor.isModified() && !wizard &&
                    !confirm(local(MODIFIED_PROCEED)))
                return false;

            // save preferences:
            settings.putGeometry("main.geometry", getBounds());
            saveSetting(SETT_TOP_TAB, toolTabs.getSelectedIndex());
            saveSetting(SETT_XTAB_SPLIT, xtab.getDividerLocation());
            saveSetting(SETT_XTAB_SPLIT2, xsplit.getDividerLocation());
            saveSetting(SETT_QTAB_SPLIT, qtab.getDividerLocation());
            saveSetting(SETT_QTAB_SPLIT2, qsplit.getDividerLocation());

            String cata = CatalogDialog.getCatalogProperty();
            if(cata != null)
                saveSetting("xml.catalogs", cata);

            if(qhistory != null)
                qhistory.saveToPrefs(settings);

            resultsView.saveSettings();
            xqEditor.saveSettings();

            recentGroups.saveToPrefs(settings);
            recentServers.saveToPrefs(settings);
            recentQueryFiles.saveToPrefs(settings);
            if (currentQueryEncoding != null)
                saveSetting(QUERY_ENCODING, currentQueryEncoding);
            
            // perhaps important!
            closeLibraries();
        }
        catch(Exception e) {
            showError("Problem saving settings", e);
        }
        return true;
    }
    
    void closeLibraries()
    {
        if(connector != null)
            try {
                libBrowser.setConnector(null);
                connector.close();
                connector = null;
            }
            catch (Exception e) {
                showError("Closing Library access", e);
            }
        setTitle(null);
    }

    /**
     * @param address either a file path or a HTTP Url
     */
    private boolean openLibraries(String address) throws Exception
    {
        if(address.startsWith("http:")) {
            try {
                waitCursor(true);
                if(!connectToServer(address))
                    return false;
            }
            finally {
                waitCursor(false);
            }
            recentServers.addItem(address);
        }

        return setConnector(address);
    }

    private boolean setConnector(String address)
        throws Exception
    {
        libBrowser.setConnector(connector);
        String[] libs = connector.listLibraries();
        currentLibrary = null;
        if(libs != null && libs.length > 0)
            currentLibrary = libs[0];
        
        connector.enableBinding(null);  // all classes
        notification("current Library: " + currentLibrary);
        setTitle(address);
        return true;
    }

    private boolean connectToServer(String group) throws IOException
    {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return authenticate(getRequestingPrompt(), getRequestingHost()); 
            }            
        });
        
        RESTConnection cx = null;
        try {
            cx = new RESTConnection(group);
            cx.login();
        }
        catch (IOException e) {
            showError("server connection error on " + group +"\n", e);
            return false;
        }

        connector = new QizxConnector(cx);
        return true;
    }

    protected PasswordAuthentication authenticate(String realm, String host)
    {
        if(userName != null && userPassword != null)
            return new PasswordAuthentication(userName, userPassword.toCharArray()); 
        
        LoginDialog loginD = new LoginDialog(this);
        Help.setDialogHelp(loginD, "login_server_dialog");
        
        loginD.showUp("Login", realm + " from " + host);
        if(loginD.isCancelled())
            return null;
        
        String login = loginD.getLogin();
        char[] password = loginD.getPassword();

        return new PasswordAuthentication(login, password); 
    }


    private void notification(String message)
    {
        messageView.appendText(message + "\n", messageView.noStyle);
    }

    private void message(String message)
    {
        messageView.appendText(message, null);
    }

    public void message( String msg, AttributeSet style)
    {
        messageView.appendText(msg, style);
    }

    public void hyperLink(String text, Object link)
    {
        messageView.hyperlinkMessage(text, messageView.linkStyle, link);
    }

    // -----------------------------------------------------------------------
    
    public void cmdAbout(ActionEvent ev, BasicAction a)
    {
        
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long totalMemory = runtime.totalMemory();
        long usedMemory = (totalMemory - runtime.freeMemory()) / 1024;
        long maxMemory = runtime.maxMemory() / 1024;
        GregorianCalendar cal = new GregorianCalendar();
        int year = cal.get(GregorianCalendar.YEAR);
        boolean freed = System.getProperty("com.qizx.fe") != null;
        String exp = System.getProperty("com.qizx.fe.expires");
        
        String vurl = Product.VENDOR_URL;
        String msg = "<html><body>"
        + Product.PRODUCT_NAME + " " + Product.FULL_VERSION 
        + (Product.VARIANT.length() > 0? ("  (" + Product.VARIANT + " variant)") : "")
        + "<br ><br >"
        + "Copyright \u00A9 2003-" + year + " " + Product.VENDOR + ", all rights reserved."
        + "<br><br>"
        + "For more information, please visit " +
        		"<a href='" + vurl + "'>" + vurl + "</a>\n" + "<br> <br> "
        + "<hr><br>Credits:<p>Most icons by Mark James: famfamfam.com"

        + "<br> <br> <hr><br>"
        + "JVM: "+ System.getProperty("java.vendor")
           + " " + System.getProperty("java.vm.version")
           + " " + System.getProperty("java.vm.info") + "<br>"
        + "Memory used " + usedMemory + " Kb, maximum " + maxMemory + " Kb\n\n";
        
        JOptionPane.showMessageDialog(this, new JLabel(msg), "About " + APP_NAME,
                                      JOptionPane.PLAIN_MESSAGE, null);
    }

    public void cmdXMLCatalogs(ActionEvent ev, BasicAction a)
    {
        catalogDialog = new CatalogDialog(this);
        catalogDialog.showUp();
    }

    public void cmdShowLog(ActionEvent ev, BasicAction a)
    {
        errorLogDialog.showUp();
    }
    
    class RecentFileAction extends BasicAction
    {
        String filePath;
        
        public RecentFileAction(String label, String file)
        {
            super(label, null, "cmdOpenRecentQuery", QizxStudio.this);
            filePath = file;
        }        
    }
    
    private void buildRecentFileMenu()
    {
        recentFilesMenu.removeAll();
        for(int i = 0; i < recentQueryFiles.getSize(); i++)
        {
            String file = (String) recentQueryFiles.getElementAt(i);
            
            String label = "" + (i + 1) + " " + file;
            RecentFileAction action = new RecentFileAction(label, file);
            recentFilesMenu.add(action);
        }
    }
    
    public void cmdNewQuery(ActionEvent ev, BasicAction a)
    {
        if (xqEditor.isModified() && !confirm(local(MODIFIED_PROCEED)))
            return;
        xqEditor.cmdClear(ev, a);
    }
    
    public void cmdOpenRecentQuery(ActionEvent ev, RecentFileAction act)
    {
        if (xqEditor.isModified() && !confirm(local(MODIFIED_PROCEED)))
            return;
        String file = act.filePath;
        try {
            loadQuery(new File(file));
            // put at head:
            recentQueryFiles.addItem(file);
        }
        catch (IOException ioe) {
            showError("cannot read file " + file + ": " + ioe.getMessage());
            recentQueryFiles.removeElement(file);
        }        
        buildRecentFileMenu();      // not lazy
    }
    
    public void cmdOpenQuery(ActionEvent ev, BasicAction a)
    {
        if (xqEditor.isModified() && !confirm(local(MODIFIED_PROCEED)))
            return;
        getFileChooser("Open XQuery source file");
        fileChooser.removeChoosableFileFilter(xqFileFilter);
        fileChooser.addChoosableFileFilter(xqFileFilter);
        
        int resp = fileChooser.showOpenDialog(this);
        if(resp != JFileChooser.APPROVE_OPTION)
            return;
        File file = fileChooser.getSelectedFile();
        try {
            loadQuery(file);
            // put at head:
            recentQueryFiles.addItem(file.getCanonicalPath());        
            buildRecentFileMenu();
        }
        catch (IOException ioe) {
            showError("cannot read file " + file + ": " + ioe.getMessage());
        }
    }

    public void cmdSaveQueryAs(ActionEvent ev, BasicAction a)
    {
        getFileChooser("Save XQuery source file");
        File curFile = xqEditor.getFile();
        if(curFile != null)
            fileChooser.setCurrentDirectory(curFile.getParentFile());
        int resp = fileChooser.showSaveDialog(this);
        if(resp != JFileChooser.APPROVE_OPTION)
            return;
        File file = fileChooser.getSelectedFile();
        String encoding = (String) fileChooserEncodings.getSelectedItem();
        if("(default)".equals(encoding))
            encoding = null;
        
        if(file.exists()) {
            if(!GUI.confirmation("File exists", "File "+file+" exists: proceed?"))
                return;
        }
        
        try {
            xqEditor.saveQuery(file, encoding);
            // put at head:
            recentQueryFiles.addItem(file.getCanonicalPath());        
            buildRecentFileMenu();
        }
        catch (IOException ioe) {
            showError(ioe);
        }
    }
    
    public void cmdSaveQuery(ActionEvent ev, BasicAction a)
    {
        if(xqEditor.getFile() == null)
            cmdSaveQueryAs(ev, a);
        else
            try {
                xqEditor.save(currentQueryEncoding);
            }
            catch (IOException ioe) {
                showError(ioe);
            }
    }
    
    private void loadQuery(File file)
        throws IOException
    {
        String encoding = (fileChooserEncodings == null) ? null
                            : (String) fileChooserEncodings.getSelectedItem();
        if ("(default)".equals(encoding))
            encoding = null;
        currentQueryEncoding = encoding;
        xqEditor.loadQuery(file, encoding);
    }

    public void cmdExecute(ActionEvent ev, BasicAction a) throws QizxException
    {
        waitCursor(true);
        currentExecButton = (JButton) ev.getSource();
        currentExecButton.setAction(cancelExecAction);
        
        String q = xqEditor.getText();
        if(connector == null) {
            connector = new QizxConnector("."); // plain session
            connector.enableBinding(null);
        }

        resultsView.execQuery(q, connector, currentLibrary, currentQueryDomain,
                              finishExecAction);
        if(qhistory != null)
            qhistory.addItem(q);
    }
    
    public void cmdCancelExec(ActionEvent ev, BasicAction a)
    {
        resultsView.cancelQuery();
    }
    
    public void cmdFinishExec(ActionEvent ev, BasicAction a)
    {
        currentExecButton.setAction(execAction);
    }
    
    public void executionStatus(long itemCount, boolean firstExecution, long time,
                                Throwable error)
    {
        if (error != null) {
            Style style = messageView.errorStyle;
            if (error instanceof IOException) { // remote 
                message(error.getMessage() + "\n", style);
                
            }
            else if (error instanceof CompilationException) { 
                message("Compilation: " + error.getMessage() +"\n", style);
                displayMessages(((CompilationException) error).getMessages());
            }
            else if (error instanceof EvaluationException) { 
                EvaluationException rte = (EvaluationException) error;
                QName errorCode = rte.getErrorCode();
                String errCode = (errorCode == null) ? "?" : errorCode.getLocalPart();
                
                message("Execution error " + errCode +": " + error.getMessage() +"\n", style);
                printStack(rte.getStack());
            }
            else {
                String name = error.getClass().getSimpleName();
                message("error " + name + ": " + error.getMessage() + "\n",
                        style);
            }
        }
        else {
            if (firstExecution)
                messageView.appendText(itemCount + " items in " + time + " ms\n",
                                       messageView.noStyle);
            else
                messageView.appendText("retrieval time " + time + " ms\n", null);
        }
    }
    
    void displayMessages( Message[] messages )
    {
        for (int i = 0; i < messages.length; i++) {
            Message msg = messages[i];

            boolean isError = (msg.getType() == Message.ERROR);
            Style style = isError? messageView.errorStyle : messageView.noStyle;
            
            String prefix = isError? "* " : "  ";
            message(prefix, style);
            if(isError || msg.getType() == Message.WARNING) {
                message(" [");
                hyperLink(printLocation(msg), msg);
                message("] ");
            }
            message(msg.getText(), style);
            message("\n");
        }
    }
    
    void printStack(EvaluationStackTrace[] stack)
    {
        for (int i = 0; i < stack.length; i++) {
            EvaluationStackTrace frame = stack[i];
    
            showFrame(frame);
        }
    }

    private void showFrame(EvaluationStackTrace frame)
    {
        message(" [");
        String loc = (frame.getModuleURI() == null) ? "line "
                                               : (frame.getModuleURI() + ":");
        hyperLink(loc + frame.getLineNumber(), frame);
        message("] ");
        
        if(frame.getSignature() != null) {
            message("function ");
            message(frame.getSignature());
        }
        else message("main query");
        message("\n");
    }
    
    private String printLocation(Message msg)
    {
        return (msg.getModuleURI() == null)
                ? ("line " + msg.getLineNumber())
                : (msg.getModuleURI() + ":" + msg.getLineNumber());
    }

    public void cmdDumpExpr(ActionEvent ev, BasicAction a)
    {
        String q = xqEditor.getText();

        try {
            XQuerySessionManager xqsm = new XQuerySessionManager(new URL("file:."));
            XQuerySession session = xqsm.createSession();
            ExpressionImpl expr = (ExpressionImpl) session.compileExpression(q);
            if(expr != null) {
                CorePushBuilder builder = new CorePushBuilder("");
                expr.dump(new XMLExprDisplay(builder));
                Node result = builder.harvest();
                resultsView.setRootNode(result);
            }
        }
        catch (Exception e) {
            showError(e);
        }
    }
    
    
    public ExportDialog getExportDialog()
    {
        if(exportDialog == null)
            exportDialog = new ExportDialog(this);
        return exportDialog;
    }

    public JFileChooser getFileChooser(String title)
    {
        if(fileChooser == null) {
            fileChooser = new JFileChooser();
            URL examp = getClass().getClassLoader().getResource("examples");
            if(examp == null)
                examp = getClass().getClassLoader().getResource(".");
            File src = examp != null? FileUtil.urlToFile(examp) : new File(".");
            fileChooser.setCurrentDirectory(src);
            
            // encodings combo box:
            JPanel frame = new JPanel();
            fileChooserEncodings = new JComboBox(ENCODINGS);
            frame.add(fileChooserEncodings);
            frame.setBorder(new TitledBorder("Encoding: "));
            fileChooser.setAccessory(frame);
            if(currentQueryEncoding != null)
                fileChooserEncodings.setSelectedItem(currentQueryEncoding);
        }
        return fileChooser;
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

    private void haveGroupChooser()
    {
        if (libGroupChooser == null) {
            libGroupChooser = newFileChooser("group");
            libGroupChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if(recentGroups.getSize() > 0) {
                String latest = (String) recentGroups.getElementAt(0);
                libGroupChooser.setSelectedFile(new File(latest));
            }
        }
    }

    private void importToCollection(final MemberAction act)
    {
        if(importDialog == null)
            importDialog = new ImportDialog(this, false);
        
        importDialog.showUpForImport(act);
    }

    private void importNonXMLToCollection(final MemberAction act)
    {
        if(importNXDialog == null)
            importNXDialog = new ImportDialog(this, true);
        
        importNXDialog.showUpForImport(act);
    }

    public void loadSecrets(String path) throws Exception
    {
        userSecrets = new Properties();
        try {
            FileInputStream input = new FileInputStream(path);
            userSecrets.load(input);
            input.close();
            userName = userSecrets.getProperty("login");
            userPassword = userSecrets.getProperty("password");
            if(userName == null || userPassword == null)
                fatal("secrets file should define properties 'login' and 'password'");
        }
        catch (IOException e) {
            fatal("cannot read secret file: "+e.getMessage());
        }
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

    private void setQueryDomain(String library, String path)
    {
        selectedLibrary(libBrowser, library);
        currentQueryDomain = path;
        notification("Query domain is " + (path == null? "" : (path + " in "))
                     + "Library " + library);
        toolTabs.setSelectedComponent(qtab);
    }

    public void selectedLibrary(LibraryBrowser source, String libraryName)
    {
        if(!libraryName.equals(currentLibrary)) {
            notification("current Library: " + libraryName);
            currentLibrary = libraryName;
            currentQueryDomain = null;
        }
        emptyDocView();
    }

    public void selectedCollection(LibraryBrowser source,
                                   String libraryName, String path)
    {
        QizxConnector ctor = source.getConnector();
        if(ctor == null)
            return;
        try {
            waitCursor(true);
            emptyDocView();

            List<Property> props = ctor.getMemberProperties(libraryName, path);
            metadataView.setProperties(props, ctor, libraryName, path);
            metadataView.changeTitle(" Properties of Collection " + path);
        }
        catch (Exception e) {
            showError("cannot get document contents", e);
        }
        finally {
            waitCursor(false);
        }
    }

    private void emptyDocView()
    {
        docView.setRootNode(null);
        docView.changeTitle("");
        docExportAction.setEnabled(false);
    }

    public void selectedDocument(LibraryBrowser source, String libraryName,
                                 String path)
    {
        QizxConnector ctor = source.getConnector();
        if (ctor == null || ctor.isClosed())
            return;
        try {
            waitCursor(true);
            docView.changeTitle(" Contents of Document " + path);
            String nature = ctor.getMemberNature(libraryName, path);
            boolean exportEnabled = true;
            
            if("document".equals(nature)) { // TODO symbol in QizxConnector
                Node contents = ctor.getDocumentTree(libraryName, path);
                docView.setRootNode(contents);
            }
            else {
                docView.setRootNode(null);
                if("non-xml".equals(nature)) { // TODO symbol in QizxConnector
                    // what?
                    //exportEnabled = false; // TODO
                }
            }
            docExportAction.browser = source;
            docExportAction.library = libraryName;
            docExportAction.path = path;
            docExportAction.setEnabled(exportEnabled);
            
            List<Property> props = ctor.getMemberProperties(libraryName, path);
            metadataView.setProperties(props, ctor, libraryName, path);
            metadataView.changeTitle(" Properties of Document " + path);
        }
        catch (Throwable e) {
            showError("cannot get document contents", e);
        }
        finally {
            waitCursor(false);
        }
    }

    public class MemberAction extends BasicAction
    {
        public LibraryBrowser browser;
        public String library;
        public String path;
        
        public MemberAction(String label,
                            String method, Object target,
                            LibraryBrowser source, String lib, String path)
        {
            super(local(label), null, method, target);
            this.browser = source;
            this.library = lib;
            this.path = path;            
        }

        public void refresh()
        {
            browser.refresh(library, path);
        }        
    }
    
    public JPopupMenu getDatabaseMenu(LibraryBrowser source)
    {
        QizxConnector ctor = source.getConnector();
        JPopupMenu popup = new JPopupMenu();
        
        connectServerAction = new MemberAction("Connect_to_Server...", "cmdOpenServer",
                                               this, source, null, null);
        closeGroupAction = new MemberAction("Disconnect", "cmdCloseLibGroup",
                                            this, source, null, null);
        popup.add(connectServerAction);
        popup.add(closeGroupAction);


        if (ctor != null) {
            popup.addSeparator();
            popup.add(new MemberAction("Create_Library", "cmdCreateLibrary",
                                       this, source, null, null));
        }
        return popup;
    }

    public JPopupMenu getLibraryMenu(LibraryBrowser source, String libraryName)
    {
        QizxConnector ctor = source.getConnector();

        JPopupMenu popup = new JPopupMenu();
        if (ctor != null && ctor.isLocal()) {
            popup.add(new MemberAction("Use_Library_as_Query_Domain",
                                       "cmdLibSetQueryDomain", this, source,
                                       libraryName, null));
            popup.addSeparator();
        }
//        popup.add(new MemberAction("Close", "cmdLibClose", this, source,
//                                   libraryName, null));
//        popup.addSeparator();

        popup.add(new MemberAction("Import_Documents...",
                                   "cmdLibImportDocuments", this, source,
                                   libraryName, null));
        popup.add(new MemberAction("Import_NonXML_Documents...",
                                   "cmdLibImportNXDocuments", this, source,
                                   libraryName, null));
        popup.addSeparator();

        MemberAction delLibAction =
            new MemberAction("Delete_Library", "cmdLibDelete", this, source,
                             libraryName, null);
        popup.add(delLibAction);
        
        popup.addSeparator();
        popup.add(new MemberAction("Refresh", "cmdRefreshNode", this,
                                   source, libraryName, null));
        return popup;
    }

    public JPopupMenu getCollectionMenu(LibraryBrowser source,
                                        String libraryName, String path)
    {
        QizxConnector ctor = source.getConnector();
        JPopupMenu popup = new JPopupMenu();
        MemberAction action =
            new MemberAction("Use_Collection_as_Query_Domain",
                             "cmdColSetQueryDomain", this, source,
                             libraryName, path);
        popup.add(action);
        action.setEnabled(ctor.isLocal());
        popup.addSeparator();

        popup.add(new MemberAction("Import_Documents...",
                                   "cmdColImportDocuments", this, source,
                                   libraryName, path));
        popup.add(new MemberAction("Import_NonXML_Documents...",
                                   "cmdColImportNXDocuments", this, source,
                                   libraryName, path));
        popup.addSeparator();
        popup.add(new MemberAction("Create_Sub-Collection...",
                                   "cmdColCreateCollection", this, source,
                                   libraryName, path));
        popup.add(new MemberAction("Rename_Collection...", "cmdColRename",
                                   this, source, libraryName, path));
        popup.add(new MemberAction("Copy_Collection...", "cmdColCopy", this,
                                   source, libraryName, path));
        if (!ctor.isLocal()) {
            popup.addSeparator();
            popup.add(new MemberAction("Modify_Access_Rights...",
                                       "cmdColEditACL", this, source,
                                       libraryName, path));
        }

        popup.addSeparator();
        popup.add(new MemberAction("Delete_Collection", "cmdColDelete", this,
                                   source, libraryName, path));
        popup.addSeparator();
        popup.add(new MemberAction("Refresh", "cmdRefreshNode", this,
                                   source, libraryName, path));
        return popup;
    }

    public JPopupMenu getDocumentMenu(LibraryBrowser source,
                                      String libraryName, String path)
    {
        QizxConnector ctor = source.getConnector();
        JPopupMenu popup = new JPopupMenu();
        MemberAction action = new MemberAction("Use_Document_as_Query_Domain",
                                               "cmdDocSetQueryDomain", this, source,
                                               libraryName, path);
        action.setEnabled(ctor.isLocal());
        popup.add(action);

        popup.addSeparator();

        MemberAction xact = new MemberAction("Export_to_File...", "cmdDocExport", this,
                                             source, libraryName, path);
        xact.setEnabled(docExportAction.isEnabled());
        popup.add(xact);

        popup.addSeparator();
        popup.add(new MemberAction("Copy_Document...", "cmdDocCopy", this,
                                   source, libraryName, path));
        popup.add(new MemberAction("Rename_Document...", "cmdDocRename", this,
                                   source, libraryName, path));
        popup.addSeparator();
        popup.add(new MemberAction("Delete_Document", "cmdDocDelete", this,
                                   source, libraryName, path));
        popup.addSeparator();
        popup.add(new MemberAction("Refresh", "cmdRefreshNode", this,
                                   source, libraryName, path));
        return popup;
    } 
    
    public void cmdCloseLibGroup(ActionEvent ev, MemberAction a)
    {
        closeLibraries();       
    }

    
    public void cmdOpenServer(ActionEvent ev, MemberAction a)
    {
        if(recentServers.getSize() == 0)
            recentServers.addItem(DEFAULT_SERVER_URL);
        
        if (serverOpenDialog == null)
            serverOpenDialog = new ServerConnectDialog(this, recentServers);
        serverOpenDialog.showUp();

        try {
            if(serverOpenDialog.isCancelled())
                return;
            String group = serverOpenDialog.getSelected();
            if(group == null)
                return;

            closeLibraries();
            if(!openLibraries(group))
                return;
        }
        catch (Exception e) {
            e.printStackTrace();
            GUI.error("Error opening Library group: " + e);
        }
    }
    
    public void cmdCreateLibrary(ActionEvent ev, MemberAction act)
    {
        ValueChooser nameInput =
            new ValueChooser(this, local("Create_XML_library..."),
                             "Enter a name for a new XML library");
        nameInput.setRegexpValidator("[\\w\\p{L}]+");
        nameInput.showUp();
        String name = nameInput.getInput();
        if(name == null)
            return;
        
        try {
            QizxConnector ctor = act.browser.getConnector();
            ctor.createLibrary(name);
            act.browser.refresh(null, null); // from root
        }
        catch(Exception lex) {
            showError("Cannot create library " + name, lex);
        }
    }

    public void cmdLibSetQueryDomain(ActionEvent ev, MemberAction act)
    {
        setQueryDomain(act.library, "/");
    }
    
    public void cmdLibImportDocuments(ActionEvent ev, MemberAction act)
    {
        importToCollection(act);
    }
    
    public void cmdLibImportNXDocuments(ActionEvent ev, MemberAction act)
    {
        importNonXMLToCollection(act);
    }

    
    public void cmdLibDelete(ActionEvent ev, MemberAction target)
    {
        if(!confirm(local("Do_you_really_want_to_delete") + " " +
                    target.library + " ?"))
            return;
        try {
            QizxConnector ctor = target.browser.getConnector();
            ctor.deleteLibrary(target.library);
            metadataView.setProperties(null, null, null, null);

            target.browser.refresh(null, null);
        }
        catch (Exception e) {
            showError("Cannot delete library " + target.library, e);
        }
     }

    public void cmdColSetQueryDomain(ActionEvent ev, MemberAction target)
    {
        setQueryDomain(target.library, target.path);
    }

    public void cmdColImportDocuments(ActionEvent ev, MemberAction target)
    {
        importToCollection(target);
    }

    public void cmdColImportNXDocuments(ActionEvent ev, MemberAction target)
    {
        importNonXMLToCollection(target);
    }

    public void cmdRefreshNode(ActionEvent ev, MemberAction target)
    {
        target.refresh();
    }

    public void cmdColCreateCollection(ActionEvent ev, MemberAction target)
    {
        ValueChooser vc =
            new ValueChooser(this, local("Create_Sub-collection..."),
                             "Enter collection name");
        Help.setDialogHelp(vc, "create_collection_dialog");
        vc.setRegexpValidator("[^/\\\\:]+");
        String name = vc.enterString("");
        if (name == null)
            return;

        try {
            String newPath = PathUtil.makePath(target.path, name);
            QizxConnector ctor = target.browser.getConnector();
            ctor.createCollection(target.library, newPath);
            target.refresh();
            // TODO select the new collection
        }
        catch (Exception e) {
            showError("Cannot create " + name, e);
        }
    }

    public void cmdColDelete(ActionEvent ev, MemberAction target)
    {
        if (!confirm(local("Do_you_really_want_to_delete")
                     + " " + target.path + " ?"))
            return;

        try {
            QizxConnector ctor = target.browser.getConnector();
            ctor.deleteMember(target.library, target.path);
            
            metadataView.setProperties(null, null, null, null);
            target.refresh();
        }
        catch (Exception ex) {
            showError("Cannot delete collection " + target.path, ex);
        }
    }

    public void cmdColCopy(ActionEvent ev, MemberAction target)
    {
        String newPath =
            ValueChooser.enterString(target.browser, "Copy Collection",
                                     "Enter destination path for collection ",
                                     target.path, null);
        if (newPath == null)
            return;
        QizxConnector ctor = target.browser.getConnector();
        try {
            ctor.copyMember(target.library, target.path, newPath);

            target.refresh();
        }
        catch (Exception e) {
            showError("Cannot copy " + target.path, e);
            try {
                ctor.rollback(target.library);
            }
            catch (Exception e1) {
                System.err.println("failed rollback: " + e1);
            }
        }
    }

    public void cmdColRename(ActionEvent ev, MemberAction target)
    {
        String newPath =
            ValueChooser.enterString(target.browser, "Rename Collection",
                                     "Enter destination path for collection ",
                                     target.path, null);
        if (newPath == null)
            return;
        try {
            QizxConnector ctor = target.browser.getConnector();
            ctor.renameMember(target.library, target.path, newPath);

            target.refresh();
        }
        catch (Exception e) {
            showError("Cannot rename " + target.path, e);
        }
    }

    public void cmdColEditACL(ActionEvent ev, MemberAction target)
    {
        QizxConnector ctor = target.browser.getConnector();
        String acls = null;
        try {
            acls = ctor.getACL(target.library, target.path, false);
        }
        catch (Exception e) {
            showError("error", e);
            return;
        }
        
        for(;;) {
            acls = ValueChooser.enterText(target.browser,
                                          "Edit Access Control Lists",
                                          60, 20, "", acls, null);
            if(acls == null)
                return;
            try {
                ctor.setACL(target.library, acls);
                break;
            }
            catch (Exception e) {
                showError("error: ", e);
            }
        }
    }

    
    public void cmdDocSetQueryDomain(ActionEvent ev, MemberAction target)
    {
        setQueryDomain(target.library, target.path);
    }

    public void cmdDocExport(ActionEvent ev, MemberAction target)
    {
        QizxConnector ctor = target.browser.getConnector();
        try {
            String nature = ctor.getMemberNature(target.library, target.path);
            if("non-xml".equals(nature)) {
                nonXMLExport(target);  
                return;
            }
        }
        catch (Exception e) {
            showError(e);
            return;
        }
        
        // XML export:
        String path = FileUtil.fileBaseName(target.path);
        XMLSerializer sout = getExportDialog().showUp(path, true,
                                                      "Export document to file");
        if(sout == null)
            return;
        try {
            Node root = docView.getRootNode();
            sout.putNodeCopy(root, 0);
            sout.flush();
            sout.getOutput().close();
        }
        catch (Throwable e) {
            showError(e);
        }
    }

    private void nonXMLExport(MemberAction target)
    {
        try {
//            JFileChooser fc = new JFileChooser(new File("."));
//            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            JFileChooser fc = getExportDialog().getFileChooser();
            
            String path = FileUtil.fileBaseName(target.path);
            fc.setSelectedFile(new File(fc.getSelectedFile(), path));
            
            if (fc.showSaveDialog(getParent()) == JFileChooser.APPROVE_OPTION) {
                QizxConnector ctor = target.browser.getConnector();
                File out = fc.getSelectedFile();
                FileOutputStream fout = new FileOutputStream(out);
                InputStream in = ctor.getNonXMLStream(target.library, target.path);
                FileUtil.copy(in, fout, null);
                in.close();
                fout.close();
            }
        }
        catch (Exception e) {
            showError(local("Exporting") + " from: " + target.library, e);
        }
    }

    public void cmdDocDelete(ActionEvent ev, MemberAction target)
    {
        if (!confirm(local("Do_you_really_want_to_delete")
                     + " " + target.path + " ?"))
            return;

        try {
            QizxConnector ctor = target.browser.getConnector();
            ctor.deleteMember(target.library, target.path);
            
            metadataView.setProperties(null, null, null, null);
            target.refresh();
        }
        catch (Exception ex) {
            showError("Cannot delete document " + target.path, ex);
        }
    }

    public void cmdDocCopy(ActionEvent ev, MemberAction target)
    {
        String newPath =
            ValueChooser.enterString(target.browser, "Copy Document",
                                     "Enter destination path for document ",
                                     target.path, null);
        if (newPath == null)
            return;
        try {
            QizxConnector ctor = target.browser.getConnector();
            ctor.copyMember(target.library, target.path, newPath);

            target.refresh();
        }
        catch (Exception e) {
            showError("Cannot copy " + target.path, e);
        }
    }

    public void cmdDocRename(ActionEvent ev, MemberAction target)
    {
        String newPath =
            ValueChooser.enterString(target.browser, "Rename Document",
                                     "Enter destination path for document ",
                                     target.path, null);
        if (newPath == null)
            return;
        try {
            QizxConnector ctor = target.browser.getConnector();
            ctor.renameMember(target.library, target.path, newPath);

            target.refresh();
        }
        catch (Exception e) {
            showError("Cannot rename " + target.path, e);
        }
    }


    public class PropertyAction extends BasicAction
    {
        protected MetadataView view;
        public String library;
        public String path;
        public Property property;
    
        public PropertyAction(String label, String method, Object target,
                              MetadataView view,
                              String library, String path, Property property)
        {
            super(local(label), method, target);
            this.view = view;
            this.library = library;
            this.path = path;
            this.property = property;
        }
        
        public void savePropertyValue(Property value)
            throws Exception
        {
            QizxConnector ctor = view.getConnector();
            ctor.setMemberProperty(library, path, value);
        }
    
        public void refresh()
        {
            QizxConnector ctor = view.getConnector();
            List<Property> props;
            try {
                props = ctor.getMemberProperties(library, path);
                view.setProperties(props, connector, library, path);
            }
            catch (Exception e) {
                showError(e);
            }
        }        
    }

    public JPopupMenu getPropertiesMenu(MetadataView source,
                                        String libraryName, String path)
    {
        JPopupMenu popup = new JPopupMenu();
        
        popup.add(new PropertyAction("Add_Property...", "cmdMetaAddProperty", this,
                                     source, libraryName, path, null));
        return popup;
    }

    public JPopupMenu getPropertyMenu(MetadataView source, String libraryName,
                                      String path, Property property)
    {
        JPopupMenu popup = new JPopupMenu();
        
        popup.add(new PropertyAction("Edit_Property...", "cmdMetaEditProperty", this,
                                     source, libraryName, path, property));
        popup.add(new PropertyAction("Add_Property...", "cmdMetaAddProperty", this,
                                     source, libraryName, path, null));
        popup.add(new PropertyAction("Delete_Property", "cmdMetaDeleteProperty", this,
                                     source, libraryName, path, property));

        return popup;
    }

    public void cmdMetaAddProperty(ActionEvent ev, PropertyAction target)
    {
        try {
            if(metaEditor == null)
                metaEditor = new MetaEditDialog(this);
            metaEditor.showUp(target);
            target.refresh();
        }
        catch (Exception e) {
            showError(local("Add_property") + " " + target.path, e);
        }
    }

    public void cmdMetaEditProperty(ActionEvent ev, PropertyAction target)
    {
        try {
            if(metaEditor == null)
                metaEditor = new MetaEditDialog(this);
            metaEditor.showUp(target);
            target.refresh();
        }
        catch (Exception e) {
            showError(local("Edit_property") + " " + target.path, e);
        }
    }

    public void cmdMetaDeleteProperty(ActionEvent ev, PropertyAction target)
    {
        try {
            target.savePropertyValue(new Property(target.property.name, null));
            target.refresh();
        }
        catch (Exception e) {
            showError(local("Delete_property") + " " + target.path, e);
        }
    }
}

