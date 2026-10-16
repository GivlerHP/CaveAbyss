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
                    String callName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(call.owner, call.name, call.desc);
                    if (!call.desc.equals("()D") || !callName.equals("getVoidFogYFactor") && !callName.equals("func_76565_k")) continue;
                    // OptiFine stores the provider factor before multiplying by camera Y.
                    // Override the resulting depth term, rather than that intermediate factor.
                    AbstractInsnNode store = null;
                    AbstractInsnNode candidate = insn.getNext();
                    for (int step = 0; candidate != null && step < 48; step++, candidate = candidate.getNext()) {
                        if (candidate.getOpcode() != DMUL) continue;
                        AbstractInsnNode next = candidate.getNext();
                        while (next != null && next.getOpcode() < 0) next = next.getNext();
                        if (next instanceof VarInsnNode && next.getOpcode() == DSTORE) {
                            store = next;
                            break;
                        }
                    }
                    if (store == null) throw new IllegalStateException("CaveAbyss fog color pattern changed");
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
                    String callName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(call.owner, call.name, call.desc);
                    if (!call.desc.equals("()Z") || !callName.equals("getWorldHasVoidParticles") && !callName.equals("func_76564_j")) continue;
                    method.instructions.insertBefore(call, new InsnNode(POP));
                    method.instructions.set(call, new InsnNode(ICONST_0));
                    patched++;
                    break;
                }
            } else if (world && method.desc.equals("(III)V") && (mapped.equals("doVoidFogParticles") || mapped.equals("func_73029_E"))) {
                // This method also calls Block.randomDisplayTick. Keep that path so
                // negative-Y ores can play sounds and display their own particles.
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    String callName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(call.owner, call.name, call.desc);
                    if (!call.desc.equals("()Z") || !callName.equals("getWorldHasVoidParticles")
                            && !callName.equals("func_76564_j")) continue;
                    method.instructions.insertBefore(call, new InsnNode(POP));
                    method.instructions.set(call, new InsnNode(ICONST_0));
                    patched++;
                    break;
                }
            }
        }
        if (patched != (renderer ? 2 : 1)) throw new IllegalStateException("CaveAbyss void visual patch count=" + patched + " class=" + transformedName);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
