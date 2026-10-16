package ru.givler.caveabyss.core;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.tree.*;

/** Verifies the installed mesh offset instructions and real polygonizer together. */
public final class NeodymiumVertexSmoke implements Opcodes {
    public static void check(File jarFile) throws Exception {
        final Map<String, byte[]> definitions = new HashMap<>();
        for (Class<?> fixture : NeodymiumVertexSmoke.class.getDeclaredClasses())
            definitions.put(fixture.getName(), read(fixture.getResourceAsStream("/" + internal(fixture) + ".class")));
        try (JarFile jar = new JarFile(jarFile)) {
            String target = "makamys.neodymium.renderer.ChunkMesh";
            byte[] patched = new NeodymiumHeightTransformer().transform(target, target, entry(jar, target));
            ClassNode mesh = node(patched);
            ClassNode offsets = node(definitions.get(OffsetsFixture.class.getName()));
            offsets.methods.removeIf(method -> !method.name.equals("<init>"));
            ClassNode region = node(entry(jar, "makamys.neodymium.renderer.NeoRegion"));
            for (MethodNode method : region.methods) if (method.name.equals("toRelativeOffset")) offsets.methods.add(method);
            int extracted = 0;
            for (MethodNode method : mesh.methods) {
                if (!method.name.equals("addTessellatorData")) continue;
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof FieldInsnNode)) continue;
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    String axis = field.name.equals("field_78408_v") ? "x" : field.name.equals("field_78407_w") ? "y"
                            : field.name.equals("field_78417_x") ? "z" : null;
                    if (axis == null) continue;
                    AbstractInsnNode negation = next(field), conversion = next(negation);
                    if (negation.getOpcode() != DNEG) continue; // Diagnostic logging also reads these fields.
                    MethodNode evaluator = new MethodNode(ACC_PUBLIC | ACC_STATIC, axis,
                            "(L" + internal(TessFixture.class) + ";)F", null, null);
                    evaluator.instructions.add(new VarInsnNode(ALOAD, 0));
                    evaluator.instructions.add(new FieldInsnNode(GETFIELD, internal(TessFixture.class), field.name, "D"));
                    evaluator.instructions.add(new InsnNode(DNEG));
                    if (conversion.getOpcode() == D2F) evaluator.instructions.add(new InsnNode(D2F));
                    else {
                        MethodInsnNode call = (MethodInsnNode) conversion;
                        evaluator.instructions.add(new MethodInsnNode(call.getOpcode(), internal(OffsetsFixture.class), call.name, call.desc, false));
                    }
                    evaluator.instructions.add(new InsnNode(FRETURN));
                    offsets.methods.add(evaluator);
                    extracted++;
                }
            }
            if (extracted != 3) throw new AssertionError("Expected three actual mesh offset expressions, got " + extracted);
            definitions.put(OffsetsFixture.class.getName(), bytes(offsets));

            ClassNode util = node(entry(jar, "makamys.neodymium.renderer.compat.RenderUtilVanilla"));
            util.methods.removeIf(method -> !method.name.equals("<init>") && !method.name.equals("polygonize"));
            util.fields.clear();
            util.interfaces.clear();
            for (MethodNode method : util.methods) if (method.name.equals("<init>")) method.access = ACC_PUBLIC;
            ClassWriter remapped = new ClassWriter(0);
            util.accept(new RemappingClassAdapter(remapped, new Remapper() {
                @Override public String map(String name) {
                    if (name.equals("makamys/neodymium/renderer/compat/RenderUtilVanilla")) return internal(PolygonizerFixture.class);
                    if (name.equals("makamys/neodymium/renderer/ChunkMesh$Flags")) return internal(FlagsFixture.class);
                    if (name.equals("makamys/neodymium/renderer/MeshPolygon")) return internal(DefaultsFixture.class);
                    return name;
                }
            }));
            definitions.put(PolygonizerFixture.class.getName(), remapped.toByteArray());
        }
        ClassLoader loader = new ClassLoader(NeodymiumVertexSmoke.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!definitions.containsKey(name)) return super.loadClass(name, resolve);
                Class<?> result = findLoadedClass(name);
                if (result == null) { byte[] bytes = definitions.get(name); result = defineClass(name, bytes, 0, bytes.length); }
                if (resolve) resolveClass(result);
                return result;
            }
        };
        Class<?> tessType = loader.loadClass(TessFixture.class.getName());
        Class<?> offsetType = loader.loadClass(OffsetsFixture.class.getName());
        Class<?> flagsType = loader.loadClass(FlagsFixture.class.getName());
        Class<?> utilType = loader.loadClass(PolygonizerFixture.class.getName());
        Object tess = tessType.getConstructor().newInstance(), flags = flagsType.getConstructor().newInstance();
        Object util = utilType.getConstructor().newInstance();
        Method polygonize = utilType.getMethod("polygonize", int[].class, int.class, int[].class, int.class,
                float.class, float.class, float.class, flagsType);
        for (int originY : new int[] {-64, -48, -32, -16, 0, 16, 240}) {
            for (int originX : new int[] {-1056, -16, 0, 1024}) {
                int originZ = -originX;
                tessType.getField("field_78408_v").setDouble(tess, -originX);
                tessType.getField("field_78407_w").setDouble(tess, -originY);
                tessType.getField("field_78417_x").setDouble(tess, -originZ);
                float xOffset = (Float) offsetType.getMethod("x", tessType).invoke(null, tess);
                float yOffset = (Float) offsetType.getMethod("y", tessType).invoke(null, tess);
                float zOffset = (Float) offsetType.getMethod("z", tessType).invoke(null, tess);
                for (float localY : new float[] {0, 0.25f, 15.75f}) {
                    int[] vertex = new int[8], polygon = new int[7];
                    vertex[0] = Float.floatToRawIntBits(0.5f);
                    vertex[1] = Float.floatToRawIntBits(localY);
                    vertex[2] = Float.floatToRawIntBits(0.75f);
                    polygonize.invoke(util, vertex, 0, polygon, 0, xOffset, yOffset, zOffset, flags);
                    float actualY = Float.intBitsToFloat(polygon[1]);
                    if (actualY != originY + localY) throw new AssertionError("Vertex Y wrapped: " + actualY + " for " + (originY + localY));
                    float actualX = Float.intBitsToFloat(polygon[0]), actualZ = Float.intBitsToFloat(polygon[2]);
                    if (actualX != Math.floorMod(originX, 1024) + 0.5f || actualZ != Math.floorMod(originZ, 1024) + 0.75f)
                        throw new AssertionError("Horizontal region wrapping changed");
                }
            }
        }
        System.out.println("Neodymium vertices: signed Y=-64..255 and unchanged X/Z region wrapping passed");
    }

    private static String internal(Class<?> type) { return type.getName().replace('.', '/'); }
    private static AbstractInsnNode next(AbstractInsnNode instruction) {
        do { instruction = instruction.getNext(); } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }
    private static byte[] entry(JarFile jar, String name) throws IOException { return read(jar.getInputStream(jar.getJarEntry(name.replace('.', '/') + ".class"))); }
    private static byte[] read(InputStream stream) throws IOException {
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; for (int n; (n = input.read(buffer)) != -1;) output.write(buffer, 0, n); return output.toByteArray();
        }
    }
    private static ClassNode node(byte[] bytes) { ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES); return node; }
    private static byte[] bytes(ClassNode node) { ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer); return writer.toByteArray(); }
    public static class TessFixture { public double field_78408_v, field_78407_w, field_78417_x; }
    public static class OffsetsFixture { }
    public static class PolygonizerFixture { }
    public static class FlagsFixture { public boolean hasColor = true, hasBrightness = true; }
    public static class DefaultsFixture { public static final int DEFAULT_COLOR = -1; public static final int DEFAULT_BRIGHTNESS = 0; }
}
