package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** A finished mining attempt must not count as the same active block. */
public final class NegativeMiningClientTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.client.multiplayer.PlayerControllerMP";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        FieldNode hitting = null;
        for (FieldNode field : node.fields)
            if (field.desc.equals("Z")) {
                if (hitting != null) throw new IllegalStateException("CaveAbyss found multiple mining state fields");
                hitting = field;
            }
        if (hitting == null) throw new IllegalStateException("CaveAbyss mining state field missing");
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!method.desc.equals("(III)Z")) continue;
            LabelNode active = new LabelNode();
            InsnList code = new InsnList();
            code.add(new VarInsnNode(ALOAD, 0));
            code.add(new FieldInsnNode(GETFIELD, node.name, hitting.name, "Z"));
            code.add(new JumpInsnNode(IFNE, active));
            code.add(new InsnNode(ICONST_0));
            code.add(new InsnNode(IRETURN));
            code.add(active);
            method.instructions.insert(code);
            patched++;
        }
        if (patched != 1) throw new IllegalStateException("CaveAbyss mining comparison patch count=" + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
