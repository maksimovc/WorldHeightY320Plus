package dev.thenexusgates.y320plus.patch;

import dev.thenexusgates.y320plus.HeightConfig;
import dev.thenexusgates.y320plus.util.AsmUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static dev.thenexusgates.y320plus.patch.PatchRegistry.*;

/**
 * Generates synthetic methods injected into target classes at {@code visitEnd}.
 * <ul>
 *   <li>{@link #addNormalizeSections} — BlockChunk: resize chunkSections array</li>
 *   <li>{@link #addNormalizeColumnSections} — ChunkColumn: resize Ref[] sections</li>
 *   <li>{@link #addNormalizeSectionHolders} — ChunkColumn: resize Holder[] sectionHolders</li>
 *   <li>{@link #addSafeAddChunkHolder} — ChunkStore: error-recovering add(Holder)</li>
 * </ul>
 */
final class SyntheticMethodGenerator {

    private SyntheticMethodGenerator() {
    }

    // ── BlockChunk: normalizeChunkSections ──────────────────────────────

    static void addNormalizeSections(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            NORMALIZE_SECTIONS, NORMALIZE_SECTIONS_DESC, null, null
        );
        mv.visitCode();

        Label resize = new Label();
        Label afterResize = new Label();
        Label skipCopy = new Label();
        Label loopCheck = new Label();
        Label nextLoop = new Label();
        Label done = new Label();

        // var1 = input (param 0)
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        // if (var1 == null || var1.length != TARGET_SECTIONS) goto resize
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IFNULL, resize);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        AsmUtil.emitIntConstant(mv, HeightConfig.TARGET_SECTIONS);
        mv.visitJumpInsn(Opcodes.IF_ICMPNE, resize);
        mv.visitJumpInsn(Opcodes.GOTO, afterResize);

        // resize: var2 = new BlockSection[TARGET_SECTIONS]
        mv.visitLabel(resize);
        AsmUtil.emitIntConstant(mv, HeightConfig.TARGET_SECTIONS);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, BLOCK_SECTION);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IFNULL, skipCopy);
        // System.arraycopy(var1, 0, var2, 0, min(var1.length, var2.length))
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(II)I", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);

        mv.visitLabel(skipCopy);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        // fill null slots with new BlockSection()
        mv.visitLabel(afterResize);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 2);

        mv.visitLabel(loopCheck);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, done);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitInsn(Opcodes.AALOAD);
        mv.visitJumpInsn(Opcodes.IFNONNULL, nextLoop);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitTypeInsn(Opcodes.NEW, BLOCK_SECTION);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, BLOCK_SECTION, "<init>", "()V", false);
        mv.visitInsn(Opcodes.AASTORE);

        mv.visitLabel(nextLoop);
        mv.visitIincInsn(2, 1);
        mv.visitJumpInsn(Opcodes.GOTO, loopCheck);

        mv.visitLabel(done);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // ── ChunkColumn: normalizeColumnSections ────────────────────────────

    static void addNormalizeColumnSections(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            NORMALIZE_COL_SECTIONS, NORMALIZE_COL_SECTIONS_DESC, null, null
        );
        mv.visitCode();

        Label resize = new Label();
        Label afterResize = new Label();
        Label skipCopy = new Label();

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IFNULL, resize);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        AsmUtil.emitIntConstant(mv, HeightConfig.TARGET_SECTIONS);
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, afterResize);

        mv.visitLabel(resize);
        AsmUtil.emitIntConstant(mv, HeightConfig.TARGET_SECTIONS);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, REF);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IFNULL, skipCopy);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(II)I", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);

        mv.visitLabel(skipCopy);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        mv.visitLabel(afterResize);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // ── ChunkColumn: normalizeSectionHolders ────────────────────────────

    static void addNormalizeSectionHolders(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            NORMALIZE_HOLDERS, NORMALIZE_HOLDERS_DESC, null, null
        );
        mv.visitCode();

        Label resize = new Label();
        Label afterResize = new Label();
        Label skipCopy = new Label();
        Label loopCheck = new Label();
        Label nextLoop = new Label();
        Label done = new Label();

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitJumpInsn(Opcodes.IFNULL, skipCopy);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        AsmUtil.emitIntConstant(mv, HeightConfig.TARGET_SECTIONS);
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, afterResize);

        mv.visitLabel(resize);
        AsmUtil.emitIntConstant(mv, HeightConfig.TARGET_SECTIONS);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, HOLDER);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(II)I", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        mv.visitJumpInsn(Opcodes.GOTO, afterResize);

        mv.visitLabel(skipCopy);
        AsmUtil.emitIntConstant(mv, HeightConfig.TARGET_SECTIONS);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, HOLDER);
        mv.visitVarInsn(Opcodes.ASTORE, 1);

        // fill null slots with REGISTRY.newHolder()
        mv.visitLabel(afterResize);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 2);

        mv.visitLabel(loopCheck);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, done);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitInsn(Opcodes.AALOAD);
        mv.visitJumpInsn(Opcodes.IFNONNULL, nextLoop);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitFieldInsn(Opcodes.GETSTATIC, CHUNK_STORE, "REGISTRY", "Lcom/hypixel/hytale/component/ComponentRegistry;");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hypixel/hytale/component/ComponentRegistry", "newHolder", "()Lcom/hypixel/hytale/component/Holder;", false);
        mv.visitInsn(Opcodes.AASTORE);

        mv.visitLabel(nextLoop);
        mv.visitIincInsn(2, 1);
        mv.visitJumpInsn(Opcodes.GOTO, loopCheck);

        mv.visitLabel(done);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // ── ChunkStore: safeAddChunkHolder ──────────────────────────────────

    static void addSafeAddChunkHolder(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            SAFE_ADD_METHOD, SAFE_ADD_DESC, null, null
        );

        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        Label rethrow = new Label();
        Label rebuild = new Label();

        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/IllegalArgumentException");
        mv.visitCode();

        // try { return store.add(holder); }
        mv.visitLabel(tryStart);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CHUNK_STORE, "add", "(Lcom/hypixel/hytale/component/Holder;)Lcom/hypixel/hytale/component/Ref;", false);
        mv.visitLabel(tryEnd);
        mv.visitInsn(Opcodes.ARETURN);

        // catch (IllegalArgumentException ex)
        mv.visitLabel(catchHandler);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        // if message doesn't match expected, rethrow
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/IllegalArgumentException", "getMessage", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitJumpInsn(Opcodes.IFNULL, rethrow);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitLdcInsn(HOLDER_ARRAY_LENGTH_MSG);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        mv.visitJumpInsn(Opcodes.IFNE, rebuild);

        mv.visitLabel(rethrow);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ATHROW);

        // rebuild holder via serialize→deserialize round-trip
        mv.visitLabel(rebuild);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("[Y320+] Rebuilding malformed chunk holder during load via registry round-trip.");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

        mv.visitFieldInsn(Opcodes.GETSTATIC, CHUNK_STORE, "REGISTRY", "Lcom/hypixel/hytale/component/ComponentRegistry;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hypixel/hytale/component/ComponentRegistry", "serialize", "(Lcom/hypixel/hytale/component/Holder;)Lorg/bson/BsonDocument;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 4);

        mv.visitFieldInsn(Opcodes.GETSTATIC, CHUNK_STORE, "REGISTRY", "Lcom/hypixel/hytale/component/ComponentRegistry;");
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hypixel/hytale/component/ComponentRegistry", "deserialize", "(Lorg/bson/BsonDocument;)Lcom/hypixel/hytale/component/Holder;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 5);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CHUNK_STORE, "add", "(Lcom/hypixel/hytale/component/Holder;)Lcom/hypixel/hytale/component/Ref;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
