package com.reverse.gui;

import com.reverse.decompile.Decompilers.Type;

import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter {
    private SyntaxHighlighter() {}


    private static final Color KW = new Color(35, 92, 173);
    private static final Color STR = new Color(163, 110, 38);
    private static final Color NUM = new Color(28, 130, 62);
    private static final Color ANN = new Color(122, 61, 158);
    private static final Color CMT = new Color(120, 120, 120);

    private static final Pattern J_KEY = Pattern.compile("\\b(abstract|assert|boolean|break|byte|case|catch|char|class|continue|default|do|double|else|enum|extends|final|finally|float|for|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)\\b");
    private static final Pattern J_STR = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern J_NUM = Pattern.compile("\\b\\d+(?:_\\d+)*(?:\\.\\d+)?[dDfFlL]?\\b");
    private static final Pattern J_ANN = Pattern.compile("@[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern J_CMT = Pattern.compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern A_OP = Pattern.compile("\\b(ILOAD|ISTORE|ALOAD|ASTORE|FLOAD|FSTORE|DLOAD|DSTORE|LLOAD|LSTORE|ICONST|FCONST|DCONST|LCONST|ACONST|BIPUSH|SIPUSH|LDC|IADD|ISUB|IMUL|IDIV|IREM|FADD|FSUB|FMUL|FDIV|FREM|DADD|DSUB|DMUL|DDIV|DREM|LADD|LSUB|LMUL|LDIV|LREM|INEG|FNEG|DNEG|LNEG|ISHL|ISHR|IUSHR|LSHL|LSHR|LUSHR|IAND|IOR|IXOR|LAND|LOR|LXOR|IINC|I2L|I2F|I2D|L2I|L2F|L2D|F2I|F2L|F2D|D2I|D2L|D2F|I2B|I2C|I2S|LCMP|FCMPL|FCMPG|DCMPL|DCMPG|IFEQ|IFNE|IFLT|IFGE|IFGT|IFLE|IF_ICMPEQ|IF_ICMPNE|IF_ICMPLT|IF_ICMPGE|IF_ICMPGT|IF_ICMPLE|IF_ACMPEQ|IF_ACMPNE|GOTO|JSR|RET|TABLESWITCH|LOOKUPSWITCH|IRETURN|LRETURN|FRETURN|DRETURN|ARETURN|RETURN|GETSTATIC|PUTSTATIC|GETFIELD|PUTFIELD|INVOKEVIRTUAL|INVOKESPECIAL|INVOKESTATIC|INVOKEINTERFACE|INVOKEDYNAMIC|NEW|NEWARRAY|ANEWARRAY|ARRAYLENGTH|ATHROW|CHECKCAST|INSTANCEOF|MONITORENTER|MONITOREXIT|IFNULL|IFNONNULL|MULTIANEWARRAY|NOP|ACONST_NULL|ICONST_M1|ICONST_0|ICONST_1|ICONST_2|ICONST_3|ICONST_4|ICONST_5|LCONST_0|LCONST_1|FCONST_0|FCONST_1|FCONST_2|DCONST_0|DCONST_1|IALOAD|LALOAD|FALOAD|DALOAD|AALOAD|BALOAD|CALOAD|SALOAD|IASTORE|LASTORE|FASTORE|DASTORE|AASTORE|BASTORE|CASTORE|SASTORE|WIDE|BREAKPOINT|IMPDEP1|IMPDEP2)\\b");
    private static final Pattern A_LBL = Pattern.compile("\\b[A-Z_][A-Z0-9_]*:");
    private static final Pattern A_TYP = Pattern.compile("\\b[BCDFIJSZ]|\\bL[^;]+;");
    private static final Pattern A_NUM = Pattern.compile("\\b-?\\d+\\b");
    private static final Pattern A_CMT = Pattern.compile("//.*");

    public static void apply(StyledDocument doc, Type t) {
        try {
            String text = doc.getText(0, doc.getLength());
            clear(doc);
            if (t == Type.ASM) {
                paint(doc, A_OP, KW);
                paint(doc, A_LBL, ANN);
                paint(doc, A_TYP, NUM);
                paint(doc, A_NUM, STR);
                paint(doc, A_CMT, CMT);
            } else {
                paint(doc, J_KEY, KW);
                paint(doc, J_STR, STR);
                paint(doc, J_NUM, NUM);
                paint(doc, J_ANN, ANN);
                paint(doc, J_CMT, CMT);
            }
        } catch (BadLocationException ignored) {}
    }

    private static void clear(StyledDocument doc) {
        var def = new SimpleAttributeSet();
        doc.setCharacterAttributes(0, doc.getLength(), def, true);
    }

    private static void paint(StyledDocument doc, Pattern p, Color c) {
        Style s = doc.getStyle(c.toString());
        if (s == null) {
            s = doc.addStyle(c.toString(), null);
            StyleConstants.setForeground(s, c);
        }
        try {
            Matcher m = p.matcher(doc.getText(0, doc.getLength()));
            while (m.find()) doc.setCharacterAttributes(m.start(), m.end()-m.start(), s, false);
        } catch (BadLocationException ignored) {}
    }
}
