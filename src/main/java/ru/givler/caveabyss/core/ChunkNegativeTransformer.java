package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Redirects negative Chunk block storage while keeping the World.setBlock pipeline. */
public final class ChunkNegativeTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.world.chunk.Chunk";
    private static final String HOOK = "ru/givler/caveabyss/core/MinusOneHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
            String desc = remapper.mapMethodDesc(method.desc);
            String mapped = remapper.mapMethodName("net/minecraft/world/chunk/Chunk", method.name, method.desc);
            if (desc.equals("(III)Lnet/minecraft/block/Block;")) {
                inject(method, "chunkGetBlock", "(Lnet/minecraft/world/chunk/Chunk;III)Lnet/minecraft/block/Block;", ARETURN, false);
                patched++;
            } else if (desc.equals("(III)I") && (mapped.equals("getBlockMetadata") || mapped.equals("func_76628_c"))) {
                inject(method, "chunkGetMetadata", "(Lnet/minecraft/world/chunk/Chunk;III)I", IRETURN, false);
                patched++;
            } else if (desc.equals("(IIILnet/minecraft/block/Block;I)Z")) {
                inject(method, "chunkSetBlock", "(Lnet/minecraft/world/chunk/Chunk;IIILnet/minecraft/block/Block;I)Z", IRETURN, true);
                patched++;
            } else if (desc.equals("(IIII)Z") && (mapped.equals("setBlockMetadata") || mapped.equals("func_76589_b"))) {
                injectMetadata(method);
                patched++;
            } else if (desc.equals("(Lnet/minecraft/world/EnumSkyBlock;III)I") && (mapped.equals("getSavedLightValue") || mapped.equals("func_76614_a"))) {
                injectLight(method, "chunkGetSavedLight", "(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/EnumSkyBlock;III)I", IRETURN, 3, 4);
                patched++;
            } else if (desc.equals("(Lnet/minecraft/world/EnumSkyBlock;IIII)V") && (mapped.equals("setLightValue") || mapped.equals("func_76633_a"))) {
                injectLight(method, "chunkSetLight", "(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/EnumSkyBlock;IIII)V", RETURN, 3, 5);
                patched++;
            } else if (desc.equals("(IIII)I") && (mapped.equals("getBlockLightValue") || mapped.equals("func_76629_c"))) {
                injectLight(method, "chunkGetBlockLight", "(Lnet/minecraft/world/chunk/Chunk;IIII)I", IRETURN, 2, 4);
                patched++;
            } else if (desc.equals("(IIILnet/minecraft/tileentity/TileEntity;)V")
                    && (mapped.equals("setBlockTileEntityInChunk") || mapped.equals("func_150812_a"))) {
                allowNegativeTileEntity(method);
                patched++;
            }
        }
        if (patched != 8) throw new IllegalStateException("CaveAbyss expected 8 Chunk methods; found " + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void inject(MethodNode method, String hook, String desc, int returnOpcode, boolean set) {
        InsnList code = new InsnList();
        LabelNode vanilla = new LabelNode();
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new JumpInsnNode(IFGE, vanilla));
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new IntInsnNode(BIPUSH, -64));
        code.add(new JumpInsnNode(IF_ICMPLT, vanilla));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ILOAD, 1));
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new VarInsnNode(ILOAD, 3));
        if (set) {
            code.add(new VarInsnNode(ALOAD, 4));
            code.add(new VarInsnNode(ILOAD, 5));
        }
        code.add(new MethodInsnNode(INVOKESTATIC, HOOK, hook, desc, false));
        code.add(new InsnNode(returnOpcode));
        code.add(vanilla);
        method.instructions.insert(code);
    }

    private static void injectMetadata(MethodNode method) {
        InsnList code = new InsnList();
        LabelNode vanilla = new LabelNode();
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new JumpInsnNode(IFGE, vanilla));
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new IntInsnNode(BIPUSH, -64));
        code.add(new JumpInsnNode(IF_ICMPLT, vanilla));
        code.add(new VarInsnNode(ALOAD, 0));
        for (int i = 1; i <= 4; i++) code.add(new VarInsnNode(ILOAD, i));
        code.add(new MethodInsnNode(INVOKESTATIC, HOOK, "chunkSetMetadata", "(Lnet/minecraft/world/chunk/Chunk;IIII)Z", false));
        code.add(new InsnNode(IRETURN));
        code.add(vanilla);
        method.instructions.insert(code);
    }

    private static void injectLight(MethodNode method, String hook, String desc, int returnOpcode, int yIndex, int lastArg) {
        InsnList code = new InsnList();
        LabelNode vanilla = new LabelNode();
        code.add(new VarInsnNode(ILOAD, yIndex));
        code.add(new JumpInsnNode(IFGE, vanilla));
        code.add(new VarInsnNode(ALOAD, 0));
        for (int i = 1; i <= lastArg; i++) code.add(new VarInsnNode(i == 1 && yIndex == 3 ? ALOAD : ILOAD, i));
        code.add(new MethodInsnNode(INVOKESTATIC, HOOK, hook, desc, false));
        code.add(new InsnNode(returnOpcode));
        code.add(vanilla);
        method.instructions.insert(code);
    }

    private static void allowNegativeTileEntity(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) insn;
            if (!call.owner.equals("net/minecraft/block/Block") || !call.desc.equals("(I)Z")
                    || !FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(call.owner, call.name, call.desc).equals("hasTileEntity"))
                continue;
            InsnList code = new InsnList();
            LabelNode vanilla = new LabelNode();
            code.add(new VarInsnNode(ILOAD, 2));
            code.add(new JumpInsnNode(IFGE, vanilla));
            code.add(new VarInsnNode(ILOAD, 2));
            code.add(new IntInsnNode(BIPUSH, -64));
            code.add(new JumpInsnNode(IF_ICMPLT, vanilla));
            code.add(new InsnNode(POP));
            code.add(new InsnNode(ICONST_1));
            code.add(vanilla);
            method.instructions.insert(insn, code);
            return;
        }
        throw new IllegalStateException("CaveAbyss could not extend Chunk tile entity loading");
    }
}
