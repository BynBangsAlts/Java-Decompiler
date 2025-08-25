package com.reverse.ui;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.reverse.Loader;
import com.reverse.decompile.Decompilers;
import com.reverse.model.ClassEntry;
import com.reverse.settings.Settings;
import com.reverse.settings.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * DecompilerFrame
 * - Shows classes/resources
 */
public class DecompilerFrame extends JFrame {

    /* ---------- Workspace / UI ---------- */
    private final JTree tree;
    private final DefaultTreeModel model;
    private final JTabbedPane editorTabs;
    private final JComboBox<Decompilers.Type> decompPicker;
    private final JTextField search;
    private final JLabel status = new JLabel("Drop a JAR or click Open — I’ll decompile it for you ✨");

    private File jar;
    // bytes to replace in export: entryName -> bytes
    private final Map<String, byte[]> modifiedBytes = new ConcurrentHashMap<>();
    // open editors for .class entries only (key = classPath). Resource tabs tracked separately.
    private final Map<String, EditorTab> openClassEditors = new HashMap<>();
    // open editors for resources: entryName -> ResourceTab
    private final Map<String, ResourceTab> openResourceEditors = new HashMap<>();
    // record jar entry renames (old -> new)
    private final Map<String, String> renames = new HashMap<>();

    public DecompilerFrame() {
        super("JDec");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 840);
        setLocationRelativeTo(null);

        try { FlatMacDarkLaf.setup(); } catch (Exception ignored) {}

        model = new DefaultTreeModel(new DefaultMutableTreeNode("Drop a JAR"));
        tree = new JTree(model);
        tree.setRowHeight(21);
        tree.setCellRenderer(new DefaultTreeCellRenderer()); // customize later if you want icons
        tree.addTreeSelectionListener(this::onPick);
        attachTreePopup();

        JScrollPane treeScroll = new JScrollPane(tree);
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(new TitledBorder("Workspace"));
        left.add(treeScroll, BorderLayout.CENTER);

        editorTabs = new JTabbedPane();
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(new TitledBorder("Editors"));
        right.add(editorTabs, BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        mainSplit.setDividerLocation(320);

        JToolBar top = new JToolBar();
        top.setFloatable(false);
        top.setBorder(new EmptyBorder(8,8,8,8));

        JButton openBtn   = button("Open", e -> openJar());
        JButton saveBtn   = button("Save All", e -> saveAll());
        JButton exportBtn = button("Export", e -> exportJar());
        JButton settingsBtn = button("Settings", e -> {
            new Settings(this).setVisible(true);
            SwingUtilities.updateComponentTreeUI(this);
        });

        decompPicker = new JComboBox<>(Decompilers.Type.values());
        decompPicker.addActionListener(e -> refreshActive());

        search = new JTextField(24);
        search.putClientProperty("JTextField.placeholderText", "Search in active editor");
        search.addActionListener(e -> {
            var et = getActiveSearchable();
            if (et == null) return;
            int hits = et.highlightSearch(search.getText());
            status.setText(hits == 0 ? "No matches" : hits + " match(es)");
        });

        top.add(openBtn); top.add(saveBtn); top.add(exportBtn);
        top.addSeparator();
        top.add(settingsBtn);
        top.addSeparator();
        top.add(new JLabel(""));
        top.add(decompPicker);
        top.add(Box.createHorizontalStrut(16));
        top.add(new JLabel(""));
        top.add(search);

        /* --- Status bar --- */
        status.setBorder(new EmptyBorder(6,8,6,8));
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.add(status, BorderLayout.WEST);

        /* --- Workspace tab --- */
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.add(top, BorderLayout.NORTH);
        workspace.add(mainSplit, BorderLayout.CENTER);
        workspace.add(statusBar, BorderLayout.SOUTH);

        /* --- Main tabs (only one for now) --- */
        JTabbedPane mainTabs = new JTabbedPane();
        mainTabs.addTab("Workspace", workspace);
        setJMenuBar(menuBar());
        add(mainTabs, BorderLayout.CENTER);

        enableDnD();

        // Ctrl+F
        var im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke("control F"), "find");
        am.put("find", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                search.requestFocusInWindow(); search.selectAll();
            }
        });
    }

    private JMenuBar menuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem open = new JMenuItem("Open JAR");
        JMenuItem saveAll = new JMenuItem("Save All");
        JMenuItem export = new JMenuItem("Export");

        open.addActionListener(e -> openJar());
        saveAll.addActionListener(e -> saveAll());
        export.addActionListener(e -> exportJar());

        file.add(open); file.add(saveAll); file.add(export);
        mb.add(file);
        return mb;
    }

    private void enableDnD() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    List<?> files = (List<?>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) loadJar((File) files.get(0));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DecompilerFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void openJar() {
        var fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar"); }
            @Override public String getDescription() { return "JAR (*.jar)"; }
        });
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) loadJar(fc.getSelectedFile());
    }

    private void loadJar(File f) {
        try {
            this.jar = f;

            // Close everything from previous jar
            modifiedBytes.clear();
            renames.clear();
            openClassEditors.clear();
            openResourceEditors.clear();
            editorTabs.removeAll();

            DefaultMutableTreeNode root = new Loader().buildTree(f); // shows all entries
            model.setRoot(root);
            expandAll(tree);

            status.setText("Loaded: " + f.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void expandAll(JTree tree) {
        for (int i=0;i<tree.getRowCount();i++) tree.expandRow(i);
    }

    private void onPick(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
        Object uo = node.getUserObject();

        if (uo instanceof ClassEntry ce) {
            openClassEditor(ce);
            return;
        }

        if (node.isLeaf() && !(uo instanceof ClassEntry)) {
            String entryName = buildEntryName(node); // reconstruct jar path
            openResourceEditor(entryName);
        }
    }

    private String buildEntryName(DefaultMutableTreeNode node) {
        TreeNode[] path = node.getPath();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < path.length; i++) {
            if (sb.length() > 0) sb.append('/');
            sb.append(path[i].toString());
        }
        return sb.toString();
    }

    private void openClassEditor(ClassEntry ce) {
        String key = ce.classPath();
        EditorTab et = openClassEditors.get(key);
        if (et == null) {
            et = new EditorTab(ce);
            openClassEditors.put(key, et);
            editorTabs.addTab(ce.simpleName(), et);
        }
        editorTabs.setSelectedComponent(et);
        et.refreshSource();
    }

    private void openResourceEditor(String entryName) {
        ResourceTab rt = openResourceEditors.get(entryName);
        if (rt == null) {
            String display = entryName.substring(entryName.lastIndexOf('/')+1);
            rt = new ResourceTab(entryName, readEntryText(entryName));
            openResourceEditors.put(entryName, rt);
            editorTabs.addTab(display.isEmpty() ? entryName : display, rt);
        }
        editorTabs.setSelectedComponent(rt);
    }

    private String readEntryText(String entryName) {
        if (jar == null) return "";
        try (JarFile jf = new JarFile(jar)) {
            JarEntry e = jf.getJarEntry(entryName);
            if (e == null) return "";
            try (InputStream is = jf.getInputStream(e)) {
                byte[] b = is.readAllBytes();
                return new String(b, StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            return "// error reading entry: " + ex.getMessage();
        }
    }

    private interface SearchableTab { int highlightSearch(String q); }

    private SearchableTab getActiveSearchable() {
        Component c = editorTabs.getSelectedComponent();
        if (c instanceof SearchableTab s) return s;
        return null;
    }

    private void saveAll() {
        for (var rt : openResourceEditors.values()) {
            String entry = resolveRename(rt.entryName);
            modifiedBytes.put(entry, rt.getText().getBytes(StandardCharsets.UTF_8));
        }

        status.setText("Saved " + modifiedBytes.size() + " resource edits.");
    }

    private String resolveRename(String oldName) {
        return renames.getOrDefault(oldName, oldName);
    }

    private void exportJar() {
        if (jar == null) { JOptionPane.showMessageDialog(this, "Open a JAR first"); return; }
        var fc = new JFileChooser();
        fc.setDialogTitle("Export JAR");
        fc.setSelectedFile(new File(jar.getName().replace(".jar", "_modified.jar")));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File out = fc.getSelectedFile().getName().toLowerCase().endsWith(".jar")
                ? fc.getSelectedFile()
                : new File(fc.getSelectedFile().getAbsolutePath() + ".jar");

        if (out.exists()) {
            int r = JOptionPane.showConfirmDialog(this, "Overwrite " + out.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }

        try {
            Map<String, byte[]> toWrite = new HashMap<>();
            for (var e : modifiedBytes.entrySet()) {
                toWrite.put(resolveRename(e.getKey()), e.getValue());
            }
            new Loader().exportJar(jar, out, toWrite);
            status.setText("Exported: " + out.getName());
            JOptionPane.showMessageDialog(this, "Exported to:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            status.setText("Export failed");
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void attachTreePopup() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem rename = new JMenuItem("Rename…");
        popup.add(rename);
        

        rename.addActionListener(e -> {
            TreePath tp = tree.getSelectionPath();
            if (tp == null) return;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
            if (!node.isLeaf()) { JOptionPane.showMessageDialog(this, "Pick a file to rename."); return; }
            String oldEntry = (node.getUserObject() instanceof ClassEntry ce) ? ce.classPath() : buildEntryName(node);
            String baseName = oldEntry.substring(oldEntry.lastIndexOf('/')+1);
            String newName = JOptionPane.showInputDialog(this, "New name", baseName);
            if (newName == null || newName.isBlank()) return;

            String newEntry = oldEntry.contains("/") ? oldEntry.substring(0, oldEntry.lastIndexOf('/')+1) + newName : newName;
            renames.put(oldEntry, newEntry);
            node.setUserObject(newName);
            model.nodeChanged(node);
            status.setText("Renamed: " + oldEntry + " → " + newName);
        });

        tree.setComponentPopupMenu(popup);
    }

    private final class EditorTab extends JPanel implements SearchableTab {
        final ClassEntry classEntry;
        private final JTabbedPane subTabs;
        private final JTextPane sourcePane, asmPane;
        private final JScrollPane sourceScroll, asmScroll;

        EditorTab(ClassEntry ce) {
            super(new BorderLayout());
            this.classEntry = ce;

            subTabs = new JTabbedPane();

            sourcePane = new JTextPane(new DefaultStyledDocument());
            sourcePane.setFont(new Font(Theme.editorFont(), Font.PLAIN, Theme.editorSize()));
            sourceScroll = new JScrollPane(sourcePane);

            asmPane = new JTextPane(new DefaultStyledDocument());
            asmPane.setFont(new Font(Theme.editorFont(), Font.PLAIN, Theme.editorSize()));
            asmPane.setEditable(true); // editable per your request
            asmScroll = new JScrollPane(asmPane);

            subTabs.addTab("Source", sourceScroll);
            subTabs.addTab("Assembly Viewer", asmScroll);
            add(subTabs, BorderLayout.CENTER);
        }

        void refreshSource() {
            if (jar == null) return;
            String key = classEntry.classPath();
            Decompilers.Type t = (Decompilers.Type) decompPicker.getSelectedItem();

            // Source
            new SwingWorker<String,Void>() {
                @Override protected String doInBackground() {
                    try { return new Decompilers(jar).decompile(key, t); }
                    catch (Exception ex) { return "// error: " + ex.getMessage(); }
                }
                @Override protected void done() {
                    try { setSourceText(get()); status.setText("Done: " + classEntry.simpleName()); }
                    catch (Exception ignored) {}
                }
            }.execute();

            // ASM
            new SwingWorker<String,Void>() {
                @Override protected String doInBackground() {
                    return new Decompilers(jar).asmText(key);
                }
                @Override protected void done() {
                    try { setAsmText(get()); } catch (Exception ignored) {}
                }
            }.execute();
        }

        String getSourceText() { return sourcePane.getText(); }
        String getAsmText()    { return asmPane.getText(); }

        void setSourceText(String s) {
            try {
                StyledDocument doc = new DefaultStyledDocument();
                doc.insertString(0, s != null ? s : "", null);
                sourcePane.setDocument(doc);
                SyntaxHighlighter.applyJava(doc);
            } catch (Exception ignored) {}
        }

        void setAsmText(String s) {
            try {
                StyledDocument doc = new DefaultStyledDocument();
                doc.insertString(0, s != null ? s : "", null);
                asmPane.setDocument(doc);
                SyntaxHighlighter.applyAsm(doc);
            } catch (Exception ignored) {}
        }

        @Override public int highlightSearch(String query) {
            if (query == null) query = "";
            Component active = subTabs.getSelectedComponent();
            if (active == sourceScroll) {
                return SearchHighlighter.highlight(sourcePane, query);
            } else {
                return SearchHighlighter.highlight(asmPane, query);
            }
        }
    }

    private final class ResourceTab extends JPanel implements SearchableTab {
        final String entryName;
        final JTextPane textPane;

        ResourceTab(String entryName, String text) {
            super(new BorderLayout());
            this.entryName = entryName;
            this.textPane = new JTextPane(new DefaultStyledDocument());
            try { this.textPane.getDocument().insertString(0, text != null ? text : "", null); } catch(Exception ignored){}
            this.textPane.setFont(new Font(Theme.editorFont(), Font.PLAIN, Theme.editorSize()));
            add(new JScrollPane(textPane), BorderLayout.CENTER);
        }

        String getText() { return textPane.getText(); }

        @Override public int highlightSearch(String query) {
            return SearchHighlighter.highlight(textPane, query == null ? "" : query);
        }
    }

    private JButton button(String text, java.awt.event.ActionListener a) {
        JButton b = new JButton(text); b.addActionListener(a); return b;
    }

    private void refreshActive() {
        Component c = editorTabs.getSelectedComponent();
        if (c instanceof EditorTab et) et.refreshSource();
    }
}
