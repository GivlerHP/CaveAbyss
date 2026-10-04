package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Records the server's block state before and after handling a dig packet. */
public final class NegativeDigTraceTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.network.NetHandlerPlayServer";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc)
                    .equals("(Lnet/minecraft/network/play/client/C07PacketPlayerDigging;)V")) continue;
            InsnList start = call(false);
            method.instructions.insert(start);
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext())
                if (insn.getOpcode() == RETURN) method.instructions.insertBefore(insn, call(true));
            patched++;
        }
        if (patched != 1) throw new IllegalStateException("CaveAbyss dig handler trace count=" + patched);
        System.out.println("[CaveAbyss negative trace] patched " + transformedName);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static InsnList call(boolean after) {
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ALOAD, 1));
        code.add(new InsnNode(after ? ICONST_1 : ICONST_0));
        code.add(new MethodInsnNode(INVOKESTATIC, "ru/givler/caveabyss/core/NegativeBlockTrace",
                "serverDig", "(Lnet/minecraft/network/NetHandlerPlayServer;Lnet/minecraft/network/play/client/C07PacketPlayerDigging;Z)V", false));
        return code;
    }
}
