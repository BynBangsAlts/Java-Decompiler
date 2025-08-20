package com.reverse.decompile;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Decompilers {

    public enum Type { VINEFLOWER, CFR, ASM }

    private final File jarFile;

    public Decompilers(File jarFile) { this.jarFile = jarFile; }

    public String decompile(String classPath, Type type) throws Exception {
        File classFile = extractClass(classPath);
        return switch (type) {
            case VINEFLOWER -> vv(classFile);
            case CFR        -> cfr(classFile);
            case ASM        -> asm(classFile);
        };
    }

    private File extractClass(String classPath) throws Exception {
        Path tmp = Files.createTempDirectory("rev");
        File out = new File(tmp.toFile(), classPath);
        out.getParentFile().mkdirs();
        try (JarFile jf = new JarFile(jarFile)) {
            JarEntry e = jf.getJarEntry(classPath);
            if (e == null) throw new FileNotFoundException("Class not in JAR: " + classPath);
            try (InputStream is = jf.getInputStream(e); OutputStream os = new FileOutputStream(out)) {
                is.transferTo(os);
            }
        }
        return out;
    }

    private String vv(File classFile) throws Exception {
        Path outDir = Files.createTempDirectory("vv_out");
        ConsoleDecompiler.main(new String[]{ classFile.getAbsolutePath(), outDir.toString() });
        File java = new File(outDir.toFile(), classFile.getName().replace(".class", ".java"));
        if (!java.exists()) {
            File[] pick = outDir.toFile().listFiles((d,n)->n.endsWith(".java"));
            if (pick != null && pick.length > 0) java = pick[0];
        }
        return java.exists() ? Files.readString(java.toPath()) : "// Vineflower: no output";
    }

    private String cfr(File classFile) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try (PrintStream cap = new PrintStream(baos)) {
            System.setOut(cap);
            org.benf.cfr.reader.Main.main(new String[]{ classFile.getAbsolutePath() });
            System.out.flush();
        } catch (Throwable t) {
            return "// CFR failed: " + t.getMessage();
        } finally { System.setOut(old); }
        return baos.toString();
    }

    private String asm(File classFile) throws Exception {
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        ClassReader r = new ClassReader(bytes);
        StringWriter sw = new StringWriter();
        Textifier t = new Textifier();
        TraceClassVisitor v = new TraceClassVisitor(null, t, new PrintWriter(sw));
        r.accept(v, ClassReader.EXPAND_FRAMES);
        return sw.toString();
    }
}
