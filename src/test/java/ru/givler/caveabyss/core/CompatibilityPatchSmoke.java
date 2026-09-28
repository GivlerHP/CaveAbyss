package ru.givler.caveabyss.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies optional-provider hooks without loading RTG or CraftBukkit. */
public final class CompatibilityPatchSmoke implements Opcodes {
    public static void check() {
        checkRtg();
        checkBundledRtg();
        checkBukkit("org.bukkit.craftbukkit.CraftWorld", "getBlockAt", "(III)Ljava/lang/Object;", false);
        checkBukkit("org.bukkit.craftbukkit.CraftChunk", "getBlock", "(III)Ljava/lang/Object;", false);
        checkBukkit("org.bukkit.craftbukkit.block.CraftBlock", "getTypeId", "()I", true);
    }

    private static void checkBundledRtg() {
        File file = new File("libs/RTG-1.7.10-1.1.1.7.jar");
        if (!file.isFile()) return;
        try (JarFile jar = new JarFile(file)) {
            InputStream stream = jar.getInputStream(jar.getJarEntry("rtg/world/gen/ChunkProviderRTG.class"));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int count; (count = stream.read(buffer)) >= 0; ) bytes.write(buffer, 0, count);
            stream.close();
            String name = "rtg.world.gen.ChunkProviderRTG";
            ClassNode patched = new ClassNode();
            new ClassReader(new RtgChunkGeneratorTransformer().transform(name, name, bytes.toByteArray()))
                    .accept(patched, 0);
            if (calls(patched, "prepareTerrain") != 1 || calls(patched, "onProvideChunk") != 1)
                throw new AssertionError("Bundled RTG chunk provider was not hooked");
        } catch (Exception error) {
            throw new AssertionError("Cannot patch bundled RTG", error);
        }
    }

    private static void checkRtg() {
        ClassNode node = new ClassNode();
        node.version = V1_6;
        node.access = ACC_PUBLIC;
        node.name = "rtg/world/gen/ChunkProviderRTG";
        node.superName = "java/lang/Object";
        node.fields.add(new org.objectweb.asm.tree.FieldNode(ACC_PRIVATE, "worldObj",
                "Lnet/minecraft/world/World;", null, null));
        MethodNode method = new MethodNode(ACC_PUBLIC, "provideChunk",
                "(II)Lnet/minecraft/world/chunk/Chunk;", null, null);
        method.instructions.add(new InsnNode(ICONST_1));
        method.instructions.add(new TypeInsnNode(ANEWARRAY, "net/minecraft/block/Block"));
        method.instructions.add(new VarInsnNode(ASTORE, 3));
        method.instructions.add(new TypeInsnNode(NEW, "net/minecraft/world/chunk/Chunk"));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new FieldInsnNode(GETFIELD, node.name, "worldObj", "Lnet/minecraft/world/World;"));
        method.instructions.add(new VarInsnNode(ALOAD, 3));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new VarInsnNode(ILOAD, 1));
        method.instructions.add(new VarInsnNode(ILOAD, 2));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, "net/minecraft/world/chunk/Chunk",
                "<init>", "(Lnet/minecraft/world/World;[Lnet/minecraft/block/Block;[BII)V", false));
        method.instructions.add(new InsnNode(ARETURN));
        method.maxLocals = 4;
        node.methods.add(method);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        byte[] changed = new RtgChunkGeneratorTransformer().transform(node.name.replace('/', '.'),
                node.name.replace('/', '.'), writer.toByteArray());
        ClassNode patched = new ClassNode();
        new ClassReader(changed).accept(patched, 0);
        if (calls(patched, "prepareTerrain") != 1 || calls(patched, "onProvideChunk") != 1
                || calls(patched, "prepareMineshafts") != 1)
            throw new AssertionError("RTG chunk hooks missing");
    }

    private static void checkBukkit(String name, String methodName, String desc, boolean fieldY) {
        ClassNode node = new ClassNode();
        node.version = V1_6;
        node.access = ACC_PUBLIC;
        node.name = name.replace('.', '/');
        node.superName = "java/lang/Object";
        MethodNode method = new MethodNode(ACC_PUBLIC, methodName, desc, null, null);
        if (fieldY) {
            node.fields.add(new org.objectweb.asm.tree.FieldNode(ACC_PRIVATE, "y", "I", null, null));
            method.instructions.add(new VarInsnNode(ALOAD, 0));
            method.instructions.add(new FieldInsnNode(GETFIELD, node.name, "y", "I"));
        } else method.instructions.add(new VarInsnNode(ILOAD, 2));
        method.instructions.add(new IntInsnNode(SIPUSH, 255));
        method.instructions.add(new InsnNode(IAND));
        method.instructions.add(new InsnNode(POP));
        if (desc.endsWith("I")) {
            method.instructions.add(new InsnNode(ICONST_0));
            method.instructions.add(new InsnNode(IRETURN));
        } else {
            method.instructions.add(new InsnNode(ACONST_NULL));
            method.instructions.add(new InsnNode(ARETURN));
        }
        node.methods.add(method);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        ClassNode patched = new ClassNode();
        new ClassReader(new CauldronBukkitTransformer().transform(name, name, writer.toByteArray()))
                .accept(patched, 0);
        for (MethodNode changed : patched.methods)
            for (AbstractInsnNode insn = changed.instructions.getFirst(); insn != null; insn = insn.getNext())
                if (insn.getOpcode() == IAND) throw new AssertionError("Bukkit still masks Y: " + name);
    }

    private static int calls(ClassNode node, String name) {
        int count = 0;
        for (MethodNode method : node.methods)
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext())
                if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).name.equals(name)) count++;
        return count;
    }
}
