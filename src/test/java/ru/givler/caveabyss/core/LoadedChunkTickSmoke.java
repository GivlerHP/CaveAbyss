package ru.givler.caveabyss.core;

import java.lang.reflect.Field;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import sun.misc.Unsafe;

/** Missing active chunks must load before ticking; loaded chunks are never regenerated. */
public final class LoadedChunkTickSmoke {
    private LoadedChunkTickSmoke() { }

    public static void check() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe)field.get(null);
        NoLoadProvider provider = (NoLoadProvider)unsafe.allocateInstance(NoLoadProvider.class);
        provider.loadedChunkHashMap = new LongHashMap();
        Chunk chunk = (Chunk)unsafe.allocateInstance(Chunk.class);
        provider.result = chunk;
        if (DeepRandomTickHooks.requireChunk(provider, -157, -15) != chunk || provider.loads != 1)
            throw new AssertionError("Missing active chunk must load synchronously");
        if (DeepRandomTickHooks.requireChunk(provider, -157, -15) != chunk || provider.loads != 1)
            throw new AssertionError("Loaded chunks must never be regenerated");
        provider.loadedChunkHashMap.remove(ChunkCoordIntPair.chunkXZ2Int(-157, -15));
        provider.nullCompletion = true;
        if (DeepRandomTickHooks.requireChunk(provider, -157, -15) != chunk || provider.fallbacks != 1)
            throw new AssertionError("Null IO completion must use real loading rather than skip the chunk");
        System.out.println("Active chunk loading, cached identity and null IO recovery passed");
    }

    public static final class NoLoadProvider extends ChunkProviderServer {
        public NoLoadProvider() { super(null, null, null); }
        Chunk result;
        int loads, fallbacks;
        boolean nullCompletion;
        @Override public Chunk loadChunk(int x, int z) {
            loads++;
            if (nullCompletion) return null;
            loadedChunkHashMap.add(ChunkCoordIntPair.chunkXZ2Int(x, z), result);
            return result;
        }
        @Override public Chunk originalLoadChunk(int x, int z) {
            fallbacks++;
            loadedChunkHashMap.add(ChunkCoordIntPair.chunkXZ2Int(x, z), result);
            return result;
        }
    }
}
