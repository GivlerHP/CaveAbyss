package ru.givler.caveabyss.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Tests core patches against the actual Forge development classes. */
public final class CorePatchSmoke {
    public static void main(String[] args) throws Exception {
        check(new WorldMinusOneTransformer(), "net.minecraft.world.World");
        check(new ChunkNegativeTransformer(), "net.minecraft.world.chunk.Chunk");
        check(new VoidVisualTransformer(), "net.minecraft.client.renderer.EntityRenderer");
        check(new VoidVisualTransformer(), "net.minecraft.client.multiplayer.WorldClient");
    }

    private static void check(IClassTransformer transformer, String name) throws Exception {
        InputStream stream = CorePatchSmoke.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class");
        if (stream == null) throw new AssertionError("Class not found: " + name);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
        stream.close();
        byte[] original = bytes.toByteArray();
        byte[] patched = transformer.transform(name, name, original);
        if (patched == null || patched.length == 0 || patched.length == original.length)
            throw new AssertionError("Class was not patched: " + name);
        new ClassReader(patched);
        if (name.equals("net.minecraft.world.chunk.Chunk")) {
            ClassNode node = new ClassNode();
            new ClassReader(patched).accept(node, 0);
            assertHook(node, "(Lnet/minecraft/world/EnumSkyBlock;III)I", "chunkGetSavedLight");
            assertHook(node, "(Lnet/minecraft/world/EnumSkyBlock;IIII)V", "chunkSetLight");
            assertHook(node, "(IIII)I", "chunkGetBlockLight");
        }
        System.out.println("Patched " + name);
    }

    private static void assertHook(ClassNode node, String methodDesc, String hook) {
        for (MethodNode method : node.methods) {
            if (!method.desc.equals(methodDesc)) continue;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("ru/givler/caveabyss/core/MinusOneHooks") && call.name.equals(hook)) return;
                }
            }
        }
        throw new AssertionError("Missing Chunk light hook: " + hook);
    }
}
