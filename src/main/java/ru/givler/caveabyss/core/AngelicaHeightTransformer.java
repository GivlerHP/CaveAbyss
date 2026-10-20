package ru.givler.caveabyss.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Adapts Angelica 2.2's Celeritas snapshots without linking its optional classes. */
public final class AngelicaHeightTransformer implements IClassTransformer, Opcodes {
    private static final String ROOT = "com/gtnewhorizons/angelica/rendering/celeritas/";
    private static final String HOOK = "ru/givler/caveabyss/core/AngelicaHeightHooks";
    private static final String WORLD = "Lnet/minecraft/world/World;";

    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null) return bytes;
        String target = transformedName.replace('.', '/');
        if (!target.equals(ROOT + "AngelicaRenderSectionManager") && !target.equals(ROOT + "CeleritasWorldRenderer")
                && !target.equals(ROOT + "world/WorldSlice") && !target.equals(ROOT + "world/cloned/ClonedChunkSection")) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changed = 0;
        for (MethodNode m : node.methods) {
            if (target.endsWith("WorldSlice") && m.name.equals("getLightLevel")) {
                for (AbstractInsnNode i : m.instructions.toArray()) if (i instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode)i;
                    if (call.name.equals("clampHeight") && call.desc.equals("(I)I")) {
                        m.instructions.insertBefore(i, world(node, "Lnet/minecraft/client/multiplayer/WorldClient;"));
                        m.instructions.set(i, new MethodInsnNode(INVOKESTATIC, HOOK, "clampHeight", "(I" + WORLD + ")I", false));
                        changed++;
                    }
                }
            }
            if (target.endsWith("AngelicaRenderSectionManager") && m.name.equals("<init>")) {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new VarInsnNode(ALOAD, 2));
                list.add(new MethodInsnNode(INVOKESTATIC, HOOK, "minimumSection", "(I" + WORLD + ")I", false));
                list.add(new VarInsnNode(ISTORE, 5));
                m.instructions.insert(list);
                changed++;
            }
            if (target.endsWith("AngelicaRenderSectionManager") && m.name.equals("isSectionVisuallyEmpty")) {
                for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == IRETURN) {
                    InsnList list = world(node, "Lnet/minecraft/client/multiplayer/WorldClient;");
                    list.add(new VarInsnNode(ILOAD, 1));
                    list.add(new VarInsnNode(ILOAD, 2));
                    list.add(new VarInsnNode(ILOAD, 3));
                    list.add(new MethodInsnNode(INVOKESTATIC, HOOK, "visuallyEmpty", "(Z" + WORLD + "III)Z", false));
                    m.instructions.insertBefore(i, list);
                }
                changed++;
            }
            boolean minimum = m.name.equals("getMinimumBuildHeight") || m.name.equals("getRawSkyLight")
                    || m.desc.equals("(IIII)I") && target.endsWith("WorldSlice");
            if (minimum) {
                for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == ICONST_0) {
                    InsnList list = world(node, target.endsWith("CeleritasWorldRenderer") ? "Ljava/lang/Object;" : "Lnet/minecraft/client/multiplayer/WorldClient;");
                    list.add(new MethodInsnNode(INVOKESTATIC, "ru/givler/caveabyss/core/MinusOneHooks", "minimumY", "(" + WORLD + ")I", false));
                    m.instructions.insertBefore(i, list);
                    m.instructions.remove(i);
                    changed++;
                    break;
                }
            }
            if (m.name.equals("isOutOfBuildLimitVertically")) {
                for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == IFLT) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(ALOAD, 0));
                    list.add(new MethodInsnNode(INVOKESTATIC, "ru/givler/caveabyss/core/MinusOneHooks", "minimumY", "(" + WORLD + ")I", false));
                    m.instructions.insertBefore(i, list);
                    m.instructions.set(i, new JumpInsnNode(IF_ICMPLT, ((JumpInsnNode)i).label));
                    changed++;
                }
            }
            if (m.name.equals("prepare") || m.name.equals("getChunkSection")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (!(i instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode)i;
                    if (!call.owner.equals("net/minecraft/world/chunk/Chunk") || !call.desc.equals("()[Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;")) continue;
                    AbstractInsnNode array = i.getNext();
                    while (array != null && array.getOpcode() != AALOAD) array = array.getNext();
                    if (array == null) throw new IllegalStateException("Angelica section array anchor missing");
                    m.instructions.remove(i);
                    m.instructions.set(array, new MethodInsnNode(INVOKESTATIC, HOOK, "section", "(Lnet/minecraft/world/chunk/Chunk;I)Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;", false));
                    changed++;
                }
            }
        }
        int expected = target.endsWith("WorldSlice") ? 4 : 2;
        if (target.endsWith("CeleritasWorldRenderer")) expected = 1;
        if (changed != expected) throw new IllegalStateException("CaveAbyss Angelica anchors " + target + ": " + changed + " expected " + expected);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static InsnList world(ClassNode node, String descriptor) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new FieldInsnNode(GETFIELD, node.name, "world", descriptor));
        if (descriptor.equals("Ljava/lang/Object;")) list.add(new TypeInsnNode(CHECKCAST, "net/minecraft/world/World"));
        return list;
    }
}
