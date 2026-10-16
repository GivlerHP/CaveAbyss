package ru.givler.caveabyss.integration;

import java.lang.reflect.Method;
import net.minecraft.world.chunk.Chunk;

/** Optional notification after custom negative data arrives after vanilla's chunk packet. */
public final class DistantHorizonsBridge {
    private static boolean resolved;
    private static Method update;
    public static void changed(Chunk chunk) {
        if (!chunk.worldObj.isRemote) return;
        if (!resolved) {
            resolved = true;
            try {
                update = Class.forName("com.seibel.distanthorizons.forge.ForgeClientProxy")
                        .getMethod("onNegativeChunkUpdated", Chunk.class);
            } catch (ReflectiveOperationException ignored) { }
        }
        if (update != null) try { update.invoke(null, chunk); }
        catch (ReflectiveOperationException error) {
            org.apache.logging.log4j.LogManager.getLogger("CaveAbyss").warn("DH negative chunk update failed", error);
            update = null;
        }
    }
}
