package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/** Retains vanilla piston mechanics while moving its lower boundary in the Overworld. */
public final class PistonHeightTransformer implements IClassTransformer, Opcodes {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !transformedName.equals("net.minecraft.block.BlockPistonBase")) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int count = 0;
        for (MethodNode method : node.methods) {
            if (!method.desc.equals("(Lnet/minecraft/world/World;IIII)Z")) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof JumpInsnNode) || insn.getOpcode() != IFLE) continue;
                AbstractInsnNode previous = insn.getPrevious();
                if (!(previous instanceof VarInsnNode) || previous.getOpcode() != ILOAD) continue;
                int worldSlot = (method.access & ACC_STATIC) != 0 ? 0 : 1;
                InsnList minimum = new InsnList();
                minimum.add(new VarInsnNode(ALOAD, worldSlot));
                minimum.add(new MethodInsnNode(INVOKESTATIC, "ru/givler/caveabyss/core/MinusOneHooks",
                        "minimumY", "(Lnet/minecraft/world/World;)I", false));
                method.instructions.insertBefore(insn, minimum);
                method.instructions.set(insn, new JumpInsnNode(IF_ICMPLE, ((JumpInsnNode)insn).label));
                count++;
                break;
            }
        }
        if (count != 2) throw new IllegalStateException("CaveAbyss piston height anchors: " + count);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
