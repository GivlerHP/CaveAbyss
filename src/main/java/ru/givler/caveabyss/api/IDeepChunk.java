package ru.givler.caveabyss.api;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;

/** X/Z arguments are local 0..15; Y arguments are absolute block coordinates. */
public interface IDeepChunk {
    Chunk unwrap();
    int getMinSectionY();
    int getMaxSectionYExclusive();
    IBlockSection getSection(int sectionY);
    Block getBlock(int x, int y, int z);
    int getMetadata(int x, int y, int z);
    TileEntity getTileEntity(int x, int y, int z);

    /** Uses World.setBlock, including its normal callbacks and notification flags. */
    boolean setBlock(int x, int y, int z, Block block, int metadata, int flags);
}
