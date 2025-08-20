package com.reverse;

import com.reverse.model.ClassEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.*;
import java.util.zip.ZipEntry;

public class Loader {

    public DefaultMutableTreeNode buildTree(File jar) throws IOException {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(jar.getName());
        try (JarFile jf = new JarFile(jar)) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                if (e.getName().endsWith(".class")) add(root, e.getName());
            }
        }
        return root;
    }

    private void add(DefaultMutableTreeNode root, String classPath) {
        String[] parts = classPath.split("/");
        DefaultMutableTreeNode cur = root;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            boolean leaf = i == parts.length - 1;
            String key = leaf ? p.substring(0, p.length() - 6) : p;

            DefaultMutableTreeNode found = null;
            for (int j = 0; j < cur.getChildCount(); j++) {
                DefaultMutableTreeNode ch = (DefaultMutableTreeNode) cur.getChildAt(j);
                if (ch.getUserObject().toString().equals(key)) { found = ch; break; }
            }
            if (found == null) {
                found = new DefaultMutableTreeNode(leaf ? new ClassEntry(classPath, key) : key);
                cur.add(found);
            }
            cur = found;
        }
    }

    public void exportJar(File baseJar, File out, Map<String,String> modified) throws IOException {
        try (JarFile jf = new JarFile(baseJar); JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                if (e.getName().endsWith(".class") && modified.containsKey(e.getName())) continue;
                jos.putNextEntry(new ZipEntry(e.getName()));
                try (InputStream is = jf.getInputStream(e)) { is.transferTo(jos); }
                jos.closeEntry();
            }
            for (var kv : modified.entrySet()) {
                String classPath = kv.getKey();
                String srcName = classPath.replace(".class", ".java");
                jos.putNextEntry(new ZipEntry(srcName));
                jos.write(kv.getValue().getBytes());
                jos.closeEntry();
            }
        }
    }
}
