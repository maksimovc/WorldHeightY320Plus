package dev.thenexusgates.y320plus.util;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class AsmUtil {

    private AsmUtil() {
    }

    public static void emitIntConstant(MethodVisitor mv, int value) {
        switch (value) {
            case -1 -> mv.visitInsn(Opcodes.ICONST_M1);
            case 0 -> mv.visitInsn(Opcodes.ICONST_0);
            case 1 -> mv.visitInsn(Opcodes.ICONST_1);
            case 2 -> mv.visitInsn(Opcodes.ICONST_2);
            case 3 -> mv.visitInsn(Opcodes.ICONST_3);
            case 4 -> mv.visitInsn(Opcodes.ICONST_4);
            case 5 -> mv.visitInsn(Opcodes.ICONST_5);
            default -> {
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.BIPUSH, value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.SIPUSH, value);
                } else {
                    mv.visitLdcInsn(value);
                }
            }
        }
    }
}
