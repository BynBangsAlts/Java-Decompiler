package com.reverse;

import com.reverse.model.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.*;
import java.util.zip.ZipEntry;

/**
 * Loader: builds the tree view and exports JARs.
 * - Shows ALL files (not only .class).
 */
public class Loader { //The Save All Is Broken ILL fix it ALSO LATER


    public DefaultMutableTreeNode buildTree(File jar) throws IOException {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(jar.getName());
        try (JarFile jf = new JarFile(jar)) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                add(root, e.getName(), e.isDirectory());
            }
        }
        return root;
    }

    private void add(DefaultMutableTreeNode root, String path, boolean dir) {
        String[] parts = path.split("/");
        DefaultMutableTreeNode cur = root;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            boolean leaf = (i == parts.length - 1);

            DefaultMutableTreeNode found = null;
            for (int j = 0; j < cur.getChildCount(); j++) {
                DefaultMutableTreeNode ch = (DefaultMutableTreeNode) cur.getChildAt(j);
                if (ch.getUserObject().toString().equals(p)) {
                    found = ch;
                    break;
                }
            }

            if (found == null) {
                Object uo;
                if (leaf && path.endsWith(".class")) {
                    uo = new ClassEntry(path, p, false, false, false);
                } else {
                    uo = p;
                }
                found = new DefaultMutableTreeNode(uo);
                cur.add(found);
            }
            cur = found;
        }
    }


    public void exportJar(File baseJar, File out, Map<String, byte[]> edits) throws IOException {
        try (JarFile jf = new JarFile(baseJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {

            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                jos.putNextEntry(new ZipEntry(e.getName()));
                try (InputStream is = jf.getInputStream(e)) {
                    is.transferTo(jos);
                }
                jos.closeEntry();
            }
        }
    }
}
