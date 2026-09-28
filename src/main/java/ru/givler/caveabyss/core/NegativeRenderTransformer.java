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
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Extends the vanilla section renderer to Y=-64 and lets ChunkCache read the lower layer. */
public final class NegativeRenderTransformer implements IClassTransformer, Opcodes {
    private static final String GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String CACHE = "net.minecraft.world.ChunkCache";
    private static final String TESSELLATOR = "net.minecraft.client.renderer.Tessellator";
    private static final String HOOK = "ru/givler/caveabyss/core/MinusOneHooks";
    private static final String SKY_HOOK = "ru/givler/caveabyss/client/SkyRenderHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !GLOBAL.equals(transformedName) && !CACHE.equals(transformedName)
                && !TESSELLATOR.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        if (GLOBAL.equals(transformedName)) patchGlobal(node);
        else if (CACHE.equals(transformedName)) patchCache(node);
        else patchTessellator(node);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void patchGlobal(ClassNode node) {
        int patched = 0;
        FieldNode worldField = null;
        for (FieldNode field : node.fields) {
            if (FMLDeobfuscatingRemapper.INSTANCE.mapDesc(field.desc).equals("Lnet/minecraft/client/multiplayer/WorldClient;")) {
                worldField = field;
                break;
            }
        }
        if (worldField == null) throw new IllegalStateException("CaveAbyss RenderGlobal world field missing");
        for (MethodNode method : node.methods) {
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name, method.desc);
            if (method.name.equals("<init>")) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (intValue(insn) != 16 || !(insn.getNext() instanceof VarInsnNode)
                            || insn.getNext().getOpcode() != ISTORE || ((VarInsnNode) insn.getNext()).var != 3) continue;
                    method.instructions.set(insn, new IntInsnNode(BIPUSH, 20));
                    patched++;
                    break;
                }
            } else if (method.desc.equals("()V") && (mapped.equals("loadRenderers") || mapped.equals("func_72712_a"))) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != PUTFIELD) continue;
                    FieldInsnNode field = (FieldInsnNode) insn;
                    String mappedField = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(node.name, field.name, field.desc);
                    if (mappedField.equals("renderChunksTall") || mappedField.equals("field_72763_n")) {
                        if (intValue(insn.getPrevious()) != 16) throw new IllegalStateException("CaveAbyss render height pattern changed");
                        method.instructions.set(insn.getPrevious(), new IntInsnNode(BIPUSH, 20));
                        patched++;
                    } else if (mappedField.equals("minBlockY") || mappedField.equals("field_72779_z")) {
                        if (intValue(insn.getPrevious()) != 0) throw new IllegalStateException("CaveAbyss minimum render Y pattern changed");
                        method.instructions.set(insn.getPrevious(), new IntInsnNode(BIPUSH, -64));
                        patched++;
                    }
                }
                // The loop's section number (0..19) maps to world Y=-64..255.
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != INVOKESPECIAL) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!call.owner.equals("net/minecraft/client/renderer/WorldRenderer") || !call.name.equals("<init>")) continue;
                    // Constructor stack contains x*16, section*16, z*16. Insert the Y offset after section*16.
                    AbstractInsnNode yScale = null;
                    AbstractInsnNode previous = insn.getPrevious();
                    for (int step = 0; step < 20 && previous != null; step++, previous = previous.getPrevious()) {
                        if (previous.getOpcode() == IMUL && intValue(previous.getPrevious()) == 16
                                && previous.getPrevious().getPrevious() instanceof VarInsnNode
                                && previous.getPrevious().getPrevious().getOpcode() == ILOAD
                                && ((VarInsnNode) previous.getPrevious().getPrevious()).var == 5) {
                            yScale = previous;
                            break;
                        }
                    }
                    if (yScale == null) throw new IllegalStateException("CaveAbyss WorldRenderer Y pattern changed");
                    InsnList offset = new InsnList();
                    offset.add(new IntInsnNode(BIPUSH, -64));
                    offset.add(new InsnNode(IADD));
                    method.instructions.insert(yScale, offset);
                    patched++;
                    break;
                }
            } else if (method.desc.equals("(F)V") && (mapped.equals("renderSky") || mapped.equals("func_72714_a"))) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    String callName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(call.owner, call.name, call.desc);
                    if (!call.desc.equals("()D") || !callName.equals("getHorizon") && !callName.equals("func_72919_O")) continue;
                    InsnList code = new InsnList();
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new FieldInsnNode(GETFIELD, node.name, worldField.name, worldField.desc));
                    code.add(new MethodInsnNode(INVOKESTATIC, SKY_HOOK, "adjustHorizon",
                            "(DLnet/minecraft/client/multiplayer/WorldClient;)D", false));
                    method.instructions.insert(insn, code);
                    patched++;
                    break;
                }
            } else if (method.desc.equals("(III)V") && (mapped.equals("markRenderersForNewPosition") || mapped.equals("func_72722_c"))) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof VarInsnNode) || insn.getOpcode() != ISTORE || ((VarInsnNode) insn).var != 13) continue;
                    if (insn.getPrevious().getOpcode() != IMUL || intValue(insn.getPrevious().getPrevious()) != 16) continue;
                    InsnList offset = new InsnList();
                    offset.add(new IntInsnNode(BIPUSH, -64));
                    offset.add(new InsnNode(IADD));
                    method.instructions.insertBefore(insn, offset);
                    patched++;
                    break;
                }
            } else if (method.desc.equals("(IIIIII)V") && (mapped.equals("markBlocksForUpdate") || mapped.equals("func_72725_b"))) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof VarInsnNode) || insn.getOpcode() != ISTORE) continue;
                    int index = ((VarInsnNode) insn).var;
                    if (index != 8 && index != 11) continue;
                    InsnList offset = new InsnList();
                    offset.add(new InsnNode(ICONST_4));
                    offset.add(new InsnNode(IADD));
                    offset.add(new InsnNode(ICONST_0));
                    offset.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "max", "(II)I", false));
                    offset.add(new IntInsnNode(BIPUSH, 19));
                    offset.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "min", "(II)I", false));
                    method.instructions.insertBefore(insn, offset);
                    patched++;
                }
            }
        }
        if (patched != 8) throw new IllegalStateException("CaveAbyss expected 8 RenderGlobal patches; found " + patched);
    }

    private static void patchCache(ClassNode node) {
        FieldNode worldField = null, emptyField = null;
        for (FieldNode field : node.fields) {
            if (FMLDeobfuscatingRemapper.INSTANCE.mapDesc(field.desc).equals("Lnet/minecraft/world/World;")) worldField = field;
            else if (field.desc.equals("Z")) emptyField = field;
        }
        if (worldField == null || emptyField == null) throw new IllegalStateException("CaveAbyss ChunkCache fields missing");
        int patched = 0;
        for (MethodNode method : node.methods) {
            String desc = FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc);
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name, method.desc);
            if (method.name.equals("<init>") && desc.equals("(Lnet/minecraft/world/World;IIIIIII)V")) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn.getOpcode() != RETURN) continue;
                    InsnList code = new InsnList();
                    LabelNode done = new LabelNode();
                    code.add(new VarInsnNode(ILOAD, 3));
                    code.add(new JumpInsnNode(IFGE, done));
                    code.add(new VarInsnNode(ALOAD, 0));
                    code.add(new InsnNode(ICONST_0));
                    code.add(new FieldInsnNode(PUTFIELD, node.name, emptyField.name, emptyField.desc));
                    code.add(done);
                    method.instructions.insertBefore(insn, code);
                    patched++;
                }
            } else if (desc.equals("(III)Lnet/minecraft/block/Block;") && (mapped.equals("getBlock") || mapped.equals("func_147439_a"))) {
                injectCacheRead(node.name, worldField, method, "getBlock", "(Lnet/minecraft/world/World;III)Lnet/minecraft/block/Block;", ARETURN, false);
                patched++;
            } else if (desc.equals("(III)I") && (mapped.equals("getBlockMetadata") || mapped.equals("func_72805_g"))) {
                injectCacheRead(node.name, worldField, method, "getMetadata", "(Lnet/minecraft/world/World;III)I", IRETURN, false);
                patched++;
            } else if (desc.equals("(Lnet/minecraft/world/EnumSkyBlock;III)I") &&
                    (mapped.equals("getSkyBlockTypeBrightness") || mapped.equals("func_72810_a")
                            || mapped.equals("getSpecialBlockBrightness") || mapped.equals("func_72812_b"))) {
                boolean special = mapped.equals("getSpecialBlockBrightness") || mapped.equals("func_72812_b");
                injectCacheRead(node.name, worldField, method, special ? "getSavedLight" : "getSkyBrightness",
                        "(Lnet/minecraft/world/World;Lnet/minecraft/world/EnumSkyBlock;III)I", IRETURN, true);
                patched++;
            }
        }
        if (patched != 5) throw new IllegalStateException("CaveAbyss expected 5 ChunkCache patches; found " + patched);
    }

    /** Empty translucent passes are possible when a mod reports a rendered block without adding quads. */
    private static void patchTessellator(ClassNode node) {
        int patched = 0;
        for (MethodNode method : node.methods) {
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name, method.desc);
            if (!method.desc.equals("(FFF)Lnet/minecraft/client/shader/TesselatorVertexState;")
                    || !mapped.equals("getVertexState") && !mapped.equals("func_147564_a")) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != INVOKESPECIAL) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!call.owner.equals("java/util/PriorityQueue") || !call.name.equals("<init>")
                        || !call.desc.equals("(ILjava/util/Comparator;)V")) continue;
                AbstractInsnNode capacity = insn.getPrevious();
                while (capacity != null) {
                    if (capacity instanceof FieldInsnNode && capacity.getOpcode() == GETFIELD) {
                        FieldInsnNode field = (FieldInsnNode) capacity;
                        String mappedField = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(node.name, field.name, field.desc);
                        if (field.desc.equals("I") && (mappedField.equals("rawBufferIndex")
                                || mappedField.equals("field_78406_i"))) break;
                    }
                    capacity = capacity.getPrevious();
                }
                if (capacity == null)
                    throw new IllegalStateException("CaveAbyss Tessellator queue capacity pattern changed");
                InsnList clamp = new InsnList();
                clamp.add(new InsnNode(ICONST_1));
                clamp.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "max", "(II)I", false));
                method.instructions.insert(capacity, clamp);
                patched++;
                break;
            }
        }
        if (patched != 1) throw new IllegalStateException("CaveAbyss expected 1 Tessellator patch; found " + patched);
    }

    private static void injectCacheRead(String owner, FieldNode worldField, MethodNode method, String hook,
                                        String desc, int returnOpcode, boolean enumArg) {
        InsnList code = new InsnList();
        LabelNode vanilla = new LabelNode();
        int y = enumArg ? 3 : 2;
        code.add(new VarInsnNode(ILOAD, y));
        code.add(new JumpInsnNode(IFGE, vanilla));
        code.add(new VarInsnNode(ILOAD, y));
        code.add(new IntInsnNode(BIPUSH, -64));
        code.add(new JumpInsnNode(IF_ICMPLT, vanilla));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, owner, worldField.name, worldField.desc));
        for (int i = 1; i <= (enumArg ? 4 : 3); i++) code.add(new VarInsnNode(enumArg && i == 1 ? ALOAD : ILOAD, i));
        code.add(new MethodInsnNode(INVOKESTATIC, HOOK, hook, desc, false));
        code.add(new InsnNode(returnOpcode));
        code.add(vanilla);
        method.instructions.insert(code);
    }

    private static int intValue(AbstractInsnNode node) {
        if (node == null) return Integer.MIN_VALUE;
        if (node instanceof IntInsnNode) return ((IntInsnNode) node).operand;
        return node.getOpcode() >= ICONST_M1 && node.getOpcode() <= ICONST_5 ? node.getOpcode() - ICONST_0 : Integer.MIN_VALUE;
    }
}
