package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Extend natural-spawn height selection into the Overworld's lower layer. */
public final class DeepSpawnTransformer implements IClassTransformer, Opcodes {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.world.SpawnerAnimals".equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            String desc = FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc);
            if ((method.access & ACC_STATIC) == 0
                    || !desc.equals("(Lnet/minecraft/world/World;II)Lnet/minecraft/world/ChunkPosition;")) continue;
            int randomCalls = 0;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!call.owner.equals("java/util/Random") || !call.name.equals("nextInt")
                        || !call.desc.equals("(I)I")) continue;
                // X and Z use the first two calls; Y uses the third.
                if (++randomCalls != 3) continue;
                method.instructions.insertBefore(call, new VarInsnNode(ALOAD, 0));
                method.instructions.set(call, new MethodInsnNode(INVOKESTATIC,
                        "ru/givler/caveabyss/core/DeepSpawnHooks", "randomHeight",
                        "(Ljava/util/Random;ILnet/minecraft/world/World;)I", false));
                patched++;
                break;
            }
        }
        if (patched != 1) throw new IllegalStateException("CaveAbyss spawn height patch count=" + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
