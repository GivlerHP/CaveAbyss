package ru.givler.caveabyss.core;

import net.minecraft.block.Block;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.ChunkCoordIntPair;
import ru.givler.caveabyss.data.MinusOneLayer;

/** Invoked from WorldServer's active-chunk tick loop after vanilla sections. */
public final class DeepRandomTickHooks {
    private DeepRandomTickHooks() { }

    /** Active chunks must be fully loaded before vanilla starts lighting and block updates. */
    public static Chunk getTickChunk(WorldServer world, int x, int z) {
        return requireChunk(world.getChunkProvider(), x, z);
    }

    static Chunk requireChunk(IChunkProvider provider, int x, int z) {
        Chunk chunk;
        if (provider instanceof ChunkProviderServer) {
            ChunkProviderServer server = (ChunkProviderServer)provider;
            chunk = (Chunk)server.loadedChunkHashMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
            if (chunk == null) chunk = server.loadChunk(x, z);
            // Forge's synchronous IO completion may return null; recheck its published map first.
            if (chunk == null)
                chunk = (Chunk)server.loadedChunkHashMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
            if (chunk == null) chunk = server.originalLoadChunk(x, z);
        } else chunk = provider.provideChunk(x, z);
        if (chunk == null)
            throw new IllegalStateException("CaveAbyss: provider failed to load active chunk " + x + ", " + z);
        return chunk;
    }

    public static int tickNegativeSections(WorldServer world, Chunk chunk, int lcg) {
        if (world.provider.dimensionId != 0) return lcg;
        int baseX = chunk.xPosition << 4;
        int baseZ = chunk.zPosition << 4;
        for (int section = 0; section < 4; section++) {
            if (!MinusOneLayer.needsRandomTick(chunk, section)) continue;
            for (int attempt = 0; attempt < 3; attempt++) {
                lcg = lcg * 3 + 1013904223;
                int bits = lcg >> 2;
                int x = bits & 15;
                int z = bits >> 8 & 15;
                int y = -64 + (section << 4) + (bits >> 16 & 15);
                Block block = MinusOneLayer.getBlock(chunk, x, y, z);
                if (block.getTickRandomly())
                    block.updateTick(world, baseX + x, y, baseZ + z, world.rand);
            }
        }
        return lcg;
    }
}
