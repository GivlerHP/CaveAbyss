package ru.givler.caveabyss.core;

import net.minecraft.world.World;
import ru.givler.caveabyss.block.CaveBlocks;

/** Allow BOP cave plants to use CaveAbyss deepslate as natural stone. */
public final class BopPlantSupportHooks {
    private BopPlantSupportHooks() { }

    public static boolean glowshroomPosition(World world, int x, int y, int z, int metadata) {
        return metadata == 3 && world.getBlock(x, y - 1, z) == CaveBlocks.deepslate;
    }

    public static boolean glowshroomStay(World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z) == 3
                && world.getBlock(x, y - 1, z) == CaveBlocks.deepslate;
    }

    public static boolean minersDelightPosition(World world, int x, int y, int z, int metadata) {
        return metadata == 6 && world.getBlock(x, y - 1, z) == CaveBlocks.deepslate;
    }

    public static boolean mossPosition(World world, int x, int y, int z, int side) {
        switch (side) {
            case 1: return world.getBlock(x, y + 1, z) == CaveBlocks.deepslate;
            case 2: return world.getBlock(x, y, z + 1) == CaveBlocks.deepslate;
            case 3: return world.getBlock(x, y, z - 1) == CaveBlocks.deepslate;
            case 4: return world.getBlock(x + 1, y, z) == CaveBlocks.deepslate;
            case 5: return world.getBlock(x - 1, y, z) == CaveBlocks.deepslate;
            default: return false;
        }
    }
}
