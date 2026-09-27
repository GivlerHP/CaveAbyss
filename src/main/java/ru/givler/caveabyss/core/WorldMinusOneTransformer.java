package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class WorldMinusOneTransformer implements IClassTransformer, Opcodes {
    private static final String WORLD = "net.minecraft.world.World";
    private static final String HOOK = "ru/givler/caveabyss/core/MinusOneHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !WORLD.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            // Descriptors distinguish these World methods even before FML remaps names.
            FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
            String desc = remapper.mapMethodDesc(method.desc);
            String mappedName = remapper.mapMethodName("net/minecraft/world/World", method.name, method.desc);
            if (desc.equals("(III)Lnet/minecraft/block/Block;")) {
                inject(method, "getBlock", "(Lnet/minecraft/world/World;III)Lnet/minecraft/block/Block;", ARETURN);
                patched++;
            } else if (desc.equals("(IIILnet/minecraft/block/Block;II)Z")) {
                extendSetBlockBounds(method);
                patched++;
            } else if (desc.equals("(III)I") && (mappedName.equals("getBlockMetadata") || mappedName.equals("func_72805_g"))) {
                inject(method, "getMetadata", "(Lnet/minecraft/world/World;III)I", IRETURN);
                patched++;
            } else if (desc.equals("(III)Z") && (mappedName.equals("blockExists") || mappedName.equals("func_72899_e"))) {
                inject(method, "blockExists", "(Lnet/minecraft/world/World;III)Z", IRETURN);
                patched++;
            } else if (desc.equals("(IIIII)Z") && (mappedName.equals("setBlockMetadataWithNotify") || mappedName.equals("func_72921_c"))) {
                extendSetBlockBounds(method);
                patched++;
            } else if (desc.equals("(IIIIII)Z") && (mappedName.equals("checkChunksExist") || mappedName.equals("func_72904_c"))) {
                injectCheckChunks(method);
                patched++;
            } else if (desc.equals("(Lnet/minecraft/world/EnumSkyBlock;III)I") && (mappedName.equals("getSavedLightValue") || mappedName.equals("func_72972_b"))) {
                injectLight(method, "getSavedLight", "(Lnet/minecraft/world/World;Lnet/minecraft/world/EnumSkyBlock;III)I", IRETURN, 3, 4);
                patched++;
            } else if (desc.equals("(Lnet/minecraft/world/EnumSkyBlock;III)I") && (mappedName.equals("getSkyBlockTypeBrightness") || mappedName.equals("func_72925_a"))) {
                injectLight(method, "getSkyBrightness", "(Lnet/minecraft/world/World;Lnet/minecraft/world/EnumSkyBlock;III)I", IRETURN, 3, 4);
                patched++;
            } else if (desc.equals("(Lnet/minecraft/world/EnumSkyBlock;IIII)V") && (mappedName.equals("setLightValue") || mappedName.equals("func_72915_b"))) {
                injectLight(method, "setLight", "(Lnet/minecraft/world/World;Lnet/minecraft/world/EnumSkyBlock;IIII)V", RETURN, 3, 5);
                patched++;
            } else if (desc.equals("(IIIZ)I") && (mappedName.equals("getBlockLightValue_do") || mappedName.equals("func_72849_a"))) {
                injectLight(method, "getBlockBrightness", "(Lnet/minecraft/world/World;IIIZ)I", IRETURN, 2, 4);
                patched++;
            }
        }
        if (patched != 10) throw new IllegalStateException("CaveAbyss expected 10 World methods; found " + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void inject(MethodNode method, String hook, String desc, int returnOpcode) {
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ILOAD, 2));
        LabelNode vanilla = new LabelNode();
        code.add(new JumpInsnNode(IFGE, vanilla));
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new IntInsnNode(BIPUSH, -64));
        code.add(new JumpInsnNode(IF_ICMPLT, vanilla));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ILOAD, 1));
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new VarInsnNode(ILOAD, 3));
        code.add(new MethodInsnNode(INVOKESTATIC, HOOK, hook, desc, false));
        code.add(new InsnNode(returnOpcode));
        code.add(vanilla);
        method.instructions.insert(code);
    }

    private static void injectCheckChunks(MethodNode method) {
        InsnList code = new InsnList();
        LabelNode vanilla = new LabelNode();
        code.add(new VarInsnNode(ILOAD, 2));
        code.add(new JumpInsnNode(IFGE, vanilla));
        code.add(new VarInsnNode(ILOAD, 5));
        code.add(new IntInsnNode(BIPUSH, -64));
        code.add(new JumpInsnNode(IF_ICMPLT, vanilla));
        code.add(new VarInsnNode(ALOAD, 0));
        for (int i = 1; i <= 6; i++) code.add(new VarInsnNode(ILOAD, i));
        code.add(new MethodInsnNode(INVOKESTATIC, HOOK, "checkNegativeChunks", "(Lnet/minecraft/world/World;IIIIII)Z", false));
        code.add(new InsnNode(IRETURN));
        code.add(vanilla);
        method.instructions.insert(code);
    }

    private static void extendSetBlockBounds(MethodNode method) {
        for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof VarInsnNode) || insn.getOpcode() != ILOAD || ((VarInsnNode)insn).var != 2) continue;
            org.objectweb.asm.tree.AbstractInsnNode next = insn.getNext();
            if (!(next instanceof JumpInsnNode) || next.getOpcode() != IFGE) continue;
            JumpInsnNode oldJump = (JumpInsnNode)next;
            method.instructions.insert(insn, new IntInsnNode(BIPUSH, -64));
            method.instructions.set(oldJump, new JumpInsnNode(IF_ICMPGE, oldJump.label));
            return;
        }
        throw new IllegalStateException("CaveAbyss could not extend World.setBlock bounds");
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
}
