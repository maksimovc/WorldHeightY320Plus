package dev.thenexusgates.y320plus;

import com.hypixel.hytale.plugin.early.ClassTransformer;
import dev.thenexusgates.y320plus.patch.HeightPatchClassVisitor;
import dev.thenexusgates.y320plus.patch.PatchFlag;
import dev.thenexusgates.y320plus.patch.PatchRegistry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Set;

public final class Y320PlusTransformer implements ClassTransformer {

    private int patchedCount;

    static {
        // Force HeightConfig initialization → prints startup banner.
        int _ = HeightConfig.TARGET_HEIGHT;
    }

    @Override
    public int priority() {
        return 10_000;
    }

    @Override
    public byte[] transform(String className, String transformedName, byte[] classBytes) {
        if (classBytes == null) {
            return null;
        }

        String internalName = normalize(className, transformedName);
        Set<PatchFlag> flags = PatchRegistry.flagsFor(internalName);
        if (flags.isEmpty()) {
            return classBytes;
        }

        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                // Hytale classes may not be resolvable via Class.forName within
                // the EarlyPlugin classloader context.  Fall back to Object
                // so frame computation never throws.
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (RuntimeException e) {
                    return "java/lang/Object";
                }
            }
        };
        HeightPatchClassVisitor visitor = new HeightPatchClassVisitor(writer, internalName, flags);
        reader.accept(visitor, 0);

        if (!visitor.isChanged()) {
            return classBytes;
        }

        patchedCount++;
        System.out.println("[Y320+] Patched " + internalName.replace('/', '.') + " (" + patchedCount + "/" + PatchRegistry.targetCount() + ")");
        return writer.toByteArray();
    }

    private static String normalize(String className, String transformedName) {
        String candidate = transformedName != null && !transformedName.isBlank() ? transformedName : className;
        if (candidate == null || candidate.isBlank()) {
            return "";
        }
        return candidate.replace('.', '/');
    }
}
