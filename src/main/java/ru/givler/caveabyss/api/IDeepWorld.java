package ru.givler.caveabyss.api;

import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

/** Height-aware view of a World. Coordinates remain real Minecraft coordinates. */
public interface IDeepWorld {
    World unwrap();
    int getMinY();
    int getMaxYExclusive();
    boolean containsY(int y);
    Block getBlock(int x, int y, int z);
    int getMetadata(int x, int y, int z);
    TileEntity getTileEntity(int x, int y, int z);
    boolean setBlock(int x, int y, int z, Block block, int metadata, int flags);
    void scheduleBlockUpdate(int x, int y, int z, Block block, int delay);
    void scheduleBlockUpdateWithPriority(int x, int y, int z, Block block, int delay, int priority);
    boolean isChunkLoaded(int chunkX, int chunkZ);

    /** Like World.getChunkFromChunkCoords, this may load the requested chunk. */
    IDeepChunk getChunk(int chunkX, int chunkZ);
}
