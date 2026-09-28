package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Preserves signed block Y when 1.7.10 CraftBukkit crosses into NMS. */
public final class CauldronBukkitTransformer implements IClassTransformer, Opcodes {
    private static final String WORLD = "org.bukkit.craftbukkit.CraftWorld";
    private static final String CHUNK = "org.bukkit.craftbukkit.CraftChunk";
    private static final String BLOCK = "org.bukkit.craftbukkit.block.CraftBlock";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !WORLD.equals(transformedName) && !CHUNK.equals(transformedName)
                && !BLOCK.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!isBlockAccess(transformedName, method.name)) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
                AbstractInsnNode next = insn.getNext();
                if (insn.getOpcode() == IAND && is255(insn.getPrevious())
                        && isYLoad(transformedName, insn.getPrevious().getPrevious())) {
                    method.instructions.remove(insn.getPrevious());
                    method.instructions.remove(insn);
                    patched++;
                }
                insn = next;
            }
        }
        // Some forks already fixed this; leave their implementation intact.
        if (patched == 0) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isBlockAccess(String owner, String method) {
        if (WORLD.equals(owner)) return method.equals("getBlockAt");
        if (CHUNK.equals(owner)) return method.equals("getBlock");
        return method.equals("getData") || method.equals("getTypeId")
                || method.equals("getLightFromSky") || method.equals("getLightFromBlocks");
    }

    private static boolean is255(AbstractInsnNode insn) {
        return insn instanceof IntInsnNode && insn.getOpcode() == SIPUSH
                && ((IntInsnNode) insn).operand == 255;
    }

    private static boolean isYLoad(String owner, AbstractInsnNode insn) {
        if (BLOCK.equals(owner)) return insn instanceof FieldInsnNode
                && insn.getOpcode() == GETFIELD && ((FieldInsnNode) insn).name.equals("y");
        return insn instanceof VarInsnNode && insn.getOpcode() == ILOAD
                && ((VarInsnNode) insn).var == 2;
    }
}
