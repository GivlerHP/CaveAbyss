package ru.givler.caveabyss.core;

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

/** Extends RTG's independently generated Overworld chunks at construction time. */
public final class RtgChunkGeneratorTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "rtg.world.gen.ChunkProviderRTG";
    private static final String CHUNK = "net/minecraft/world/chunk/Chunk";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String GENERATOR = "ru/givler/caveabyss/world/DeepWorldGenerator";
    private static final String STRUCTURES = "ru/givler/caveabyss/world/DeepStructureHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        String worldField = null;
        for (FieldNode field : node.fields) if (WORLD.equals(field.desc)) {
            worldField = field.name;
            break;
        }
        if (worldField == null) throw new IllegalStateException("CaveAbyss: RTG world field missing");
        int patched = 0;
        for (MethodNode method : node.methods) {
            if ((method.name.equals("provideChunk") || method.name.equals("func_73154_d"))
                    && method.desc.equals("(II)L" + CHUNK + ";")) {
                int blocksSlot = -1;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn.getOpcode() == ANEWARRAY && insn instanceof TypeInsnNode
                            && ((TypeInsnNode) insn).desc.equals("net/minecraft/block/Block")) {
                        AbstractInsnNode next = insn.getNext();
                        if (next instanceof VarInsnNode && next.getOpcode() == ASTORE)
                            blocksSlot = ((VarInsnNode) next).var;
                    }
                }
                if (blocksSlot < 0) throw new IllegalStateException("CaveAbyss: RTG block array missing");
                int terrainSlot = method.maxLocals++;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != INVOKESPECIAL) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!call.owner.equals(CHUNK) || !call.name.equals("<init>")
                            || !call.desc.equals("(Lnet/minecraft/world/World;[Lnet/minecraft/block/Block;[BII)V")) continue;
                    // The constructor's arguments are already on the stack. Insert before the NEW,
                    // then attach storage to the new Chunk before RTG publishes it in inGeneration.
                    AbstractInsnNode creation = insn;
                    while (creation != null && (creation.getOpcode() != NEW ||
                            !((TypeInsnNode) creation).desc.equals(CHUNK))) creation = creation.getPrevious();
                    if (creation == null) throw new IllegalStateException("CaveAbyss: RTG chunk allocation missing");
                    InsnList before = new InsnList();
                    before.add(new VarInsnNode(ALOAD, 0));
                    before.add(new FieldInsnNode(GETFIELD, node.name, worldField, WORLD));
                    before.add(new VarInsnNode(ALOAD, blocksSlot));
                    before.add(new VarInsnNode(ILOAD, 1));
                    before.add(new VarInsnNode(ILOAD, 2));
                    before.add(new MethodInsnNode(INVOKESTATIC, GENERATOR, "prepareTerrain",
                            "(Lnet/minecraft/world/World;[Lnet/minecraft/block/Block;II)[I", false));
                    before.add(new VarInsnNode(ASTORE, terrainSlot));
                    before.add(new VarInsnNode(ALOAD, 0));
                    before.add(new VarInsnNode(ALOAD, 0));
                    before.add(new FieldInsnNode(GETFIELD, node.name, worldField, WORLD));
                    before.add(new VarInsnNode(ILOAD, 1));
                    before.add(new VarInsnNode(ILOAD, 2));
                    before.add(new MethodInsnNode(INVOKESTATIC, STRUCTURES, "prepareMineshafts",
                            "(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/World;II)V", false));
                    method.instructions.insertBefore(creation, before);
                    InsnList after = new InsnList();
                    after.add(new InsnNode(DUP));
                    after.add(new VarInsnNode(ALOAD, terrainSlot));
                    after.add(new MethodInsnNode(INVOKESTATIC, GENERATOR, "onProvideChunk",
                            "(Lnet/minecraft/world/chunk/Chunk;[I)V", false));
                    method.instructions.insert(insn, after);
                    patched++;
                    break;
                }
            } else if ((method.name.equals("populate") || method.name.equals("func_73153_a"))
                    && method.desc.equals("(Lnet/minecraft/world/chunk/IChunkProvider;II)V")) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!call.owner.equals("net/minecraft/world/gen/structure/MapGenMineshaft")
                            || !call.name.equals("generateStructuresInChunk") && !call.name.equals("func_75051_a")) continue;
                    InsnList code = new InsnList();
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new FieldInsnNode(GETFIELD, node.name, worldField, WORLD));
                    code.add(new VarInsnNode(ILOAD, 2));
                    code.add(new VarInsnNode(ILOAD, 3));
                    code.add(new MethodInsnNode(INVOKESTATIC, STRUCTURES, "populateMineshafts",
                            "(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/World;II)V", false));
                    method.instructions.insert(insn, code);
                    patched++;
                    break;
                }
            }
        }
        if (patched < 1) throw new IllegalStateException("CaveAbyss: RTG chunk hook missing");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
