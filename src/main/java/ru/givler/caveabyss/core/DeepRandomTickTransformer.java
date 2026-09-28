package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adds the four negative sections to the vanilla active-chunk random-tick pass. */
public final class DeepRandomTickTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.world.WorldServer";
    private static final String HOOK = "ru/givler/caveabyss/core/DeepRandomTickHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        MethodNode tick = null;
        for (MethodNode method : node.methods) {
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                    node.name, method.name, method.desc);
            if (method.desc.equals("()V")
                    && (mapped.equals("func_147456_g") || mapped.equals("tickBlocksAndAmbiance")
                    || method.name.equals("func_147456_g"))) {
                tick = method;
                break;
            }
        }
        if (tick == null) throw new IllegalStateException("CaveAbyss WorldServer tick method missing");

        int chunkLocal = -1;
        FieldInsnNode lcgField = null;
        boolean inBlockTicks = false;
        AbstractInsnNode insertion = null;
        for (AbstractInsnNode instruction = tick.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof LdcInsnNode
                    && "tickBlocks".equals(((LdcInsnNode)instruction).cst))
                inBlockTicks = true;
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode)instruction;
                String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(
                        field.owner, field.name, field.desc);
                if (field.getOpcode() == PUTFIELD && field.desc.equals("I")
                        && (mapped.equals("updateLCG") || mapped.equals("field_73012_v")
                        || field.name.equals("updateLCG") || field.name.equals("field_73012_v")))
                    lcgField = field;
            }
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode)instruction;
                String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                        call.owner, call.name, call.desc);
                if (call.desc.equals("()[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;")
                        && (mapped.equals("getBlockStorageArray") || mapped.equals("func_76587_i")
                        || call.name.equals("getBlockStorageArray") || call.name.equals("func_76587_i"))) {
                    AbstractInsnNode load = call.getPrevious();
                    if (!(load instanceof VarInsnNode) || load.getOpcode() != ALOAD)
                        throw new IllegalStateException("CaveAbyss chunk local not found");
                    chunkLocal = ((VarInsnNode)load).var;
                }
                if (inBlockTicks && call.desc.equals("()V")
                        && (mapped.equals("endSection") || mapped.equals("func_76319_b")
                        || call.name.equals("endSection") || call.name.equals("func_76319_b"))) {
                    // Insert before the ALOAD 0 / GETFIELD profiler pair, with an empty stack.
                    AbstractInsnNode field = call.getPrevious();
                    AbstractInsnNode load = field == null ? null : field.getPrevious();
                    if (!(field instanceof FieldInsnNode) || load == null || load.getOpcode() != ALOAD)
                        throw new IllegalStateException("CaveAbyss tick profiler anchor changed");
                    insertion = load;
                    break;
                }
            }
        }
        if (chunkLocal < 0 || lcgField == null || insertion == null)
            throw new IllegalStateException("CaveAbyss WorldServer random tick anchors missing");
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ALOAD, chunkLocal));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, lcgField.owner, lcgField.name, lcgField.desc));
        code.add(new MethodInsnNode(INVOKESTATIC, HOOK, "tickNegativeSections",
                "(Lnet/minecraft/world/WorldServer;Lnet/minecraft/world/chunk/Chunk;I)I", false));
        code.add(new FieldInsnNode(PUTFIELD, lcgField.owner, lcgField.name, lcgField.desc));
        tick.instructions.insertBefore(insertion, code);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
