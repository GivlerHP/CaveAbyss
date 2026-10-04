package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.InsnNode;

/** Preserve negative Y coordinates in block interaction and correction packets. */
public final class NegativeBlockPacketTransformer implements IClassTransformer, Opcodes {
    private static final String DIG = "net.minecraft.network.play.client.C07PacketPlayerDigging";
    private static final String PLACE = "net.minecraft.network.play.client.C08PacketPlayerBlockPlacement";
    private static final String CHANGE = "net.minecraft.network.play.server.S23PacketBlockChange";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !DIG.equals(transformedName) && !PLACE.equals(transformedName)
                && !CHANGE.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int ordinal = DIG.equals(transformedName) ? 2 : 1;
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc)
                    .equals("(Lnet/minecraft/network/PacketBuffer;)V")) continue;
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name, method.desc);
            boolean read = mapped.equals("readPacketData") || mapped.equals("func_148837_a");
            boolean write = mapped.equals("writePacketData") || mapped.equals("func_148840_b");
            if (!read && !write) continue;
            int position = 0;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (read && call.name.equals("readUnsignedByte") && call.desc.equals("()S")
                        || write && call.name.equals("writeByte")
                        && call.desc.equals("(I)Lio/netty/buffer/ByteBuf;")) {
                    if (++position != ordinal) continue;
                    call.name = read ? "readShort" : "writeShort";
                    // Both methods return the same type as the original call.
                    patched++;
                    break;
                }
            }
            if (DIG.equals(transformedName) || CHANGE.equals(transformedName)) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn.getOpcode() != RETURN) continue;
                    InsnList trace = new InsnList();
                    trace.add(new VarInsnNode(ALOAD, 0));
                    trace.add(new InsnNode(read ? ICONST_1 : ICONST_0));
                    trace.add(new MethodInsnNode(INVOKESTATIC, "ru/givler/caveabyss/core/NegativeBlockTrace",
                            DIG.equals(transformedName) ? "packet" : "correction",
                            DIG.equals(transformedName)
                                    ? "(Lnet/minecraft/network/play/client/C07PacketPlayerDigging;Z)V"
                                    : "(Lnet/minecraft/network/play/server/S23PacketBlockChange;Z)V", false));
                    method.instructions.insertBefore(insn, trace);
                }
            }
        }
        if (patched != 2) throw new IllegalStateException("CaveAbyss expected two Y packet fields in " + transformedName
                + "; found " + patched);
        System.out.println("[CaveAbyss negative trace] patched " + transformedName);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
