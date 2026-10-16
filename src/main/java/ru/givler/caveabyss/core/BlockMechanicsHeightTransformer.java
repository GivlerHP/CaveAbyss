package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Explicit coordinate checks only: metadata, timers and inventory indices keep their vanilla limits. */
public final class BlockMechanicsHeightTransformer implements IClassTransformer, Opcodes {
    @Override public byte[] transform(String name, String target, byte[] bytes) {
        boolean falling = target.equals("net.minecraft.block.BlockFalling")
                || target.equals("net.minecraft.block.BlockDragonEgg");
        boolean beacon = target.equals("net.minecraft.tileentity.TileEntityBeacon");
        boolean entity = target.equals("net.minecraft.entity.item.EntityFallingBlock");
        if (bytes == null || !falling && !beacon && !entity) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int count = 0;
        for (MethodNode method : node.methods) {
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name, method.desc);
            if (falling && FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc).equals("(Lnet/minecraft/world/World;III)V")) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (!(insn instanceof JumpInsnNode)) continue;
                    int op = insn.getOpcode();
                    AbstractInsnNode previous = insn.getPrevious();
                    if (!(previous instanceof VarInsnNode) || previous.getOpcode() != ILOAD
                            || ((VarInsnNode)previous).var != 3
                            || op != IFLT && op != IFLE) continue;
                    method.instructions.insertBefore(insn, minimumFromArgument());
                    method.instructions.set(insn, new JumpInsnNode(op == IFLT ? IF_ICMPLT : IF_ICMPLE,
                            ((JumpInsnNode)insn).label));
                    count++;
                }
            }
            if (beacon && (mapped.equals("func_146003_y") || mapped.equals("updateBeacon"))
                    && method.desc.equals("()V")) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() != IFGE || !(insn.getPrevious() instanceof VarInsnNode)
                            || ((VarInsnNode)insn.getPrevious()).var != 3) continue;
                    method.instructions.insertBefore(insn, minimumFromField(method));
                    method.instructions.set(insn, new JumpInsnNode(IF_ICMPGE, ((JumpInsnNode)insn).label));
                    count++;
                }
            }
            if (entity && (mapped.equals("onUpdate") || mapped.equals("func_70071_h_"))) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() != ICONST_1 || !(insn.getNext() instanceof JumpInsnNode)
                            || insn.getNext().getOpcode() != IF_ICMPLT
                            || !(insn.getPrevious() instanceof VarInsnNode)
                            || ((VarInsnNode)insn.getPrevious()).var != 2) continue;
                    InsnList min = minimumFromField(method);
                    min.add(new InsnNode(ICONST_1)); min.add(new InsnNode(IADD));
                    method.instructions.insertBefore(insn, min);
                    method.instructions.remove(insn);
                    count++;
                }
            }
        }
        int expected = falling ? 3 : 1;
        if (count != expected) throw new IllegalStateException("CaveAbyss height anchors for " + target + ": " + count);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static InsnList minimumFromArgument() {
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 1));
        code.add(minimumCall());
        return code;
    }

    private static InsnList minimumFromField(MethodNode method) {
        FieldInsnNode world = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode) || instruction.getOpcode() != GETFIELD) continue;
            FieldInsnNode field = (FieldInsnNode)instruction;
            if (FMLDeobfuscatingRemapper.INSTANCE.mapDesc(field.desc).equals("Lnet/minecraft/world/World;")) {
                world = field; break;
            }
        }
        if (world == null) throw new IllegalStateException("CaveAbyss world field missing in " + method.name);
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, world.owner, world.name, world.desc));
        code.add(minimumCall());
        return code;
    }

    private static MethodInsnNode minimumCall() {
        return new MethodInsnNode(INVOKESTATIC, "ru/givler/caveabyss/core/MinusOneHooks",
                "minimumY", "(Lnet/minecraft/world/World;)I", false);
    }
}
