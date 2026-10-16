package ru.givler.caveabyss.core;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.*;

/** Executes patched, installed NeoChunk/NeoRenderer code with GPU and world boundaries stubbed. */
public final class NeodymiumHeightSmoke {
    public static void check(File jarFile) throws Exception {
        final Map<String, String> types = new HashMap<>();
        types.put("makamys/neodymium/renderer/NeoRenderer", internal(RendererFixture.class));
        types.put("makamys/neodymium/renderer/NeoChunk", internal(ChunkFixture.class));
        types.put("makamys/neodymium/renderer/NeoRegion", internal(RegionFixture.class));
        types.put("makamys/neodymium/renderer/Mesh", internal(MeshFixture.class));
        types.put("makamys/neodymium/renderer/ChunkMesh", internal(ChunkMeshFixture.class));
        types.put("makamys/neodymium/renderer/NeoRenderer$WorldRendererChange", internal(ChangeFixture.class));
        types.put("makamys/neodymium/Neodymium", internal(GlobalsFixture.class));
        types.put("makamys/neodymium/ducks/NeodymiumWorldRenderer", internal(DuckFixture.class));
        types.put("net/minecraft/client/renderer/WorldRenderer", internal(WorldRendererFixture.class));
        types.put("net/minecraft/client/Minecraft", internal(MinecraftFixture.class));
        types.put("net/minecraft/client/multiplayer/WorldClient", internal(WorldFixture.class));
        types.put("net/minecraft/world/chunk/Chunk", internal(WorldChunkFixture.class));
        final Map<String, byte[]> definitions = new HashMap<>();
        for (Class<?> fixture : NeodymiumHeightSmoke.class.getDeclaredClasses())
            definitions.put(fixture.getName(), read(fixture.getResourceAsStream("/" + internal(fixture) + ".class")));
        try (JarFile jar = new JarFile(jarFile)) {
            ClassNode renderer = node(definitions.get(RendererFixture.class.getName()));
            for (String target : Arrays.asList("makamys.neodymium.renderer.NeoChunk", "makamys.neodymium.renderer.NeoRenderer")) {
                byte[] original = read(jar.getInputStream(jar.getJarEntry(target.replace('.', '/') + ".class")));
                byte[] patched = new NeodymiumHeightTransformer().transform(target, target, original);
                ClassWriter remapped = new ClassWriter(0);
                new ClassReader(patched).accept(new RemappingClassAdapter(remapped, new Remapper() {
                    @Override public String map(String name) { return types.getOrDefault(name, name); }
                }), ClassReader.EXPAND_FRAMES);
                ClassNode actual = node(remapped.toByteArray());
                if (target.endsWith("NeoChunk")) {
                    actual.methods.removeIf(method -> !Arrays.asList("<init>", "hasChunkMeshes", "putChunkMeshes").contains(method.name));
                    actual.innerClasses.clear();
                    definitions.put(ChunkFixture.class.getName(), bytes(actual));
                } else {
                    for (MethodNode method : actual.methods)
                        if (Arrays.asList("onWorldRendererChanged", "onWorldRendererPost", "neoChunkChanged").contains(method.name)) {
                            renderer.methods.removeIf(old -> old.name.equals(method.name));
                            renderer.methods.add(method);
                        }
                    definitions.put(RendererFixture.class.getName(), bytes(renderer));
                }
            }
        }
        ClassLoader loader = new ClassLoader(NeodymiumHeightSmoke.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!definitions.containsKey(name)) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    byte[] bytes = definitions.get(name);
                    type = defineClass(name, bytes, 0, bytes.length);
                }
                if (resolve) resolveClass(type);
                return type;
            }
        };
        Class<?> rendererType = loader.loadClass(RendererFixture.class.getName());
        Class<?> chunkType = loader.loadClass(ChunkFixture.class.getName());
        Class<?> regionType = loader.loadClass(RegionFixture.class.getName());
        Class<?> wrType = loader.loadClass(WorldRendererFixture.class.getName());
        Class<?> meshType = loader.loadClass(ChunkMeshFixture.class.getName());
        Class<?> changeType = loader.loadClass(ChangeFixture.class.getName());
        Object renderer = rendererType.getConstructor().newInstance();
        field(loader.loadClass(GlobalsFixture.class.getName()), "renderer").set(null, renderer);
        Object region = regionType.getConstructor().newInstance();
        Object chunk = chunkType.getConstructor(int.class, int.class, regionType).newInstance(0, 0, region);
        field(rendererType, "chunk").set(renderer, chunk);
        boolean[] visible = (boolean[]) field(chunkType, "isSectionVisible").get(chunk);
        Object[] meshes = (Object[]) field(chunkType, "chunkMeshes").get(chunk);
        if (visible.length != 20 || meshes.length != 40) throw new AssertionError("Wrong Neodymium capacity");
        Method post = rendererType.getMethod("onWorldRendererPost", wrType, boolean.class);
        Method change = rendererType.getMethod("onWorldRendererChanged", wrType, changeType);
        Object visibleChange = field(changeType, "VISIBLE").get(null);
        Object invisibleChange = field(changeType, "INVISIBLE").get(null);
        Object deletedChange = field(changeType, "DELETED").get(null);
        List<Object> renderers = new ArrayList<>();
        // Populate all sections and both passes through the real post/update paths.
        for (int section = 0; section < 20; section++) {
            Object wr = wrType.getConstructor().newInstance();
            field(wrType, "field_78920_d").setInt(wr, section * 16 - 64);
            List<Object> fresh = Arrays.asList(meshType.getConstructor().newInstance(), meshType.getConstructor().newInstance());
            field(wrType, "meshes").set(wr, fresh);
            post.invoke(renderer, wr, false);
            renderers.add(wr);
            if (!visible[section] || meshes[section * 2] != fresh.get(0) || meshes[section * 2 + 1] != fresh.get(1))
                throw new AssertionError("Section alias or missing mesh at " + section);
            for (Object mesh : fresh)
                if (!field(mesh.getClass(), "visible").getBoolean(mesh) || field(mesh.getClass(), "uploads").getInt(mesh) == 0)
                    throw new AssertionError("Section was not processed by visibility loop: " + section);
        }
        if (field(regionType, "meshes").getInt(region) != 40) throw new AssertionError("Wrong mesh count");
        // Sorting updates only the supplied pass, preserving its neighbor.
        Object firstPass = meshes[0], replaced = meshes[1];
        Object replacement = meshType.getConstructor().newInstance();
        field(wrType, "meshes").set(renderers.get(0), Arrays.asList(null, replacement));
        post.invoke(renderer, renderers.get(0), true);
        if (meshes[0] != firstPass || meshes[1] != replacement || !field(meshType, "destroyed").getBoolean(replaced))
            throw new AssertionError("Sort update lost a render pass");
        for (int section = 0; section < 20; section++) {
            Object wr = renderers.get(section);
            Object opaque = meshes[section * 2], transparent = meshes[section * 2 + 1];
            change.invoke(renderer, wr, invisibleChange);
            if (visible[section] || field(meshType, "visible").getBoolean(opaque) || field(meshType, "visible").getBoolean(transparent))
                throw new AssertionError("Invisible section retained visible meshes");
            change.invoke(renderer, wr, visibleChange);
            if (!visible[section] || !field(meshType, "visible").getBoolean(opaque) || !field(meshType, "visible").getBoolean(transparent))
                throw new AssertionError("Visible section lost meshes");
            change.invoke(renderer, wr, deletedChange);
            if (meshes[section * 2] != null || meshes[section * 2 + 1] != null
                    || !field(meshType, "destroyed").getBoolean(opaque) || !field(meshType, "destroyed").getBoolean(transparent))
                throw new AssertionError("Deletion failed to clear both passes");
            if (field(regionType, "meshes").getInt(region) != 40 - (section + 1) * 2)
                throw new AssertionError("Deletion corrupted mesh accounting");
        }
        for (int y : new int[] {-999, -65, 256}) {
            Object wr = wrType.getConstructor().newInstance();
            field(wrType, "field_78920_d").setInt(wr, y);
            post.invoke(renderer, wr, false);
            change.invoke(renderer, wr, deletedChange);
        }
        System.out.println("Neodymium runtime: all 20 sections, two passes, sorting, visibility, deletion and bounds passed");
    }

    private static String internal(Class<?> type) { return type.getName().replace('.', '/'); }
    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { Field field = current.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static byte[] read(InputStream stream) throws IOException {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            for (int n; (n = input.read(buffer)) != -1;) output.write(buffer, 0, n);
            return output.toByteArray();
        }
    }
    private static ClassNode node(byte[] bytes) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES); return node;
    }
    private static byte[] bytes(ClassNode node) {
        ClassWriter output = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(output); return output.toByteArray();
    }

    public static class RendererFixture {
        public ChunkFixture chunk;
        private ChunkFixture getNeoChunk(int x, int z) { return chunk; }
        public void neoChunkChanged(ChunkFixture chunk) { }
        public void removeMesh(MeshFixture mesh) { if (mesh != null) mesh.visible = false; }
        protected void setMeshVisible(MeshFixture mesh, boolean visible) { mesh.visible = visible; }
        protected void uploadMeshToGPU(MeshFixture mesh) { mesh.uploads++; }
    }
    public static class ChunkFixture { }
    public static class RegionFixture { public int meshes; }
    public static class MeshFixture { public boolean visible; public int uploads; }
    public static class ChunkMeshFixture extends MeshFixture { public boolean destroyed; public void destroy() { destroyed = true; } }
    public static class GlobalsFixture { public static RendererFixture renderer; }
    public enum ChangeFixture { VISIBLE, INVISIBLE, DELETED }
    public interface DuckFixture { boolean nd$isDrawn(); List<ChunkMeshFixture> nd$getChunkMeshes(); }
    public static class WorldRendererFixture implements DuckFixture {
        public int field_78923_c, field_78920_d, field_78921_e;
        public List<ChunkMeshFixture> meshes = Collections.emptyList();
        public boolean nd$isDrawn() { return true; }
        public List<ChunkMeshFixture> nd$getChunkMeshes() { return meshes; }
    }
    public static class MinecraftFixture {
        public WorldFixture field_71441_e = new WorldFixture();
        public static MinecraftFixture func_71410_x() { return new MinecraftFixture(); }
    }
    public static class WorldFixture { public WorldChunkFixture func_72964_e(int x, int z) { return new WorldChunkFixture(); } }
    public static class WorldChunkFixture { public boolean field_76636_d = true; }
}
