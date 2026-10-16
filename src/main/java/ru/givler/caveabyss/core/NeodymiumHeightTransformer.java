package ru.givler.caveabyss.core;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Maps Neodymium's section storage to Y=-64..255, keeping mesh coordinates signed. */
public final class NeodymiumHeightTransformer implements IClassTransformer, Opcodes {
    private static final String CHUNK = "makamys/neodymium/renderer/NeoChunk";
    private static final String RENDERER = "makamys/neodymium/renderer/NeoRenderer";
    private static final String MESH = "makamys/neodymium/renderer/ChunkMesh";

    @Override public byte[] transform(String name, String target, byte[] bytes) {
        if (bytes == null || !target.equals(CHUNK.replace('/', '.')) && !target.equals(RENDERER.replace('/', '.'))
                && !target.equals(MESH.replace('/', '.')))
            return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);
        if (node.name.equals(CHUNK)) patchChunk(node);
        else if (node.name.equals(MESH)) patchVertexHeight(node);
        else patchRenderer(node);
        ClassWriter output = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(output);
        return output.toByteArray();
    }

    private static void patchVertexHeight(ClassNode node) {
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("addTessellatorData")) continue;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!call.owner.equals("makamys/neodymium/renderer/NeoRegion")
                        || !call.name.equals("toRelativeOffset") || !call.desc.equals("(D)F")) continue;
                AbstractInsnNode negation = previous(call);
                AbstractInsnNode load = previous(negation);
                if (negation == null || negation.getOpcode() != DNEG || !(load instanceof FieldInsnNode)) continue;
                FieldInsnNode field = (FieldInsnNode) load;
                String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(field.owner, field.name, field.desc);
                if (!mapped.equals("yOffset") && !mapped.equals("field_78407_w")) continue;
                // Regions wrap horizontally every 1024 blocks, but their vertical origin
                // is always zero. Preserve signed world Y instead of wrapping -64 to 960.
                method.instructions.set(call, new InsnNode(D2F));
                patched++;
            }
        }
        require(patched, 1, "signed vertex height");
    }

    private static void patchChunk(ClassNode node) {
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<init>")) continue;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != PUTFIELD) continue;
                FieldInsnNode field = (FieldInsnNode) insn;
                int before, after;
                if (field.name.equals("chunkMeshes")) { before = 32; after = 40; }
                else if (field.name.equals("isSectionVisible")) { before = 16; after = 20; }
                else continue;
                AbstractInsnNode allocation = previous(insn);
                AbstractInsnNode size = previous(allocation);
                if (allocation == null || allocation.getOpcode() != ANEWARRAY && allocation.getOpcode() != NEWARRAY
                        || !(size instanceof IntInsnNode) || ((IntInsnNode) size).operand != before)
                    throw new IllegalStateException("CaveAbyss Neodymium array pattern changed: " + field.name);
                method.instructions.set(size, new IntInsnNode(BIPUSH, after));
                patched++;
            }
        }
        require(patched, 2, "arrays");
    }

    private static void patchRenderer(ClassNode node) {
        int coordinates = 0, loops = 0, deletions = 0;
        for (MethodNode method : node.methods) {
            if (method.name.equals("onWorldRendererChanged") || method.name.equals("onWorldRendererPost")) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (!(insn instanceof FieldInsnNode) || insn.getOpcode() != GETFIELD) continue;
                    FieldInsnNode field = (FieldInsnNode) insn;
                    String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(field.owner, field.name, field.desc);
                    if (!mapped.equals("posY") && !mapped.equals("field_78920_d")) continue;
                    AbstractInsnNode divisor = next(insn);
                    AbstractInsnNode division = next(divisor);
                    AbstractInsnNode store = next(division);
                    if (!(divisor instanceof IntInsnNode) || ((IntInsnNode) divisor).operand != 16
                            || !(division instanceof MethodInsnNode) || !((MethodInsnNode) division).owner.equals("java/lang/Math")
                            || !((MethodInsnNode) division).name.equals("floorDiv")
                            || !(store instanceof VarInsnNode) || store.getOpcode() != ISTORE)
                        throw new IllegalStateException("CaveAbyss Neodymium section coordinate pattern changed");
                    InsnList offset = new InsnList();
                    offset.add(new InsnNode(ICONST_4));
                    offset.add(new InsnNode(IADD));
                    method.instructions.insert(division, offset);
                    // Ignore temporary renderer positions outside the supported vertical range.
                    int slot = ((VarInsnNode) store).var;
                    if (slot != 4) throw new IllegalStateException("CaveAbyss Neodymium section local changed");
                    Type[] args = Type.getArgumentTypes(method.desc);
                    Object[] locals = {node.name, args[0].getInternalName(),
                            args[1].getSort() == Type.OBJECT ? args[1].getInternalName() : INTEGER,
                            INTEGER, INTEGER};
                    InsnList guard = new InsnList();
                    LabelNode invalid = new LabelNode(), valid = new LabelNode();
                    guard.add(new VarInsnNode(ILOAD, slot));
                    guard.add(new JumpInsnNode(IFLT, invalid));
                    guard.add(new VarInsnNode(ILOAD, slot));
                    guard.add(new IntInsnNode(BIPUSH, 20));
                    guard.add(new JumpInsnNode(IF_ICMPLT, valid));
                    guard.add(invalid);
                    guard.add(new FrameNode(F_NEW, locals.length, locals, 0, new Object[0]));
                    guard.add(new InsnNode(RETURN));
                    guard.add(valid);
                    guard.add(new FrameNode(F_NEW, locals.length, locals, 0, new Object[0]));
                    method.instructions.insert(store, guard);
                    coordinates++;
                }
            }
            if (method.name.equals("neoChunkChanged")) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (!(insn instanceof IntInsnNode) || ((IntInsnNode) insn).operand != 16
                            || next(insn).getOpcode() != IF_ICMPGE) continue;
                    method.instructions.set(insn, new IntInsnNode(BIPUSH, 20));
                    loops++;
                }
            }
            if (method.name.equals("onWorldRendererChanged")) {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (!(insn instanceof FieldInsnNode) || !((FieldInsnNode) insn).name.equals("DELETED")) continue;
                    AbstractInsnNode branch = next(insn);
                    if (!(branch instanceof JumpInsnNode) || branch.getOpcode() != IF_ACMPNE)
                        throw new IllegalStateException("CaveAbyss Neodymium deletion branch changed");
                    LabelNode end = ((JumpInsnNode) branch).label;
                    AbstractInsnNode start = next(branch);
                    // The installed 0.4.3 method keeps NeoChunk in local 6, section index in 4.
                    if (!(start instanceof VarInsnNode) || start.getOpcode() != ALOAD || ((VarInsnNode) start).var != 0
                            || !(next(start) instanceof VarInsnNode) || ((VarInsnNode) next(start)).var != 6)
                        throw new IllegalStateException("CaveAbyss Neodymium deletion locals changed");
                    for (AbstractInsnNode old = branch.getNext(); old != end;) {
                        AbstractInsnNode following = old.getNext();
                        method.instructions.remove(old);
                        old = following;
                    }
                    InsnList clear = new InsnList();
                    clear.add(new VarInsnNode(ALOAD, 6));
                    clear.add(new VarInsnNode(ILOAD, 4));
                    clear.add(new MethodInsnNode(INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false));
                    clear.add(new InsnNode(ICONST_0));
                    clear.add(new MethodInsnNode(INVOKEVIRTUAL, CHUNK, "putChunkMeshes", "(ILjava/util/List;Z)V", false));
                    method.instructions.insert(branch, clear);
                    deletions++;
                }
            }
        }
        require(coordinates, 2, "section coordinates");
        require(loops, 1, "visibility loop");
        require(deletions, 1, "two-pass deletion");
    }

    private static AbstractInsnNode next(AbstractInsnNode insn) {
        if (insn == null) return null;
        do { insn = insn.getNext(); } while (insn != null && insn.getOpcode() < 0);
        return insn;
    }

    private static AbstractInsnNode previous(AbstractInsnNode insn) {
        if (insn == null) return null;
        do { insn = insn.getPrevious(); } while (insn != null && insn.getOpcode() < 0);
        return insn;
    }

    private static void require(int actual, int expected, String patch) {
        if (actual != expected) throw new IllegalStateException("CaveAbyss Neodymium " + patch + ": " + actual + ", expected " + expected);
    }
}
