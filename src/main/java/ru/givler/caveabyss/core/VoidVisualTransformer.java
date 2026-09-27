package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Removes the vanilla near-bedrock fog and depthsuspend particles. */
public final class VoidVisualTransformer implements IClassTransformer, Opcodes {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return bytes;
        boolean renderer = transformedName.equals("net.minecraft.client.renderer.EntityRenderer");
        boolean world = transformedName.equals("net.minecraft.client.multiplayer.WorldClient");
        if (!renderer && !world) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name, method.desc);
            if (renderer && method.desc.equals("(F)V") && (mapped.equals("updateFogColor") || mapped.equals("func_78466_h"))) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode)insn;
                    if (!call.desc.equals("()D") || !call.name.equals("getVoidFogYFactor") && !call.name.equals("func_76565_k")) continue;
                    AbstractInsnNode store = insn.getNext().getNext();
                    if (!(store instanceof VarInsnNode) || store.getOpcode() != DSTORE) throw new IllegalStateException("CaveAbyss fog color pattern changed");
                    InsnList code = new InsnList();
                    code.add(new InsnNode(DCONST_1));
                    code.add(new VarInsnNode(DSTORE, ((VarInsnNode)store).var));
                    method.instructions.insert(store, code);
                    patched++;
                    break;
                }
            } else if (renderer && method.desc.equals("(IF)V") && (mapped.equals("setupFog") || mapped.equals("func_78468_a"))) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode)insn;
                    if (!call.desc.equals("()Z") || !call.name.equals("getWorldHasVoidParticles") && !call.name.equals("func_76564_j")) continue;
                    method.instructions.insertBefore(call, new InsnNode(POP));
                    method.instructions.set(call, new InsnNode(ICONST_0));
                    patched++;
                    break;
                }
            } else if (world && method.desc.equals("(III)V") && (mapped.equals("doVoidFogParticles") || mapped.equals("func_73029_E"))) {
                method.instructions.insert(new InsnNode(RETURN));
                patched++;
            }
        }
        if (patched != (renderer ? 2 : 1)) throw new IllegalStateException("CaveAbyss void visual patch count=" + patched + " class=" + transformedName);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
