package ru.givler.caveabyss.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

/** Tests core patches against the actual Forge development classes. */
public final class CorePatchSmoke {
    public static void main(String[] args) throws Exception {
        check(new WorldMinusOneTransformer(), "net.minecraft.world.World");
        check(new WorldMinusOneTransformer(), "net.minecraft.world.WorldManager");
        check(new ChunkNegativeTransformer(), "net.minecraft.world.chunk.Chunk");
        check(new VoidVisualTransformer(), "net.minecraft.client.renderer.EntityRenderer");
        check(new VoidVisualTransformer(), "net.minecraft.client.multiplayer.WorldClient");
        check(new NegativeRenderTransformer(), "net.minecraft.client.renderer.RenderGlobal");
        check(new NegativeRenderTransformer(), "net.minecraft.world.ChunkCache");
        check(new DeepChunkGeneratorTransformer(), "net.minecraft.world.gen.ChunkProviderGenerate");
        check(new DeepChunkGeneratorTransformer(), "net.minecraft.world.gen.feature.WorldGenDungeons");
        check(new DeepChunkGeneratorTransformer(), "net.minecraft.world.gen.structure.StructureBoundingBox");
        assertDirectChunkStorage();
        ru.givler.caveabyss.data.StorageSmoke.check();
        ru.givler.caveabyss.world.DeepWorldGeneratorSmoke.check();
    }

    private static void assertDirectChunkStorage() throws Exception {
        InputStream stream = CorePatchSmoke.class.getClassLoader().getResourceAsStream(
                "ru/givler/caveabyss/core/MinusOneHooks.class");
        if (stream == null) throw new AssertionError("MinusOneHooks class missing");
        ClassNode node = new ClassNode();
        try {
            new ClassReader(stream).accept(node, 0);
        } finally {
            stream.close();
        }
        assertChunkStorageCall(node, "chunkGetBlock", "getBlock");
        assertChunkStorageCall(node, "chunkGetMetadata", "getMetadata");
        assertChunkStorageCall(node, "chunkGetSavedLight", "getLight");
    }

    private static void assertChunkStorageCall(ClassNode node, String hook, String storageMethod) {
        for (MethodNode method : node.methods) {
            if (!method.name.equals(hook)) continue;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals("ru/givler/caveabyss/data/MinusOneLayer")
                        && call.name.equals(storageMethod)
                        && call.desc.startsWith("(Lnet/minecraft/world/chunk/Chunk;")) return;
            }
        }
        throw new AssertionError("Chunk hook still loads its own chunk: " + hook);
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
        StringWriter verification = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(patched), CorePatchSmoke.class.getClassLoader(), false,
                new PrintWriter(verification));
        if (verification.getBuffer().length() != 0)
            throw new AssertionError("Invalid transformed bytecode in " + name + ": " + verification);
        if (name.equals("net.minecraft.world.chunk.Chunk")) {
            ClassNode node = new ClassNode();
            new ClassReader(patched).accept(node, 0);
            assertHook(node, "(Lnet/minecraft/world/EnumSkyBlock;III)I", "chunkGetSavedLight");
            assertHook(node, "(Lnet/minecraft/world/EnumSkyBlock;IIII)V", "chunkSetLight");
            assertHook(node, "(IIII)I", "chunkGetBlockLight");
            assertNegativeBoundary(node, "(IIILnet/minecraft/tileentity/TileEntity;)V");
        }
        if (name.equals("net.minecraft.world.World")) {
            ClassNode node = new ClassNode();
            new ClassReader(patched).accept(node, 0);
            assertNegativeBoundary(node, "(III)Lnet/minecraft/tileentity/TileEntity;");
        }
        if (name.equals("net.minecraft.client.renderer.RenderGlobal")) {
            ClassNode node = new ClassNode();
            new ClassReader(patched).accept(node, 0);
            boolean skyHook = false;
            for (MethodNode method : node.methods) {
                if (!method.desc.equals("(F)V")) continue;
                for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("ru/givler/caveabyss/client/SkyRenderHooks")
                            && call.name.equals("adjustHorizon")) skyHook = true;
                }
            }
            if (!skyHook) throw new AssertionError("RenderGlobal sky horizon hook missing");
        }
        System.out.println("Patched " + name);
    }

    private static void assertNegativeBoundary(ClassNode node, String descriptor) {
        for (MethodNode method : node.methods) {
            if (!method.desc.equals(descriptor)) continue;
            boolean minus64 = false, lowerBranch = false;
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction instanceof IntInsnNode && ((IntInsnNode) instruction).operand == -64) minus64 = true;
                if (instruction instanceof JumpInsnNode && instruction.getOpcode() == Opcodes.IF_ICMPLT) lowerBranch = true;
            }
            if (minus64 && lowerBranch) return;
        }
        throw new AssertionError("Missing negative tile entity boundary: " + descriptor);
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
