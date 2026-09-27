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
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Places negative terrain in the chunk before vanilla hands it to the world. */
public final class DeepChunkGeneratorTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.world.gen.ChunkProviderGenerate";
    private static final String DUNGEON = "net.minecraft.world.gen.feature.WorldGenDungeons";
    private static final String BOUNDING_BOX = "net.minecraft.world.gen.structure.StructureBoundingBox";
    private static final String GENERATOR = "ru/givler/caveabyss/world/DeepWorldGenerator";
    private static final String STRUCTURES = "ru/givler/caveabyss/world/DeepStructureHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName) && !DUNGEON.equals(transformedName)
                && !BOUNDING_BOX.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        if (DUNGEON.equals(transformedName)) return patchDungeon(node);
        if (BOUNDING_BOX.equals(transformedName)) return patchBoundingBox(node);
        FieldNode worldField = null;
        FieldNode randomField = null;
        for (FieldNode field : node.fields) {
            if (FMLDeobfuscatingRemapper.INSTANCE.mapDesc(field.desc).equals("Lnet/minecraft/world/World;")) {
                worldField = field;
            }
            if (field.desc.equals("Ljava/util/Random;")) randomField = field;
        }
        if (worldField == null || randomField == null) throw new IllegalStateException("CaveAbyss generator fields missing");
        int patched = 0;
        for (MethodNode method : node.methods) {
            String desc = FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc);
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name, method.desc);
            if (desc.equals("(Lnet/minecraft/world/chunk/IChunkProvider;II)V")
                    && (mapped.equals("populate") || mapped.equals("func_73153_a"))) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!call.owner.equals("java/util/Random") || !call.name.equals("nextInt")
                            || !call.desc.equals("(I)I") || !(insn.getPrevious() instanceof IntInsnNode)
                            || ((IntInsnNode) insn.getPrevious()).operand != 256) continue;
                    AbstractInsnNode next = insn.getNext();
                    if (!(next instanceof VarInsnNode) || next.getOpcode() != ISTORE) continue;
                    int slot = ((VarInsnNode) next).var;
                    if (slot != 14) continue;
                    method.instructions.remove(insn.getPrevious());
                    InsnList world = new InsnList();
                    world.add(new VarInsnNode(ALOAD, 0));
                    world.add(new FieldInsnNode(GETFIELD, node.name, worldField.name, worldField.desc));
                    method.instructions.insertBefore(insn, world);
                    method.instructions.set(insn, new MethodInsnNode(INVOKESTATIC, STRUCTURES, "dungeonY",
                            "(Ljava/util/Random;Lnet/minecraft/world/World;)I", false));
                    patched++;
                    break;
                }
                int lakeStores = 0;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn.getOpcode() != ISTORE || !(insn instanceof VarInsnNode)
                            || ((VarInsnNode) insn).var != 13) continue;
                    if (++lakeStores != 2) continue;
                    InsnList code = new InsnList();
                    code.add(new VarInsnNode(ILOAD, 13));
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new FieldInsnNode(GETFIELD, node.name, randomField.name, randomField.desc));
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new FieldInsnNode(GETFIELD, node.name, worldField.name, worldField.desc));
                    code.add(new MethodInsnNode(INVOKESTATIC, STRUCTURES, "lavaLakeY",
                            "(ILjava/util/Random;Lnet/minecraft/world/World;)I", false));
                    code.add(new VarInsnNode(ISTORE, 13));
                    method.instructions.insert(insn, code);
                    patched++;
                    break;
                }
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!call.owner.equals("net/minecraft/world/gen/structure/MapGenMineshaft")
                            || !FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(call.owner, call.name, call.desc)
                            .equals("generateStructuresInChunk")) continue;
                    InsnList code = new InsnList();
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new FieldInsnNode(GETFIELD, node.name, worldField.name, worldField.desc));
                    code.add(new VarInsnNode(ILOAD, 2));
                    code.add(new VarInsnNode(ILOAD, 3));
                    code.add(new MethodInsnNode(INVOKESTATIC, STRUCTURES, "populateMineshafts",
                            "(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/World;II)V", false));
                    method.instructions.insert(insn, code);
                    patched++;
                    break;
                }
            }
            if (!desc.equals("(II)Lnet/minecraft/world/chunk/Chunk;")
                    || !mapped.equals("provideChunk") && !mapped.equals("func_73154_d")) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (call.owner.equals("net/minecraft/world/gen/structure/MapGenMineshaft")
                            && FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(call.owner, call.name, call.desc)
                            .equals("func_151539_a")) {
                        InsnList code = new InsnList();
                        code.add(new VarInsnNode(ALOAD, 0));
                        code.add(new VarInsnNode(ALOAD, 0));
                        code.add(new FieldInsnNode(GETFIELD, node.name, worldField.name, worldField.desc));
                        code.add(new VarInsnNode(ILOAD, 1));
                        code.add(new VarInsnNode(ILOAD, 2));
                        code.add(new MethodInsnNode(INVOKESTATIC, STRUCTURES, "prepareMineshafts",
                                "(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/World;II)V", false));
                        method.instructions.insert(insn, code);
                        patched++;
                    }
                }
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
        if (patched != 6) throw new IllegalStateException("CaveAbyss expected 6 terrain hooks; found " + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] patchDungeon(ClassNode node) {
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc)
                    .equals("(Lnet/minecraft/world/World;Ljava/util/Random;III)Z")) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != GETSTATIC) continue;
                FieldInsnNode field = (FieldInsnNode) insn;
                String name = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(field.owner, field.name, field.desc);
                if (!field.owner.equals("net/minecraft/init/Blocks") ||
                        !name.equals("cobblestone") && !name.equals("mossy_cobblestone")) continue;
                InsnList context = new InsnList();
                context.add(new VarInsnNode(ALOAD, 1));
                context.add(new VarInsnNode(ILOAD, 4));
                method.instructions.insertBefore(insn, context);
                method.instructions.insert(insn, new MethodInsnNode(INVOKESTATIC, STRUCTURES, "dungeonWall",
                        "(Lnet/minecraft/world/World;ILnet/minecraft/block/Block;)Lnet/minecraft/block/Block;", false));
                patched++;
            }
        }
        if (patched != 2) throw new IllegalStateException("CaveAbyss expected 2 dungeon wall patches; found " + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] patchBoundingBox(ClassNode node) {
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<init>") || !method.desc.equals("(IIII)V")) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != PUTFIELD) continue;
                FieldInsnNode field = (FieldInsnNode) insn;
                String name = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(node.name, field.name, field.desc);
                if (!name.equals("minY") || insn.getPrevious().getOpcode() != ICONST_1) continue;
                method.instructions.set(insn.getPrevious(), new IntInsnNode(BIPUSH, -64));
                patched++;
                break;
            }
        }
        if (patched != 1) throw new IllegalStateException("CaveAbyss structure chunk box patch missing");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
