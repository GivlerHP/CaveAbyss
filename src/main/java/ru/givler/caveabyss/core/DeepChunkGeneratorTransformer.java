package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Places negative terrain in the chunk before vanilla hands it to the world. */
public final class DeepChunkGeneratorTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.world.gen.ChunkProviderGenerate";
    private static final String GENERATOR = "ru/givler/caveabyss/world/DeepWorldGenerator";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        FieldNode worldField = null;
        for (FieldNode field : node.fields)
            if (FMLDeobfuscatingRemapper.INSTANCE.mapDesc(field.desc).equals("Lnet/minecraft/world/World;")) {
                worldField = field;
                break;
            }
        if (worldField == null) throw new IllegalStateException("CaveAbyss generator World field missing");
        int patched = 0;
        for (MethodNode method : node.methods) {
            String desc = FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc);
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name, method.desc);
            if (!desc.equals("(II)Lnet/minecraft/world/chunk/Chunk;")
                    || !mapped.equals("provideChunk") && !mapped.equals("func_73154_d")) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == NEW && insn instanceof TypeInsnNode
                        && ((TypeInsnNode) insn).desc.equals("net/minecraft/world/chunk/Chunk")) {
                    InsnList code = new InsnList();
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new FieldInsnNode(GETFIELD, node.name, worldField.name, worldField.desc));
                    code.add(new VarInsnNode(ALOAD, 3));
                    code.add(new VarInsnNode(ILOAD, 1));
                    code.add(new VarInsnNode(ILOAD, 2));
                    code.add(new MethodInsnNode(INVOKESTATIC, GENERATOR, "prepareTerrain",
                            "(Lnet/minecraft/world/World;[Lnet/minecraft/block/Block;II)[I", false));
                    code.add(new VarInsnNode(ASTORE, method.maxLocals));
                    method.instructions.insertBefore(insn, code);
                    patched++;
                } else if (insn.getOpcode() == ARETURN) {
                    InsnList code = new InsnList();
                    code.add(new InsnNode(DUP));
                    code.add(new VarInsnNode(ALOAD, method.maxLocals));
                    code.add(new MethodInsnNode(INVOKESTATIC, GENERATOR, "onProvideChunk",
                            "(Lnet/minecraft/world/chunk/Chunk;[I)V", false));
                    method.instructions.insertBefore(insn, code);
                    patched++;
                }
            }
        }
        if (patched != 2) throw new IllegalStateException("CaveAbyss expected 2 terrain hooks; found " + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
