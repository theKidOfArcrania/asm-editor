package com.theKidOfArcrania.asm.editor.context;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;

@SuppressWarnings({"JavaDoc", "MagicNumber", "deprecation"})
class MethodBody extends MethodVisitor {
    private static class Action {
        private final int method;
        private final Object[] args;

        public Action(int method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

    private final ArrayList<Action> actions = new ArrayList<>();

    private boolean fixed;

    public MethodBody() {
        super(Opcodes.ASM5);
    }

    //WARNING: THE FOLLOWING LINES IS GENERATED. DO NOT MODIFY!
    public void accept(MethodVisitor visitor) {
        for (Action action : actions) {
            switch (action.method) {
                case 0:
                    visitor.visitEnd();
                    break;
                case 1:
                    visitor.visitLookupSwitchInsn((org.objectweb.asm.Label) action.args[0], (int[]) action.args[1], (org.objectweb.asm.Label[]) action.args[2]);
                    break;
                case 2:
                    visitor.visitInvokeDynamicInsn((java.lang.String) action.args[0], (java.lang.String) action.args[1], (org.objectweb.asm.Handle) action.args[2], (java.lang.Object[]) action.args[3]);
                    break;
                case 3:
                    visitor.visitTableSwitchInsn((int) action.args[0], (int) action.args[1], (org.objectweb.asm.Label) action.args[2], (org.objectweb.asm.Label[]) action.args[3]);
                    break;
                case 4:
                    visitor.visitMultiANewArrayInsn((java.lang.String) action.args[0], (int) action.args[1]);
                    break;
                case 5:
                    visitor.visitLocalVariable((java.lang.String) action.args[0], (java.lang.String) action.args[1], (java.lang.String) action.args[2], (org.objectweb.asm.Label) action.args[3], (org.objectweb.asm.Label) action.args[4], (int) action.args[5]);
                    break;
                case 6:
                    visitor.visitTryCatchBlock((org.objectweb.asm.Label) action.args[0], (org.objectweb.asm.Label) action.args[1], (org.objectweb.asm.Label) action.args[2], (java.lang.String) action.args[3]);
                    break;
                case 7:
                    visitor.visitAttribute((org.objectweb.asm.Attribute) action.args[0]);
                    break;
                case 8:
                    visitor.visitIincInsn((int) action.args[0], (int) action.args[1]);
                    break;
                case 9:
                    visitor.visitLineNumber((int) action.args[0], (org.objectweb.asm.Label) action.args[1]);
                    break;
                case 10:
                    visitor.visitJumpInsn((int) action.args[0], (org.objectweb.asm.Label) action.args[1]);
                    break;
                case 11:
                    visitor.visitFrame((int) action.args[0], (int) action.args[1], (java.lang.Object[]) action.args[2], (int) action.args[3], (java.lang.Object[]) action.args[4]);
                    break;
                case 12:
                    visitor.visitIntInsn((int) action.args[0], (int) action.args[1]);
                    break;
                case 13:
                    visitor.visitLdcInsn(action.args[0]);
                    break;
                case 14:
                    visitor.visitTypeInsn((int) action.args[0], (java.lang.String) action.args[1]);
                    break;
                case 15:
                    visitor.visitVarInsn((int) action.args[0], (int) action.args[1]);
                    break;
                case 16:
                    visitor.visitMethodInsn((int) action.args[0], (java.lang.String) action.args[1], (java.lang.String) action.args[2], (java.lang.String) action.args[3], (boolean) action.args[4]);
                    break;
                case 17:
                    visitor.visitMethodInsn((int) action.args[0], (java.lang.String) action.args[1], (java.lang.String) action.args[2], (java.lang.String) action.args[3]);
                    break;
                case 18:
                    visitor.visitLabel((org.objectweb.asm.Label) action.args[0]);
                    break;
                case 19:
                    visitor.visitInsn((int) action.args[0]);
                    break;
                case 20:
                    visitor.visitFieldInsn((int) action.args[0], (java.lang.String) action.args[1], (java.lang.String) action.args[2], (java.lang.String) action.args[3]);
                    break;
                case 21:
                    visitor.visitCode();
                    break;
                case 22:
                    visitor.visitParameter((java.lang.String) action.args[0], (int) action.args[1]);
                    break;
                case 23:
                    visitor.visitMaxs((int) action.args[0], (int) action.args[1]);
                    break;
            }
        }
    }
    public void visitEnd() {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        fixed = true;
        actions.add(new Action(0, new Object[] {}));
    }
    public void visitLookupSwitchInsn(org.objectweb.asm.Label arg0, int[] arg1, org.objectweb.asm.Label[] arg2) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(1, new Object[] {arg0, arg1, arg2}));
    }
    public void visitInvokeDynamicInsn(java.lang.String arg0, java.lang.String arg1, org.objectweb.asm.Handle arg2, java.lang.Object[] arg3) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(2, new Object[] {arg0, arg1, arg2, arg3}));
    }
    public void visitTableSwitchInsn(int arg0, int arg1, org.objectweb.asm.Label arg2, org.objectweb.asm.Label[] arg3) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(3, new Object[] {arg0, arg1, arg2, arg3}));
    }
    public void visitMultiANewArrayInsn(java.lang.String arg0, int arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(4, new Object[] {arg0, arg1}));
    }
    public void visitLocalVariable(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, org.objectweb.asm.Label arg3, org.objectweb.asm.Label arg4, int arg5) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(5, new Object[] {arg0, arg1, arg2, arg3, arg4, arg5}));
    }
    public void visitTryCatchBlock(org.objectweb.asm.Label arg0, org.objectweb.asm.Label arg1, org.objectweb.asm.Label arg2, java.lang.String arg3) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(6, new Object[] {arg0, arg1, arg2, arg3}));
    }
    public void visitAttribute(org.objectweb.asm.Attribute arg0) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(7, new Object[] {arg0}));
    }
    public void visitIincInsn(int arg0, int arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(8, new Object[] {arg0, arg1}));
    }
    public void visitLineNumber(int arg0, org.objectweb.asm.Label arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(9, new Object[] {arg0, arg1}));
    }
    public void visitJumpInsn(int arg0, org.objectweb.asm.Label arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(10, new Object[] {arg0, arg1}));
    }
    public void visitFrame(int arg0, int arg1, java.lang.Object[] arg2, int arg3, java.lang.Object[] arg4) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(11, new Object[] {arg0, arg1, arg2, arg3, arg4}));
    }
    public void visitIntInsn(int arg0, int arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(12, new Object[] {arg0, arg1}));
    }
    public void visitLdcInsn(java.lang.Object arg0) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(13, new Object[] {arg0}));
    }
    public void visitTypeInsn(int arg0, java.lang.String arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(14, new Object[] {arg0, arg1}));
    }
    public void visitVarInsn(int arg0, int arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(15, new Object[] {arg0, arg1}));
    }
    public void visitMethodInsn(int arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, boolean arg4) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(16, new Object[] {arg0, arg1, arg2, arg3, arg4}));
    }
    public void visitMethodInsn(int arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(17, new Object[] {arg0, arg1, arg2, arg3}));
    }
    public void visitLabel(org.objectweb.asm.Label arg0) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(18, new Object[] {arg0}));
    }
    public void visitInsn(int arg0) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(19, new Object[] {arg0}));
    }
    public void visitFieldInsn(int arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(20, new Object[] {arg0, arg1, arg2, arg3}));
    }
    public void visitCode() {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(21, new Object[] {}));
    }
    public void visitParameter(java.lang.String arg0, int arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(22, new Object[] {arg0, arg1}));
    }
    public void visitMaxs(int arg0, int arg1) {
        if (fixed) {
            throw new IllegalStateException("Cannot modify this method body.");
        }
        actions.add(new Action(23, new Object[] {arg0, arg1}));
    }
}