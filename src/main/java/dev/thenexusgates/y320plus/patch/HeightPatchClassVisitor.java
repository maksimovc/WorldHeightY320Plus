package dev.thenexusgates.y320plus.patch;

import dev.thenexusgates.y320plus.HeightConfig;
import dev.thenexusgates.y320plus.util.AsmUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Objects;
import java.util.Set;

import static dev.thenexusgates.y320plus.patch.PatchFlag.*;
import static dev.thenexusgates.y320plus.patch.PatchRegistry.*;

public final class HeightPatchClassVisitor extends ClassVisitor {

    private final String className;
    private final Set<PatchFlag> flags;
    private boolean changed;

    public HeightPatchClassVisitor(ClassVisitor delegate, String className, Set<PatchFlag> flags) {
        super(Opcodes.ASM9, delegate);
        this.className = className;
        this.flags = flags;
    }

    public boolean isChanged() {
        return changed;
    }

    // ── Field patching ──────────────────────────────────────────────────

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        int newAccess = access;
        Object newValue = value;

        if (flags.contains(CHUNK_UTIL_FIELDS) && CHUNK_UTIL.equals(className)) {
            newValue = switch (name) {
                case "HEIGHT"          -> HeightConfig.TARGET_HEIGHT;
                case "HEIGHT_MINUS_1"  -> HeightConfig.TARGET_HEIGHT_M1;
                case "HEIGHT_SECTIONS" -> HeightConfig.TARGET_SECTIONS;
                case "SIZE_BLOCKS_COLUMN" -> HeightConfig.TARGET_SIZE_BLOCKS_COLUMN;
                default -> newValue;
            };
        }

        if (flags.contains(REMOVE_FINAL) && CHUNK_COLUMN.equals(className)
                && "sections".equals(name)
                && REF_ARRAY_DESC.equals(descriptor)
                && (newAccess & Opcodes.ACC_FINAL) != 0) {
            newAccess &= ~Opcodes.ACC_FINAL;
        }

        if (newAccess != access || !Objects.equals(newValue, value)) {
            changed = true;
        }

        return super.visitField(newAccess, name, descriptor, signature, newValue);
    }

    // ── Method patching ─────────────────────────────────────────────────

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);

        boolean normBlockEntry = flags.contains(NORM_BLOCK_CHUNK) && needsBlockChunkNormOnEntry(name);
        boolean normColEntry = flags.contains(NORM_CHUNK_COLUMN) && needsColumnNormOnEntry(access, name);
        boolean normColExit = flags.contains(NORM_CHUNK_COLUMN) && "<init>".equals(name);
        boolean normHoldersExit = flags.contains(NORM_CHUNK_COLUMN) && needsHolderNormOnExit(name, descriptor);
        boolean blockChunkInitReturn = flags.contains(NORM_BLOCK_CHUNK)
                && BLOCK_CHUNK.equals(className)
                && "<init>".equals(name)
                && BLOCK_CHUNK_4ARG_CTOR_DESC.equals(descriptor);

        return new MethodVisitor(api, delegate) {
            private boolean trackArrayLength;
            private boolean patchProbeMoveGroundMask;

            @Override
            public void visitCode() {
                super.visitCode();
                if (normBlockEntry) {
                    changed = true;
                    emitBlockChunkNorm(this);
                }
                if (normColEntry) {
                    changed = true;
                    emitColumnSectionNorm(this, 0);
                }
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                super.visitFieldInsn(opcode, owner, fieldName, fieldDescriptor);

                // Track GETFIELD BlockChunk.chunkSections for block packet clamp
                if (flags.contains(CLAMP_BLOCK_PACKET) && BLOCK_CHUNK_LOAD.equals(className)
                        && "fetch".equals(name)
                        && opcode == Opcodes.GETFIELD
                        && BLOCK_CHUNK.equals(owner)
                        && "chunkSections".equals(fieldName)
                        && BLOCK_SECTION_ARRAY_DESC.equals(fieldDescriptor)) {
                    trackArrayLength = true;
                }
            }

            @Override
            public void visitInsn(int opcode) {
                // Clamp array length after tracked GETFIELD / getSections
                if (trackArrayLength && opcode == Opcodes.ARRAYLENGTH) {
                    changed = true;
                    super.visitInsn(opcode);
                    AsmUtil.emitIntConstant(this, HeightConfig.TARGET_SECTIONS);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(II)I", false);
                    trackArrayLength = false;
                    return;
                }
                trackArrayLength = false;

                // probeMove bitmask: don't consume flag on ICONST_1
                if (patchProbeMoveGroundMask && opcode != Opcodes.ICONST_1) {
                    patchProbeMoveGroundMask = false;
                }

                // BlockChunk 4-arg ctor: normalize before RETURN
                if (blockChunkInitReturn && opcode == Opcodes.RETURN) {
                    changed = true;
                    emitBlockChunkNorm(this);
                }

                // ChunkColumn <init>: normalize sections before RETURN
                if (normColExit && opcode == Opcodes.RETURN) {
                    changed = true;
                    emitColumnSectionNorm(this, 0);
                }

                // ChunkColumn: normalize holders before RETURN
                if (normHoldersExit && opcode == Opcodes.RETURN) {
                    changed = true;
                    emitHolderNorm(this);
                }

                super.visitInsn(opcode);
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                Integer replacement = replaceInt(name, opcode, operand);
                if (replacement != null) {
                    changed = true;
                    AsmUtil.emitIntConstant(this, replacement);
                    return;
                }
                super.visitIntInsn(opcode, operand);
            }

            @Override
            public void visitLdcInsn(Object value) {
                Object replacement = replaceLdc(value);
                if (replacement != null) {
                    changed = true;
                    if (replacement instanceof Integer intVal) {
                        AsmUtil.emitIntConstant(this, intVal);
                    } else {
                        super.visitLdcInsn(replacement);
                    }
                    return;
                }
                super.visitLdcInsn(value);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                // FluidSystems$LoadPacketGenerator: track getSections for fluid clamp
                if (flags.contains(CLAMP_FLUID_PACKET) && FLUID_LOAD.equals(className)
                        && "fetch".equals(name)
                        && opcode == Opcodes.INVOKEVIRTUAL
                        && CHUNK_COLUMN.equals(owner)
                        && "getSections".equals(methodName)
                        && ("()[L" + REF + ";").equals(methodDescriptor)) {
                    super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    trackArrayLength = true;
                    return;
                }

                // ChunkStore.postLoadChunk: redirect add → safeAdd
                if (flags.contains(SAFE_ADD) && CHUNK_STORE.equals(className)
                        && "postLoadChunk".equals(name)
                        && opcode == Opcodes.INVOKEVIRTUAL
                        && CHUNK_STORE.equals(owner)
                        && "add".equals(methodName)
                        && "(Lcom/hypixel/hytale/component/Holder;)Lcom/hypixel/hytale/component/Ref;".equals(methodDescriptor)) {
                    changed = true;
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, CHUNK_STORE, SAFE_ADD_METHOD, SAFE_ADD_DESC, false);
                    return;
                }

                // PositionProbeBase.probePosition: getHeight → updateHeight
                if (flags.contains(REDIRECT_HEIGHT) && POSITION_PROBE_BASE.equals(className)
                        && "probePosition".equals(name)
                        && opcode == Opcodes.INVOKEVIRTUAL
                        && BLOCK_CHUNK.equals(owner)
                        && "getHeight".equals(methodName)
                        && "(II)S".equals(methodDescriptor)) {
                    changed = true;
                    super.visitMethodInsn(opcode, owner, "updateHeight", methodDescriptor, isInterface);
                    return;
                }

                // MotionControllerWalk.probeMove: track validatePosition
                if (flags.contains(BITMASK_PROBE_MOVE) && MOTION_CTRL_WALK.equals(className)
                        && "probeMove".equals(name)
                        && opcode == Opcodes.INVOKEVIRTUAL
                        && COLLISION_MODULE.equals(owner)
                        && "validatePosition".equals(methodName)
                        && VALIDATE_POSITION_DESC.equals(methodDescriptor)) {
                    patchProbeMoveGroundMask = true;
                    super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    return;
                }

                patchProbeMoveGroundMask = false;
                super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
            }

            @Override
            public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {
                // MotionControllerWalk.probeMove: replace if(result != 1) with if((result & 1) == 0)
                if (patchProbeMoveGroundMask && opcode == Opcodes.IF_ICMPNE) {
                    changed = true;
                    patchProbeMoveGroundMask = false;
                    super.visitInsn(Opcodes.IAND);
                    super.visitJumpInsn(Opcodes.IFEQ, label);
                    return;
                }
                if (patchProbeMoveGroundMask) {
                    patchProbeMoveGroundMask = false;
                }
                super.visitJumpInsn(opcode, label);
            }
        };
    }

    // ── Synthetic method injection ──────────────────────────────────────

    @Override
    public void visitEnd() {
        if (flags.contains(NORM_BLOCK_CHUNK) && BLOCK_CHUNK.equals(className)) {
            SyntheticMethodGenerator.addNormalizeSections(cv);
        }
        if (flags.contains(NORM_CHUNK_COLUMN) && CHUNK_COLUMN.equals(className)) {
            SyntheticMethodGenerator.addNormalizeColumnSections(cv);
            SyntheticMethodGenerator.addNormalizeSectionHolders(cv);
        }
        if (flags.contains(SAFE_ADD) && CHUNK_STORE.equals(className)) {
            SyntheticMethodGenerator.addSafeAddChunkHolder(cv);
        }
        super.visitEnd();
    }

    // ── Literal replacement logic ───────────────────────────────────────

    private Integer replaceInt(String methodName, int opcode, int operand) {
        if (opcode != Opcodes.BIPUSH && opcode != Opcodes.SIPUSH) {
            return null;
        }

        // sipush 320 → targetHeight
        if (operand == HeightConfig.ENGINE_HEIGHT && flags.contains(HEIGHT)) {
            return HeightConfig.TARGET_HEIGHT;
        }

        // sipush 319 → targetHeight - 1
        if (operand == HeightConfig.ENGINE_HEIGHT_M1 && flags.contains(HEIGHT_M1)) {
            return HeightConfig.TARGET_HEIGHT_M1;
        }

        // bipush 10 → targetSections
        if (operand == HeightConfig.ENGINE_SECTIONS && flags.contains(SECTIONS)) {
            return HeightConfig.TARGET_SECTIONS;
        }

        return null;
    }

    private Object replaceLdc(Object value) {
        if (value instanceof Integer intVal) {
            if (intVal == HeightConfig.ENGINE_HEIGHT && flags.contains(HEIGHT)) {
                return HeightConfig.TARGET_HEIGHT;
            }
            if (intVal == HeightConfig.ENGINE_HEIGHT_M1 && flags.contains(HEIGHT_M1)) {
                return HeightConfig.TARGET_HEIGHT_M1;
            }
            // ChunkUtil SIZE_BLOCKS_COLUMN
            if (flags.contains(CHUNK_UTIL_FIELDS) && CHUNK_UTIL.equals(className)
                    && intVal == HeightConfig.BLOCKS_PER_COLUMN * HeightConfig.ENGINE_HEIGHT) {
                return HeightConfig.TARGET_SIZE_BLOCKS_COLUMN;
            }
        }

        if (value instanceof Double dblVal) {
            if (dblVal == (double) HeightConfig.ENGINE_HEIGHT && flags.contains(HEIGHT_DOUBLE)) {
                return (double) HeightConfig.TARGET_HEIGHT;
            }
        }

        return null;
    }

    // ── Array normalization helpers ──────────────────────────────────────

    private static boolean needsBlockChunkNormOnEntry(String methodName) {
        return switch (methodName) {
            case "getChunkSections", "getSectionCount", "getSectionAtIndex",
                 "getSectionAtBlockY", "preTick", "forEachTicking",
                 "mergeTickingBlocks", "getTickingBlocksCount" -> true;
            default -> false;
        };
    }

    private static boolean needsColumnNormOnEntry(int access, String methodName) {
        if ((access & Opcodes.ACC_STATIC) != 0) {
            return "lambda$static$0".equals(methodName) || "lambda$static$1".equals(methodName);
        }
        // Only normalize on methods that directly read the sections array.
        // The constructor handles normalization on exit for new objects.
        return switch (methodName) {
            case "getSection", "getSections", "getSectionCount",
                 "forEachSection", "getBlockDataSection", "getFluidDataSection",
                 "putSectionHolders", "forEachBlockSection" -> true;
            default -> false;
        };
    }

    private boolean needsHolderNormOnExit(String methodName, String descriptor) {
        return ("<init>".equals(methodName) && ("(" + HOLDER_ARRAY_DESC + ")V").equals(descriptor))
            || ("putSectionHolders".equals(methodName) && ("(" + HOLDER_ARRAY_DESC + ")V").equals(descriptor))
            || ("lambda$static$0".equals(methodName)
                && ("(L" + CHUNK_COLUMN + ";" + HOLDER_ARRAY_DESC + ")V").equals(descriptor));
    }

    private void emitBlockChunkNorm(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, BLOCK_CHUNK, "chunkSections", BLOCK_SECTION_ARRAY_DESC);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, BLOCK_CHUNK, NORMALIZE_SECTIONS, NORMALIZE_SECTIONS_DESC, false);
        mv.visitFieldInsn(Opcodes.PUTFIELD, BLOCK_CHUNK, "chunkSections", BLOCK_SECTION_ARRAY_DESC);
    }

    private void emitColumnSectionNorm(MethodVisitor mv, int ownerLocal) {
        mv.visitVarInsn(Opcodes.ALOAD, ownerLocal);
        mv.visitVarInsn(Opcodes.ALOAD, ownerLocal);
        mv.visitFieldInsn(Opcodes.GETFIELD, CHUNK_COLUMN, "sections", REF_ARRAY_DESC);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, CHUNK_COLUMN, NORMALIZE_COL_SECTIONS, NORMALIZE_COL_SECTIONS_DESC, false);
        mv.visitFieldInsn(Opcodes.PUTFIELD, CHUNK_COLUMN, "sections", REF_ARRAY_DESC);
    }

    private void emitHolderNorm(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, CHUNK_COLUMN, "sectionHolders", HOLDER_ARRAY_DESC);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, CHUNK_COLUMN, NORMALIZE_HOLDERS, NORMALIZE_HOLDERS_DESC, false);
        mv.visitFieldInsn(Opcodes.PUTFIELD, CHUNK_COLUMN, "sectionHolders", HOLDER_ARRAY_DESC);
    }
}
