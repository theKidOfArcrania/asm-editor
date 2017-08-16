package com.theKidOfArcrania.asm.editor.context;

import java.util.*;

import com.theKidOfArcrania.asm.editor.code.parsing.CodeSymbols;
import com.theKidOfArcrania.asm.editor.code.parsing.inst.InstOpcodes;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * This loads a method body into assembly code.
 * @author Henry Wang
 */
class MethodBodyLoader extends MethodVisitor {
    private static final int ASCII_START = 0x20;
    private static final int ASCII_END = 0x7E;
    private static final int HEX_END = 0xFF;

    private final ArrayList<String> lines;
    private final HashMap<Label, String> labels;
    private final CodeSymbols handles;
    private int nextLabel;

    /**
     * Constructs a new method body loader.
     * @param global the global code symbols
     */
    public MethodBodyLoader(CodeSymbols global) {
        super(Opcodes.ASM5);
        this.handles = new CodeSymbols(global, global.getThisContext());

        lines = new ArrayList<>();
        labels = new HashMap<>();

        nextLabel = 0;
    }

    /**
     * Obtains the current code that we have.
     * @return the code currently accumulated.
     */
    public String toCode()
    {
        return String.join("\n", lines);
    }

    @Override
    public void visitLabel(Label label)
    {
        lines.add(labelToStr(label) + ":");
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack)
    {
        //TODO
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
    {
        //TODO
        return null;
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
    {
        //TODO
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
    {
        //TODO
        return null;
    }

    @Override
    public void visitInsn(int opcode)
    {
        code(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand)
    {
        code(opcode, Integer.toString(operand));
    }

    @Override
    public void visitVarInsn(int opcode, int var)
    {
        code(opcode, Integer.toString(var));
    }

    @Override
    public void visitTypeInsn(int opcode, String type)
    {
        code(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc)
    {
        code(opcode, owner, name, "@" + desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
    {
        code(opcode, owner, name, "@" + desc);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs)
    {
        String[] args = new String[bsmArgs.length + 2];
        args[0] = name;
        args[1] = "@" + desc;
        args[2] = handleToStr(bsm);
        for (int i = 0; i < bsmArgs.length; i++)
            args[i + 3] = argToCode(bsmArgs[i]);
        code(INVOKEDYNAMIC, args);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label)
    {
        code(opcode, labelToStr(label));
    }

    @Override
    public void visitLdcInsn(Object cst)
    {
        code(LDC, argToCode(cst));
    }

    @Override
    public void visitIincInsn(int var, int increment)
    {
        code(IINC, Integer.toString(var), Integer.toString(var));
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels)
    {
        String[] args = new String[labels.length + 3];
        args[0] = Integer.toString(min);
        args[1] = Integer.toString(max);
        args[2] = labelToStr(dflt);
        for (int i = 0; i < labels.length; i++)
            args[i + 3] = labelToStr(labels[i]);
        code(TABLESWITCH, args);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels)
    {
        String[] args = new String[keys.length * 2 + 1];
        args[0] = labelToStr(dflt);
        for (int i = 0; i < keys.length; i++)
        {
            args[i * 2 + 1] = Integer.toString(keys[i]);
            args[i * 2 + 2] = labelToStr(labels[i]);
        }
        code(LOOKUPSWITCH, args);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims)
    {
        code(MULTIANEWARRAY, desc, Integer.toString(dims));
    }

    /**
     * Writes a line of code with an opcode and a series of arguments.
     * @param opcode the opcode of instruction
     * @param args the codified arguments (if any).
     */
    private void code(int opcode, String... args)
    {
        StringBuilder sb = new StringBuilder();
        String name = InstOpcodes.fromNumber(opcode).getInstName();

        sb.append("  ").append(name);

        boolean first = true;
        for (String arg : args)
        {
            sb.append(first ? " " : ", ").append(arg);
            first = false;
        }

        lines.add(sb.toString());
    }

    /**
     * Converts this argument into the respective code.
     * @param arg the argument.
     * @return the string code form.
     */
    private String argToCode(Object arg)
    {
        String prefix = "";
        String suffix = "";
        if (arg instanceof Integer)
            prefix = "";
        else if (arg instanceof Float)
            suffix = "F";
        else if (arg instanceof Long)
            suffix = "L";
        else if (arg instanceof Double)
            suffix = "D";
        else if (arg instanceof String)
            return quoteStr((String) arg);
        else if (arg instanceof Type)
            prefix = "@";
        else if (arg instanceof Handle)
            return "&" + handleToStr((Handle)arg);
        else
            return null;
        return prefix + arg + suffix;
    }

    /**
     * Ensures that the following text is escaped so that it can fit within the quotations.
     * @param text the string text
     * @return an escaped string surrounded by quotes.
     */
    private String quoteStr(String text)
    {
        StringBuilder sb = new StringBuilder(text.length() + 2);
        sb.append('"');
        for (char c : text.toCharArray())
        {
            switch (c)
            {
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\000': sb.append("\\0"); break;
                case '"': sb.append("\\\""); break;
                default:
                    if (c < ASCII_START || c > ASCII_END)
                    {
                        if (c > HEX_END)
                            sb.append(String.format("\\u%04x", (int)c));
                        else
                            sb.append(String.format("\\x%02x", (int)c));
                    }
                    else
                        sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /**
     * Converts a label into a string form.
     * @param lbl the label in code.
     * @return the string form
     */
    private String labelToStr(Label lbl)
    {
        return labels.computeIfAbsent(lbl, key -> "L" + (nextLabel++));
    }

    /**
     * Converts a handle into a string form.
     * @param handle the handle in code.
     * @return the string form
     */
    private String handleToStr(Handle handle)
    {
        String name = handles.getHandleName(handle);
        if (name == null)
        {
            int num = 0;
            do
            {
                name = "H" + handle.getName() + (num == 0 ? "" : num);
                num++;
            }
            while (handles.getHandle(name) != null);
            handles.addHandle(name, handle);
            //TODO: add handles.
        }
        return name;
    }


}