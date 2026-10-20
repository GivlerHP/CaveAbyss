package ru.givler.caveabyss.core;

import java.io.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Runs the actual patched RenderGlobal invalidation path without an OpenGL context. */
public final class RenderInvalidationSmoke {
    public static class Renderer {
        public boolean needsUpdate;
        public int invalidations;
        public void markDirty() { needsUpdate = true; invalidations++; }
    }
    static void check() throws Exception {
        String originalName = "net.minecraft.client.renderer.RenderGlobal";
        byte[] original;
        try (InputStream in = RenderInvalidationSmoke.class.getResourceAsStream("/" + originalName.replace('.', '/') + ".class")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            original = out.toByteArray();
        }
        byte[] patched = new NegativeRenderTransformer().transform(originalName, originalName, original);
        String generated = "ru/givler/caveabyss/core/GeneratedGlobalInvalidation";
        ClassWriter remapped = new ClassWriter(0);
        new ClassReader(patched).accept(new RemappingClassAdapter(remapped, new Remapper() {
            @Override public String map(String name) {
                if (name.equals(originalName.replace('.', '/'))) return generated;
                if (name.equals("net/minecraft/client/renderer/WorldRenderer")) return Type.getInternalName(Renderer.class);
                return name;
            }
        }), ClassReader.EXPAND_FRAMES);
        ClassNode node = new ClassNode();
        new ClassReader(remapped.toByteArray()).accept(node, 0);
        node.superName = "java/lang/Object";
        node.interfaces.clear(); node.innerClasses.clear();
        node.methods.removeIf(m -> !m.name.equals("markBlocksForUpdate"));
        node.fields.removeIf(f -> !Arrays.asList("renderChunksWide", "renderChunksTall", "renderChunksDeep", "worldRenderers", "worldRenderersToUpdate").contains(f.name));
        for (FieldNode field : node.fields) field.access = Opcodes.ACC_PUBLIC;
        MethodNode init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        init.instructions.add(new InsnNode(Opcodes.RETURN)); node.methods.add(init);
        ClassWriter output = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(output);
        Class<?> type = new ClassLoader(RenderInvalidationSmoke.class.getClassLoader()) {
            Class<?> loadFixture(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
        }.loadFixture(output.toByteArray());
        Object global = type.newInstance();
        for (String field : Arrays.asList("renderChunksWide", "renderChunksDeep")) type.getField(field).setInt(global, 1);
        type.getField("renderChunksTall").setInt(global, 20);
        Renderer[] sections = new Renderer[20];
        for (int i = 0; i < sections.length; i++) sections[i] = new Renderer();
        List<Renderer> queue = new ArrayList<>();
        type.getField("worldRenderers").set(global, sections);
        type.getField("worldRenderersToUpdate").set(global, queue);
        for (int i = 0; i < 20; i++) {
            int y = i * 16 - 64;
            for (int update = 0; update < 2; update++)
                type.getMethod("markBlocksForUpdate", int.class, int.class, int.class, int.class, int.class, int.class)
                    .invoke(global, 0, y, 0, 0, y, 0);
            if (sections[i].invalidations != 2 || Collections.frequency(queue, sections[i]) != 1)
                throw new AssertionError("Lost in-flight invalidation or duplicate queue entry at Y=" + y);
        }
        System.out.println("Render invalidation: all 20 sections invalidate in-flight builds without duplicate queue entries");
    }
}
