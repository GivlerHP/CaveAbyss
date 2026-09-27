package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Keeps BOP's own plant rules, adding deepslate as one more valid support. */
public final class BopPlantSupportTransformer implements IClassTransformer, Opcodes {
    private static final String MUSHROOM = "biomesoplenty.common.blocks.BlockBOPMushroom";
    private static final String MOSS = "biomesoplenty.common.blocks.BlockMoss";
    private static final String HOOKS = "ru/givler/caveabyss/core/BopPlantSupportHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !MUSHROOM.equals(transformedName) && !MOSS.equals(transformedName))
            return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (MUSHROOM.equals(transformedName)) {
                if ("isValidPosition".equals(method.name)
                        && "(Lnet/minecraft/world/World;IIII)Z".equals(method.desc)) {
                    prepend(method, "glowshroomPosition", true);
                    patched++;
                } else if (("canBlockStay".equals(method.name) || "func_149718_j".equals(method.name))
                        && "(Lnet/minecraft/world/World;III)Z".equals(method.desc)) {
                    prepend(method, "glowshroomStay", false);
                    patched++;
                }
            } else if (("canPlaceBlockOnSide".equals(method.name) || "func_149707_d".equals(method.name))
                    && "(Lnet/minecraft/world/World;IIII)Z".equals(method.desc)) {
                prepend(method, "mossPosition", true);
                patched++;
            }
        }
        int expected = MUSHROOM.equals(transformedName) ? 2 : 1;
        if (patched != expected)
            throw new IllegalStateException("CaveAbyss BOP plant patch count=" + patched
                    + " class=" + transformedName);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void prepend(MethodNode method, String hook, boolean hasExtraArgument) {
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 1));
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new VarInsnNode(ILOAD, 3));
        code.add(new VarInsnNode(ILOAD, 4));
        if (hasExtraArgument) code.add(new VarInsnNode(ILOAD, 5));
        code.add(new MethodInsnNode(INVOKESTATIC, HOOKS, hook,
                hasExtraArgument ? "(Lnet/minecraft/world/World;IIII)Z"
                        : "(Lnet/minecraft/world/World;III)Z", false));
        LabelNode original = new LabelNode();
        code.add(new JumpInsnNode(IFEQ, original));
        code.add(new InsnNode(ICONST_1));
        code.add(new InsnNode(IRETURN));
        code.add(original);
        method.instructions.insert(code);
    }
}
