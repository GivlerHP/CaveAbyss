package ru.givler.caveabyss.core;

import net.minecraft.block.Block;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import ru.givler.caveabyss.data.MinusOneLayer;

/** Invoked from WorldServer's active-chunk tick loop after vanilla sections. */
public final class DeepRandomTickHooks {
    private DeepRandomTickHooks() { }

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
