package ru.givler.caveabyss.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.*;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

/** Runs every vanilla target against Forge's actual pre-remap production bytecode. */
public final class ProductionTransformSmoke {
    private static final Map<String, ClassNode> headers = new HashMap<>();
    private static final Map<String, Map<String, String>> methods = new HashMap<>();
    private static final Map<String, Map<String, String>> fields = new HashMap<>();
    private static final BiMap<String, String> classes = HashBiMap.create();
    private static final Map<String, String> mcpNames = new HashMap<>();
    private static Map<String, String> map(Map<String, Map<String, String>> maps, String owner) {
        return maps.computeIfAbsent(owner, key -> new HashMap<String, String>());
    }
    private static String owner(String member) { return member.substring(0, member.lastIndexOf('/')); }
    private static String member(String member) { return member.substring(member.lastIndexOf('/') + 1); }

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        for (String line : Files.readAllLines(new File(root, "srgs/srg-mcp.srg").toPath(), StandardCharsets.UTF_8)) {
            String[] parts = line.split(" ");
            if (parts[0].equals("MD:")) mcpNames.put(member(parts[1]), member(parts[3]));
            if (parts[0].equals("FD:")) mcpNames.put(member(parts[1]), member(parts[2]));
        }
        try (JarFile jar = new JarFile(new File(root, "forge-binpatched-1.7.10-10.13.4.1614-1.7.10.jar"))) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode node = new ClassNode();
                try (InputStream input = jar.getInputStream(entry)) {
                    new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                headers.put(node.name, node);
            }
            for (String line : Files.readAllLines(new File(root, "srgs/notch-srg.srg").toPath(), StandardCharsets.UTF_8)) {
                String[] parts = line.split(" ");
                if (parts[0].equals("CL:")) classes.put(parts[1], parts[2]);
                if (parts[0].equals("MD:")) map(methods, owner(parts[1])).put(member(parts[1]) + parts[2], member(parts[3]));
                if (parts[0].equals("FD:")) {
                    ClassNode node = headers.get(owner(parts[1]));
                    if (node != null) for (FieldNode field : node.fields)
                        if (field.name.equals(member(parts[1])))
                            map(fields, node.name).put(field.name + ":" + field.desc, member(parts[2]));
                }
            }
            for (String name : headers.keySet()) inherit(name, new HashSet<String>());
            // External owners have no Minecraft mappings; avoid loading a live LaunchClassLoader.
            Map<String, Map<String, String>> methodLookup = defaults(methods);
            Map<String, Map<String, String>> fieldLookup = defaults(fields);
            Object[] saved = new Object[3];
            String[] names = {"classNameBiMap", "methodNameMaps", "fieldNameMaps"};
            Object[] values = {classes, methodLookup, fieldLookup};
            Field[] reflection = new Field[3];
            for (int i = 0; i < 3; i++) {
                reflection[i] = FMLDeobfuscatingRemapper.class.getDeclaredField(names[i]);
                reflection[i].setAccessible(true);
                saved[i] = reflection[i].get(FMLDeobfuscatingRemapper.INSTANCE);
                reflection[i].set(FMLDeobfuscatingRemapper.INSTANCE, values[i]);
            }
            List<String> failures = new ArrayList<>();
            int checked = 0;
            IClassTransformer optifine = null;
            URLClassLoader optifineLoader = null;
            if (args.length > 1 && new File(args[1], "1.7.10/OptiFine_1.7.10_HD_U_E7.jar").isFile()) {
                File optifineJar = new File(args[1], "1.7.10/OptiFine_1.7.10_HD_U_E7.jar");
                optifineLoader = new URLClassLoader(new URL[] {optifineJar.toURI().toURL(), new File(args[2]).toURI().toURL()}, ProductionTransformSmoke.class.getClassLoader());
                optifine = (IClassTransformer) Class.forName("optifine.OptiFineClassTransformer", true, optifineLoader).getDeclaredConstructor().newInstance();
            }
            try {
                for (String transformerName : new CaveAbyssCorePlugin().getASMTransformerClass()) {
                    IClassTransformer transformer = (IClassTransformer) Class.forName(transformerName).getDeclaredConstructor().newInstance();
                    // Targets are shared with the development smoke suite, so additions cannot silently escape this check.
                    String source = new String(Files.readAllBytes(new File("src/test/java/ru/givler/caveabyss/core/CorePatchSmoke.java").toPath()), StandardCharsets.UTF_8);
                    java.util.regex.Matcher targets = java.util.regex.Pattern.compile("check\\(new " + transformer.getClass().getSimpleName() + "\\(\\), \"([^\"]+)\"\\)").matcher(source);
                    while (targets.find()) {
                        String target = targets.group(1);
                        if (!target.startsWith("net.minecraft.")) continue;
                        String obf = classes.inverse().get(target.replace('.', '/'));
                        if (obf == null) throw new AssertionError("No production mapping for " + target);
                        byte[] original;
                        try (InputStream input = jar.getInputStream(jar.getJarEntry(obf + ".class"))) {
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            byte[] buffer = new byte[8192];
                            for (int n; (n = input.read(buffer)) != -1;) output.write(buffer, 0, n);
                            original = output.toByteArray();
                        }
                        checked++;
                        try {
                            byte[] patched = transformer.transform(obf.replace('/', '.'), target, original);
                            if (Arrays.equals(original, patched)) throw new AssertionError("No patches applied");
                            new ClassReader(patched).accept(new ClassNode(), 0);
                            verify(remap(patched, true), target);
                            byte[] srg = remap(original, false);
                            byte[] patchedSrg = transformer.transform(target, target, srg);
                            if (Arrays.equals(srg, patchedSrg)) throw new AssertionError("No SRG patches applied");
                            verify(remap(patchedSrg, true), target);
                            if (optifine != null) {
                                byte[] withOptifine = optifine.transform(obf.replace('/', '.'), target, original);
                                if (!Arrays.equals(original, withOptifine)) {
                                    byte[] compat = transformer.transform(obf.replace('/', '.'), target, withOptifine);
                                    if (Arrays.equals(withOptifine, compat)) throw new AssertionError("No OptiFine patches applied");
                                    verifyStack(compat);
                                    System.out.println("OptiFine anchors passed " + target);
                                }
                            }
                            System.out.println("Production patched " + target);
                        } catch (Throwable error) {
                            failures.add(target + ": " + error);
                        }
                    }
                }
                if (args.length > 1) checkMods(new File(args[1]));
            } finally {
                for (int i = 0; i < 3; i++) reflection[i].set(FMLDeobfuscatingRemapper.INSTANCE, saved[i]);
                if (optifineLoader != null) optifineLoader.close();
            }
            if (!failures.isEmpty()) throw new AssertionError("Production transform failures:\n" + String.join("\n", failures));
            if (checked < 24) throw new AssertionError("Missing production targets: " + checked);
            System.out.println("Production targets passed: " + checked);
        }
    }

    private static void checkMods(File directory) throws Exception {
        checkMod(new File(directory, "BiomesOPlenty-1.7.10-2.1.0.2308-universal.jar"), new BopPlantSupportTransformer(),
                "biomesoplenty.common.blocks.BlockBOPMushroom", "biomesoplenty.common.blocks.BlockMoss", "biomesoplenty.common.blocks.BlockBOPFlower2");
        checkMod(new File(directory, "RTG-1.7.10-1.1.1.7.jar"), new RtgChunkGeneratorTransformer(), "rtg.world.gen.ChunkProviderRTG");
        File neodymium = new File(directory, "neodymium-mc1.7.10-0.4.3-unofficial.jar");
        if (neodymium.isFile()) {
            checkMod(neodymium, new NeodymiumHeightTransformer(), "makamys.neodymium.renderer.NeoChunk", "makamys.neodymium.renderer.NeoRenderer", "makamys.neodymium.renderer.ChunkMesh");
            NeodymiumHeightSmoke.check(neodymium);
            NeodymiumVertexSmoke.check(neodymium);
        }
    }

    private static void checkMod(File file, IClassTransformer transformer, String... targets) throws Exception {
        try (JarFile jar = new JarFile(file)) {
            for (String target : targets) {
                byte[] original;
                try (InputStream input = jar.getInputStream(jar.getJarEntry(target.replace('.', '/') + ".class"))) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    for (int n; (n = input.read(buffer)) != -1;) output.write(buffer, 0, n);
                    original = output.toByteArray();
                }
                byte[] patched = transformer.transform(target, target, original);
                if (Arrays.equals(original, patched)) throw new AssertionError("No mod patches applied: " + target);
                verifyStack(patched);
                System.out.println("Installed mod anchors passed " + target);
            }
        }
    }

    // Optional mods add types absent from the development classpath. Check their
    // stack categories without resolving those types or initializing the game.
    private static void verifyStack(byte[] bytes) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) {
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) continue;
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name, method);
        }
    }

    private static byte[] remap(byte[] bytes, final boolean mcp) {
        ClassWriter output = new ClassWriter(0);
        new ClassReader(bytes).accept(new RemappingClassAdapter(output, new Remapper() {
            @Override public String map(String name) { return FMLDeobfuscatingRemapper.INSTANCE.map(name); }
            @Override public String mapMethodName(String owner, String name, String desc) {
                String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc);
                return mcp ? mcpNames.getOrDefault(mapped, mapped) : mapped;
            }
            @Override public String mapFieldName(String owner, String name, String desc) {
                String mapped = FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(owner, name, desc);
                return mcp ? mcpNames.getOrDefault(mapped, mapped) : mapped;
            }
        }), ClassReader.EXPAND_FRAMES);
        return output.toByteArray();
    }

    private static void verify(byte[] bytes, String target) {
        StringWriter errors = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(bytes), ProductionTransformSmoke.class.getClassLoader(), false, new PrintWriter(errors));
        if (errors.getBuffer().length() != 0) throw new AssertionError("Invalid production patch " + target + ": " + errors);
    }

    private static Map<String, Map<String, String>> defaults(Map<String, Map<String, String>> source) {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>() {
            @Override public boolean containsKey(Object key) { return true; }
            @Override public Map<String, String> get(Object key) {
                Map<String, String> value = super.get(key);
                return value == null ? Collections.<String, String>emptyMap() : value;
            }
        };
        result.putAll(source);
        return result;
    }

    private static void inherit(String name, Set<String> visited) {
        if (!visited.add(name)) return;
        ClassNode node = headers.get(name);
        if (node == null) return;
        List<String> parents = new ArrayList<>(node.interfaces);
        if (node.superName != null) parents.add(node.superName);
        Map<String, String> ownMethods = new HashMap<>(map(methods, name));
        Map<String, String> ownFields = new HashMap<>(map(fields, name));
        for (String parent : parents) {
            inherit(parent, visited);
            map(methods, name).putAll(map(methods, parent));
            map(fields, name).putAll(map(fields, parent));
        }
        map(methods, name).putAll(ownMethods);
        map(fields, name).putAll(ownFields);
    }
}
