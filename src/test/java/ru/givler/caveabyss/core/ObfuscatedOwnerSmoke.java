package ru.givler.caveabyss.core;

import com.google.common.collect.HashBiMap;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Exercises pre-FML method lookup with mappings keyed by the bytecode owner. */
final class ObfuscatedOwnerSmoke {
    static void check() throws Exception {
        check("net.minecraft.world.World", "smoke/World", new WorldMinusOneTransformer());
        check("net.minecraft.world.chunk.Chunk", "smoke/Chunk", new ChunkNegativeTransformer());
    }

    private static void check(String target, String owner, IClassTransformer transformer) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = ObfuscatedOwnerSmoke.class.getClassLoader()
                .getResourceAsStream(target.replace('.', '/') + ".class")) {
            if (input == null) throw new AssertionError("Missing " + target);
            new ClassReader(input).accept(node, 0);
        }
        node.name = owner;
        Map<String, String> methods = new HashMap<>();
        int index = 0;
        for (MethodNode method : node.methods) {
            if (method.name.startsWith("<")) continue;
            String original = method.name;
            method.name = "obf" + index++;
            methods.put(method.name + method.desc, original);
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);

        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        Field classesField = FMLDeobfuscatingRemapper.class.getDeclaredField("classNameBiMap");
        Field methodsField = FMLDeobfuscatingRemapper.class.getDeclaredField("methodNameMaps");
        classesField.setAccessible(true);
        methodsField.setAccessible(true);
        Object oldClasses = classesField.get(remapper);
        Object oldMethods = methodsField.get(remapper);
        try {
            Map<String, Map<String, String>> owners = new HashMap<>();
            owners.put(owner, methods);
            // A wrong deobfuscated owner must not accidentally resolve the same methods.
            owners.put(target.replace('.', '/'), new HashMap<String, String>());
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    String callOwner = ((MethodInsnNode) instruction).owner;
                    if (!owners.containsKey(callOwner))
                        owners.put(callOwner, new HashMap<String, String>());
                }
            }
            com.google.common.collect.BiMap<String, String> classes = HashBiMap.create();
            classes.put(owner, target.replace('.', '/'));
            classesField.set(remapper, classes);
            methodsField.set(remapper, owners);
            byte[] original = writer.toByteArray();
            byte[] patched = transformer.transform(owner.replace('/', '.'), target, original);
            if (patched.length == original.length) throw new AssertionError("Not patched: " + target);
            System.out.println("Patched obfuscated owner " + target);
        } finally {
            classesField.set(remapper, oldClasses);
            methodsField.set(remapper, oldMethods);
        }
    }
}
