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
package com.qizx.apps.studio.dialogs;

import com.qizx.api.util.ImportOptions;
import com.qizx.apps.studio.Help;
import com.qizx.apps.studio.QizxStudio;
import com.qizx.apps.studio.QizxStudio.MemberAction;
import com.qizx.apps.studio.gui.*;
import com.qizx.apps.util.QizxConnector;
import com.qizx.apps.util.QizxConnector.ImportException;
import com.qizx.util.LikePattern;
import com.qizx.util.basic.FileCollector;
import com.qizx.util.basic.FileUtil;
import com.qizx.util.basic.PathUtil;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.Style;

/**
 * Importing documents.
 */
public class ImportDialog extends DialogBase
    implements ListSelectionListener
{
    private static final int HSPACE = HGAP;
    private static Localization local =
        new Localization(QizxStudio.class, "ImportDialog");
    
    private static final String IMAGE_EXTS =
        " jpg jpeg png gif tif tiff ico pbm ";
    
    private AppFrame app;
 
    private ImportOptions impOptions = new ImportOptions();
    private boolean nonXML;
    
    private JButton delFileButton;

    private FileModel fileModel;
    private JTable fileTable;
    private ComboSelector filterCombo;
    private JFileChooser fileChooser;
    
    private JButton runImport;

    private JProgressBar bar;
    private JTextField currentDoc;
    private JLabel docCountDisplay, timeDisplay;
    private TextPort messages;
    private Style errorStyle;
    private boolean importCancelled;
    private ImportTask importTask;
    private JPanel optionPanel;
    private JCheckBox optionStripBox;

    private MemberAction target;    // lib, path and connector

    private QizxConnector connector;


    public ImportDialog(AppFrame owner, boolean nonXML)
    {
        super(owner, local.text(nonXML? "Import_NonXML_Documents" : "Import_Documents"));
        this.nonXML = nonXML;
        this.app = owner;
        buildContents();
        haveOnlyCloseButton();
        
        Help.setDialogHelp(this, "import_documents_dialog");
        pack();
    }
    
    public void showUpForImport(QizxStudio.MemberAction target)
    {
        this.target = target;
        connector = target.browser.getConnector();
        if(target.path == null)
            target.path = "/";

        setHint(local.text("Import_into") + " " + target.library
                + ", collection " + target.path
                + local.text("hint"), true);
        // The policy is: clear all selected docs/folders
        fileModel.clear();
        showUp();
    }

    private void buildContents()
    {
        GridBagger grid = new GridBagger(form);
        grid.setInsets(0, VGAP);

        // Bar with edit buttons
        grid.newRow();
        Box tableEditBox = Box.createHorizontalBox();
        grid.add(tableEditBox, grid.prop("xfill"));
        
        tableEditBox.add(Box.createHorizontalGlue());
        
        JButton addFileButton =
            new JButton(new BasicAction(local.text("Add_File/Folder..."), null,
                                        "cmdAddFile", this));
        tableEditBox.add(addFileButton);
        GUI.betterLookButton(addFileButton);
        addFileButton.setToolTipText(local.text("addFile_tip"));
        
        tableEditBox.add(Box.createHorizontalStrut(HSPACE));

        delFileButton = new JButton(new BasicAction(local.text("Remove"), null,
                                    "cmdDelFile", this));
        tableEditBox.add(delFileButton);
        GUI.betterLookButton(delFileButton);
        delFileButton.setEnabled(false);
        tableEditBox.add(Box.createHorizontalStrut(HSPACE));
        
        JButton clearButton =
            new JButton(new BasicAction(local.text("Clear_all"), null,
                                        "cmdClearFiles", this));
        tableEditBox.add(clearButton);
        GUI.betterLookButton(clearButton);
        
        String[] extens = nonXML? new String[] { "*.*" } : new String[] { "*.xml", "*.*" };
        
        filterCombo = new ComboSelector(local.text("Filter") + ":", extens, true);
        tableEditBox.add(filterCombo);
        
        fileChooser = app.newFileChooser("import");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        // ------------ Table of files to import ------
        grid.newRow();
        fileModel = new FileModel();
        fileTable = new JTable(fileModel);
        fileTable.setIntercellSpacing(new Dimension(4, 5));
        TableColumnModel cmodel = fileTable.getColumnModel();
        cmodel.getColumn(0).setPreferredWidth(350);
        defineColumn(cmodel, 0, 350, SwingConstants.LEFT);
        defineColumn(cmodel, 1, 50, SwingConstants.CENTER);
        defineColumn(cmodel, 2, 90, SwingConstants.RIGHT);
        defineColumn(cmodel, 3, 50, SwingConstants.CENTER);
        fileTable.getSelectionModel().addListSelectionListener(this);
        
        JScrollPane tblScrollPane = new JScrollPane(fileTable);
        tblScrollPane.setPreferredSize(new Dimension(500, 100));
        grid.add(tblScrollPane, grid.prop("fill").weighted(1, 1));
        
        // ------------ Import options -------------------
        grid.newRow();
        optionPanel = new JPanel();
        optionPanel.setBorder(new TitledBorder("Import Options"));
        grid.add(optionPanel, grid.prop("xfill"));
        GridBagger ogrid = new GridBagger(optionPanel);        
        
        optionStripBox = new JCheckBox("Strip Whitespace");
        ogrid.add(optionStripBox, ogrid.prop("left"));
        // ------------ Launch import --------------------
        grid.newRow();
        runImport = new JButton(new BasicAction(local.text("Start_import"),
                                                   "cmdRunImport", this));
        grid.add(runImport, grid.prop("center"));
        
        enableImport(true);
        
        // ------------ progress: ------------------------
        grid.newRow();
        JPanel progressBox = new JPanel();
        grid.add(progressBox, grid.prop("xfill"));

        GridBagger pgrid = new GridBagger(progressBox);
        pgrid.newRow();
        
        bar = new JProgressBar(0, 100);
        pgrid.add(bar, pgrid.prop("left"));
        bar.setPreferredSize(new Dimension(100, 12));
        bar.setMinimumSize(new Dimension(80, 12));
        bar.setValue(0);
        
        currentDoc = new JTextField();
        currentDoc.setEditable(false);
        pgrid.add(currentDoc, pgrid.prop("xfill").leftMargin(HGAP));
       
        pgrid.newRow();
        docCountDisplay = new JLabel("0/0");
        pgrid.add(docCountDisplay, grid.prop("left"));
        timeDisplay = new JLabel("\u00a0");
        pgrid.add(timeDisplay, grid.prop("left").insets(8, 0));

        // ------------ messages --------------------------
        grid.newRow();
        messages = new TextPort("Messages", -1);
        messages.setPreferredSize(new Dimension(500, 120));
        grid.add(messages, grid.prop("fill").weighted(1, 2));
        errorStyle = messages.addStyle("error", Color.red);
    }

    private void defineColumn(TableColumnModel cmodel, int index, 
                              int width, int alignment)
    {
        TableColumn column = cmodel.getColumn(index);
        column.setPreferredWidth(width);
        column.setCellRenderer(new AlignedCellRenderer(alignment));
    }

    private void enableImport(boolean enable)
    {
        runImport.setText(enable ? "Start Import" : "Cancel");
    }

    private void traceError(final String message)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                messages.appendText("*** error " + message + "\n", errorStyle);
            }
        });
    }
    
    private void clear()
    {
        currentDoc.setText("");
        docCountDisplay.setText("\u00a0");
        timeDisplay.setText("\u00a0");
        bar.setValue(0);
        app.waitCursor(false);
        importTask = null;
    }

    public void cmdAddFile(ActionEvent e, BasicAction a)
    {
        fileChooser.setDialogTitle(local.text("fileChooser_title"));
        if(!app.showOpenDialog(fileChooser, this))
            return;
        File[] files = fileChooser.getSelectedFiles();
        String pattern = (String) filterCombo.getValue();
        LikePattern filter = ("*.*".equals(pattern) || "*".equals(pattern))?
                null : new LikePattern(pattern);
        
        for (int i = 0, asize = files.length; i < asize; i++) {
            try {
                FileCollector collec = new FileCollector(files[i], true, filter);
                fileModel.addFile(collec);
                app.waitCursor(true);
                collec.collect();
            }
            finally {
                app.waitCursor(false);
            }
        }
    }

    public void cmdDelFile(ActionEvent e, BasicAction a)
    {
        int[] rows = fileTable.getSelectedRows();
        for (int r = rows.length; --r >= 0; ) {
            fileModel.removeFile(rows[r]);
        }
    }

    public void cmdClearFiles(ActionEvent e, BasicAction a)
    {
        fileModel.clear();
    }
    
    public void cmdRunImport(ActionEvent e, BasicAction a)
    {
        if(importTask != null) {
            // cancel:
            importCancelled = true;
            try {
                importTask.join();
            }
            catch (InterruptedException ignored) { ; }
            return;
        }
        
        enableImport(false);
        importCancelled = false;
        clear();

        app.waitCursor(true);
        GUI.paintImmediately(currentDoc);
        
        importTask = new ImportTask();
        if(importTask.totalFileCount == 0) {
            GUI.warning("Import", "No matching document found");
            clear();
            enableImport(true);
            return;
        }
        messages.appendText("--- Starting import of " + importTask.totalFileCount +
                            " document(s) ---\n", null);
        bar.setValue(0);
        
        importTask.start();
    }

    private static String guessContentType(String docPath, File file)
    {
        String ext = FileUtil.fileExtension(docPath);
        if(IMAGE_EXTS.contains(" " + ext.toLowerCase() + " "))
            return "image/" + ext;
        if("pdf".equalsIgnoreCase(ext))
            return "application/pdf";
        
        return "application/octet-stream";
    }

    class ImportTask extends Thread
    {
        public int totalFileCount;
        public long totalFileSize;
        
        private double sizeDone;    // current
        private long fileSize;
        private long startTime;
        private int previousEstimation;
        
        public ImportTask()
        {
            for (int f = 0, asize = fileModel.getRowCount(); f < asize; f++) {
                FileCollector collector = fileModel.getRow(f);
                totalFileCount += collector.getSize();
                totalFileSize += collector.getByteSize();
            }
        }

        public void run()
        {
            //connector.setProgressObserver(this);

            // import options: FUTURE
//                impOptions.setStripWhiteSpace(optionStripBox.isSelected());
//                ((LibraryPlus) lib).setImportOptions(impOptions);
            
            sizeDone = 0;
            long realSize = 0;
            int realCount = 0;
            startTime = System.currentTimeMillis();
            String targetPath = target.path;
            previousEstimation = Integer.MAX_VALUE;
            
            try {
                if(nonXML)
                    connector.importNonXMLStart(target.library);
                else
                    connector.importStart(target.library);
              loop:
                for(int f = 0, fsize = fileModel.getRowCount(); f < fsize; f++)
                {
                    FileCollector collector =  fileModel.getRow(f);
                    int fileCount = collector.getSize();
                    
                    String rootPath;
                    if(collector.hasDirRoot()) {
                        rootPath = collector.getRoot().getAbsolutePath();
                    }
                    else {
                        rootPath = collector.getRoot().getParent();
                    }    
                    int prefixLen = rootPath.length() + 1;
                    
                    for (int i = 0; i < fileCount; i++)
                    {
                        if(importCancelled)
                            break loop;
                        File file = collector.getFile(i);
                        docCountDisplay.setText((i+1) +"/"+ fileCount +" files");
                        String path = file.getPath();
                        fileSize = file.length();
                        currentDoc.setText(path);
                        currentDoc.select(path.length(), path.length());

                        // document path: TODO option flatten
                        String name = path.substring(prefixLen);
                        String docPath = PathUtil.makePath(targetPath, name);

                        try {
                            // in remote mode, flushes when a max size or 
                            // doc count is reached.
                            if(nonXML) {
                                // just for the demo...
                                connector.importNonXMLDocument(docPath, file,
                                                guessContentType(name, file));
                            }
                            else {
                                connector.importDocument(docPath, file);
                            }

                            realCount ++;
                            realSize += fileSize;
                        }
                        catch(ImportException ex) { // remote
                            for(String err : ex.getErrors())
                                if(!err.startsWith("IMPORT ERRORS"))
                                    traceError(err.replace('\t', ' '));
                        }
                        catch (RuntimeException ex) {
                            traceError("on document " + file + ": unexpected " + ex);
                            ex.printStackTrace();
                        }
                        catch (Exception ex) {
                            traceError("in document " + file
                                       + ": " + ex.getMessage());
                        }

                        sizeDone += fileSize;
                        setBar(sizeDone / totalFileSize);
                    }
                }

                // Finish with a commit:
                try {
                    if(importCancelled)
                        connector.rollback(target.library);
                    else {
                        currentDoc.setText("Finishing indexing...");
                        timeDisplay.setText("");
                        connector.commit(target.library);
                    }
                }
                catch (Exception ex) {
                    //ex.printStackTrace();
                    traceError("in commit: " + ex.getMessage());
                    importCancelled = true;
                }
            }
            catch (Throwable fatal) {
                fatal.printStackTrace();
                traceError("fatal " + fatal);
                importCancelled = true;
            }

            // finish:
            clear();
            enableImport(true);
            
            if(importCancelled) {
                if(connector.isLocal())
                    messages.appendText("*** rolled back ***\n", null);
                else
                    messages.appendText("*** import not complete ***\n", null);
            }
            else {
                long T1 = System.currentTimeMillis() - startTime;
                messages.appendText("--- done in " + T1 + " ms ---\n", null);
                int speed = (int) (realSize / T1);
                messages.appendText(realCount + " documents, total size: "
                                    + realSize
                                    + ", load speed: " + speed + " Kb/s\n", null);
                // refresh display:
                target.refresh();
            }
        }
        
        void setBar(final double ratio)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    bar.setValue((int) Math.rint(100 * ratio));
                }
            });
        }

        public void importProgress(double size)
        {
            final double done = sizeDone + size;
            
            setBar(done / totalFileSize);

            long t = System.currentTimeMillis() - startTime;
            // "psychological" factor to look optimistic...
            double speed = done / t * 1.05;
            if(t < 1000)   // arbitrary value
                speed = 2000; // Kb/s
            int seconds = (int) ((totalFileSize - done) / 1000 / speed);
            if(seconds > previousEstimation) {
                if((seconds - previousEstimation) / (double) seconds < 0.01)
                    return;
            }
            timeDisplay.setText(seconds + " seconds remaining");
            previousEstimation = seconds;
        }

        public void commitProgress(double fraction)
        {
            setBar(fraction);
        }

        public void backupProgress(double fraction) { }

        public void optimizationProgress(double fraction) { }

        public void reindexingProgress(double fraction) { }
    }
    
    
    static class FileModel extends AbstractTableModel
    {
        ArrayList<FileCollector> fileList = new ArrayList<FileCollector>();
        
        public int getColumnCount()
        {
            return 4;
        }

        public String getColumnName(int column)
        {
            switch(column) {
            case 0:
                return "file or directory";
            case 1:
                return "#files";
            case 2:
                return "total size";
            case 3:
                return "filter";
            }
            return "?";
        }

        public int getRowCount()
        {
            return fileList.size();
        }

        public Object getValueAt(int row, int column)
        {
            switch(column) {
            case 0:
                return getRow(row).getRoot();
            case 1:
                return getRow(row).hasDirRoot() ?
                        Long.toString(getRow(row).getSize()) : null;
            case 2:
                return Long.toString(getRow(row).getByteSize());
            case 3:
                return getRow(row).hasDirRoot() ?
                        getRow(row).getFilter() : null;
            }
            return null;
        }
        
        FileCollector getRow(int index)
        {
            return fileList.get(index);
        }
        
        public void addFile(FileCollector file)
        {
            int row = getRowCount();
            fileList.add(file);
            fireTableRowsInserted(row, row);
        }

        public void removeFile(int index)
        {
            fileList.remove(index);
            fireTableRowsDeleted(index, index);
        }
        
        public void clear()
        {
            fileList.clear();
            fireTableDataChanged();
        }
    }
    
    static class AlignedCellRenderer extends DefaultTableCellRenderer
    {
        int alignment;
        
        public AlignedCellRenderer(int alignment)
        {
            this.alignment = alignment;
        }
        
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column)
        {
            JLabel c = (JLabel) super.getTableCellRendererComponent(
                                                     table, value, isSelected,
                                                     hasFocus, row, column);
            c.setHorizontalAlignment(alignment);
            return c;
        }
        
    }

    public void valueChanged(ListSelectionEvent e)
    {
         delFileButton.setEnabled(fileTable.getSelectedRowCount() > 0);
    }
}
