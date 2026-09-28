package ru.givler.caveabyss.api;

import net.minecraft.block.Block;
import net.minecraft.world.EnumSkyBlock;

/** Uniform 16x16x16 view; X, Y and Z here are all section-local 0..15. */
public interface IBlockSection {
    int getSectionY();
    int getMinY();
    Block getBlock(int x, int y, int z);
    int getMetadata(int x, int y, int z);
    int getSavedLight(EnumSkyBlock type, int x, int y, int z);
    boolean setBlock(int x, int y, int z, Block block, int metadata, int flags);
}
