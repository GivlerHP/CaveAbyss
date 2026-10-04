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

public final class NegativeItemTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.client.network.NetHandlerPlayClient";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            String desc = FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc);
            if (!desc.equals("(Lnet/minecraft/network/play/server/S14PacketEntity;)V")
                    && !desc.equals("(Lnet/minecraft/network/play/server/S18PacketEntityTeleport;)V")) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!call.owner.equals("net/minecraft/entity/Entity")
                        || !call.desc.equals("(DDDFFI)V")) continue;
                String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(call.owner, call.name, call.desc);
                if (!mapped.equals("setPositionAndRotation2") && !mapped.equals("func_70056_a")) continue;
                method.instructions.set(call, new MethodInsnNode(INVOKESTATIC,
                        "ru/givler/caveabyss/core/NegativeItemHooks", "applyNetworkPosition",
                        "(Lnet/minecraft/entity/Entity;DDDFFI)V", false));
                patched++;
            }
        }
        if (patched != 2) throw new IllegalStateException("CaveAbyss item packet patch count=" + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
