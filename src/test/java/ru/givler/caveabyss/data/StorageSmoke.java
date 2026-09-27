package ru.givler.caveabyss.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Exercises the compact block and light codecs without starting Minecraft. */
public final class StorageSmoke {
    private StorageSmoke() { }

    public static void check() throws Exception {
        Class<?> layerType = Class.forName("ru.givler.caveabyss.data.MinusOneLayer$Layer");
        Constructor<?> constructor = layerType.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object layer = constructor.newInstance();
        Method setState = MinusOneLayer.class.getDeclaredMethod("setState", layerType, int.class, int.class);
        Method state = MinusOneLayer.class.getDeclaredMethod("state", layerType, int.class);
        Method legacyLight = MinusOneLayer.class.getDeclaredMethod("readLegacyLight", byte[].class);
        setState.setAccessible(true);
        state.setAccessible(true);
        legacyLight.setAccessible(true);

        int[] ids = {1, 255, 256, 4095};
        for (int i = 0; i < ids.length; i++) {
            int encoded = ids[i] | (i << 16);
            setState.invoke(null, layer, i, encoded);
            int decoded = (Integer) state.invoke(null, layer, i);
            if (decoded != encoded) throw new AssertionError("Block state round trip failed: " + encoded);
        }
        setState.invoke(null, layer, 1, 0);
        if ((Integer) state.invoke(null, layer, 1) != 0) throw new AssertionError("Block removal failed");
        if ((Integer) state.invoke(null, layer, 0) != 1) throw new AssertionError("Adjacent nibble changed");
        if ((Integer) state.invoke(null, layer, 2) != (256 | (2 << 16)))
            throw new AssertionError("High ID or metadata changed");

        byte[] old = new byte[16384];
        for (int i = 0; i < old.length; i++) old[i] = (byte) (i & 15);
        byte[] packed = (byte[]) legacyLight.invoke(null, (Object) old);
        if (packed.length != 8192) throw new AssertionError("Old light size not compressed");
        for (int i = 0; i < old.length; i++) {
            int value = (i & 1) == 0 ? packed[i >> 1] & 15 : (packed[i >> 1] & 255) >>> 4;
            if (value != (old[i] & 15)) throw new AssertionError("Light migration failed at " + i);
        }
        Field blockIds = layerType.getDeclaredField("ids");
        blockIds.setAccessible(true);
        if (((byte[]) blockIds.get(layer)).length != 16384) throw new AssertionError("Unexpected ID storage size");
        System.out.println("Packed negative storage round trip passed");
    }
}
