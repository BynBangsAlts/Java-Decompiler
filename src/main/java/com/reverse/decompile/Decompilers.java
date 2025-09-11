package com.reverse.decompile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

import java.io.*;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 *  - FernFlower
 *  - Procyon
 *  - CFR
 *  - ASM (Not Really An Decompiler)
 *
 */
public class Decompilers {

    public enum Type { FERNFLOWER, PROCYON, CFR}

    private final File jarFile;

    public Decompilers(File jarFile) { this.jarFile = jarFile; }

    /** Decompile Logic */
    public String decompile(String classPath, Type preferred) throws Exception {
        Extracted ex = extractClassWithRoot(classPath);
        Set<Object> order = orderedWithFallback(preferred);
        Exception lastErr = null;

        for (Object token : order) {
            try {
                if (token == Type.FERNFLOWER) return fernflower(ex.classFile);
                else if (token == Type.PROCYON) return procyon(ex.rootDir, ex.classPath);
                else if (token == Type.CFR) return cfr(ex.classFile);
                else if ("SOON".equals(token)) {
                } else if ("SOON".equals(token)) {
                }
            } catch (Throwable err) {
                lastErr = (err instanceof Exception) ? (Exception) err : new Exception(err);
            }
        }
        return "// All decompilers failed: " +
                (lastErr != null ? lastErr.getMessage() : "unknown");
    }

    /** ASM bytecode dump **/
    public String asmText(String classPath) {
        try {
            Extracted ex = extractClassWithRoot(classPath);
            byte[] bytes = Files.readAllBytes(ex.classFile.toPath());
            ClassReader r = new ClassReader(bytes);
            StringWriter sw = new StringWriter();
            Textifier t = new Textifier();
            TraceClassVisitor v = new TraceClassVisitor(null, t, new PrintWriter(sw));
            r.accept(v, ClassReader.EXPAND_FRAMES);
            return sw.toString();
        } catch (Exception e) {
            return "// ASM view failed: " + e.getMessage();
        }
    }


    private record Extracted(File classFile, File rootDir, String classPath) {}

    private Extracted extractClassWithRoot(String classPath) throws Exception {
        Path tmpRoot = Files.createTempDirectory("rev");
        File out = new File(tmpRoot.toFile(), classPath);
        out.getParentFile().mkdirs();
        try (JarFile jf = new JarFile(jarFile)) {
            JarEntry e = jf.getJarEntry(classPath);
            if (e == null) throw new FileNotFoundException("Class not in JAR: " + classPath);
            try (InputStream is = jf.getInputStream(e);
                 OutputStream os = new FileOutputStream(out)) {
                is.transferTo(os);
            }
        }
        return new Extracted(out, tmpRoot.toFile(), classPath);
    }

    private Set<Object> orderedWithFallback(Type preferred) {
        LinkedHashSet<Object> order = new LinkedHashSet<>();
        if (preferred != null) order.add(preferred);
        order.add(Type.FERNFLOWER);
        order.add(Type.PROCYON);
        order.add(Type.CFR);
        return order;
    }

    private String fernflower(File classFile) throws Exception {
        Path outDir = Files.createTempDirectory("fern_out");
        org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler.main(
                new String[]{classFile.getAbsolutePath(), outDir.toString()});
        File java = new File(outDir.toFile(), classFile.getName().replace(".class", ".java"));
        if (!java.exists()) {
            File[] pick = outDir.toFile().listFiles((d, n) -> n.endsWith(".java"));
            if (pick != null && pick.length > 0) java = pick[0];
        }
        return java.exists() ? Files.readString(java.toPath()) : "// FernFlower: no output";
    }

    private String procyon(File rootDir, String classPath) throws Exception {
        String typeName = classPath.replace('/', '.')
                .replace('\\', '.')
                .replaceAll("\\.class$", "");

        java.net.URL[] urls = {rootDir.toURI().toURL()};
        try (java.net.URLClassLoader isolated = new java.net.URLClassLoader(urls, null)) {
            DecompilerSettings settings = DecompilerSettings.javaDefaults();
            settings.setTypeLoader(new IsolatedTypeLoader(isolated));

            StringWriter sw = new StringWriter();
            PlainTextOutput out = new PlainTextOutput(sw);
            Decompiler.decompile(typeName, out, settings);
            return sw.toString();
        }
    }

    private String cfr(File classFile) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try (PrintStream cap = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
            System.setOut(cap);
            org.benf.cfr.reader.Main.main(new String[]{classFile.getAbsolutePath()});
            System.out.flush();
        } catch (Throwable t) {
            return "// CFR failed: " + t.getMessage();
        } finally { System.setOut(old); }
        return baos.toString(StandardCharsets.UTF_8);
    }

    static final class IsolatedTypeLoader implements ITypeLoader {

        private final URLClassLoader cl;

        IsolatedTypeLoader(URLClassLoader cl) {
            this.cl = cl;
        }

        @Override
        public boolean tryLoadType(String internalName, Buffer buffer) {
            String resource = internalName.replace('.', '/') + ".class";
            try (InputStream in = cl.getResourceAsStream(resource)) {
                if (in == null) return false;
                byte[] b = in.readAllBytes();
                buffer.reset(b.length);
                buffer.putByteArray(b, 0, b.length);
                buffer.position(0);
                return true;
            } catch (IOException ignored) {
                return false;
            }
        }
    }
}
