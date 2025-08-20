package com.reverse.gui;

import com.reverse.model.ClassEntry;
import com.reverse.decompile.Decompilers;
import com.reverse.Loader;
import com.reverse.settings.Settings;
import com.reverse.settings.Settings;
import com.reverse.settings.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DecompilerFrame extends JFrame {

    private final JTree tree;
    private final DefaultTreeModel model;
    private final JTextPane code;
    private final JScrollPane codeScroll;
    private final JComboBox<Decompilers.Type> picker;
    private final JLabel status;
    private JTextField search; // not final (fixes “cannot assign to final”)

    private LineNumberGutter gutter;

    private File jar;
    private final Map<String,String> edits = new ConcurrentHashMap<>();
    private ClassEntry current;

    public DecompilerFrame() {
        super("JDec");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 740);
        setLocationRelativeTo(null);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(new TitledBorder("Classes"));
        model = new DefaultTreeModel(new DefaultMutableTreeNode("Drop a JAR"));
        tree = new JTree(model);
        tree.setRowHeight(22);
        tree.addTreeSelectionListener(this::onPick);
        var leftScroll = new JScrollPane(tree);
        leftScroll.setBorder(new EmptyBorder(0,0,0,0));
        left.add(leftScroll, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(new TitledBorder("Code"));

        StyledDocument doc = new DefaultStyledDocument();
        code = new JTextPane();
        code.setDocument(doc);
        applyEditorLook();

        codeScroll = new JScrollPane(code);
        codeScroll.setBorder(new EmptyBorder(0,0,0,0));
        gutter = Theme.showLineNumbers() ? new LineNumberGutter(code) : null;
        codeScroll.setRowHeaderView(gutter);

        right.add(toolbar(), BorderLayout.NORTH);
        right.add(codeScroll, BorderLayout.CENTER);
        right.add(editorFooter(), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(320);
        split.setBorder(new EmptyBorder(0,0,0,0));

        picker = new JComboBox<>(Decompilers.Type.values());
        picker.addActionListener(e -> refresh());
        status = new JLabel("Let’s work.");
        status.setBorder(new EmptyBorder(0,8,0,8));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(10,10,10,10));
        var rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBox.add(new JLabel("Decompiler:"));
        rightBox.add(picker);
        bottom.add(status, BorderLayout.WEST);
        bottom.add(rightBox, BorderLayout.EAST);

        setJMenuBar(menu());
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        enableDnD();
        setupShortcuts();
    }

    private void applyEditorLook() {
        code.setFont(new Font(Theme.editorFont(), Font.PLAIN, Theme.editorSize()));
        code.putClientProperty("JTextPane.placeholderText", "Open a class to view code");
        code.setMargin(Theme.zenMode() ? new Insets(16,16,16,16) : new Insets(8,8,8,8));
    }

    private JToolBar toolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new EmptyBorder(10,10,10,10));

        JButton open = button("Open", e -> openJar());
        JButton save = button("Save", e -> saveCurrent());
        JButton export = button("Export", e -> exportJar());
        JButton settingsBtn = button("Settings", e -> {
            new Settings(this).setVisible(true);
            applyEditorLook();
            if (Theme.showLineNumbers()) {
                if (gutter == null) gutter = new LineNumberGutter(code);
                codeScroll.setRowHeaderView(gutter);
            } else {
                codeScroll.setRowHeaderView(null);
                gutter = null;
            }
        });

        search = new JTextField(20);
        search.putClientProperty("JTextField.placeholderText", "Search in file");
        search.addActionListener(e -> {
            int hits = SearchHighlighter.highlight(code, search.getText());
            status.setText(hits == 0 ? "No matches" : hits + " matches");
        });

        tb.add(open); tb.add(save); tb.add(export);
        tb.addSeparator();
        tb.add(settingsBtn);
        tb.add(Box.createHorizontalStrut(12));
        tb.add(search);
        return tb;
    }

    private JPanel editorFooter() {
        var bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        var tip = new JLabel("Tip: Ctrl+F to find, Ctrl+S to save. You’ve got this.");
        tip.setForeground(UIManager.getColor("Label.disabledForeground"));
        bar.add(tip);
        return bar;
    }

    private JButton button(String text, java.awt.event.ActionListener a) {
        JButton b = new JButton(text);
        b.addActionListener(a);
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JMenuBar menu() {
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenu view = new JMenu("View");

        var open = new JMenuItem("Open JAR"); open.addActionListener(e -> openJar());
        var saveAll = new JMenuItem("Save All"); saveAll.addActionListener(e -> { saveCurrent(); status.setText("Saved " + edits.size() + " classes"); });
        var exp = new JMenuItem("Export JAR"); exp.addActionListener(e -> exportJar());
        var quit = new JMenuItem("Exit"); quit.addActionListener(e -> System.exit(0));

        var settings = new JMenuItem("Settings…");
        settings.addActionListener(e -> {
            new Settings(this).setVisible(true);
            applyEditorLook();
            if (Theme.showLineNumbers()) {
                if (gutter == null) gutter = new LineNumberGutter(code);
                codeScroll.setRowHeaderView(gutter);
            } else {
                codeScroll.setRowHeaderView(null);
                gutter = null;
            }
        });

        file.add(open); file.add(saveAll); file.add(exp); file.addSeparator(); file.add(quit);
        view.add(settings);
        mb.add(file); mb.add(view);
        return mb;
    }

    private void setupShortcuts() {
        var im = code.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var am = code.getActionMap();

        im.put(KeyStroke.getKeyStroke("control S"), "save");
        am.put("save", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { saveCurrent(); } });

        im.put(KeyStroke.getKeyStroke("control F"), "focusFind");
        am.put("focusFind", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { search.requestFocusInWindow(); search.selectAll(); } });
    }

    private void enableDnD() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    List<?> files = (List<?>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File f = (File) files.get(0);
                        if (f.getName().toLowerCase().endsWith(".jar")) loadJar(f);
                        else JOptionPane.showMessageDialog(DecompilerFrame.this, "Drop a .jar file");
                    }
                    e.dropComplete(true);
                } catch (Exception ex) {
                    e.dropComplete(false);
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
            this.edits.clear();
            DefaultMutableTreeNode root = new Loader().buildTree(f);
            model.setRoot(root);
            for (int i=0;i<tree.getRowCount();i++) tree.expandRow(i);
            status.setText("Loaded: " + f.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onPick(TreeSelectionEvent e) {
        var node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
        var obj = node.getUserObject();
        if (obj instanceof ClassEntry ce) { current = ce; refresh(); }
    }

    private void refresh() {
        if (current == null || jar == null) return;
        var type = (Decompilers.Type) picker.getSelectedItem();
        String key = current.classPath();
        if (edits.containsKey(key)) {
            setCode(edits.get(key), type);
            status.setText("Edited: " + current.simpleName());
            return;
        }
        setCode("// Decompiling " + current.simpleName() + " via " + type + "...", type);
        new SwingWorker<String,Void>() {
            @Override protected String doInBackground() {
                try { return new Decompilers(jar).decompile(key, type); }
                catch (Exception ex) { return "// error: " + ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String text = get();
                    setCode(text, type);
                    status.setText("Done");
                } catch (Exception ex) {
                    setCode("// error: " + ex.getMessage(), type);
                    status.setText("Failed");
                }
            }
        }.execute();
    }

    private void setCode(String s, Decompilers.Type t) {
        try {
            StyledDocument doc = code.getDocument() instanceof StyledDocument sd ? sd : new DefaultStyledDocument();
            if (doc.getLength()>0) doc.remove(0, doc.getLength());
            doc.insertString(0, s, null);
            code.setDocument(doc);
            SyntaxHighlighter.apply(doc, t);
            SearchHighlighter.highlight(code, search.getText());
        } catch (Exception ignored) {}
    }

    private void saveCurrent() {
        if (current == null) { status.setText("Nothing selected"); return; }
        edits.put(current.classPath(), code.getText());
        status.setText("Saved: " + current.simpleName());
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
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() {
                status.setText("Exporting...");
                try {
                    new Loader().exportJar(jar, out, edits);
                    status.setText("Exported: " + out.getName());
                    JOptionPane.showMessageDialog(DecompilerFrame.this, "Exported to:\n" + out.getAbsolutePath());
                } catch (Exception ex) {
                    status.setText("Export failed");
                    JOptionPane.showMessageDialog(DecompilerFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }
        }.execute();
    }
}
